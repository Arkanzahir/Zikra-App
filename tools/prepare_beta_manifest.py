"""Prepare a HOLD-only artifact record; never publish or approve a release."""
import argparse
import hashlib
import json
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--audit', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    audit = json.loads(args.audit.read_text(encoding='utf-8-sig'))
    size = args.apk.stat().st_size
    digest = hashlib.sha256()
    with args.apk.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    fingerprint = digest.hexdigest()
    if not (0 < size < 100_000_000 and size == audit.get('bytes')
            and fingerprint == audit.get('sha256')):
        raise ValueError('Artifact differs from audit receipt')
    archive, signing = audit['archive'], audit['signing']
    if (archive['privateConfigurationFiles'] != 0 or archive['knownCredentialEntries'] != 0
            or signing['signatureValid'] is not True):
        raise ValueError('Artifact audit prerequisites failed')
    manifest = {
        'schemaVersion': 1, 'releaseStatus': 'HOLD',
        'version': audit['version'], 'versionCode': audit['versionCode'],
        'bytes': size, 'sha256': fingerprint,
        'signerSha256': signing['signerSha256'],
        'debugSigningKey': signing['debugSigningKey'],
        'distributionApproved': False, 'downloadUrl': None,
        'runtimeVerification': 'deferred',
        'remainingGates': ['content-and-audio-rights', 'publisher-and-privacy',
                           'release-signing-and-migration', 'combined-runtime-and-device-tests'],
    }
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(manifest, stream, indent=2)
        stream.write('\n')
    print(json.dumps({'written': True, 'releaseStatus': 'HOLD', 'bytes': size}))


if __name__ == '__main__':
    main()
