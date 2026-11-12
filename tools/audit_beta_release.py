"""Inspect a beta APK without printing secrets. A successful audit is not release approval.

Pipe a JSON list of known server-secret values on stdin. Firebase Android client
identifiers are public configuration and must not be included in this list.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import zipfile

LIMIT = 100_000_000
PRIVATE_NAME = re.compile(
    r"(^|/)(?:\.local|\.env(?:\.[^/]*)?|google-services\.json|firebase-server\.json|"
    r"[^/]*(?:service.account|\.jks|\.keystore|\.p12|\.pem))(?:/|$)", re.I)


def sha256(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def inspect_entries(apk, secrets):
    secret_patterns = [value.encode(encoding) for value in secrets
                       for encoding in ('utf-8', 'utf-16le')]
    private_files = 0
    credential_entries = 0
    with zipfile.ZipFile(apk) as archive:
        entries = archive.infolist()
        if (len(entries) > 50_000 or len({item.filename for item in entries}) != len(entries)
                or sum(item.file_size for item in entries) > 300_000_000
                or any(item.file_size > LIMIT for item in entries)):
            raise ValueError('Invalid APK archive budget or duplicate entries')
        for item in entries:
            private_files += bool(PRIVATE_NAME.search(item.filename))
            data = archive.read(item)
            credential_entries += any(pattern in data for pattern in secret_patterns)
        audio = {name: hashlib.sha256(archive.read('assets/adhan/' + name + '.mp3')).hexdigest()
                 for name in ('selected', 'fajr')
                 if 'assets/adhan/' + name + '.mp3' in archive.namelist()}
    return {'privateConfigurationFiles': private_files,
            'knownCredentialEntries': credential_entries,
            'audioSha256': audio}


def inspect_manifest(badging, manifest, version, code):
    package = re.search(r"^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'",
                        badging, re.M)
    def disabled(name):
        # Missing and ambiguous attributes fail closed, rather than implying false.
        return bool(re.search(r'android:' + name + r'[^\n]*=(?:false|\(type 0x12\)0x0)\b', manifest))
    debug = re.search(r'android:debuggable[^\n]*=(?:true|\(type 0x12\)0xffffffff)\b', manifest)
    test_only = re.search(r'android:testOnly[^\n]*=(?:true|\(type 0x12\)0xffffffff)\b', manifest)
    return {
        'expectedIdentity': bool(package and package.groups() == ('com.zikra.app', str(code), version)),
        'notDebuggable': not debug and 'application-debuggable' not in badging,
        'notTestOnly': not test_only and 'testOnly' not in badging,
        'backupDisabled': disabled('allowBackup'),
        'cleartextDisabled': disabled('usesCleartextTraffic'),
    }


def check_signature(java, apksigner, apk, expected):
    result = subprocess.run([str(java), '-jar', str(apksigner), 'verify', '--print-certs', str(apk)],
                            capture_output=True, text=True, encoding='utf-8', timeout=60)
    fingerprints = re.findall(r'^Signer #[0-9]+ certificate SHA-256 digest: ([a-fA-F0-9]{64})$',
                              result.stdout, re.M)
    valid = result.returncode == 0 and len(fingerprints) == 1
    fingerprint = fingerprints[0].lower() if valid else None
    debug_key = 'CN=Android Debug' in result.stdout or fingerprint == \
        'c7888fe2ca40e14698726ed20b2f93947033cef0bdee9cfd127c429d0ea0ca1b'
    return {'signatureValid': valid, 'signerSha256': fingerprint,
            'debugSigningKey': debug_key,
            'expectedReleaseSigner': bool(valid and not debug_key and expected and fingerprint == expected)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--version-code', type=int, required=True)
    parser.add_argument('--aapt2', type=Path, required=True)
    parser.add_argument('--java', type=Path, required=True)
    parser.add_argument('--apksigner', type=Path, required=True)
    parser.add_argument('--expected-signer', default='')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    try:
        if args.expected_signer and not re.fullmatch(r'[a-f0-9]{64}', args.expected_signer):
            raise ValueError('Invalid signer pin')
        if not 0 < args.apk.stat().st_size < LIMIT:
            raise ValueError('APK must be below 100 MB')
        raw = sys.stdin.buffer.read(1_048_577)
        if len(raw) > 1_048_576:
            raise ValueError('Secret input exceeds budget')
        secrets = json.loads(raw)
        if not isinstance(secrets, list) or not secrets or any(
                not isinstance(value, str) or not 16 <= len(value) <= 65_536 for value in secrets):
            raise ValueError('A nonempty known-secret list is required')
        def dump(*arguments):
            return subprocess.check_output([str(args.aapt2), 'dump', *arguments],
                                           text=True, encoding='utf-8', stderr=subprocess.PIPE, timeout=60)
        checks = inspect_manifest(dump('badging', str(args.apk)),
                                  dump('xmltree', str(args.apk), '--file', 'AndroidManifest.xml'),
                                  args.version, args.version_code)
        archive = inspect_entries(args.apk, secrets)
        signature = check_signature(args.java, args.apksigner, args.apk, args.expected_signer)
        passed = all(checks.values()) and archive['privateConfigurationFiles'] == 0 \
            and archive['knownCredentialEntries'] == 0 and signature['expectedReleaseSigner']
        report = {'schemaVersion': 1, 'version': args.version, 'versionCode': args.version_code,
                  'bytes': args.apk.stat().st_size, 'sha256': sha256(args.apk), 'below100MB': True,
                  'manifest': checks, 'archive': archive, 'signing': signature,
                  'artifactChecksPassed': bool(passed), 'distributionApproved': False,
                  'remainingGates': 'Content rights, audio rights, device tests, privacy/support, service terms and source/build provenance require separate evidence.'}
        with args.output.open('x', encoding='utf-8') as stream:
            json.dump(report, stream, indent=2)
            stream.write('\n')
        print(json.dumps(report))
        return 0 if passed else 1
    except (ValueError, OSError, KeyError, zipfile.BadZipFile, subprocess.SubprocessError):
        print('Beta APK audit failed; secret values and tool output are suppressed.', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
