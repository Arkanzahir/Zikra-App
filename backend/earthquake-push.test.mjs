import test from 'node:test';
import assert from 'node:assert/strict';
import { generateKeyPairSync, verify, randomBytes } from 'node:crypto';
import { parseQuake, parseFeed, loadQuakes } from './earthquake-feed.mjs';
import { createFcmSender, readPushConfig } from './earthquake-fcm.mjs';
import { pollEarthquakes, createEarthquakePollHandler } from './earthquake-poll.mjs';
import { redisFixture } from './test-support/redis-fixture.mjs';

const now = Date.parse('2026-11-08T06:00:00Z');
const quake = (at = now - 60000) => ({ DateTime: new Date(at).toISOString(), Magnitude: '5.1',
  Kedalaman: '10 km', Coordinates: '-7.280,112.790', Wilayah: 'Contoh wilayah uji',
  Tanggal: '08 Nov 2026', Jam: '12:59:00 WIB', Potensi: 'Contoh data uji', Dirasakan: '', Shakemap: '20261108125900.mmi.jpg' });
const event = at => parseQuake(quake(at), now);
const integration = { skip: !process.env.ZIKRA_REDIS_TEST_PYTHON, timeout: 15000 };

test('official report snapshots validate fields and deduplicate revisions across feeds', () => {
  const original = parseQuake(quake(), now);
  const changed = parseQuake({ ...quake(), Magnitude: '5.2', Coordinates: '-7.281,112.791' }, now);
  assert.equal(original.id, changed.id);
  const snapshot = JSON.parse(original.snapshot);
  assert.equal(snapshot.version, 1); assert.equal(snapshot.fetchedAt, now);
  assert.equal(JSON.parse(snapshot.report).Infogempa.gempa.Wilayah, quake().Wilayah);
  assert.equal(parseFeed(JSON.stringify({ Infogempa: { gempa: [quake(), quake(now - 300000)] } }), now).length, 2);
  for (const patch of [{ DateTime: '2026-02-31T06:00:00Z' }, { DateTime: new Date(now + 600000).toISOString() },
    { Magnitude: 'NaN' }, { Kedalaman: '2000 km' }, { Coordinates: '99,0' }, { Wilayah: 'x'.repeat(241) }]) {
    assert.throws(() => parseQuake({ ...quake(), ...patch }, now));
  }
});

test('source requests are fixed HTTPS, bounded and fail closed on a malformed feed', async () => {
  const urls = [];
  const reports = await loadQuakes({ now, fetchImpl: async (url, options) => {
    urls.push(url); assert.equal(options.redirect, 'error');
    return new Response(JSON.stringify({ Infogempa: { gempa: quake() } }));
  } });
  assert.equal(urls.length, 3); assert.ok(urls.every(url => url.startsWith('https://data.bmkg.go.id/DataMKG/TEWS/')));
  assert.equal(reports.length, 1);
  await assert.rejects(loadQuakes({ now, fetchImpl: async () => new Response('x'.repeat(65537)) }));
  await assert.rejects(loadQuakes({ now, fetchImpl: async () => new Response('{}') }));
});

test('durable baseline, event claims and shared interval survive cold starts without duplicate sends', integration, async t => {
  const redis = await redisFixture(t); let time = now, sent = 0, items = [event(now - 60000)];
  const options = { redis, prefix: 'test:', clock: () => time, load: async () => items, send: async () => { sent++; return 'fixture'; } };
  assert.equal((await pollEarthquakes(options)).state, 'baseline'); assert.equal(sent, 0);
  time += 61000; await redis.command(['DEL', 'test:e02:interval']); items = [items[0], event(now)];
  assert.equal((await pollEarthquakes(options)).accepted, 1); assert.equal(sent, 1);
  assert.equal((await pollEarthquakes(options)).state, 'throttled');
  time += 61000; await redis.command(['DEL', 'test:e02:interval']);
  assert.equal((await pollEarthquakes({ ...options })).accepted, 0); assert.equal(sent, 1);
});

test('ambiguous FCM result remains reserved, while explicit rejection can retry on a later poll', integration, async t => {
  const redis = await redisFixture(t); let time = now, items = [event(now - 60000)];
  const options = { redis, prefix: 'test:', clock: () => time, load: async () => items, send: async () => { throw new Error('timeout'); } };
  await pollEarthquakes(options); time += 61000; await redis.command(['DEL', 'test:e02:interval']); items.push(event(now));
  assert.equal((await pollEarthquakes(options)).state, 'delivery_uncertain'); time += 61000; await redis.command(['DEL', 'test:e02:interval']);
  let sent = 0;
  assert.equal((await pollEarthquakes({ ...options, send: async () => { sent++; return 'receipt'; } })).accepted, 0); assert.equal(sent, 0);
  time += 61000; await redis.command(['DEL', 'test:e02:interval']); items.push(event(now + 60000));
  await assert.rejects(pollEarthquakes({ ...options, send: async () => { throw Object.assign(new Error(), { notAccepted: true }); } }));
  time += 61000; await redis.command(['DEL', 'test:e02:interval']);
  assert.equal((await pollEarthquakes({ ...options, send: async () => 'receipt' })).accepted, 1);
});

test('stale events, parallel polls and authorization failures do not send notifications', integration, async t => {
  const redis = await redisFixture(t); let time = now, resolveLoad;
  const options = { redis, prefix: 'test:', clock: () => time, send: async () => { throw new Error('must not send'); }, load: async () => [event(now - 7200000)] };
  await pollEarthquakes(options); time += 61000; await redis.command(['DEL', 'test:e02:interval']);
  assert.equal((await pollEarthquakes(options)).accepted, 0);
  time += 61000; await redis.command(['DEL', 'test:e02:interval']);
  const first = pollEarthquakes({ ...options, load: () => new Promise(resolve => { resolveLoad = resolve; }) });
  while (!resolveLoad) await new Promise(resolve => setTimeout(resolve, 1));
  assert.equal((await pollEarthquakes(options)).state, 'busy');
  resolveLoad([event(now - 7200000)]); await first;
});

test('FCM uses scoped RSA OAuth and a data-only payload pinned to the exact source report', async () => {
  const pair = generateKeyPairSync('rsa', { modulusLength: 2048 });
  const account = { type: 'service_account', project_id: 'fixture-project', client_email: 'sender@fixture-project.iam.gserviceaccount.com',
    private_key: pair.privateKey.export({ type: 'pkcs8', format: 'pem' }).toString() };
  const config = readPushConfig({ ZIKRA_FIREBASE_SERVICE_ACCOUNT_JSON: JSON.stringify(account) });
  let oauth = 0, messages = 0;
  const send = createFcmSender(config, { clock: () => now, fetchImpl: async (url, options) => {
    assert.equal(options.redirect, 'error');
    if (url === 'https://oauth2.googleapis.com/token') {
      oauth++; const jwt = options.body.get('assertion').split('.');
      assert.ok(verify('RSA-SHA256', Buffer.from(jwt.slice(0, 2).join('.')), pair.publicKey, Buffer.from(jwt[2], 'base64url')));
      const payload = JSON.parse(Buffer.from(jwt[1], 'base64url')); assert.equal(payload.scope, 'https://www.googleapis.com/auth/firebase.messaging');
      return Response.json({ access_token: randomBytes(30).toString('hex'), expires_in: 3600 });
    }
    messages++; assert.equal(url, 'https://fcm.googleapis.com/v1/projects/fixture-project/messages:send');
    const body = JSON.parse(options.body); assert.equal(body.message.notification, undefined);
    assert.equal(body.message.data.quake_report, event(now - 60000).snapshot);
    assert.equal(body.message.android.priority, 'HIGH'); assert.equal(body.message.android.ttl, '900s');
    return Response.json({ name: 'projects/fixture-project/messages/fixture' });
  } });
  await send.prepare(); await send(event(now - 60000)); await send(event(now - 60000));
  assert.equal(oauth, 1); assert.equal(messages, 2);
});

test('poll route denies missing credentials and never exposes configuration or upstream failures', async () => {
  for (const [method, header, status] of [['GET', '', 405], ['POST', '', 401], ['POST', 'Bearer ' + 'a'.repeat(64), 503]]) {
    let code, body;
    await createEarthquakePollHandler({ ZIKRA_QUAKE_CRON_SECRET: 'a'.repeat(64) })({ method, headers: { authorization: header } },
      { writeHead: value => { code = value; }, end: value => { body = value; } });
    assert.equal(code, status); assert.ok(!body.includes('private_key')); assert.ok(!body.includes('aaaa'));
  }
});
