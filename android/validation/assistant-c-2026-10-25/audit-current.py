"""Known-secret/preservation audit; credentials come from the private environment, never argv."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import zipfile

root = Path(__file__).resolve().parents[3]
git = ["git", "-c", f"safe.directory={root.as_posix()}"]
values = [os.environ.get(name, "") for name in ("FEATHERLESS_API_KEY", "ZIKRA_CLIENT_TOKEN")]
if not all(values):
    raise SystemExit("Private environment missing; no values printed.")
needles = [value.encode(encoding) for value in values for encoding in ("utf-8", "utf-16-le")]
tracked = subprocess.check_output(git + ["ls-files", "-z"], cwd=root).decode().split("\0")
added = subprocess.check_output(git + ["ls-files", "--others", "--exclude-standard", "-z", "--",
    "backend", "android/app", "android/validation/assistant-c-2026-10-25"], cwd=root).decode().split("\0")
files = sorted(set(filter(None, tracked + added)))
matches = 0
for relative in files:
    path = root / relative
    if path.is_file():
        data = path.read_bytes()
        matches += int(any(needle in data for needle in needles))
apk = root / "android/app/build/outputs/apk/debug/app-debug.apk"
apk_matches = private_entries = fixture_entries = 0
with zipfile.ZipFile(apk) as archive:
    for entry in archive.infolist():
        data = archive.read(entry)
        apk_matches += int(any(needle in data for needle in needles))
        private_entries += int(".local/" in entry.filename or Path(entry.filename).name.startswith(".env"))
        fixture_entries += int(b"[DATA UJI C04]" in data or b"PRIVATE-FIXTURE-NAME" in data)
    entries = len(archive.infolist())
baseline = json.loads((root / ".local/backend-c01-baseline.json").read_text(encoding="utf-8-sig"))
preserved = {name: hashlib.sha256((root / name).read_bytes()).hexdigest() == expected
    for name, expected in baseline["sha256"].items()}
preserved["audio_stash"] = subprocess.check_output(git + ["rev-parse", "refs/stash"], cwd=root).decode().strip() == baseline["stash"]
private_tracked = [path for path in tracked if path.startswith(".local/") or (
    path.startswith("backend/.env") and path not in ("backend/.env.example", "backend/.env.production.example"))]
report = {"scope": "Current known provider key/client token, UTF-8 and UTF-16LE, tracked working tree plus current C source and uncompressed APK; not all historical secrets",
    "filesScanned": len(files), "currentSecretMatches": matches, "privateConfigFilesTracked": len(private_tracked),
    "apk": {"version": "1.8.0 beta", "versionCode": 25, "bytes": apk.stat().st_size,
        "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(), "entriesScanned": entries,
        "currentSecretMatches": apk_matches, "privateConfigEntries": private_entries,
        "newTestFixtureEntries": fixture_entries, "under100000000Bytes": apk.stat().st_size < 100000000},
    "protectedUnchanged": preserved}
report["passed"] = (matches == apk_matches == private_entries == fixture_entries == 0
    and not private_tracked and all(preserved.values()) and apk.stat().st_size < 100000000)
Path(__file__).with_name("secret-and-preservation.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
raise SystemExit(0 if report["passed"] else 1)
