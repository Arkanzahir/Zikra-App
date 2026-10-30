"""Check backup exclusions in source and, optionally, the packaged Android APK."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
DOMAINS = {"root", "file", "database", "sharedpref", "external", "device_root",
           "device_file", "device_database", "device_sharedpref"}


def exclusions(section):
    assert section is not None, "Missing backup transport"
    assert not section.findall("include"), "Private data must not be included"
    entries = section.findall("exclude")
    assert len(entries) == len(DOMAINS), "Incomplete backup exclusions"
    assert {entry.get("domain") for entry in entries} == DOMAINS
    assert all(entry.get("path") == "." for entry in entries)
    return [entry.get("domain") for entry in entries]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--aapt2", type=Path)
    args = parser.parse_args()
    source = args.root / "android/app/src/main"
    app = ET.parse(source / "AndroidManifest.xml").getroot().find("application")
    assert app.get(ANDROID + "allowBackup") == "false"
    assert app.get(ANDROID + "fullBackupContent") == "@xml/backup_rules"
    assert app.get(ANDROID + "dataExtractionRules") == "@xml/data_extraction_rules"
    legacy = ET.parse(source / "res/xml/backup_rules.xml").getroot()
    modern = ET.parse(source / "res/xml/data_extraction_rules.xml").getroot()
    assert legacy.tag == "full-backup-content" and modern.tag == "data-extraction-rules"
    expected = {"backup_rules": exclusions(legacy),
                "data_extraction_rules": exclusions(modern.find("cloud-backup"))
                + exclusions(modern.find("device-transfer"))}
    report = {"allowBackup": False, "legacyExcludedDomains": 9,
              "cloudExcludedDomains": 9, "deviceTransferExcludedDomains": 9,
              "packagedApkChecked": False, "actualOemTransferTested": False}
    if args.apk:
        assert args.aapt2 and args.aapt2.is_file(), "Provide the Android aapt2 executable"

        def dump(*arguments):
            return subprocess.check_output([str(args.aapt2), "dump", *arguments],
                                           text=True, encoding="utf-8", timeout=60)

        resources = dump("resources", str(args.apk))
        manifest = dump("xmltree", str(args.apk), "--file", "AndroidManifest.xml")
        assert re.search(r"android:allowBackup[^\n]*=(?:false|\(type 0x12\)0x0)\b", manifest)
        for name, domains in expected.items():
            resource = re.search(r"resource (0x[0-9a-f]+) xml/" + name + r"\b", resources)
            assert resource, "Backup resource missing from APK"
            attribute = "fullBackupContent" if name == "backup_rules" else "dataExtractionRules"
            assert re.search(r"android:" + attribute + r"[^\n]*=@" + resource.group(1), manifest)
            xml = dump("xmltree", str(args.apk), "--file", "res/xml/" + name + ".xml")
            assert re.findall(r'A: domain="([^"]+)"', xml) == domains
            assert re.findall(r'A: path="([^"]+)"', xml) == ["."] * len(domains)
            assert "E: include" not in xml
        report["packagedApkChecked"] = True
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
