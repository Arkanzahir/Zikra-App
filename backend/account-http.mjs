const ROUTES = new Map([
  ['GET /v1/account', 'status'], ['POST /v1/account/register', 'register'],
  ['POST /v1/account/renew', 'renew'], ['POST /v1/account/withdraw', 'withdraw'],
  ['POST /v1/account/guardian/request', 'requestGuardian'],
  ['POST /v1/account/guardian/decision', 'decideGuardian'],
  ['POST /v1/account/guardian/revoke', 'revokeGuardian']
]);
const ERRORS = new Map([
  ['unauthorized', 401], ['invalid_input', 400], ['invalid_json', 400], ['too_large', 413],
  ['invalid_proof', 400], ['policy_changed', 409], ['account_exists', 409],
  ['account_conflict', 409], ['not_registered', 409], ['not_eligible', 403],
  ['guardian_required', 403], ['invalid_request', 400], ['rate_limited', 429],
  ['request_timeout', 408], ['invalid_content_type', 415]
]);
async function json(req) {
  if (!/^application\/json(?:\s*;\s*charset=utf-8)?$/i.test(req.headers['content-type'] ?? ''))
    throw new Error('invalid_content_type');
  const length = req.headers['content-length'];
  if (length !== undefined && (!/^\d+$/.test(length) || Number(length) > 16384))
    throw new Error('too_large');
  const iterator = (req.iterator ? req.iterator({ destroyOnReturn: false }) : req)[Symbol.asyncIterator]();
  const chunks = []; let size = 0, timer;
  const deadline = new Promise((_, reject) => {
    timer = setTimeout(() => reject(new Error('request_timeout')), 8000); timer.unref?.();
  });
  try {
    for (;;) {
      const next = await Promise.race([iterator.next(), deadline]);
      if (next.done) break;
      size += next.value.length;
      if (size > 16384) throw new Error('too_large');
      chunks.push(next.value);
    }
  } finally {
    clearTimeout(timer); Promise.resolve(iterator.return?.()).catch(() => {});
  }
  let value;
  try { value = JSON.parse(Buffer.concat(chunks).toString('utf8')); }
  catch { throw new Error('invalid_json'); }
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('invalid_input');
  return value;
}

// No implicit public activation. beforeAuth must apply host-trusted address/global limits
// before Firebase verification. Caller input headers alone must never identify an IP.
export function createAccountHandler({ service, beforeAuth }) {
  if (typeof beforeAuth !== 'function'
      || [...new Set(ROUTES.values())].some(method => typeof service?.[method] !== 'function'))
    throw new Error('invalid_account_config');
  return async (req, res) => {
    const reply = (status, value) => {
      if (res.destroyed || res.writableEnded) return;
      if (!req.complete) res.setHeader('Connection', 'close');
      res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8',
        'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff',
        ...(status === 429 ? { 'Retry-After': '60' } : {}) });
      res.end(JSON.stringify(value));
    };
    const action = ROUTES.get(`${req.method} ${req.url}`);
    if (!action) return reply(404, { error: 'not_found' });
    try {
      if (await beforeAuth(req) !== true) throw new Error('rate_limited');
      const header = req.headers.authorization;
      if (typeof header !== 'string' || !/^Bearer [A-Za-z0-9._-]{32,8192}$/.test(header))
        throw new Error('unauthorized');
      const token = header.slice(7), input = req.method === 'POST' ? await json(req) : null;
      const empty = ['withdraw', 'requestGuardian'];
      if (empty.includes(action) && Object.keys(input).length) throw new Error('invalid_input');
      if (action === 'revokeGuardian'
          && (Object.keys(input).length !== 1 || !Object.hasOwn(input, 'childAccountId')))
        throw new Error('invalid_input');
      const result = await service[action](token, action === 'revokeGuardian' ? input.childAccountId : input);
      return reply(200, result);
    } catch (error) {
      const code = ERRORS.has(error?.message) ? error.message : 'access_unavailable';
      return reply(ERRORS.get(code) ?? 503, { error: code });
    }
  };
}
