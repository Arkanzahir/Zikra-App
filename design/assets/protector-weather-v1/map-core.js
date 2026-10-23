/* Geographic coordinates are latitude, longitude; GeoJSON is longitude, latitude. */
(function (root) {
  "use strict";
  function valid(lat, lon) {
    return Number.isFinite(lat) && Number.isFinite(lon) && Math.abs(lat) <= 90 && Math.abs(lon) <= 180;
  }
  function project(lat, lon, zoom) {
    if (!valid(lat, lon)) throw new RangeError("Koordinat tidak valid");
    const size = 256 * 2 ** zoom, clamped = Math.max(-85.05112878, Math.min(85.05112878, lat));
    const sin = Math.sin(clamped * Math.PI / 180);
    return {x: (lon + 180) / 360 * size, y: (.5 - Math.log((1 + sin) / (1 - sin)) / (4 * Math.PI)) * size};
  }
  function unproject(x, y, zoom) {
    const size = 256 * 2 ** zoom;
    return {lon: ((x / size * 360) % 360 + 360) % 360 - 180,
      lat: Math.atan(Math.sinh(Math.PI * (1 - 2 * Math.max(0, Math.min(size, y)) / size))) * 180 / Math.PI};
  }
  const api = {valid, project, unproject};
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.ZikraGeo = api;
})(typeof window === "undefined" ? globalThis : window);
