import { QuranContentSync, syncPath, validateQuranGeneration } from './quran-content-sync.mjs';

async function jsonBody(response, limit) {
  if (!response.ok || Number(response.headers.get('content-length')) > limit) {
    await response.body?.cancel();
    const error = Error('quran_source_unavailable');
    error.status = response.status;
    throw error;
  }
  const chunks = []; let bytes = 0;
  for await (const chunk of response.body) {
    bytes += chunk.length;
    if (bytes > limit) throw Error('quran_source_budget');
    chunks.push(chunk);
  }
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

/** Server-only fixed-origin transport. Never accept URLs, credentials or environment from the client. */
export function createQuranSyncTransport(env, { fetchImpl = fetch, clock = Date.now } = {}) {
  if (env.QF_ENV !== 'production' || !/^[A-Za-z0-9._~-]{4,256}$/.test(env.QF_CLIENT_ID ?? '')
      || typeof env.QF_CLIENT_SECRET !== 'string' || env.QF_CLIENT_SECRET.length < 16
      || env.QF_CLIENT_SECRET.length > 2048) throw Error('invalid_quran_config');
  let token, expiry = 0;
  return async (path, signal) => {
    const safe = syncPath(path);
    const deadline = AbortSignal.timeout(45000);
    signal = signal ? AbortSignal.any([signal, deadline]) : deadline;
    signal?.throwIfAborted();
    if (!token || clock() >= expiry) {
      const response = await fetchImpl('https://oauth2.quran.foundation/oauth2/token', {
        method: 'POST', redirect: 'error', signal,
        headers: { Authorization: 'Basic ' + Buffer.from(env.QF_CLIENT_ID + ':' + env.QF_CLIENT_SECRET).toString('base64'),
          'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': 'Zikra-ContentSync/1.0' },
        body: 'grant_type=client_credentials&scope=content'
      });
      const auth = await jsonBody(response, 16384);
      if (typeof auth.access_token !== 'string' || !auth.access_token || auth.access_token.length > 12000
          || !Number.isInteger(auth.expires_in) || auth.expires_in < 60 || auth.expires_in > 86400)
        throw Error('invalid_quran_authorization');
      token = auth.access_token; expiry = clock() + (auth.expires_in - 30) * 1000;
    }
    const response = await fetchImpl('https://apis.quran.foundation/content' + safe, {
      redirect: 'error', signal, headers: { 'x-client-id': env.QF_CLIENT_ID, 'x-auth-token': token,
        Accept: 'application/json', 'User-Agent': 'Zikra-ContentSync/1.0' }
    });
    if (response.status === 401) { token = undefined; expiry = 0; }
    return jsonBody(response, safe.includes('/snapshots/') ? 64 * 1024 * 1024 : 2 * 1024 * 1024);
  };
}

export function createQuranSyncEngine(env, options = {}) {
  return new QuranContentSync({ read: createQuranSyncTransport(env, options),
    clock: options.clock, validateGeneration: validateQuranGeneration });
}
