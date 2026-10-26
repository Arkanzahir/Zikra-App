import { randomBytes } from 'node:crypto';
import { writeFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

function deploymentOrigin(value) {
  const url = new URL(value);
  if (url.protocol !== 'https:' || !/^[a-z0-9-]+\.vercel\.app$/.test(url.hostname)
      || url.username || url.password || url.port || url.pathname !== '/' || url.search || url.hash)
    throw new Error('invalid_monitor_origin');
  return url.origin;
}

async function probe(origin, path, headers, fetchImpl, timeoutMs) {
  const start = performance.now();
  try {
    const response = await fetchImpl(origin + path, { headers, redirect: 'error', signal: AbortSignal.timeout(timeoutMs) });
    const chunks = []; let bytes = 0;
    for await (const chunk of response.body) {
      bytes += chunk.length;
      if (bytes > 4096) { await response.body.cancel().catch(() => {}); throw new Error(); }
      chunks.push(chunk);
    }
    return { status: response.status, value: JSON.parse(Buffer.concat(chunks).toString('utf8')),
      durationMs: Math.round(performance.now() - start) };
  } catch {
    return { status: null, value: null, durationMs: Math.round(performance.now() - start) };
  }
}

/** Checks the live Redis authentication path without an issued token, quota or inference. */
export async function checkDeployment(value, { fetchImpl = fetch, timeoutMs = 10000 } = {}) {
  const origin = deploymentOrigin(value);
  if (!Number.isInteger(timeoutMs) || timeoutMs < 1 || timeoutMs > 30000) throw new Error('invalid_monitor_timeout');
  // A fresh, never-issued credential must trigger Redis lookup and then be denied.
  const nonce = randomBytes(32).toString('hex');
  const [health, denied] = await Promise.all([
    probe(origin, '/health', {}, fetchImpl, timeoutMs),
    probe(origin, '/v1/capabilities', { Authorization: `Bearer ${nonce}` }, fetchImpl, timeoutMs)
  ]);
  const liveness = health.status === 200 && health.value?.status === 'ok' && Object.keys(health.value).length === 1;
  const redisAccessDenied = denied.status === 401 && denied.value?.error === 'unauthorized'
      && Object.keys(denied.value).length === 1;
  return { scope: 'https-liveness-and-redis-authentication-no-inference', checkedAt: new Date().toISOString(), origin,
    liveness: { status: health.status, passed: liveness, durationMs: health.durationMs },
    redisAccess: { status: denied.status, unknownCredentialDenied: redisAccessDenied, durationMs: denied.durationMs },
    inferenceChecked: false, credentialsRequired: false, passed: liveness && redisAccessDenied };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    const [origin, output, ...extra] = process.argv.slice(2);
    if (extra.length) throw new Error();
    const report = await checkDeployment(origin);
    if (output) writeFileSync(output, JSON.stringify(report, null, 2) + '\n', { flag: 'wx', mode: 0o600 });
    console.log(JSON.stringify(report));
    if (!report.passed) process.exitCode = 1;
  } catch {
    console.error('Deployment check failed; response content and credentials are not printed.');
    process.exitCode = 1;
  }
}
