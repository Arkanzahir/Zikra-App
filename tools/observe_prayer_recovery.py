"""Read-only host observations. Never starts apps, instruments, reboots, or changes power/settings."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.zikra.app"
ROOT = Path(__file__).resolve().parents[1]
PRAYERS = {0: "subuh", 2: "dzuhur", 3: "ashar", 4: "maghrib", 5: "isya"}
MAX_BYTES = 4 * 1024 * 1024


class ObservationUnavailable(Exception):
    pass


def parse_alarms(text: str) -> list[dict]:
    headers = list(re.finditer(r"(?m)^\s*\w+ #\d+: Alarm\{[^\r\n]+", text))
    result = []
    for i, header in enumerate(headers):
        if not re.search(r"(?:\s|=)com\.zikra\.app(?:\s|})", header.group()):
            continue
        block = text[header.end():headers[i + 1].start() if i + 1 < len(headers) else header.end() + 1500]
        # The first tag belongs to this alarm. Never borrow a tag from cancellation history.
        tag = re.search(r"(?m)^\s*tag=([^\r\n]+)", block)
        prayer = re.fullmatch(r"\*walarm\*:com\.zikra\.app\.PRAYER_([02345])\s*", tag[1]) if tag else None
        epoch = re.search(r"\borigWhen (\d+)\b", header.group())
        if prayer and epoch:
            result.append({"index": int(prayer[1]), "epoch": int(epoch[1]),
                           "exact": bool(re.search(r"\bwindow=0\b", block))})
    return sorted(result, key=lambda row: (row["epoch"], row["index"]))


def parse_preferences(raw: bytes) -> dict:
    if len(raw) > 262144 or b"<!DOCTYPE" in raw.upper() or b"<!ENTITY" in raw.upper():
        raise ObservationUnavailable("invalid_preferences")
    try:
        entries = {node.get("name"): node.get("value", node.text or "") for node in ET.fromstring(raw)}
        modes = {}
        delivered = {}
        for index, name in PRAYERS.items():
            enabled = entries.get("reminder_" + name, "true") == "true"
            value = entries.get("prayer_mode_" + name)
            modes[str(index)] = (value if value in {"ADHAN", "NOTIFICATION", "VIBRATE", "OFF"}
                                 else "NOTIFICATION") if enabled else "OFF"
            delivered[str(index)] = int(entries.get("prayer_delivered_" + str(index), "-1"))
        return {"master_enabled": entries.get("reminder_enabled", "false") == "true",
                "modes": modes, "delivered": delivered}
    except (ET.ParseError, ValueError) as error:
        raise ObservationUnavailable("invalid_preferences") from error


def adhan_foreground(text: str) -> bool:
    headers = list(re.finditer(r"(?m)^\s*\*?\s*ServiceRecord\{[^\r\n]+", text))
    for i, header in enumerate(headers):
        if not re.search(r"com\.zikra\.app/(?:com\.zikra\.app)?\.reminders\.AdhanService\b", header.group()):
            continue
        block = text[header.end():headers[i + 1].start() if i + 1 < len(headers) else len(text)]
        if re.search(r"\bisForeground=true\b", block):
            return True
    return False


def adhan_notice(text: str) -> bool:
    # Headers only; never retain notification titles, messages, coordinates, or other apps' notices.
    return any(re.search(r"\bpkg=com\.zikra\.app(?=\s|\))", line)
               and re.search(r"\bid=(?:7400|0x1ce8)\b", line)
               for line in text.splitlines() if line.lstrip().startswith("NotificationRecord("))


def active_ui(text: str) -> bool:
    return any(PACKAGE + "/" in line and re.search(r"(?:mResumedActivity|topResumedActivity)\s*[:=]", line)
               for line in text.splitlines())


def boot_activity_started(text: str, boot_epoch: float) -> bool:
    for line in text.splitlines():
        at = re.match(r"\s*(\d+\.\d+)", line)
        if (at and float(at[1]) >= boot_epoch - 2 and "START u0" in line
                and re.search(r"\bcmp=com\.zikra\.app/", line)):
            return True
    return False


def _alarm_set(rows: list[dict], now_ms: int) -> set[tuple]:
    return {(int(row["index"]), int(row["epoch"]), bool(row["exact"]))
            for row in rows if int(row["epoch"]) > now_ms}


class BootObserver:
    def __init__(self, baseline: dict, grace: float = 90, stable: int = 2):
        if not baseline.get("boot_id") or not baseline.get("alarms"):
            raise ValueError("A normal, nonempty baseline is required")
        if not 30 <= grace <= 180 or stable < 2:
            raise ValueError("Use at least two samples and a 30–180 second observation grace")
        self.baseline, self.grace, self.stable = baseline, grace, stable
        self.unlocked_at = None
        self.matches = 0
        self.last = "waiting_for_reboot"
        self.terminal = None

    def sample(self, at: float, value: dict) -> dict:
        if self.terminal is not None:
            return self.terminal
        if not value.get("connected"):
            self.matches = 0
            return self.result("waiting", "device_disconnected")
        if value.get("boot_id") == self.baseline["boot_id"]:
            self.matches = 0
            return self.result("waiting", "waiting_for_reboot")
        if not value.get("boot_id") or not value.get("boot_complete") or not value.get("unlocked"):
            self.matches = 0
            return self.result("waiting", "waiting_for_unlock")
        if self.unlocked_at is None:
            self.unlocked_at = at
        if value.get("ui_active") or value.get("ui_started_since_boot") or value.get("instrumentation_active"):
            return self.result("inconclusive", "application_or_instrumentation_opened")
        if not value.get("complete"):
            self.matches = 0
            return self.result("waiting", "incomplete_snapshot")
        now = value["device_epoch_ms"]
        expected = _alarm_set(self.baseline["alarms"], now)
        actual = _alarm_set(value["alarms"], now)
        self.matches = self.matches + 1 if expected and expected == actual else 0
        if self.matches >= self.stable:
            return self.result("observed", "future_alarm_set_restored")
        return self.result("waiting", "awaiting_stable_alarm_set")

    def result(self, status: str, reason: str) -> dict:
        self.last = reason
        value = {"status": status, "reason": reason, "stableSamples": self.matches,
                 "physicalGateAutomaticallyClosed": False}
        if status != "waiting":
            self.terminal = value
        return value

    def finish(self, at: float) -> dict:
        if self.terminal is not None:
            return self.terminal
        reason = ("recovery_not_observed_in_window" if self.unlocked_at is not None
                  and at - self.unlocked_at >= self.grace else "observation_window_incomplete")
        return self.result("inconclusive", reason)


class DozeObserver:
    def __init__(self, index: int, epoch: int, require_power_save: bool = True):
        if index not in PRAYERS or not isinstance(epoch, int) or epoch <= 0:
            raise ValueError("Select one existing prayer epoch")
        self.index, self.epoch, self.require_power_save = index, epoch, require_power_save
        self.last = "waiting_for_alarm"
        self.armed = False
        self.terminal = None

    def sample(self, _at: float, value: dict) -> dict:
        if self.terminal is not None:
            return self.terminal
        reason = "waiting_for_alarm"
        if value.get("instrumentation_active") or value.get("ui_active"):
            return self.result("inconclusive", "application_or_instrumentation_opened")
        if value.get("connected") and value.get("complete") and value.get("unlocked"):
            if not value.get("master_enabled") or value.get("modes", {}).get(str(self.index)) != "ADHAN":
                return self.result("inconclusive", "selected_prayer_is_not_enabled_adhan")
            delivered = value.get("delivered", {}).get(str(self.index)) == self.epoch
            age = value["device_epoch_ms"] - self.epoch
            if age < 0 and not delivered and not value.get("adhan_fgs") and not value.get("adhan_notice"):
                self.armed = any(row == {"index": self.index, "epoch": self.epoch, "exact": True}
                                 for row in value.get("alarms", []))
            delivery = self.armed and delivered and 0 <= age <= 120000 and value.get("adhan_fgs") and value.get("adhan_notice")
            if delivery:
                if value.get("idle") is not True or value.get("screen_off") is not True:
                    reason = "delivery_observed_outside_required_idle"
                elif self.require_power_save and value.get("power_save_setting") is not True:
                    reason = "delivery_observed_without_power_save_setting"
                else:
                    return self.result("observed", "alarm_service_and_notice_during_idle")
        return self.result("waiting", reason)

    def result(self, status: str, reason: str) -> dict:
        self.last = reason
        value = {"status": status, "reason": reason, "audibleSoundVerified": False,
                 "fullRecordingVerified": False, "physicalGateAutomaticallyClosed": False}
        if status != "waiting":
            self.terminal = value
        return value

    def finish(self, _at: float) -> dict:
        if self.terminal is not None:
            return self.terminal
        return self.result("inconclusive", "required_delivery_not_observed_in_window")


class ReadOnlyAdb:
    COMMANDS = {
        ("shell", "cat", "/proc/sys/kernel/random/boot_id"),
        ("shell", "getprop", "sys.boot_completed"),
        ("shell", "run-as", PACKAGE, "ls", "-d", "files"),
        ("shell", "date", "+%s"), ("shell", "cat", "/proc/uptime"),
        ("shell", "dumpsys", "activity", "activities"),
        ("shell", "dumpsys", "activity", "instrumentation"),
        ("shell", "logcat", "-d", "-t", "3000", "-v", "epoch", "-s", "ActivityTaskManager:I"),
        ("shell", "dumpsys", "alarm"),
        ("exec-out", "run-as", PACKAGE, "cat", "shared_prefs/zikra_preferences.xml"),
        ("shell", "dumpsys", "deviceidle"), ("shell", "dumpsys", "power"),
        ("shell", "settings", "get", "global", "low_power"),
        ("shell", "dumpsys", "activity", "services", PACKAGE),
        ("shell", "dumpsys", "notification"),
    }
    def __init__(self, executable: str, serial: str, runner=subprocess.run):
        if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._:-]{0,99}", serial):
            raise ValueError("Invalid explicit device serial")
        self.executable, self.serial, self.runner = executable, serial, runner

    def read(self, *args: str) -> bytes:
        if args not in self.COMMANDS:
            raise ObservationUnavailable("command_not_allowed")
        try:
            result = self.runner([self.executable, "-s", self.serial, *args], capture_output=True,
                                 timeout=5, check=False)
            if result.returncode or len(result.stdout) > MAX_BYTES:
                raise ObservationUnavailable("device_read_unavailable")
            return result.stdout
        except (OSError, subprocess.TimeoutExpired) as error:
            raise ObservationUnavailable("device_read_unavailable") from error

    def snapshot(self, mode: str) -> dict:
        value = {"connected": False, "complete": False, "unlocked": False}
        try:
            boot_id = self.read("shell", "cat", "/proc/sys/kernel/random/boot_id").strip()
            if not re.fullmatch(rb"[a-f0-9-]{36}", boot_id):
                raise ObservationUnavailable("invalid_boot_identity")
            value.update(connected=True, boot_id=hashlib.sha256(boot_id).hexdigest(),
                         boot_complete=self.read("shell", "getprop", "sys.boot_completed").strip() == b"1")
            if not value["boot_complete"]:
                return value
            self.read("shell", "run-as", PACKAGE, "ls", "-d", "files")
            value["unlocked"] = True
            now = int(self.read("shell", "date", "+%s").strip()) * 1000
            uptime = float(self.read("shell", "cat", "/proc/uptime").split()[0])
            value.update(device_epoch_ms=now, ui_active=active_ui(self.read("shell", "dumpsys", "activity", "activities").decode(errors="replace")),
                         instrumentation_active=PACKAGE in self.read("shell", "dumpsys", "activity", "instrumentation").decode(errors="replace"))
            if mode == "boot":
                value["ui_started_since_boot"] = boot_activity_started(self.read("shell", "logcat", "-d", "-t", "3000", "-v", "epoch", "-s", "ActivityTaskManager:I").decode(errors="replace"), now / 1000 - uptime)
                value["alarms"] = parse_alarms(self.read("shell", "dumpsys", "alarm").decode(errors="replace"))
            else:
                value["alarms"] = parse_alarms(self.read("shell", "dumpsys", "alarm").decode(errors="replace"))
                value.update(parse_preferences(self.read("exec-out", "run-as", PACKAGE, "cat", "shared_prefs/zikra_preferences.xml")))
                idle = self.read("shell", "dumpsys", "deviceidle").decode(errors="replace")
                power = self.read("shell", "dumpsys", "power").decode(errors="replace")
                state = re.search(r"\bmState=(\w+)", idle)
                awake = re.search(r"\bmWakefulness=(\w+)", power)
                value.update(idle=state[1] == "IDLE" if state else None,
                             screen_off=awake[1] in {"Asleep", "Dozing"} if awake else None,
                             power_save_setting=self.read("shell", "settings", "get", "global", "low_power").strip() == b"1",
                             adhan_fgs=adhan_foreground(self.read("shell", "dumpsys", "activity", "services", PACKAGE).decode(errors="replace")),
                             adhan_notice=adhan_notice(self.read("shell", "dumpsys", "notification").decode(errors="replace")))
            value["complete"] = True
        except (ObservationUnavailable, ValueError, IndexError):
            # No raw device error, preferences or notification contents escape this adapter.
            pass
        return value


def load_json(path: str) -> dict:
    with Path(path).open("rb") as stream:
        data = stream.read(MAX_BYTES + 1)
    if len(data) > MAX_BYTES:
        raise ValueError("Input exceeds size limit")
    return json.loads(data)


def validate_selected_alarm(baseline: dict, index: int, epoch: int) -> None:
    if (not baseline.get("complete") or baseline.get("instrumentation_active")
            or not any(row["index"] == index and row["epoch"] == epoch and row["exact"]
                       for row in baseline.get("alarms", []))
            or epoch <= baseline.get("device_epoch_ms", epoch)):
        raise ValueError("Select a future exact prayer alarm from a complete normal baseline")


def private_write(path: str, value: dict) -> None:
    target = Path(path).resolve()
    if not target.is_relative_to((ROOT / ".local").resolve()):
        raise ValueError("Device observations must remain under the project's .local directory")
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("x", encoding="utf-8") as stream:
        json.dump(value, stream, indent=2)
        stream.write("\n")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=["snapshot", "boot", "doze", "replay"])
    parser.add_argument("--adb", default="E:/AndroidSDK/platform-tools/adb.exe")
    parser.add_argument("--serial")
    parser.add_argument("--baseline")
    parser.add_argument("--output")
    parser.add_argument("--fixture")
    parser.add_argument("--index", type=int, choices=sorted(PRAYERS))
    parser.add_argument("--epoch", type=int)
    parser.add_argument("--timeout", type=float, default=180)
    args = parser.parse_args()
    if not math.isfinite(args.timeout) or not 30 <= args.timeout <= 900:
        parser.error("timeout must be 30–900 seconds")
    if args.mode == "replay":
        if not args.fixture:
            parser.error("replay requires --fixture")
        fixture = load_json(args.fixture)
        if fixture.get("mode") not in {"boot", "doze"}:
            parser.error("fixture mode must be boot or doze")
        observer = (BootObserver(fixture["baseline"]) if fixture["mode"] == "boot"
                    else DozeObserver(fixture["index"], fixture["epoch"]))
        result = None
        at = -1.0
        for event in fixture["samples"]:
            next_at = float(event["at"])
            if not math.isfinite(next_at) or next_at < 0 or next_at <= at:
                raise ValueError("Replay samples must have finite increasing times")
            at = next_at
            result = observer.sample(at, event["value"])
            if result["status"] != "waiting":
                break
        if result is None or result["status"] == "waiting":
            result = observer.finish(at)
        print(json.dumps({"replayOnly": True, "phoneAccessed": False, **result}))
        return
    if not args.serial:
        parser.error("An explicit serial is required; no automatic device selection")
    if args.mode in {"boot", "doze"} and not args.baseline:
        parser.error("boot/doze requires a normal --baseline")
    device = ReadOnlyAdb(args.adb, args.serial)
    if args.mode == "snapshot":
        value = device.snapshot("boot")
        if not value.get("complete") or value.get("instrumentation_active") or not value.get("alarms"):
            raise ObservationUnavailable("A complete normal-app alarm baseline is required")
        if not args.output:
            parser.error("snapshot requires a private --output path")
        private_write(args.output, value)
        print(json.dumps({"baselineSaved": True, "alarmCount": len(value["alarms"]), "deviceChanged": False}))
        return
    baseline = load_json(args.baseline)
    if args.mode == "doze":
        validate_selected_alarm(baseline, args.index, args.epoch)
    observer = BootObserver(baseline) if args.mode == "boot" else DozeObserver(args.index, args.epoch)
    start = time.monotonic()
    try:
        while time.monotonic() - start < args.timeout:
            snapshot = device.snapshot(args.mode)
            result = observer.sample(time.monotonic() - start, snapshot)
            if result["status"] != "waiting":
                break
            time.sleep(2)
        else:
            result = observer.finish(time.monotonic() - start)
    except KeyboardInterrupt:
        result = {"status": "inconclusive", "reason": "operator_cancelled", "physicalGateAutomaticallyClosed": False}
    if args.output:
        private_write(args.output, result)
    print(json.dumps({"readOnlyDeviceObservation": True, **result}))


if __name__ == "__main__":
    main()
