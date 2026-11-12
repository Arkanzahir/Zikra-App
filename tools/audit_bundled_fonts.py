"""Verify pinned bundled fonts and their notices in source and a built APK.

This is a packaging check, not clearance for downloaded fonts or other content.
"""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def sha(data):
    return hashlib.sha256(data).hexdigest()


def audit(root, apk, manifest):
    rows = []
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        expected = {row['font'] for row in manifest['fonts']}
        actual = {p.relative_to(root / 'android/app/src/main').as_posix()
                  for p in (root / 'android/app/src/main/res/font').glob('*') if p.is_file()}
        packaged = {name for name in names if name.startswith('res/font/')}
        coverage = actual == expected == packaged
        for row in manifest['fonts']:
            checks = []
            for key in ('font', 'notice'):
                path = row[key]
                source = (root / 'android/app/src/main' / path).read_bytes()
                matches = (names.count(path) == 1 and sha(source) == row[key + 'Sha256']
                           and sha(archive.read(path)) == row[key + 'Sha256'])
                if key == 'notice':
                    matches = matches and b'Copyright' in source and b'SIL OPEN FONT LICENSE Version 1.1' in source
                checks.append(matches)
            rows.append({'name': row['name'], 'fontAndNoticeMatch': all(checks)})
    return {'scope': 'three-bundled-fonts-only', 'apkSha256': sha(apk.read_bytes()),
            'completeBundledFontInventory': coverage, 'fonts': rows,
            'passed': coverage and all(row['fontAndNoticeMatch'] for row in rows),
            'downloadedMushafFontsReviewed': False, 'distributionApproved': False}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    manifest = json.loads((ROOT / 'docs/beta/bundled-fonts.json').read_text())
    result = audit(ROOT, args.apk, manifest)
    with args.output.open('x', encoding='utf-8') as output:
        json.dump(result, output, indent=2)
        output.write('\n')
    print(json.dumps(result))
    raise SystemExit(0 if result['passed'] else 1)


if __name__ == '__main__':
    main()
