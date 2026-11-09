import { createHash } from 'node:crypto';

export const MAX_ALERT_AGE_MS = 60 * 60 * 1000;
export const FEEDS = ['autogempa.json', 'gempaterkini.json', 'gempadirasakan.json']
  .map(name => `https://data.bmkg.go.id/DataMKG/TEWS/${name}`);

function text(value, limit, optional = false) {
  if (optional && value === undefined) return '';
  if (typeof value !== 'string' || value.length > limit || /[\u0000-\u001f\u007f]/.test(value)) throw new Error('invalid_bmkg_report');
  return value.trim();
}

export function parseQuake(value, fetchedAt = Date.now()) {
  const time = text(value?.DateTime, 40);
  if (!/^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d{1,3})?(?:Z|\+00:00)$/.test(time)) throw new Error('invalid_bmkg_time');
  const at = Date.parse(time);
  if (!Number.isFinite(at) || !new Date(at).toISOString().startsWith(time.slice(0, 19)) || at > fetchedAt + 300000) throw new Error('invalid_bmkg_time');
  const magnitude = text(value.Magnitude, 12), depth = text(value.Kedalaman, 24);
  if (!/^\d(?:\.\d{1,2})?$/.test(magnitude) || Number(magnitude) > 10
      || !/^\d{1,4}(?:[.,]\d{1,3})?\s*km$/.test(depth) || Number(depth.replace('km', '').replace(',', '.').trim()) > 1000) throw new Error('invalid_bmkg_measurement');
  const coordinates = text(value.Coordinates, 60).split(',').map(s => s.trim());
  if (coordinates.length !== 2 || coordinates.some(s => !/^-?\d{1,3}(?:\.\d{1,6})?$/.test(s))) throw new Error('invalid_bmkg_coordinates');
  const [lat, lon] = coordinates.map(Number);
  if (Math.abs(lat) > 90 || Math.abs(lon) > 180) throw new Error('invalid_bmkg_coordinates');
  const area = text(value.Wilayah, 240); if (!area) throw new Error('invalid_bmkg_area');
  const filename = text(value.Shakemap, 90, true);
  const report = { DateTime: time, Magnitude: magnitude, Kedalaman: depth, Wilayah: area,
    Coordinates: coordinates.join(','), Tanggal: text(value.Tanggal, 80, true), Jam: text(value.Jam, 40, true),
    Lintang: text(value.Lintang, 40, true), Bujur: text(value.Bujur, 40, true),
    Potensi: text(value.Potensi, 240, true), Dirasakan: text(value.Dirasakan, 500, true),
    Shakemap: /^[A-Za-z0-9_-]{1,80}(?:\.mmi)?\.jpg$/.test(filename) ? filename : '' };
  // The source UTC instant is shared by all three feeds and survives magnitude/coordinate revisions.
  const id = createHash('sha256').update(`bmkg|${at}`).digest('hex');
  const snapshot = JSON.stringify({ version: 1, fetchedAt, report: JSON.stringify({ Infogempa: { gempa: report } }) });
  if (Buffer.byteLength(snapshot) > 2800) throw new Error('bmkg_snapshot_too_large');
  return { id, at, snapshot };
}

export function parseFeed(body, now = Date.now()) {
  const raw = JSON.parse(body)?.Infogempa?.gempa;
  const items = Array.isArray(raw) ? raw : [raw];
  if (!items.length || items.length > 30) throw new Error('invalid_bmkg_feed');
  // Reject the entire feed rather than treating malformed input as a valid empty baseline.
  return items.map(value => parseQuake(value, now));
}

export async function boundedJson(response, maximum) {
  if (!response.ok || !response.body) { await response.body?.cancel(); throw new Error('upstream_unavailable'); }
  const chunks = []; let length = 0;
  for await (const chunk of response.body) {
    length += chunk.length; if (length > maximum) { throw new Error('upstream_too_large'); }
    chunks.push(chunk);
  }
  return Buffer.concat(chunks).toString('utf8');
}

export async function loadQuakes({ fetchImpl = fetch, now = Date.now() } = {}) {
  const groups = await Promise.all(FEEDS.map(async url => parseFeed(await boundedJson(await fetchImpl(url,
    { redirect: 'error', headers: { Accept: 'application/json', 'User-Agent': 'Zikra-Earthquake/1.0' }, signal: AbortSignal.timeout(8000) }), 65536), now)));
  return [...new Map(groups.flat().map(event => [event.id, event])).values()].sort((a, b) => a.at - b.at);
}
