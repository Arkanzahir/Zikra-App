"""Rebuild authoring assets; existing Android application resources are read-only inputs."""
from pathlib import Path
import hashlib
import json
import math
import shutil
import xml.etree.ElementTree as ET
from PIL import Image
from fontTools import subset

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
SOURCE = ROOT / "android/app/src/main"
ASSETS = HERE / "assets"
A = "{http://schemas.android.com/apk/res/android}"
SVG = "http://www.w3.org/2000/svg"
ET.register_namespace("", SVG)
ET.register_namespace("android", A[1:-1])

PALETTES = {
    "light": {"cloud": "#F8FCFA", "back": "#C6DED4", "edge": "#52786B", "sun": "#EAC17B", "ray": "#BC914F", "rain": "#518A9B", "moon": "#D6E6E8", "ink": "#15332F"},
    "dark": {"cloud": "#D6E8E0", "back": "#8AA99B", "edge": "#B0CEC0", "sun": "#EDC98B", "ray": "#DFBA78", "rain": "#95C7D7", "moon": "#D9EAEE", "ink": "#E3F0E8"},
}


def shape(d, fill="none", stroke=None, width=2.2):
    result = {"d": d, "fill": fill}
    if stroke:
        result.update(stroke=stroke, width=width)
    return result


def circle(x, y, r):
    return f"M{x-r},{y}a{r},{r} 0,1 0,{2*r},0a{r},{r} 0,1 0,{-2*r},0"


def sun(p, x=43, y=22, r=12, rays=False):
    paths = [shape(circle(x, y, r), p["sun"])]
    if rays:
        for angle in range(0, 360, 45):
            rad = math.radians(angle)
            x1, y1 = x + math.cos(rad)*(r+5), y + math.sin(rad)*(r+5)
            x2, y2 = x + math.cos(rad)*(r+10), y + math.sin(rad)*(r+10)
            paths.append(shape(f"M{x1:.2f},{y1:.2f}L{x2:.2f},{y2:.2f}", stroke=p["ray"], width=2.5))
    return paths


def moon(p):
    return [shape("M49,7C43,9 40,15 40,21C40,27 43,31 49,34C47,35 45,35 43,35C35,35 29,29 29,21C29,13 35,7 43,7Z", p["moon"], p["edge"])]


def cloud(p, wet=False):
    # These silhouettes extend the existing weather_symbol_fair/rain family.
    if wet:
        return [shape("M16,38C4,38 5,25 15,23C17,10 34,8 40,22C53,19 62,33 51,38Z", p["cloud"], p["edge"])]
    return [
        shape("M16,49C5,49 3,35 14,32C15,21 30,17 38,27C48,23 60,35 54,44C51,49 47,49 43,49Z", p["back"]),
        shape("M14,45C5,44 7,33 16,32C17,22 30,21 35,30C44,27 53,33 50,41C48,45 44,45 40,45Z", p["cloud"], p["edge"]),
    ]


def weather(name, p):
    if name == "clear-day":
        return sun(p, 32, 32, 12, True)
    if name == "clear-night":
        return moon(p) + [shape("M18,16L20,21L25,23L20,25L18,30L16,25L11,23L16,21Z", p["edge"])]
    if name == "partly-cloudy-day":
        return sun(p) + cloud(p)
    if name == "partly-cloudy-night":
        return moon(p) + cloud(p)
    if name == "cloudy":
        return cloud(p)
    if name in ("light-rain", "rain", "thunderstorm"):
        paths = cloud(p, True)
        drops = {"light-rain": "M23,45L20,51M41,45L38,51", "rain": "M18,45L14,54M32,45L28,54M46,45L42,54", "thunderstorm": "M18,45L15,52M47,45L44,52"}
        paths.append(shape(drops[name], stroke=p["rain"], width=2.8))
        if name == "thunderstorm":
            paths.append(shape("M34,37L26,49H32L28,60L40,44H34L38,37Z", p["sun"]))
        return paths
    if name == "fog":
        return cloud(p, True) + [shape("M13,46H48M19,54H42", stroke=p["edge"], width=2.8)]
    if name == "unavailable":
        return [shape("M16,38C4,38 5,25 15,23C17,10 34,8 40,22C53,19 62,33 51,38Z", stroke=p["edge"]), shape("M25,50H39", stroke=p["edge"], width=2.8)]
    raise ValueError(name)


def svg(paths, size, label):
    root = ET.Element(f"{{{SVG}}}svg", {"viewBox": f"0 0 {size} {size}", "width": str(size), "height": str(size), "role": "img", "aria-label": label})
    for p in paths:
        attrs = {"d": p["d"], "fill": p["fill"], "stroke-linecap": "round", "stroke-linejoin": "round"}
        if "stroke" in p:
            attrs.update(stroke=p["stroke"], **{"stroke-width": str(p["width"])})
        ET.SubElement(root, f"{{{SVG}}}path", attrs)
    return ET.tostring(root, encoding="unicode") + "\n"


def vector(paths, size):
    root = ET.Element("vector", {A+"width": f"{size}dp", A+"height": f"{size}dp", A+"viewportWidth": str(size), A+"viewportHeight": str(size)})
    for p in paths:
        attrs = {A+"pathData": p["d"], A+"fillColor": "#00000000" if p["fill"] == "none" else p["fill"], A+"strokeLineCap": "round", A+"strokeLineJoin": "round"}
        if "stroke" in p:
            attrs.update({A+"strokeColor": p["stroke"], A+"strokeWidth": str(p["width"])})
        ET.SubElement(root, "path", attrs)
    ET.indent(root)
    return '<?xml version="1.0" encoding="utf-8"?>\n' + ET.tostring(root, encoding="unicode") + "\n"


def resolve_drawable(name):
    path = SOURCE / "res/drawable" / (name + ".xml")
    if path.exists():
        return path
    for path in (SOURCE / "res/values").glob("*.xml"):
        for item in ET.parse(path).getroot():
            if item.tag == "item" and item.get("type") == "drawable" and item.get("name") == name:
                return resolve_drawable(item.text.strip().removeprefix("@drawable/"))
    raise FileNotFoundError(name)


def export_existing(name, ink):
    original = resolve_drawable(name)
    root = ET.parse(original).getroot()
    assert not root.findall("group"), original
    paths = []
    for node in root.findall("path"):
        fill = node.get(A+"fillColor", "#00000000")
        is_transparent = fill in ("#00000000", "@android:color/transparent")
        p = shape(node.get(A+"pathData"), "none" if is_transparent else ink)
        if node.get(A+"strokeColor"):
            p.update(stroke=ink, width=float(node.get(A+"strokeWidth", "1.5")))
        paths.append(p)
    return paths, root.get(A+"viewportWidth"), original


def build():
    inputs = {}
    names = ["clear-day", "clear-night", "partly-cloudy-day", "partly-cloudy-night", "cloudy", "light-rain", "rain", "thunderstorm", "fog", "unavailable"]
    for folder in ["backgrounds", "fonts", "licenses", "icons/light", "icons/dark", "navigation/light", "navigation/dark", "android/res/drawable", "android/res/drawable-night"]:
        (ASSETS / folder).mkdir(parents=True, exist_ok=True)
    for theme, palette in PALETTES.items():
        for name in names:
            paths = weather(name, palette)
            (ASSETS / f"icons/{theme}/{name}.svg").write_text(svg(paths, 64, name), encoding="utf8")
            folder = "drawable" if theme == "light" else "drawable-night"
            (ASSETS / f"android/res/{folder}/protector_weather_{name.replace('-', '_')}.xml").write_text(vector(paths, 64), encoding="utf8")
        for name, target in [("ic_home", "home"), ("feature_icon_quran", "quran"), ("feature_icon_ai", "ai"), ("feature_icon_study", "study"), ("ic_user", "profile")]:
            paths, size, source = export_existing(name, palette["ink"])
            (ASSETS / f"navigation/{theme}/{target}.svg").write_text(svg(paths, size, target), encoding="utf8")
            inputs[source.relative_to(ROOT).as_posix()] = hashlib.sha256(source.read_bytes()).hexdigest()
    for source_name, target in [("protector_city", "city"), ("protector_clouds", "clouds"), ("protector_storm", "storm")]:
        source = SOURCE / f"res/drawable-nodpi/{source_name}.png"
        with Image.open(source) as image:
            image.thumbnail((900, 900))
            image.save(ASSETS / f"backgrounds/{target}.webp", format="WEBP", quality=84, method=6)
        inputs[source.relative_to(ROOT).as_posix()] = hashlib.sha256(source.read_bytes()).hexdigest()
    font_path = SOURCE / "res/font/protector_roboto.ttf"
    font = subset.load_font(str(font_path), subset.Options())
    options = subset.Options()
    options.flavor = "woff2"
    worker = subset.Subsetter(options=options)
    worker.populate(unicodes=list(range(32, 256)) + [0x2013, 0x2014, 0x2022, 0x2026, 0x2192, 0x2197])
    worker.subset(font)
    font.flavor = "woff2"
    font.save(ASSETS / "fonts/roboto-latin-preview.woff2")
    inputs[font_path.relative_to(ROOT).as_posix()] = hashlib.sha256(font_path.read_bytes()).hexdigest()
    for source_name, target in [("protector-roboto-ofl.txt", "roboto-OFL.txt"), ("protector-weather.txt", "existing-weather-artwork.txt")]:
        shutil.copyfile(SOURCE / f"assets/licenses/{source_name}", ASSETS / f"licenses/{target}")
    shutil.copyfile(SOURCE / "res/raw/protector_map_attribution.txt", ASSETS / "licenses/natural-earth.txt")
    files = [{"path": p.relative_to(HERE).as_posix(), "bytes": p.stat().st_size, "sha256": hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(ASSETS.rglob("*")) if p.is_file()]
    report = {"status": "design-assets-only-not-installed-in-apk", "weatherStates": names, "themeVariants": list(PALETTES), "weatherSvgFiles": 20, "weatherAndroidVectorFiles": 20, "navigationShapePreserved": True, "sourceInputs": inputs, "assetBytes": sum(p["bytes"] for p in files), "files": files}
    (HERE / "asset-manifest.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")
    print(json.dumps({k: report[k] for k in ["status", "weatherSvgFiles", "weatherAndroidVectorFiles", "assetBytes"]}))


if __name__ == "__main__":
    build()
