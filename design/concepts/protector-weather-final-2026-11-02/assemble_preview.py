"""Assemble the inspectable design from editable files and local assets, with no data requests."""
from pathlib import Path
import argparse
import base64
import json

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]


def data_uri(path):
    types = {".svg": "image/svg+xml", ".webp": "image/webp", ".woff2": "font/woff2"}
    return "data:" + types[path.suffix] + ";base64," + base64.b64encode(path.read_bytes()).decode()


def assemble():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mirror", type=Path, help="Optional writable conversation fragment path")
    args = parser.parse_args()
    assets = {category: {theme: {p.stem: data_uri(p) for p in sorted((HERE / f"assets/{category}/{theme}").glob("*.svg"))} for theme in ["light", "dark"]} for category in ["icons", "navigation"]}
    css = (HERE / "preview.css").read_text(encoding="utf8")
    for token, file in [("__CITY_DATA__", "backgrounds/city.webp"), ("__CLOUD_DATA__", "backgrounds/clouds.webp"), ("__FONT_DATA__", "fonts/roboto-latin-preview.woff2"), ("__STORM_DATA__", "backgrounds/storm.webp")]:
        css = css.replace(token, data_uri(HERE / "assets" / file))
    replacements = {
        "__GEOMETRY__": (ROOT / "android/app/src/main/res/raw/protector_indonesia.geojson").read_text(encoding="utf8"),
        "__ASSET_DATA__": json.dumps(assets, separators=(",", ":")),
        "__STYLES__": css,
        "__BEHAVIOR__": (HERE / "preview.js").read_text(encoding="utf8"),
    }
    document = (HERE / "layout.fragment.html").read_text(encoding="utf8")
    for token, value in replacements.items():
        assert token in document, token
        document = document.replace(token, value)
    assert len(document.encode("utf8")) < 1_000_000
    for token in replacements:
        assert token not in document
    (HERE / "preview.fragment.html").write_text(document, encoding="utf8")
    if args.mirror:
        args.mirror.write_text(document, encoding="utf8")
    print(json.dumps({"fragmentBytes": len(document.encode("utf8")), "liveWeatherRequests": False, "phoneAccessed": False, "runtimeChanged": False}))


if __name__ == "__main__":
    assemble()
