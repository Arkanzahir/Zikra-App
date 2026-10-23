"""Loopback design preview. Only the fixed public BMKG report is proxied."""
import argparse
import json
import math
import re
import threading
import time
import urllib.request
from datetime import datetime, timezone
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

SOURCE = "https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json"
LIMIT = 262144
ROOT = Path(__file__).resolve().parent
LOCK = threading.Lock()
CACHE = None
CACHE_AT = 0


def parse_report(payload):
    q = payload["Infogempa"]["gempa"]
    magnitude = str(q.get("Magnitude", "")).strip()
    area = str(q.get("Wilayah", "")).strip()
    if not area or not re.fullmatch(r"\d{1,2}(?:\.\d{1,2})?", magnitude):
        raise ValueError("Laporan tidak lengkap")
    if not 0 <= float(magnitude) <= 10:
        raise ValueError("Magnitudo tidak valid")
    lat = lon = None
    parts = str(q.get("Coordinates", "")).split(",")
    try:
        a, b = map(float, parts)
        if math.isfinite(a) and math.isfinite(b) and abs(a) <= 90 and abs(b) <= 180:
            lat, lon = a, b
    except (ValueError, TypeError):
        pass
    timestamp = str(q.get("DateTime", ""))
    try:
        dt = datetime.fromisoformat(timestamp.replace("Z", "+00:00"))
        if dt.tzinfo is None:
            timestamp = ""
    except ValueError:
        timestamp = ""
    filename = str(q.get("Shakemap", ""))
    shake_url = "https://static.bmkg.go.id/" + filename if re.fullmatch(r"[A-Za-z0-9_-]{1,80}(?:\.mmi)?\.jpg", filename) else None
    def field(name):
        return str(q.get(name, "")).strip()[:1000]
    return dict(lat=lat, lon=lon, magnitude=magnitude, area=area[:1000],
                depth=field("Kedalaman"), time=timestamp, date=field("Tanggal"),
                clock=field("Jam"), felt=field("Dirasakan"), potential=field("Potensi"),
                shakemap=shake_url, source=SOURCE,
                fetchedAt=datetime.now(timezone.utc).isoformat())


def load_report():
    global CACHE, CACHE_AT
    with LOCK:
        if CACHE is not None and time.monotonic() - CACHE_AT < 60:
            return CACHE
        request = urllib.request.Request(SOURCE, headers={"Accept": "application/json",
            "User-Agent": "Zikra-Protector-Preview/1.0 (+https://github.com/Arkanzahir/Zikra-App)"})
        with urllib.request.urlopen(request, timeout=12) as response:
            body = response.read(LIMIT + 1)
        if len(body) > LIMIT:
            raise ValueError("Laporan melebihi batas ukuran")
        CACHE = parse_report(json.loads(body))
        CACHE_AT = time.monotonic()
        return CACHE


class Handler(SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path != "/api/earthquake":
            return super().do_GET()
        try:
            body = json.dumps(load_report(), ensure_ascii=False).encode("utf-8")
            self.send_response(200)
        except Exception:
            body = json.dumps({"error": "Laporan BMKG belum dapat dimuat. Coba lagi nanti."}).encode("utf-8")
            self.send_response(502)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8770)
    args = parser.parse_args()
    print(f"Protector preview: http://127.0.0.1:{args.port}/preview.html", flush=True)
    ThreadingHTTPServer(("127.0.0.1", args.port), partial(Handler, directory=str(ROOT))).serve_forever()
