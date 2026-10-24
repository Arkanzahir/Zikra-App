const cache = new Map();
export async function loadTafsir(verse, fetchImpl = fetch, signal) {
  const key = `${verse.chapter}:${verse.number}`, existing = cache.get(key);
  if (existing && Date.now() - existing.time < 3600000) return existing.source;
  // Fixed source; no client URL, credentials, TLS override or redirect forwarding.
  const response = await fetchImpl(`https://equran.id/api/v2/tafsir/${verse.chapter}`, {
    signal, redirect: 'error', headers: { Accept: 'application/json' }
  });
  if (!response.ok) { await response.body?.cancel(); throw new Error('source_unavailable'); }
  const chunks = []; let bytes = 0;
  for await (const chunk of response.body) {
    bytes += chunk.length;
    if (bytes > 2 * 1024 * 1024) throw new Error('source_too_large');
    chunks.push(chunk);
  }
  const data = JSON.parse(Buffer.concat(chunks).toString('utf8'));
  const found = data?.data?.tafsir?.find(item => item.ayat === verse.number);
  if (data?.code !== 200 || data.data.nomor !== verse.chapter
      || typeof found?.teks !== 'string' || found.teks.trim().length < 10) throw new Error('source_unavailable');
  const full = found.teks.replace(/<[^>]*>/g, '').trim();
  const source = { chapter: verse.chapter, number: verse.number,
    label: 'Petikan tafsir · sumber Kemenag menurut EQuran.id',
    url: `https://equran.id/tafsir/${verse.chapter}`, text: full.slice(0, 6000), truncated: full.length > 6000 };
  cache.set(key, { time: Date.now(), source });
  while (cache.size > 12) cache.delete(cache.keys().next().value);
  return source;
}
