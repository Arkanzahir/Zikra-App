import json
from pathlib import Path
import tempfile
import unittest

from audit_dua_reference import audit


class ReferenceCandidatesTest(unittest.TestCase):
    def run_audit(self, lines=None, rows=None):
        if lines is None:
            lines = {i: "" for i in range(3890)}
            lines.update({300: "Hisn al-Muslim 1", 301: "رَبِّ اغْفِرْ لِي وَارْحَمْنِي",
                          302: "Reference  |  : source", 303: "موضوع ليس من نص الدعاء"})
        if rows is None:
            rows = [{"id": 1, "ar": "رب اغفر لي وارحمني", "idn": "Arti", "tentang": "Sumber"}]
        with tempfile.TemporaryDirectory() as directory:
            snapshot, reference = Path(directory) / "data.json", Path(directory) / "ref.json"
            snapshot.write_text(json.dumps({"data": rows}), encoding="utf-8")
            reference.write_text(json.dumps(list(lines.items())), encoding="utf-8")
            return audit(snapshot, reference)

    def test_matching_is_only_a_candidate(self):
        report = self.run_audit()
        self.assertEqual(1, report["candidateCount"])
        self.assertEqual([1], report["entries"][0]["normalizedArabicCandidates"])
        self.assertFalse(report["entries"][0]["authenticityCertified"])
        self.assertFalse(report["entries"][0]["meaningReviewed"])

    def test_does_not_match_a_chapter_after_reference(self):
        report = self.run_audit(rows=[{"id": 2, "ar": "موضوع ليس من نص الدعاء", "idn": "Arti", "tentang": "Sumber"}])
        self.assertEqual(0, report["candidateCount"])

    def test_missing_meaning_cannot_be_a_candidate(self):
        report = self.run_audit(rows=[{"id": 3, "ar": "رب اغفر لي وارحمني", "idn": "", "tentang": "Sumber"}])
        self.assertEqual(0, report["candidateCount"])

    def test_incomplete_reference_is_rejected(self):
        with self.assertRaises(ValueError):
            self.run_audit(lines={0: "Hisn al-Muslim 1"})


if __name__ == "__main__":
    unittest.main()
