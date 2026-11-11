import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

import observe_prayer_recovery as o


def alarm(epoch=200000, index=2, exact=True):
    return {"index": index, "epoch": epoch, "exact": exact}


def sample(**overrides):
    value = dict(connected=True, complete=True, unlocked=True, boot_complete=True,
                 boot_id="new", device_epoch_ms=100000, alarms=[alarm()],
                 ui_active=False, ui_started_since_boot=False, instrumentation_active=False)
    value.update(overrides)
    return value


class RecoveryTests(unittest.TestCase):
    def baseline(self):
        return sample(boot_id="old")

    def test_delayed_boot_does_not_fail_early_zero_snapshot(self):
        observer = o.BootObserver(self.baseline())
        self.assertEqual("waiting", observer.sample(0, sample(alarms=[]))["status"])
        self.assertEqual("waiting", observer.sample(42, sample())["status"])
        self.assertEqual("observed", observer.sample(44, sample())["status"])
        self.assertFalse(observer.finish(180)["physicalGateAutomaticallyClosed"])

    def test_contamination_is_terminal(self):
        for key in ("ui_active", "ui_started_since_boot", "instrumentation_active"):
            observer = o.BootObserver(self.baseline())
            self.assertEqual("inconclusive", observer.sample(0, sample(**{key: True}))["status"])
            self.assertEqual("inconclusive", observer.sample(2, sample())["status"])

    def test_disconnect_lock_same_boot_and_incomplete_reset_stability(self):
        for change in (dict(connected=False), dict(unlocked=False), dict(complete=False)):
            observer = o.BootObserver(self.baseline())
            observer.sample(0, sample())
            observer.sample(2, sample(**change))
            self.assertEqual("waiting", observer.sample(4, sample())["status"])
        self.assertEqual("waiting_for_reboot", o.BootObserver(self.baseline()).sample(0, self.baseline())["reason"])

    def test_timeout_is_never_proven_application_failure(self):
        observer = o.BootObserver(self.baseline())
        observer.sample(10, sample(alarms=[]))
        self.assertEqual("observation_window_incomplete", observer.finish(20)["reason"])
        observer = o.BootObserver(self.baseline())
        observer.sample(0, sample(alarms=[]))
        self.assertEqual("inconclusive", observer.finish(180)["status"])

    def test_expired_alarms_ignored_but_empty_future_set_not_success(self):
        baseline = self.baseline()
        baseline["alarms"].append(alarm(50000, 0))
        observer = o.BootObserver(baseline)
        observer.sample(0, sample())
        self.assertEqual("observed", observer.sample(2, sample())["status"])
        observer = o.BootObserver(baseline)
        self.assertEqual("waiting", observer.sample(0, sample(device_epoch_ms=300000, alarms=[]))["status"])

    def test_alarm_parser_excludes_other_packages_and_history(self):
        text = """ RTC_WAKEUP #0: Alarm{abc type 0 origWhen 200000 whenElapsed 9 com.zikra.app}
        tag=*walarm*:com.zikra.app.PRAYER_2
        window=0
 RTC_WAKEUP #1: Alarm{def type 0 origWhen 300000 com.zikra.app.test}
        tag=*walarm*:com.zikra.app.PRAYER_4
        window=0
 RTC_WAKEUP #2: Alarm{ghi type 0 origWhen 400000 com.zikra.app}
        tag=*walarm*:com.zikra.app.STUDY
        window=0
 Removed alarms:
        tag=*walarm*:com.zikra.app.PRAYER_5
"""
        self.assertEqual([alarm()], o.parse_alarms(text))
        self.assertEqual([], o.parse_alarms("incomplete dump"))

    def test_preferences_only_exports_selected_fields(self):
        raw = b'<map><boolean name="reminder_enabled" value="true"/><string name="prayer_mode_dzuhur">ADHAN</string><long name="prayer_delivered_2" value="200000"/><string name="token">PRIVATE</string></map>'
        value = o.parse_preferences(raw)
        self.assertTrue(value["master_enabled"])
        self.assertEqual("ADHAN", value["modes"]["2"])
        self.assertNotIn("PRIVATE", json.dumps(value))
        for invalid in (b'<!DOCTYPE map><map/>', b'<map>', b'<map><long name="prayer_delivered_2" value="bad"/></map>'):
            with self.assertRaises(o.ObservationUnavailable):
                o.parse_preferences(invalid)

    def test_foreground_scope_and_notice_identity(self):
        own = '* ServiceRecord{a u0 com.zikra.app/.reminders.AdhanService}\n isForeground=false\n'
        quran = '* ServiceRecord{b u0 com.zikra.app/.QuranService}\n isForeground=true\n'
        self.assertFalse(o.adhan_foreground(own + quran))
        self.assertTrue(o.adhan_foreground(own.replace('false', 'true')))
        self.assertFalse(o.adhan_notice('NotificationRecord(a pkg=com.zikra.app.test id=7400)'))
        self.assertTrue(o.adhan_notice('NotificationRecord(a pkg=com.zikra.app id=7400)'))

    def test_doze_requires_current_delivery_idle_screen_and_power(self):
        value = sample(device_epoch_ms=201000, master_enabled=True, modes={"2": "ADHAN"},
                       delivered={"2": 200000}, adhan_fgs=True, adhan_notice=True,
                       idle=True, screen_off=True, power_save_setting=True)
        def armed():
            observer = o.DozeObserver(2, 200000)
            observer.sample(0, {**value, "device_epoch_ms": 100000, "delivered": {},
                                "adhan_fgs": False, "adhan_notice": False})
            return observer
        result = armed().sample(1, value)
        self.assertEqual("observed", result["status"])
        self.assertFalse(result["audibleSoundVerified"])
        self.assertFalse(result["fullRecordingVerified"])
        for change in (dict(delivered={"2": 199999}), dict(adhan_fgs=False),
                       dict(adhan_notice=False), dict(idle=False), dict(screen_off=False),
                       dict(power_save_setting=False), dict(device_epoch_ms=400000)):
            self.assertNotEqual("observed", armed().sample(1, {**value, **change})["status"])
        self.assertNotEqual("observed", o.DozeObserver(2, 200000).sample(0, value)["status"])

    def test_selected_alarm_must_be_future_exact_baseline(self):
        o.validate_selected_alarm(self.baseline(), 2, 200000)
        for change in (dict(alarms=[]), dict(complete=False), dict(instrumentation_active=True),
                       dict(device_epoch_ms=200000), dict(alarms=[alarm(exact=False)])):
            with self.assertRaises(ValueError):
                o.validate_selected_alarm({**self.baseline(), **change}, 2, 200000)

    def test_idle_disconnect_aborts_after_ten_seconds(self):
        observer = o.DozeObserver(2, 200000)
        self.assertEqual("waiting", observer.sample(0, {"connected": False})["status"])
        self.assertEqual("waiting", observer.sample(9, {"connected": False})["status"])
        self.assertEqual("device_disconnected_during_idle",
                         observer.sample(10, {"connected": False})["reason"])
        self.assertEqual("inconclusive", observer.sample(11, sample())["status"])

    def test_idle_transient_disconnect_resets_after_reconnect(self):
        observer = o.DozeObserver(2, 200000)
        observer.sample(0, {"connected": False})
        observer.sample(9, sample(complete=False))
        self.assertEqual("waiting", observer.sample(15, {"connected": False})["status"])

    def test_adapter_rejects_mutations_before_runner(self):
        calls = []
        adapter = o.ReadOnlyAdb("adb", "fixture-device", runner=lambda *a, **k: calls.append(a))
        for args in (("reboot",), ("shell", "settings", "put", "global", "low_power", "1"),
                     ("shell", "am", "start", "com.zikra.app"), ("install", "test.apk")):
            with self.assertRaises(o.ObservationUnavailable):
                adapter.read(*args)
        self.assertEqual([], calls)
        with self.assertRaises(ValueError):
            o.ReadOnlyAdb("adb", "device;reboot")

    def test_snapshot_fake_runner_exports_no_raw_content(self):
        calls = []
        def runner(command, **kwargs):
            calls.append(tuple(command[3:]))
            args = command[3:]
            output = b"PRIVATE CONTENT TO DISCARD"
            if args == ["shell", "cat", "/proc/sys/kernel/random/boot_id"]:
                output = b"11111111-1111-1111-1111-111111111111"
            elif args == ["shell", "getprop", "sys.boot_completed"]:
                output = b"1"
            elif args == ["shell", "date", "+%s"]:
                output = b"200"
            elif args == ["shell", "cat", "/proc/uptime"]:
                output = b"100.0 90.0"
            elif args == ["shell", "run-as", o.PACKAGE, "ls", "-d", "files"]:
                output = b"files"
            elif args == ["shell", "dumpsys", "activity", "processes"]:
                output = b"ACTIVITY MANAGER RUNNING PROCESSES (dumpsys activity processes)"
            return SimpleNamespace(returncode=0, stdout=output)
        value = o.ReadOnlyAdb("fake-adb", "fixture-device", runner).snapshot("boot")
        self.assertTrue(value["complete"])
        self.assertNotIn("PRIVATE", json.dumps(value))
        self.assertNotIn("11111111-1111", json.dumps(value))
        self.assertTrue(all(command in o.ReadOnlyAdb.COMMANDS for command in calls))

    def test_instrumentation_dump_rejects_unsupported_command(self):
        with self.assertRaises(o.ObservationUnavailable):
            o.instrumentation_active('Unknown command: instrumentation')
        prefix = 'ACTIVITY MANAGER RUNNING PROCESSES\n Active instrumentation:\n'
        record = ' Instrumentation #0: ActiveInstrumentation{a runner} 1 procs}\n mTargetProcesses=[com.zikra.app]\n'
        self.assertTrue(o.instrumentation_active(prefix + record))
        self.assertFalse(o.instrumentation_active(prefix + record.replace('[com.zikra.app]', '[com.zikra.app.test]')))
        self.assertFalse(o.instrumentation_active(prefix))

    def test_invalid_success_exit_storage_or_process_read_stays_incomplete(self):
        for bad_command in (("shell", "run-as", o.PACKAGE, "ls", "-d", "files"),
                            ("shell", "dumpsys", "activity", "processes")):
            def runner(command, **kwargs):
                args = tuple(command[3:])
                values = {
                    ("shell", "cat", "/proc/sys/kernel/random/boot_id"): b"11111111-1111-1111-1111-111111111111",
                    ("shell", "getprop", "sys.boot_completed"): b"1",
                    ("shell", "run-as", o.PACKAGE, "ls", "-d", "files"): b"files",
                    ("shell", "date", "+%s"): b"200",
                    ("shell", "cat", "/proc/uptime"): b"100.0 90.0",
                }
                output = b"Unknown command" if args == bad_command else values.get(args, b"")
                return SimpleNamespace(returncode=0, stdout=output)
            value = o.ReadOnlyAdb("fake-adb", "fixture-device", runner).snapshot("boot")
            self.assertFalse(value["complete"])

    def test_failed_device_read_never_exports_error_or_claims_complete(self):
        runner = lambda *a, **k: SimpleNamespace(returncode=1, stdout=b"PRIVATE ERROR")
        value = o.ReadOnlyAdb("fake-adb", "fixture-device", runner).snapshot("boot")
        self.assertFalse(value["complete"])
        self.assertNotIn("PRIVATE", json.dumps(value))

    def test_private_outputs_never_overwrite_or_escape(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(o, "ROOT", Path(temp)):
            target = Path(temp) / ".local" / "result.json"
            o.private_write(str(target), {"status": "inconclusive"})
            with self.assertRaises(FileExistsError):
                o.private_write(str(target), {})
            with self.assertRaises(ValueError):
                o.private_write(str(Path(temp) / "public.json"), {})

    def test_boot_log_and_foreground_detection(self):
        self.assertTrue(o.active_ui('mResumedActivity: ActivityRecord{ com.zikra.app/.MainActivity }'))
        self.assertFalse(o.active_ui('mResumedActivity: ActivityRecord{ com.zikra.app.test/.MainActivity }'))
        self.assertFalse(o.boot_activity_started('90.0 I START u0 cmp=com.zikra.app/.MainActivity', 100))
        self.assertTrue(o.boot_activity_started('101.0 I START u0 cmp=com.zikra.app/.MainActivity', 100))


if __name__ == "__main__":
    unittest.main()
