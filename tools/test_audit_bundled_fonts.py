import json
from pathlib import Path
import tempfile
import unittest
import zipfile
from audit_bundled_fonts import audit, sha


class BundledFontAuditTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.files = {'res/font/sample.ttf': b'synthetic-font',
                      'assets/licenses/sample.txt': b'Copyright fixture\nSIL OPEN FONT LICENSE Version 1.1'}
        for name, data in self.files.items():
            path = self.root / 'android/app/src/main' / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
        self.manifest = {'fonts': [{'name': 'fixture', 'font': 'res/font/sample.ttf',
            'notice': 'assets/licenses/sample.txt', 'fontSha256': sha(self.files['res/font/sample.ttf']),
            'noticeSha256': sha(self.files['assets/licenses/sample.txt'])}]}

    def check(self, files):
        path = self.root / 'candidate.apk'
        with zipfile.ZipFile(path, 'w') as archive:
            for name, value in files.items():
                archive.writestr(name, value)
        return audit(self.root, path, self.manifest)

    def test_matching_package_does_not_approve_distribution(self):
        result = self.check(self.files)
        self.assertTrue(result['passed'])
        self.assertFalse(result['distributionApproved'])
        self.assertFalse(result['downloadedMushafFontsReviewed'])

    def test_missing_notice_rejected(self):
        self.assertFalse(self.check({'res/font/sample.ttf': b'synthetic-font'})['passed'])

    def test_altered_font_rejected(self):
        self.assertFalse(self.check({**self.files, 'res/font/sample.ttf': b'changed'})['passed'])

    def test_uninventoried_packaged_font_rejected(self):
        self.assertFalse(self.check({**self.files, 'res/font/extra.ttf': b'extra'})['passed'])

    def test_source_drift_rejected_even_when_apk_still_matches_pin(self):
        (self.root/'android/app/src/main/res/font/sample.ttf').write_bytes(b'changed-source')
        self.assertFalse(self.check(self.files)['passed'])


if __name__ == '__main__':
    unittest.main()
