import copy
import unittest
from serve_preview import parse_report


class ReportTests(unittest.TestCase):
    def setUp(self):
        self.payload = {"Infogempa": {"gempa": {
            "Coordinates": "-3.11,102.14", "Magnitude": "5.2", "Kedalaman": "120 km",
            "Wilayah": "Contoh uji", "DateTime": "2026-10-22T10:57:49+00:00",
            "Shakemap": "20261022175749.mmi.jpg"}}}

    def test_source_fields_and_coordinate_order(self):
        q = parse_report(self.payload)
        self.assertEqual((q["lat"], q["lon"]), (-3.11, 102.14))
        self.assertEqual(q["time"], "2026-10-22T10:57:49+00:00")
        self.assertEqual(q["shakemap"], "https://static.bmkg.go.id/20261022175749.mmi.jpg")

    def test_invalid_coordinates_never_become_a_marker(self):
        for coordinates in ["102.14,-3.11", "nan,100", "-3,inf", "", "-3", "-3,100,5"]:
            with self.subTest(coordinates=coordinates):
                p = copy.deepcopy(self.payload)
                p["Infogempa"]["gempa"]["Coordinates"] = coordinates
                q = parse_report(p)
                self.assertEqual((q["lat"], q["lon"]), (None, None))

    def test_malformed_magnitude_is_rejected(self):
        for magnitude in ["", "NaN", "100", "<b>5</b>"]:
            p = copy.deepcopy(self.payload)
            p["Infogempa"]["gempa"]["Magnitude"] = magnitude
            with self.assertRaises(ValueError):
                parse_report(p)

    def test_shakemap_cannot_choose_another_host_or_file(self):
        for name in ["https://example.org/a.jpg", "../a.jpg", "a/b.jpg", "a.jpg?key=1", ""]:
            p = copy.deepcopy(self.payload)
            p["Infogempa"]["gempa"]["Shakemap"] = name
            self.assertIsNone(parse_report(p)["shakemap"])

    def test_naive_and_invalid_timestamp_not_labelled_utc(self):
        for date in ["2026-10-22T10:57:49", "yesterday", ""]:
            p = copy.deepcopy(self.payload)
            p["Infogempa"]["gempa"]["DateTime"] = date
            self.assertEqual(parse_report(p)["time"], "")


if __name__ == "__main__":
    unittest.main()
