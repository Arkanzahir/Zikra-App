import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { once } from 'node:events';
import { createAccountHandler } from './account-http.mjs';
import { createAccountPreAuth } from './account-abuse.mjs';
import { redisFixture } from './test-support/redis-fixture.mjs';

const authorization = 'Bearer synthetic-auth-token-for-http-tests';
async function fixture(t, changes = {}) {
  const calls = [];
  const service = Object.fromEntries(['status','register','renew','withdraw','requestGuardian',
    'decideGuardian','revokeGuardian'].map(action => [action, async (...args) => {
    calls.push([action, ...args]); return { status: action };
  }]));
  Object.assign(service, changes.service);
  const server = http.createServer(createAccountHandler({ service, beforeAuth: changes.beforeAuth ?? (async () => true) }));
  server.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(() => { server.closeAllConnections(); server.close(); });
  const url = `http://127.0.0.1:${server.address().port}`;
  const request = (path, body, extra = {}) => fetch(url + path, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { authorization, 'content-type':'application/json', ...extra.headers },
    ...(body === undefined ? {} : { body: typeof body === 'string' ? body : JSON.stringify(body) }), ...extra
  });
  return { calls, request };
}
test('account HTTP routes map only supported actions and return no-store minimal JSON', async t => {
  const f = await fixture(t);
  for (const [path, action, body] of [
    ['/v1/account','status',undefined], ['/v1/account/register','register',{ageProof:'opaque',policyVersion:'v1'}],
    ['/v1/account/renew','renew',{ageProof:'opaque',policyVersion:'v1'}],
    ['/v1/account/withdraw','withdraw',{}], ['/v1/account/guardian/request','requestGuardian',{}],
    ['/v1/account/guardian/decision','decideGuardian',{decision:'deny'}],
    ['/v1/account/guardian/revoke','revokeGuardian',{childAccountId:'a'.repeat(64)}]
  ]) {
    const response = await f.request(path, body);
    assert.equal(response.status,200); assert.equal(response.headers.get('cache-control'),'no-store');
    assert.equal(response.headers.get('x-content-type-options'),'nosniff');
    assert.deepEqual(await response.json(), {status:action});
    assert.equal(f.calls.at(-1)[0],action);
  }
  const count = f.calls.length;
  assert.equal((await f.request('/v1/account?token=not-accepted')).status,404);
  assert.equal((await f.request('/v1/account/admin',{})).status,404);
  assert.equal(f.calls.length,count);
});
test('rate gate rejects before identity verification and invalid authorization never reaches service', async t => {
  const blocked = await fixture(t,{beforeAuth:async()=>false});
  const r = await blocked.request('/v1/account');
  assert.equal(r.status,429); assert.equal(r.headers.get('retry-after'),'60');
  assert.equal(blocked.calls.length,0);
  const f = await fixture(t);
  for (const value of ['', 'Basic secret', 'Bearer short', 'Bearer ' + 'a'.repeat(8193)]) {
    assert.equal((await f.request('/v1/account',undefined,{headers:{authorization:value}})).status,401);
  }
  assert.equal(f.calls.length,0);
});
test('HTTP body type, size and unwanted fields are rejected before account mutation', async t => {
  const f = await fixture(t);
  for (const [body,status,extra] of [
    ['{',400,{}], ['null',400,{}], ['[]',400,{}], ['a'.repeat(17000),413,{}],
    [{},415,{headers:{authorization,'content-type':'text/plain'}}]
  ]) assert.equal((await f.request('/v1/account/register',body,extra)).status,status);
  for (const path of ['/v1/account/withdraw','/v1/account/guardian/request','/v1/account/guardian/revoke'])
    assert.equal((await f.request(path,{admin:true})).status,400);
  assert.equal(f.calls.length,0);
});
test('server never serializes provider errors, tokens or registry data', async t => {
  const f = await fixture(t,{service:{status:async()=>{throw new Error('private-provider-detail');}}});
  const r = await f.request('/v1/account'); assert.equal(r.status,503);
  assert.deepEqual(await r.json(),{error:'access_unavailable'});
});
test('account handler cannot initialize without a pre-auth limit',()=>{
  assert.throws(()=>createAccountHandler({service:{}}),/invalid_account_config/);
});
test('account rate Lua is shared, expiring, IP-pseudonymized and fail closed',
  {skip:!process.env.ZIKRA_REDIS_TEST_PYTHON},async t=>{
    const redis = await redisFixture(t);
    const config={redis,prefix:'synthetic:',addressKey:'b'.repeat(64),trustedAddress:req=>req.address};
    const gate=createAccountPreAuth(config), other=createAccountPreAuth(config);
    for(let n=0;n<20;n++) assert.equal(await gate({address:'synthetic-address'}),true);
    assert.equal(await other({address:'synthetic-address'}),false);
    assert.equal(await other({address:'different-address'}),true);
    const keys=await redis.command(['KEYS','synthetic:account-rate:ip:*']);
    assert.equal(keys.length,2);assert.ok(keys.every(key=>!key.includes('address')));
    const ttl=await redis.command(['TTL',keys[0]]);assert.ok(ttl>0&&ttl<=60);
    await assert.rejects(gate({address:''}),/access_unavailable/);
    await redis.command(['SET','synthetic:account-rate:global-minute','corrupt']);
    await assert.rejects(gate({address:'new'}));
  });
