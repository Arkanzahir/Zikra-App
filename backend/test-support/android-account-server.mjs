import http from 'node:http';
import { initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { createAdultServices } from '../adult-account-runtime.mjs';
import { RedisAccess } from '../redis-access.mjs';
import { createRequestHandler } from '../server.mjs';
import { redisFixture } from './redis-fixture.mjs';

const projectId = 'demo-zikra-accounts';
if (process.env.FIREBASE_AUTH_EMULATOR_HOST !== '127.0.0.1:9099'
    || !process.env.ZIKRA_REDIS_TEST_PYTHON) throw Error('Local emulator required');
const cleanup = [];
const redis = await redisFixture({ after: fn => cleanup.push(fn) });
const auth = getAuth(initializeApp({ projectId }));
const prefix = 'synthetic-android:';
const runtime = createAdultServices({ auth, redis, prefix, projectId, subjectKey: 'a'.repeat(64),
  trustedAddress: req => req.socket.remoteAddress, deleteUser: uid => auth.deleteUser(uid) });
const accessStore = new RedisAccess({ redis, prefix });
let timeouts = 0;
const gateway = createRequestHandler({
  provider: 'cloudflare', apiKey: 'synthetic-local-provider-key', accountId: 'a'.repeat(32),
  model: '@cf/meta/synthetic-model', freePlanConfirmed: true, accessMode: 'installation', accessStore,
  accountSelfService: true, accountPreAuth: runtime.beforeAuth, adultAccessGate: token => runtime.service.authorize(token),
  fetchImpl: async (_, options) => {
    if (String(options.body).includes('Synthetic edge timeout')) {
      timeouts++;
      throw Error('Synthetic upstream failure');
    }
    return new Response(JSON.stringify({ choices: [{ message: { content: 'Synthetic adult flow answer' } }] }));
  }
});
const server = http.createServer((req, res) => {
  if (req.url.startsWith('/v1/adult-account')) return runtime.handler(req, res);
  if (req.url === '/__qa') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ timeouts }));
  }
  if (req.headers.authorization) {
    res.writeHead(400);
    return res.end('Fixture does not accept installation tokens');
  }
  const writeHead = res.writeHead.bind(res);
  const end = res.end.bind(res);
  // Simulate the non-JSON 504 page an edge gateway can return.
  let edgeTimeout = false;
  res.writeHead = (status, headers) => {
    edgeTimeout = status === 502;
    return edgeTimeout ? writeHead(504, { 'Content-Type': 'text/html' }) : writeHead(status, headers);
  };
  res.end = body => end(edgeTimeout ? '<html>Gateway timeout</html>' : body);
  return gateway(req, res);
});
server.listen(9098, '127.0.0.1');
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => {
  server.closeAllConnections();
  server.close();
  for (const close of cleanup) close();
});
