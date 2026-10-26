import http from 'node:http';
import { once } from 'node:events';
import { setTimeout as delay } from 'node:timers/promises';
import { createRequestHandler } from '../server.mjs';

const identity = '00000000-0000-4000-8000-000000000001';
const auth = 'synthetic-timeout-fixture-access-only';

/** Preview-only: real HTTP sockets and production handler, controlled local upstream. */
export async function runTimeoutFixture(stage, { providerTimeoutMs } = {}) {
  if (!['headers', 'body'].includes(stage)) throw new Error('invalid_fixture_stage');
  const deadline = providerTimeoutMs ?? 35000;
  if (!Number.isInteger(deadline) || deadline < 20 || deadline > 35000)
    throw new Error('invalid_fixture_deadline');
  let healthy = false, calls = 0, releases = 0, active = 0, abortObserved = false;
  let upstreamClosed = false, slowWrites = 0;
  const sockets = new Set(), timers = new Set();
  const upstream = http.createServer((_req, res) => {
    if (healthy) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ choices: [{ message: { content: 'Uji pemulihan selesai.' } }] }));
      return;
    }
    res.on('close', () => { upstreamClosed = true; });
    if (stage === 'body') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.flushHeaders();
      res.write('{"choices":');
    }
    const timer = setTimeout(() => { slowWrites++; res.end('{}'); }, deadline + 10000);
    timers.add(timer);
    res.on('close', () => { clearTimeout(timer); timers.delete(timer); });
  });
  upstream.on('connection', socket => {
    sockets.add(socket);
    socket.on('close', () => sockets.delete(socket));
  });
  const store = {
    async identify(value) { return value === `Bearer ${auth}` ? identity : null; },
    async acquire() {
      if (active) return { allowed: false, status: 429, error: 'try_later' };
      active++; return { allowed: true, lease: 'fixture-lease' };
    },
    async release() { active--; releases++; }
  };
  let gateway;
  try {
    upstream.listen(0, '127.0.0.1'); await once(upstream, 'listening');
    const upstreamUrl = `http://127.0.0.1:${upstream.address().port}`;
    const handler = createRequestHandler({
      apiKey: 'synthetic-upstream-no-provider-key', model: 'fixture/model',
      accessMode: 'installation', accessStore: store,
      ...(providerTimeoutMs === undefined ? {} : { providerTimeoutMs }),
      fetchImpl: async (url, request) => {
        if (url !== 'https://api.featherless.ai/v1/chat/completions')
          throw new Error('unexpected_upstream');
        calls++;
        request.signal.addEventListener('abort', () => { abortObserved = true; }, { once: true });
        // No credentials or payload leave this cloud invocation's loopback interface.
        return fetch(upstreamUrl, { method: 'POST', signal: request.signal, redirect: 'error',
          headers: { 'Content-Type': 'application/json' }, body: request.body });
      }
    });
    gateway = http.createServer(handler);
    gateway.listen(0, '127.0.0.1'); await once(gateway, 'listening');
    const url = `http://127.0.0.1:${gateway.address().port}/v1/assistant`;
    const send = () => fetch(url, { method: 'POST', redirect: 'error',
      signal: AbortSignal.timeout(deadline + 5000),
      headers: { Authorization: `Bearer ${auth}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ messages: [{ role: 'user', content: 'Uji teknis batas waktu.' }] }) });
    const start = performance.now();
    const first = await send(), firstBody = await first.json();
    const elapsedMs = Math.round(performance.now() - start);
    for (let i = 0; i < 40 && (active !== 0 || !upstreamClosed); i++) await delay(25);
    const timeoutReleases = releases, callsBeforeRetry = calls;
    const timeoutAborted = abortObserved, timedOutSocketClosed = upstreamClosed;
    healthy = true;
    const second = await send(); await second.body?.cancel();
    for (let i = 0; i < 40 && active !== 0; i++) await delay(25);
    const report = {
      scope: 'production-handler-in-cloud-runtime-with-controlled-loopback-http-upstream',
      stage, checkedAt: new Date().toISOString(), deadlineMs: deadline,
      usesProductionDefaultDeadline: providerTimeoutMs === undefined,
      timeout: { status: first.status, sanitizedError: firstBody?.error === 'provider_unavailable'
        && Object.keys(firstBody).length === 1, elapsedMs, abortObserved: timeoutAborted,
        upstreamSocketClosed: timedOutSocketClosed, callsBeforeRetry, released: timeoutReleases === 1 },
      explicitRetry: { status: second.status, upstreamCallsTotal: calls, releasesTotal: releases,
        capacityAvailable: active === 0 },
      delayedUpstreamWrites: slowWrites, externalInferenceRequests: 0,
      productionRedisUsed: false, productionDeploymentModified: false,
      passed: first.status === 504 && firstBody?.error === 'provider_unavailable'
        && Object.keys(firstBody).length === 1 && elapsedMs >= deadline - 15
        && elapsedMs < deadline + 4000 && timeoutAborted && timedOutSocketClosed
        && callsBeforeRetry === 1 && timeoutReleases === 1 && second.status === 200
        && calls === 2 && releases === 2 && active === 0 && slowWrites === 0
    };
    return report;
  } finally {
    for (const timer of timers) clearTimeout(timer);
    for (const socket of sockets) socket.destroy();
    await Promise.all([upstream, gateway].filter(Boolean).map(server => new Promise(resolve => {
      server.closeAllConnections(); server.close(() => resolve());
    })));
  }
}
