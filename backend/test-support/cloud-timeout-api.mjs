import { timingSafeEqual } from 'node:crypto';
import { runTimeoutFixture } from './cloud-timeout-fixture.mjs';

/** Copied only to an isolated preview; production's deployment allowlist excludes it. */
export default async function timeoutFixtureApi(req, res) {
  const reply = (status, value) => {
    if (res.destroyed || res.writableEnded) return;
    res.writeHead(status, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store',
      'X-Content-Type-Options': 'nosniff' });
    res.end(JSON.stringify(value));
  };
  const expected = process.env.ZIKRA_TIMEOUT_FIXTURE_TOKEN;
  const actual = req.headers.authorization;
  const expiry = Number(process.env.ZIKRA_TIMEOUT_FIXTURE_EXPIRES);
  if (!expected || !/^[a-f0-9]{64}$/.test(expected) || !Number.isSafeInteger(expiry)
      || expiry <= Date.now() || expiry > Date.now() + 3600000)
    return reply(503, { error: 'fixture_unavailable' });
  const a = Buffer.from(actual ?? ''), b = Buffer.from(`Bearer ${expected}`);
  if (a.length !== b.length || !timingSafeEqual(a, b)) return reply(401, { error: 'unauthorized' });
  if (req.method !== 'POST') return reply(405, { error: 'method_not_allowed' });
  const stage = req.headers['x-zikra-timeout-stage'];
  if (!['headers', 'body'].includes(stage) || Number(req.headers['content-length'] || 0) > 2)
    return reply(400, { error: 'invalid_fixture_request' });
  try { return reply(200, await runTimeoutFixture(stage)); }
  catch { return reply(500, { error: 'fixture_failed' }); }
}
