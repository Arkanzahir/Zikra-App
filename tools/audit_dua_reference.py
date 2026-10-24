"""Find Arabic-text comparison candidates, not religious validation or grading.

Inputs: saved EQuran response, numbered web-reference lines, output JSON.
Only source hashes, IDs and comparison URLs are written to the report.
Diacritics, spacing and hamza marks are ignored; reviewers must check them separately.
"""
import hashlib
import json
from pathlib import Path
import re
import sys
import unicodedata


def normalize(text):
    decomposed = unicodedata.normalize("NFKD", text)
    return "".join(c for c in decomposed if "\u0621" <= c <= "\u064a" and c != "\u0640").replace("ى", "ي")


def audit(snapshot, reference):
    lines = dict(json.loads(reference.read_text(encoding="utf-8")))
    if set(lines) != set(range(3890)):
        raise ValueError("Reference page is incomplete or its layout changed")
    entries = {}
    current = None
    for _, text in sorted(lines.items()):
        title = re.fullmatch(r"Hisn al-Muslim (\d+)", text.strip())
        if title:
            current = int(title[1])
            entries.setdefault(current, [])
        elif text.startswith("Reference  |") or "Report Error |" in text:
            current = None
        elif current is not None and re.search("[\u0621-\u064a]", text) and not re.search("[A-Za-z]", text):
            entries[current].append(text)
    reference_text = {number: normalize(" ".join(parts)) for number, parts in entries.items()}
    output = []
    seen = set()
    for row in json.loads(snapshot.read_text(encoding="utf-8"))["data"]:
        if row["id"] in seen:
            raise ValueError("Duplicate provider ID")
        seen.add(row["id"])
        displayable = all(row[k].strip() for k in ("ar", "idn", "tentang"))
        arabic = normalize(row["ar"])
        candidates = [number for number, text in reference_text.items()
                      if displayable and len(arabic) > 12 and arabic in text]
        output.append({"id": row["id"], "displayable": bool(displayable),
                       "normalizedArabicCandidates": candidates,
                       "comparisonUrls": [f"https://sunnah.com/hisn:{n}" for n in candidates],
                       "status": "candidate_requires_context_review" if candidates else "no_candidate_in_this_reference",
                       "harakatReviewed": False, "meaningReviewed": False, "authenticityCertified": False})
    return {"sourceSha256": hashlib.sha256(snapshot.read_bytes()).hexdigest(),
            "referenceUrl": "https://sunnah.com/hisn",
            "referenceSha256": hashlib.sha256(reference.read_bytes()).hexdigest(),
            "referenceLines": len(lines), "referenceEntryHeadings": len(entries),
            "candidateCount": sum(bool(r["normalizedArabicCandidates"]) for r in output),
            "limits": "Normalized substring matching only. Absence is not invalidity; presence is not validation of context, meaning, diacritics, attribution or grading.",
            "entries": output}


if __name__ == "__main__":
    snapshot, reference, output = map(Path, sys.argv[1:])
    result = audit(snapshot, reference)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Comparison candidates: {result['candidateCount']}; not a validation count.")
