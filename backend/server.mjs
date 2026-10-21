import http from 'node:http';
import { timingSafeEqual } from 'node:crypto';
import { pathToFileURL } from 'node:url';

const SYSTEM = 'Kamu Pendamping Zikra. Jawab dalam bahasa Indonesia yang singkat dan membantu. '
  + 'Bantu rencana belajar, rutinitas, dan hafalan. Jangan mengklaim telah mengubah tugas atau timer. '
  + 'Jangan mengarang kutipan ayat/hadis, hasil penilaian bacaan, atau waktu salat. '
  + 'Untuk teks Quran dan jadwal, arahkan pengguna ke fitur sumber terverifikasi di aplikasi. '
  + 'Kamu tidak punya akses ke file, lokasi, mikrofon, atau data perangkat pengguna.';
const BODY_LIMIT = 48 * 1024;

function equalToken(actual, expected) {
  const a = Buffer.from(actual || ''), b = Buffer.from(expected);
  return a.length === b.length && timingSafeEqual(a, b);
}

export function validateMessages(value) {
  if (!Array.isArray(value) || value.length < 1 || value.length > 24) return null;
  let chars = 0;
  const messages = [];
  for (const item of value) {
    if (!item || !['user', 'assistant'].includes(item.role)
        || typeof item.content !== 'string' || !item.content.trim()
        || item.content.length > 2000) return null;
    chars += item.content.length;
    if (chars > 16000) return null;
    messages.push({ role: item.role, content: item.content.trim() });
  }
  return messages.at(-1).role === 'user' ? messages : null;
}

async function boundedJson(stream, limit) {
  let count = 0;
  const chunks = [];
  for await (const chunk of stream) {
    count += chunk.length;
    if (count > limit) { const error = new Error('too_large'); error.status = 413; throw error; }
    chunks.push(chunk);
  }
  try { return JSON.parse(Buffer.concat(chunks).toString('utf8')); }
  catch { const error = new Error('invalid_json'); error.status = 400; throw error; }
}

export function createGateway({ apiKey, model, clientToken, fetchImpl = fetch, now = Date.now }) {
  if (!apiKey || !model || !clientToken || clientToken.length < 32)
    throw new Error('Configure FEATHERLESS_API_KEY, FEATHERLESS_MODEL and a 32+ character ZIKRA_CLIENT_TOKEN.');
  const counters = new Map();
  let active = 0;
  const server = http.createServer(async (req, res) => {
    const reply = (status, value) => {
      if (res.destroyed || res.writableEnded) return;
      res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8',
        'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' });
      res.end(JSON.stringify(value));
    };
    // Health reveals no configuration or credentials.
    if (req.method === 'GET' && req.url === '/health') return reply(200, { status: 'ok' });
    if (req.method !== 'POST' || req.url !== '/v1/assistant') return reply(404, { error: 'not_found' });
    if (!equalToken(req.headers.authorization, `Bearer ${clientToken}`))
      return reply(401, { error: 'unauthorized' });
    if (!/^application\/json(?:\s*;|$)/i.test(req.headers['content-type'] || ''))
      return reply(415, { error: 'json_required' });
    if (Number(req.headers['content-length'] || 0) > BODY_LIMIT)
      return reply(413, { error: 'too_large' });
    // Trust the socket, never caller-supplied X-Forwarded-For. Intended for one private client.
    const ip = req.socket.remoteAddress || 'unknown', time = now();
    for (const [key, window] of counters) if (time - window.start > 60000) counters.delete(key);
    let window = counters.get(ip);
    if (!window) { window = { start: time, count: 0 }; counters.set(ip, window); }
    if (++window.count > 10 || active >= 2) {
      res.setHeader('Retry-After', '60'); return reply(429, { error: 'try_later' });
    }
    active++;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 35000);
    const closed = () => { if (!res.writableEnded) controller.abort(); };
    res.on('close', closed);
    try {
      const body = await boundedJson(req, BODY_LIMIT);
      const messages = validateMessages(body.messages);
      if (!messages) return reply(400, { error: 'invalid_messages' });
      const upstream = await fetchImpl('https://api.featherless.ai/v1/chat/completions', {
        method: 'POST', signal: controller.signal, redirect: 'error',
        headers: { Authorization: `Bearer ${apiKey}`, 'Content-Type': 'application/json',
          'X-Title': 'Zikra', 'HTTP-Referer': 'https://github.com/Arkanzahir/Zikra-App' },
        body: JSON.stringify({ model, messages: [{ role: 'system', content: SYSTEM }, ...messages],
          max_tokens: 512, temperature: 0.5, stream: false })
      });
      if (!upstream.ok) {
        await upstream.body?.cancel();
        return reply(upstream.status === 429 ? 429 : 502,
          { error: upstream.status === 429 ? 'provider_busy' : 'provider_unavailable' });
      }
      const data = await boundedJson(upstream.body, 128 * 1024);
      const content = data?.choices?.[0]?.message?.content;
      if (typeof content !== 'string' || !content.trim() || content.length > 8000)
        return reply(502, { error: 'invalid_provider_response' });
      reply(200, { reply: content.trim(), provider: 'featherless' });
    } catch (error) {
      reply(error.status || (controller.signal.aborted ? 504 : 502),
        { error: error.status ? error.message : 'provider_unavailable' });
    } finally {
      clearTimeout(timeout); res.off('close', closed); active--;
    }
  });
  server.requestTimeout = 15000;
  server.headersTimeout = 10000;
  server.maxConnections = 16;
  return server;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const server = createGateway({ apiKey: process.env.FEATHERLESS_API_KEY,
    model: process.env.FEATHERLESS_MODEL, clientToken: process.env.ZIKRA_CLIENT_TOKEN });
  server.listen(Number(process.env.PORT || 8787), process.env.HOST || '127.0.0.1', () => {
    console.log('Zikra assistant gateway ready. No conversation logging.');
  });
}
