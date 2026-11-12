import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from types import SimpleNamespace
import zipfile

spec = importlib.util.spec_from_file_location('audit_beta_release', Path(__file__).with_name('audit_beta_release.py'))
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


class ReleaseAuditTest(unittest.TestCase):
    def test_valid_signature_requires_pinned_non_debug_key(self):
        pin = 'a' * 64
        with patch.object(audit.subprocess, 'run', return_value=SimpleNamespace(
                returncode=0, stdout='Signer #1 certificate SHA-256 digest: ' + pin)):
            self.assertTrue(audit.check_signature('java', 'signer', 'apk', pin)['expectedReleaseSigner'])
            self.assertFalse(audit.check_signature('java', 'signer', 'apk', '')['expectedReleaseSigner'])
            self.assertFalse(audit.check_signature('java', 'signer', 'apk', 'b' * 64)['expectedReleaseSigner'])

    def test_debug_key_is_rejected_even_if_pinned(self):
        pin = 'a' * 64
        with patch.object(audit.subprocess, 'run', return_value=SimpleNamespace(
                returncode=0, stdout='Signer #1 certificate DN: CN=Android Debug\n'
                'Signer #1 certificate SHA-256 digest: ' + pin)):
            self.assertFalse(audit.check_signature('java', 'signer', 'apk', pin)['expectedReleaseSigner'])

    def test_invalid_signature_never_passes_by_printing_a_digest(self):
        pin = 'a' * 64
        with patch.object(audit.subprocess, 'run', return_value=SimpleNamespace(
                returncode=1, stdout='Signer #1 certificate SHA-256 digest: ' + pin)):
            self.assertFalse(audit.check_signature('java', 'signer', 'apk', pin)['expectedReleaseSigner'])

    def test_debug_apk_is_not_release(self):
        badge = "package: name='com.zikra.app' versionCode='67' versionName='1.9.40'\napplication-debuggable"
        tree = 'android:allowBackup(0x1)=(type 0x12)0x0\nandroid:usesCleartextTraffic(0x2)=(type 0x12)0x0'
        result = audit.inspect_manifest(badge, tree, '1.9.40', 67)
        self.assertFalse(result['notDebuggable'])
        self.assertTrue(result['expectedIdentity'])
        self.assertTrue(result['backupDisabled'])
        self.assertFalse(audit.inspect_manifest(badge, tree, '1.9.41', 68)['expectedIdentity'])

    def test_missing_protection_attributes_fail_closed(self):
        result = audit.inspect_manifest('', '', '1.9.40', 67)
        self.assertFalse(result['backupDisabled'])
        self.assertFalse(result['cleartextDisabled'])
        self.assertFalse(result['expectedIdentity'])

    def test_manifest_true_flags_and_test_only_rejected(self):
        result = audit.inspect_manifest('', 'android:debuggable(0x1)=(type 0x12)0xffffffff\nandroid:testOnly(0x2)=true', 'v', 1)
        self.assertFalse(result['notDebuggable'])
        self.assertFalse(result['notTestOnly'])

    def test_private_filenames_and_utf16_secrets_are_detected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'fake.apk'
            with zipfile.ZipFile(path, 'w') as archive:
                archive.writestr('assets/.env.production', 'private')
                archive.writestr('assets/signing.jks', 'key')
                archive.writestr('classes.dex', 'synthetic-secret-for-test'.encode('utf-16le'))
                archive.writestr('res/firebase_client.xml', 'public-client-id')
            result = audit.inspect_entries(path, ['synthetic-secret-for-test'])
            self.assertEqual(2, result['privateConfigurationFiles'])
            self.assertEqual(1, result['knownCredentialEntries'])

    def test_duplicate_apk_entries_rejected(self):
        import warnings
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'fake.apk'
            with warnings.catch_warnings():
                warnings.simplefilter('ignore', UserWarning)
                with zipfile.ZipFile(path, 'w') as archive:
                    archive.writestr('classes.dex', 'one')
                    archive.writestr('classes.dex', 'two')
            with self.assertRaises(ValueError):
                audit.inspect_entries(path, ['synthetic-secret-for-test'])


if __name__ == '__main__':
    unittest.main()
