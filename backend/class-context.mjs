const keys = (value, allowed) => value && typeof value === 'object' && !Array.isArray(value)
  && Object.keys(value).every(key => allowed.includes(key));
const clock = value => typeof value === 'string' && /^(?:[01][0-9]|2[0-3]):[0-5][0-9]$/.test(value);
function date(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return NaN;
  const epoch = Date.parse(value + 'T00:00:00Z');
  return Number.isFinite(epoch) && new Date(epoch).toISOString().slice(0, 10) === value ? epoch : NaN;
}

export function validateClasses(value) {
  if (!keys(value, ['fromDate', 'throughDate', 'zone', 'items', 'omitted'])) return null;
  const from = date(value.fromDate), through = date(value.throughDate);
  if (!Number.isFinite(from) || through - from !== 6 * 86400000
      || typeof value.zone !== 'string' || value.zone.length > 64
      || !Array.isArray(value.items) || value.items.length > 10
      || !Number.isInteger(value.omitted) || value.omitted < 0 || value.omitted > 64 - value.items.length
      || (value.omitted > 0 && value.items.length !== 10)) return null;
  try { new Intl.DateTimeFormat('id', { timeZone: value.zone }); } catch { return null; }
  const items = [];
  let last;
  for (const row of value.items) {
    const day = date(row?.date);
    if (!keys(row, ['date', 'title', 'start', 'end']) || !Number.isFinite(day) || day < from || day > through
        || typeof row.title !== 'string' || !row.title.trim() || row.title.length > 100
        || /[\u0000-\u001f\u007f-\u009f]/.test(row.title)
        || !clock(row.start) || !clock(row.end) || row.end <= row.start
        || (last && (row.date < last.date || (row.date === last.date && row.start < last.end)))) return null;
    const item = { date: row.date, title: row.title.trim(), start: row.start, end: row.end };
    items.push(item); last = item;
  }
  return { fromDate: value.fromDate, throughDate: value.throughDate, zone: value.zone, items, omitted: value.omitted };
}
