import test from 'node:test';
import assert from 'node:assert/strict';
import { AccountEligibility, RedisAccountRepository } from './account-eligibility.mjs';
import { redisFixture } from './test-support/redis-fixture.mjs';

const DAY = 86400000;
function fixture(repository) {
  let time = 1800000000000;
  const data = new Map(), disabled = new Set(), proofs = new Map();
  const memory = {
    read: async id => structuredClone(data.get(id) ?? null),
    compareAndSet: async (id, revision, record) => {
      if ((data.get(id)?.revision ?? 0) !== revision) return false;
      data.set(id, structuredClone(record)); return true;
    }
  };
  const config = {
    repository: repository ?? memory, subjectKey: 'a'.repeat(64), policyVersion: 'beta-v1',
    now: () => time,
    identity: {
      verify: async subject => {
        if (disabled.has(subject) || !['child', 'parent', 'other', 'adult', 'young', 'unknown'].includes(subject))
          throw new Error('unauthorized');
        return { subject };
      },
      assertActive: async subject => { if (disabled.has(subject)) throw new Error('unauthorized'); }
    },
    proofs: {
      age: async ref => structuredClone(proofs.get(ref)),
      guardian: async ref => structuredClone(proofs.get(ref))
    }
  };
  const service = new AccountEligibility(config);
  async function register(subject, band) {
    const ageProof = `synthetic-proof-${subject}`;
    proofs.set(ageProof, { subject, band, issuedAt: time, expiresAt: time + 7 * DAY });
    return service.register(subject, { ageProof, policyVersion: 'beta-v1' });
  }
  // Only the public challenge fields belong in the request body.
  async function decide(request, parent = 'parent', outcome = 'grant', overrides = {}) {
    const { expiresAt, ...input } = request;
    const ref = `synthetic-guardian-proof-${proofs.size}`;
    proofs.set(ref, { childSubject: 'child', guardianSubject: parent,
      requestId: input.requestId, policyVersion: 'beta-v1', decision: outcome, expiresAt: time + DAY });
    return service.decideGuardian(parent, { ...input, decision: outcome, policyVersion: 'beta-v1',
      guardianProof: ref, ...overrides });
  }
  return { service, register, decide, proofs, data, disabled, config,
    advance: ms => { time += ms; } };
}

test('self-service eligibility accepts trusted adult proof, never a client age flag or missing evidence', async () => {
  const f = fixture();
  await assert.rejects(f.service.register('adult', { ageProof: 'missing', policyVersion: 'beta-v1' }), /invalid_proof/);
  await assert.rejects(f.service.register('adult', { age: 18, ageProof: 'missing', policyVersion: 'beta-v1' }), /invalid_input/);
  const result = await f.register('adult', 'adult');
  assert.equal(result.status, 'adult_eligible');
  assert.deepEqual(await f.service.authorize('adult'), { accountId: result.accountId, ageBand: 'adult' });
  assert.ok(!JSON.stringify(result).includes('subject'));
});
test('unknown, under13, expired age evidence and email/session failures cannot open online AI', async () => {
  const f = fixture();
  await f.register('young', 'under13'); await f.register('unknown', 'unknown');
  for (const subject of ['young', 'unknown']) {
    await assert.rejects(f.service.authorize(subject), /not_eligible/);
    await assert.rejects(f.service.requestGuardian(subject), /not_eligible/);
  }
  await f.register('adult', 'adult'); f.advance(7 * DAY);
  await assert.rejects(f.service.authorize('adult'), /not_eligible/);
  f.disabled.add('adult'); await assert.rejects(f.service.authorize('adult'), /unauthorized/);
});
test('teen authorization requires distinct verified adult, bound guardian proof and an unused challenge', async () => {
  const f = fixture(); await f.register('child', 'teen'); await f.register('parent', 'adult');
  await assert.rejects(f.service.authorize('child'), /guardian_required/);
  const request = await f.service.requestGuardian('child');
  await assert.rejects(f.decide(request, 'child'), /not_eligible/);
  await assert.rejects(f.decide(request, 'parent', 'grant', { secret: 'b'.repeat(64) }), /invalid_request/);
  await f.decide(request);
  assert.equal((await f.service.authorize('child')).ageBand, 'teen');
  await assert.rejects(f.decide(request), /invalid_request/);
});
test('guardian proof cannot be reused for a different child, parent, request, policy or action', async () => {
  for (const mutation of [ { childSubject: 'other' }, { guardianSubject: 'other' },
    { requestId: 'other' }, { policyVersion: 'old' }, { decision: 'deny' }, { expiresAt: 0 } ]) {
    const f = fixture(); await f.register('child', 'teen'); await f.register('parent', 'adult');
    const request = await f.service.requestGuardian('child');
    f.proofs.set('bad', { childSubject: 'child', guardianSubject: 'parent', requestId: request.requestId,
      policyVersion: 'beta-v1', decision: 'grant', expiresAt: 1800000000000 + DAY, ...mutation });
    await assert.rejects(f.decide(request, 'parent', 'grant', { guardianProof: 'bad' }), /invalid_proof/);
    await assert.rejects(f.service.authorize('child'), /guardian_required/);
  }
});
test('denial, guardian withdrawal, revoked sessions and guardian expiry remove eligibility immediately', async () => {
  for (const action of ['deny', 'withdraw', 'revoke', 'disabled', 'expiry']) {
    const f = fixture(); await f.register('child', 'teen'); await f.register('parent', 'adult');
    const request = await f.service.requestGuardian('child');
    await f.decide(request, 'parent', action === 'deny' ? 'deny' : 'grant');
    if (action === 'withdraw') await f.service.withdraw('parent');
    if (action === 'revoke') await f.service.revokeGuardian('parent', request.childAccountId);
    if (action === 'disabled') f.disabled.add('parent');
    if (action === 'expiry') f.advance(DAY);
    await assert.rejects(f.service.authorize('child'));
  }
});
test('expired and replaced challenges fail; request creation is bounded per account/day', async () => {
  const f = fixture(); await f.register('child', 'teen'); await f.register('parent', 'adult');
  const first = await f.service.requestGuardian('child');
  await f.service.requestGuardian('child');
  await assert.rejects(f.decide(first), /invalid_request/);
  const third = await f.service.requestGuardian('child');
  await assert.rejects(f.service.requestGuardian('child'), /rate_limited/);
  f.advance(DAY);
  await assert.rejects(f.decide(third), /invalid_request/);
  await f.service.requestGuardian('child');
});
test('withdrawal and policy updates cannot be bypassed by registration retry or an old grant', async () => {
  const f = fixture(); await f.register('adult', 'adult'); await f.service.withdraw('adult');
  await assert.rejects(f.register('adult', 'adult'), /account_exists/);
  await assert.rejects(f.service.authorize('adult'), /not_eligible/);
  await f.register('child', 'teen'); await f.register('parent', 'adult');
  await f.decide(await f.service.requestGuardian('child'));
  const changed = new AccountEligibility({ ...f.config, policyVersion: 'beta-v2' });
  await assert.rejects(changed.authorize('child'), /not_eligible/);
});
test('unavailable or corrupt registry does not fall back to online and never returns raw proofs', async () => {
  const f = fixture(); const registered = await f.register('adult', 'adult');
  const record = f.data.get(registered.accountId);
  assert.ok(!JSON.stringify(record).includes('synthetic-proof'));
  record.age.expiresAt = 'never';
  await assert.rejects(f.service.authorize('adult'), /invalid_account/);
  f.config.repository.read = async () => { throw new Error('access_unavailable'); };
  await assert.rejects(f.service.authorize('adult'), /access_unavailable/);
});

test('status is owner-scoped and tracks request/grant/denial without exposing challenge hashes', async () => {
  const f = fixture(); assert.equal((await f.service.status('child')).status,'not_registered');
  await f.register('child','teen'); await f.register('parent','adult');
  assert.equal((await f.service.status('child')).status,'guardian_required');
  const request=await f.service.requestGuardian('child');
  const pending=await f.service.status('child'); assert.equal(pending.status,'guardian_pending');
  assert.ok(!JSON.stringify(pending).includes(request.secret));
  await f.decide(request); assert.equal((await f.service.status('child')).status,'teen_eligible');
  await f.service.revokeGuardian('parent',request.childAccountId);
  assert.equal((await f.service.status('child')).status,'guardian_required');
  await f.decide(await f.service.requestGuardian('child'),'parent','deny');
  assert.equal((await f.service.status('child')).status,'guardian_denied');
});
test('explicit renewal needs fresh trusted proof and invalidates prior guardian linkage', async () => {
  const f=fixture();await f.register('child','teen');await f.register('parent','adult');
  await f.decide(await f.service.requestGuardian('child'));
  await f.service.withdraw('parent');
  await assert.rejects(f.service.renew('parent',{ageProof:'missing',policyVersion:'beta-v1'}),/invalid_proof/);
  await assert.rejects(f.service.renew('parent',{ageProof:'synthetic-proof-parent',policyVersion:'beta-v1'}),/invalid_proof/);
  f.advance(1000);
  f.proofs.get('synthetic-proof-parent').issuedAt+=1000;
  await f.service.renew('parent',{ageProof:'synthetic-proof-parent',policyVersion:'beta-v1'});
  assert.equal((await f.service.status('parent')).status,'adult_eligible');
  await assert.rejects(f.service.authorize('child'),/guardian_required/);
  await f.decide(await f.service.requestGuardian('child'));
  f.advance(1000);
  f.proofs.get('synthetic-proof-child').issuedAt+=2000;
  await f.service.renew('child',{ageProof:'synthetic-proof-child',policyVersion:'beta-v1'});
  await assert.rejects(f.service.authorize('child'),/guardian_required/);
  await assert.rejects(f.service.renew('child',{ageProof:'synthetic-proof-child',policyVersion:'old'}),/policy_changed/);
});

const redisEnabled = Boolean(process.env.ZIKRA_REDIS_TEST_PYTHON);
test('Redis CAS executes production Lua: concurrent registration/challenge consumption have one winner',
  { skip: !redisEnabled }, async t => {
    const redis = await redisFixture(t), repo = new RedisAccountRepository(redis, 'synthetic:');
    const f = fixture(repo);
    const registered = await Promise.allSettled([f.register('child', 'teen'), f.register('child', 'teen')]);
    assert.equal(registered.filter(x => x.status === 'fulfilled').length, 1);
    await f.register('parent', 'adult');
    const request = await f.service.requestGuardian('child');
    const consumed = await Promise.allSettled([f.decide(request), f.decide(request)]);
    assert.equal(consumed.filter(x => x.status === 'fulfilled').length, 1);
    assert.equal((await f.service.authorize('child')).ageBand, 'teen');
    const ttl = await redis.command(['TTL', repo.key(request.childAccountId)]);
    assert.ok(ttl > 0 && ttl <= 90 * 86400);
    await redis.command(['SET', repo.key(request.childAccountId), '{bad']);
    await assert.rejects(f.service.authorize('child'), /invalid_account/);
    assert.throws(() => repo.key('*'), /invalid_input/);
  });
