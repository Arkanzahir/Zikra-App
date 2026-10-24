"""Exact current-secret scan. Run via Node --env-file, never pass secrets as arguments."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile

root = Path(__file__).resolve().parents[3]
git = ["git", "-c", f"safe.directory={root.as_posix()}"]
secrets = [os.environ.get(name, "") for name in ("FEATHERLESS_API_KEY", "ZIKRA_CLIENT_TOKEN")]
if not all(secrets):
    raise SystemExit("Required private environment is missing; no values printed.")
needles = [value.encode(encoding) for value in secrets for encoding in ("utf-8", "utf-16-le")]
tracked = subprocess.check_output(git + ["ls-files", "-z"], cwd=root).decode().split("\0")
added = subprocess.check_output(git + ["ls-files", "--others", "--exclude-standard", "-z", "--",
    "backend", "android/validation/backend-c01-2026-10-25"], cwd=root).decode().split("\0")
paths = sorted(set(filter(None, tracked + added)))
matches = 0
for relative in paths:
    path = root / relative
    if path.is_file():
        data = path.read_bytes()
        matches += int(any(needle in data for needle in needles))
apk = root.parent / "Zikra-Content-v1.7.6-beta-debug.apk"
apk_matches = 0
private_apk_entries = 0
with zipfile.ZipFile(apk) as archive:
    for entry in archive.infolist():
        normalized = entry.filename.replace("\\", "/")
        private_apk_entries += int(".local/" in normalized or Path(normalized).name.startswith(".env"))
        data = archive.read(entry)
        if any(needle in data for needle in needles):
            apk_matches += 1
    entry_count = len(archive.infolist())

baseline = json.loads((root / ".local/backend-c01-baseline.json").read_text(encoding="utf-8-sig"))
expected = baseline["sha256"]
protected = {name: hashlib.sha256((root / name).read_bytes()).hexdigest() == digest
    for name, digest in expected.items()}
stash = subprocess.check_output(git + ["rev-parse", "refs/stash"], cwd=root).decode().strip()
protected["audio_stash"] = stash == baseline["stash"]
private_tracked = [name for name in tracked if name.startswith(".local/")
    or (name.startswith("backend/.env") and name != "backend/.env.example")]
report = {
    "scope": "current working tree tracked files plus C01 new files; current key/token exact UTF-8 and UTF-16LE; not all historical secrets",
    "filesScanned": len(paths), "filesMatchingCurrentSecrets": matches,
    "privateConfigFilesTracked": len(private_tracked),
    "apk": {"version": "1.7.6-beta-debug", "bytes": apk.stat().st_size,
        "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
        "uncompressedEntriesScanned": entry_count, "entriesMatchingCurrentSecrets": apk_matches,
        "privateConfigEntries": private_apk_entries, "below100000000Bytes": apk.stat().st_size < 100000000},
    "protectedUnchanged": protected,
    "passed": matches == 0 and apk_matches == 0 and not private_tracked and private_apk_entries == 0
        and all(protected.values()) and apk.stat().st_size < 100000000,
}
destination = Path(__file__).with_name("secret-and-preservation-check.json")
destination.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
raise SystemExit(0 if report["passed"] else 1)
