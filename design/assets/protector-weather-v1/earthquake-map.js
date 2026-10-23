"use strict";
// Requests only tiles intersecting the visible map. The browser manages the HTTP cache.
class EarthquakeMap {
  constructor(element, interactive = false) {
    this.el = element; this.zoom = 5; this.center = {lat: -8, lon: 110.7};
    this.marker = {lat: -9.3, lon: 110.7}; this.example = true; this.tiles = new Map();
    this.el.innerHTML = '<canvas class="map-fallback" aria-hidden="true"></canvas><div class="map-tiles" aria-hidden="true"></div><div class="epicenter-pin" role="img"></div><span class="map-load-note">Memuat peta…</span><a class="map-credit" href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">© OpenStreetMap contributors</a>';
    this.canvas = element.querySelector("canvas"); this.layer = element.querySelector(".map-tiles");
    this.pin = element.querySelector(".epicenter-pin"); this.note = element.querySelector(".map-load-note");
    this.pin.innerHTML = '<svg viewBox="0 0 24 32" aria-hidden="true"><path d="M12 31C9 24 1 18 1 12a11 11 0 0 1 22 0c0 6-8 12-11 19Z" fill="#c99220" stroke="white" stroke-width="2"/><circle cx="12" cy="12" r="4" fill="white"/></svg>';
    this.note.textContent = "Memuat peta…";
    this.observer = new ResizeObserver(() => this.draw()); this.observer.observe(element);
    fetch("map-geography.json").then(r => {if (!r.ok) throw Error(); return r.json();})
      .then(data => {this.geography = data; this.drawFallback();}).catch(() => {});
    if (interactive) {
      element.tabIndex = 0; element.setAttribute("role", "region");
      element.setAttribute("aria-label", "Peta episentrum, geser atau gunakan tombol panah");
      element.addEventListener("pointerdown", e => {
        if (e.target.closest("a,button")) return;
        const start = ZikraGeo.project(this.center.lat, this.center.lon, this.zoom);
        this.drag = {id: e.pointerId, x: e.clientX, y: e.clientY, start};
        element.setPointerCapture(e.pointerId);
      });
      element.addEventListener("pointermove", e => {
        if (!this.drag || e.pointerId !== this.drag.id) return;
        const scale = element.getBoundingClientRect().width / element.clientWidth;
        this.center = ZikraGeo.unproject(this.drag.start.x - (e.clientX - this.drag.x) / scale,
          this.drag.start.y - (e.clientY - this.drag.y) / scale, this.zoom);
        this.draw();
      });
      for (const name of ["pointerup", "pointercancel", "lostpointercapture"]) element.addEventListener(name, () => {this.drag = null;});
      element.addEventListener("keydown", e => {
        const delta = {ArrowLeft: [-60, 0], ArrowRight: [60, 0], ArrowUp: [0, -60], ArrowDown: [0, 60]}[e.key];
        if (!delta) return; e.preventDefault(); const p = ZikraGeo.project(this.center.lat, this.center.lon, this.zoom);
        this.center = ZikraGeo.unproject(p.x + delta[0], p.y + delta[1], this.zoom); this.draw();
      });
    }
  }
  setReport(report) {
    this.example = !report; this.marker = report && ZikraGeo.valid(report.lat, report.lon)
      ? {lat: report.lat, lon: report.lon} : report ? null : {lat: -9.3, lon: 110.7};
    this.home = report ? this.marker || {lat: -2.5, lon: 118} : {lat: -8, lon: 110.7};
    this.center = {...this.home}; this.zoom = report && !this.marker ? 3 : 5; this.draw();
  }
  changeZoom(delta) {this.zoom = Math.max(3, Math.min(9, this.zoom + delta)); this.draw();}
  reset() {this.center = {...(this.home || {lat: -8, lon: 110.7})}; this.zoom = 5; this.draw();}
  drawFallback() {
    if (!this.width || !this.geography) return;
    const ctx = this.canvas.getContext("2d"), dpr = Math.min(devicePixelRatio || 1, 2);
    this.canvas.width = this.width * dpr; this.canvas.height = this.height * dpr; ctx.scale(dpr, dpr);
    ctx.fillStyle = "#a8d5e1"; ctx.fillRect(0, 0, this.width, this.height);
    ctx.fillStyle = "#dfebc8"; ctx.strokeStyle = "#a8ba95"; ctx.lineWidth = .8;
    for (const f of this.geography.features || []) {
      const polygons = f.geometry.type === "Polygon" ? [f.geometry.coordinates] : f.geometry.coordinates;
      for (const polygon of polygons) {
        ctx.beginPath();
        for (const ring of polygon) for (let i = 0; i < ring.length; i++) {
          const p = ZikraGeo.project(ring[i][1], ring[i][0], this.zoom);
          ctx[i ? "lineTo" : "moveTo"](p.x - this.left, p.y - this.top);
        }
        ctx.fill("evenodd"); ctx.stroke();
      }
    }
  }
  updateNote() {
    const entries = [...this.tiles.values()];
    const loaded = entries.filter(i => i.dataset.state === "loaded").length;
    const failed = entries.some(i => i.dataset.state === "error");
    this.note.textContent = failed ? "Sebagian peta belum termuat · geografi dasar tersedia" : loaded === entries.length ? "" : "Memuat peta…";
    this.note.hidden = !this.note.textContent;
  }
  draw() {
    if (!this.el.getClientRects().length) return;
    this.width = this.el.clientWidth; this.height = this.el.clientHeight;
    if (!this.width || !this.height) return;
    const p = ZikraGeo.project(this.center.lat, this.center.lon, this.zoom);
    this.left = p.x - this.width / 2; this.top = p.y - this.height / 2;
    this.drawFallback();
    const active = new Set(), count = 2 ** this.zoom;
    for (let x = Math.floor(this.left / 256); x <= Math.floor((this.left + this.width - 1) / 256); x++) {
      for (let y = Math.floor(this.top / 256); y <= Math.floor((this.top + this.height - 1) / 256); y++) {
        if (y < 0 || y >= count) continue;
        const tx = (x % count + count) % count, key = `${this.zoom}/${tx}/${y}`; active.add(key);
        let tile = this.tiles.get(key);
        if (!tile) {
          tile = new Image(); tile.alt = ""; tile.draggable = false; tile.dataset.state = "loading";
          tile.onload = () => {tile.dataset.state = "loaded"; this.updateNote();};
          tile.onerror = () => {tile.dataset.state = "error"; this.updateNote();};
          tile.src = `https://tile.openstreetmap.org/${key}.png`;
          this.tiles.set(key, tile); this.layer.append(tile);
        }
        tile.style.left = `${x * 256 - this.left}px`; tile.style.top = `${y * 256 - this.top}px`;
      }
    }
    for (const [key, tile] of this.tiles) if (!active.has(key)) {tile.remove(); this.tiles.delete(key);}
    this.updateNote();
    this.pin.hidden = !this.marker;
    if (this.marker) {
      const point = ZikraGeo.project(this.marker.lat, this.marker.lon, this.zoom);
      this.pin.style.left = `${point.x - this.left}px`; this.pin.style.top = `${point.y - this.top}px`;
      this.pin.setAttribute("aria-label", this.example ? "Episentrum contoh, bukan kejadian aktual" : `Episentrum BMKG ${this.marker.lat}, ${this.marker.lon}`);
      this.pin.dataset.example = String(this.example);
    }
  }
}
