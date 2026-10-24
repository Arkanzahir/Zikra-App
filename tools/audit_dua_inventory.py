"""Inventory a saved provider response without treating field presence as text review.

Usage: python tools/audit_dua_inventory.py SNAPSHOT.json OUTPUT.json
No network access or provider credentials are needed.
"""
import collections
import hashlib
import json
from pathlib import Path
import re
import sys


def digest(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def inventory(snapshot, reviews):
    raw = snapshot.read_bytes()
    rows = json.loads(raw)["data"]
    checked = {entry["id"]: entry for entry in reviews["reviewedSamples"]}
    ids = set()
    results = []
    duplicate_arabic = collections.defaultdict(list)
    groups = collections.Counter()
    for row in rows:
        number = row["id"]
        if type(number) is not int or number in ids:
            raise ValueError("Invalid or duplicate provider ID")
        ids.add(number)
        for field in ("nama", "grup", "ar", "idn", "tentang"):
            if not isinstance(row[field], str) or "\0" in row[field]:
                raise ValueError("Invalid source text")
        fingerprint = digest("\0".join(row[key].strip() for key in ("nama", "ar", "idn", "tentang")))
        flags = []
        if not row["ar"].strip() or not row["idn"].strip():
            flags.append("missing_arabic_or_meaning")
        if not row["tentang"].strip():
            flags.append("missing_reference")
        sample = checked.get(number)
        matches = sample is not None and sample["sourceFingerprint"] == fingerprint
        displayable = not flags
        if displayable:
            groups[row["grup"]] += 1
        duplicate_arabic[digest(row["ar"].strip())].append(number)
        results.append({
            "id": number, "title": row["nama"], "group": row["grup"],
            "sourceFingerprint": fingerprint,
            "displayable": displayable, "fieldIssues": flags,
            "referenceUrls": sorted(set(re.findall(r"https?://[^\s\"<>]+", row["tentang"]))),
            "review": "initial_sample_comparison" if matches else "not_individually_reviewed",
            "comparisonUrl": sample["comparisonUrl"] if matches else None,
        })
    displayable = [entry for entry in results if entry["displayable"]]
    return {
        "sourceUrl": "https://equran.id/api/doa",
        "sourceBytes": len(raw), "sourceSha256": hashlib.sha256(raw).hexdigest(),
        "sourceCount": len(rows), "displayableCount": len(displayable),
        "excludedIds": [entry["id"] for entry in results if not entry["displayable"]],
        "initialSampleCount": sum(entry["review"] == "initial_sample_comparison" for entry in displayable),
        "unreviewedDisplayableCount": sum(entry["review"] != "initial_sample_comparison" for entry in displayable),
        "groups": dict(sorted(groups.items())),
        "exactArabicDuplicates": [numbers for numbers in duplicate_arabic.values() if len(numbers) > 1],
        "limits": [
            "Field completeness and duplicate checks are automated, not validation of religious meaning or authenticity.",
            "Initial sample comparison is not a complete harakat, translation or chain review.",
            "Repeated Arabic in different contexts is retained rather than automatically deduplicated.",
        ],
        "entries": results,
    }


def main():
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    root = Path(__file__).resolve().parent.parent
    reviews = json.loads((root / "android/validation/source-review-b-2026-10-24/doa-review.json").read_text(encoding="utf-8"))
    report = inventory(Path(sys.argv[1]), reviews)
    Path(sys.argv[2]).write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: report[key] for key in ("sourceCount", "displayableCount", "excludedIds", "initialSampleCount", "unreviewedDisplayableCount")}))


if __name__ == "__main__":
    main()
