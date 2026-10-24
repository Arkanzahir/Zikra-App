import http from 'node:http';
import { timingSafeEqual } from 'node:crypto';
import { pathToFileURL } from 'node:url';
import { ConfigError, readConfig, validateCredentials } from './config.mjs';

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

class InputError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}

async function boundedJson(stream, limit) {
  let count = 0;
  const chunks = [];
  // Keep the request socket alive long enough to return a controlled 413.
  const source = stream.iterator ? stream.iterator({ destroyOnReturn: false }) : stream;
  for await (const chunk of source) {
    count += chunk.length;
    if (count > limit) {
      throw new InputError('too_large', 413);
    }
    chunks.push(chunk);
  }
  try { return JSON.parse(Buffer.concat(chunks).toString('utf8')); }
  catch { throw new InputError('invalid_json', 400); }
}

export function createGateway({ apiKey, model, clientToken, fetchImpl = fetch, now = Date.now,
  providerTimeoutMs = 35000 }) {
  validateCredentials({ apiKey, model, clientToken });
  const counters = new Map();
  let active = 0;
  const server = http.createServer(async (req, res) => {
    const reply = (status, value) => {
      if (res.destroyed || res.writableEnded) return;
      if (!req.complete) res.setHeader('Connection', 'close');
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
    const timeout = setTimeout(() => controller.abort(), providerTimeoutMs);
    const closed = () => { if (!res.writableEnded) controller.abort(); };
    res.on('close', closed);
    try {
      let body;
      try { body = await boundedJson(req, BODY_LIMIT); }
      catch (error) {
        if (error instanceof InputError) return reply(error.status, { error: error.message });
        throw error;
      }
      const messages = validateMessages(body?.messages);
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
      let data;
      try { data = await boundedJson(upstream.body, 128 * 1024); }
      catch (error) {
        if (controller.signal.aborted) throw error;
        return reply(502, { error: 'invalid_provider_response' });
      }
      const content = data?.choices?.[0]?.message?.content;
      if (typeof content !== 'string' || !content.trim() || content.length > 8000)
        return reply(502, { error: 'invalid_provider_response' });
      reply(200, { reply: content.trim(), provider: 'featherless' });
    } catch (error) {
      reply(controller.signal.aborted ? 504 : 502, { error: 'provider_unavailable' });
    } finally {
      clearTimeout(timeout); res.off('close', closed); active--;
    }
  });
  server.requestTimeout = 15000;
  server.headersTimeout = 10000;
  server.maxConnections = 16;
  server.maxHeadersCount = 64;
  return server;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    const config = readConfig();
    if (process.argv.includes('--check-config')) {
      console.log('Configuration valid. Private gateway only; model access and key rotation not verified.');
    } else {
      const server = createGateway(config);
      server.on('error', () => {
        console.error('Gateway could not listen. Check the private bind configuration.');
        process.exitCode = 1;
      });
      let stopping = false;
      const stop = () => {
        if (stopping) return;
        stopping = true;
        const deadline = setTimeout(() => server.closeAllConnections(), 40000);
        deadline.unref();
        server.close(() => { clearTimeout(deadline); });
      };
      process.on('SIGTERM', stop);
      process.on('SIGINT', stop);
      server.listen(config.port, config.host, () => {
        console.log('Zikra private assistant gateway ready. No conversation logging.');
      });
    }
  } catch (error) {
    console.error(error instanceof ConfigError ? error.message : 'Gateway startup failed.');
    process.exitCode = 1;
  }
}
