import { RedisAccess, RedisRest, readRedisConfig } from './redis-access.mjs';
import { QuranSyncService } from './quran-sync-service.mjs';

export function createQuranHandler(env = process.env, { redis, service } = {}) {
  const config = readRedisConfig(env);
  const contentRedis = redis ?? new RedisRest({ ...config, responseLimit: 524288 });
  const access = new RedisAccess({ redis: contentRedis, prefix: config.prefix });
  const content = service ?? new QuranSyncService({ redis: contentRedis, prefix: config.prefix, env });
  return async (req, res) => {
    const reply = (status, value) => {
      if (res.destroyed || res.writableEnded) return;
      res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store',
        'X-Content-Type-Options': 'nosniff', ...(status === 429 || status === 503 ? { 'Retry-After': '60' } : {}) });
      res.end(JSON.stringify(value));
    };
    if (req.method !== 'GET') return reply(405, { error: 'method_not_allowed' });
    if (typeof req.url !== 'string' || req.url.length > 2048) return reply(400, { error: 'invalid_quran_page' });
    if (env.ZIKRA_QURAN_SYNC_ENABLED !== 'true') return reply(503, { error: 'quran_sync_disabled' });
    try {
      const id = await access.identify(req.headers.authorization);
      if (!id) return reply(401, { error: 'unauthorized' });
      const url = new URL(req.url, 'https://zikra.invalid');
      const kind = url.searchParams.get('kind') ?? 'reader';
      const allowed = { reader: ['kind', 'chapter', 'page'], mushaf: ['kind', 'page'],
        locate: ['kind', 'chapter', 'verse', 'position', 'preferred', 'juz'] }[kind];
      if (url.pathname !== '/v1/quran' || !allowed || [...url.searchParams.keys()].some(key => !allowed.includes(key)
          || url.searchParams.getAll(key).length !== 1
          || (key !== 'kind' && !/^(0|[1-9][0-9]{0,2})$/.test(url.searchParams.get(key)))))
        return reply(400, { error: 'invalid_quran_page' });
      if (kind === 'mushaf') {
        const page = Number(url.searchParams.get('page'));
        if (page < 1 || page > 604) return reply(400, { error: 'invalid_quran_page' });
        return reply(200, await content.mushaf(page, id));
      }
      if (kind === 'locate') return reply(200, await content.locate(Object.fromEntries(
        allowed.filter(key => key !== 'kind').map(key => [key, Number(url.searchParams.get(key) ?? 0)])), id));
      const chapter = Number(url.searchParams.get('chapter')), page = Number(url.searchParams.get('page'));
      if (chapter < 1 || chapter > 114 || page < 1 || page > 6) return reply(400, { error: 'invalid_quran_page' });
      return reply(200, await content.page(chapter, page, id));
    } catch (error) {
      const code = ['invalid_quran_page', 'quran_rate_limited', 'quran_resource_unavailable',
        'quran_source_access_unavailable', 'quran_sync_busy'].includes(error.message) ? error.message : 'quran_sync_unavailable';
      return reply([400, 429].includes(error.status) ? error.status : 503, { error: code });
    }
  };
}

let handler;
export default async function handlerEntry(req, res) {
  try { handler ??= createQuranHandler(); return await handler(req, res); }
  catch {
    res.writeHead(503, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
    res.end('{"error":"quran_sync_unavailable"}');
  }
}
