import { createHmac, createHash, randomBytes, randomUUID, timingSafeEqual } from 'node:crypto';

const ID = /^[a-f0-9]{64}$/;
const UUID = /^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/;
const PROOF_LIMIT = 4096;
const DAY = 86400000;
const digest = value => createHash('sha256').update(value).digest('hex');
const object = value => value !== null && typeof value === 'object' && !Array.isArray(value);
function fail(code) { throw new Error(code); }
function fields(value, keys) {
  if (!object(value) || Object.keys(value).some(key => !keys.includes(key))) fail('invalid_input');
}
function reference(value) {
  if (typeof value !== 'string' || !value || value.length > PROOF_LIMIT) fail('invalid_proof');
  return value;
}
function instant(value) { return Number.isSafeInteger(value) && value >= 0; }

// This service does not accept a client age/guardian boolean. Trusted proof adapters are
// mandatory. No adapter is shipped or enabled in production until assurance is selected.
export class AccountEligibility {
  constructor({ identity, proofs, repository, subjectKey, policyVersion, now = Date.now }) {
    if (typeof identity?.verify !== 'function' || typeof identity?.assertActive !== 'function'
        || typeof proofs?.age !== 'function' || typeof proofs?.guardian !== 'function'
        || typeof repository?.read !== 'function' || typeof repository?.compareAndSet !== 'function'
        || typeof subjectKey !== 'string' || !/^[a-f0-9]{64}$/.test(subjectKey)
        || !/^[a-zA-Z0-9._-]{1,64}$/.test(policyVersion ?? '')) fail('invalid_account_config');
    Object.assign(this, { identity, proofs, repository, subjectKey, policyVersion, now });
  }
  accountId(subject) {
    if (typeof subject !== 'string' || subject.length < 1 || subject.length > 128) fail('unauthorized');
    return createHmac('sha256', this.subjectKey).update(subject).digest('hex');
  }
  async principal(token, recent = false) {
    const verified = await this.identity.verify(token, recent);
    return { subject: verified.subject, id: this.accountId(verified.subject) };
  }
  async read(id) {
    if (!ID.test(id ?? '')) fail('invalid_input');
    const record = await this.repository.read(id);
    if (record === null) return null;
    if (!object(record) || record.schema !== 1 || record.id !== id
        || this.accountId(record.subject) !== id || !Number.isSafeInteger(record.revision)
        || record.revision < 1 || !['active', 'withdrawn'].includes(record.state)
        || typeof record.policy !== 'string' || !instant(record.updatedAt)
        || !object(record.age) || !['under13', 'teen', 'adult', 'unknown'].includes(record.age.band)
        || !instant(record.age.expiresAt) || !instant(record.age.issuedAt)) fail('invalid_account');
    return record;
  }
  async save(previous, next) {
    next.revision = (previous?.revision ?? 0) + 1;
    next.updatedAt = this.now();
    if (!await this.repository.compareAndSet(next.id, previous?.revision ?? 0, next)) fail('account_conflict');
    return this.summary(next);
  }
  validAge(record, band) {
    return record?.state === 'active' && record.policy === this.policyVersion
      && record.age.band === band && record.age.issuedAt <= this.now()
      && record.age.expiresAt > this.now();
  }
  summary(record) {
    let status = 'age_unverified';
    if (record.state === 'withdrawn') status = 'withdrawn';
    else if (record.policy !== this.policyVersion) status = 'policy_changed';
    else if (this.validAge(record, 'under13')) status = 'under13';
    else if (this.validAge(record, 'adult')) status = 'adult_eligible';
    else if (this.validAge(record, 'teen')) status = 'guardian_required';
    return { accountId: record.id, revision: record.revision, status };
  }
  async verifiedAge(subject, ageProof) {
    const proof = await this.proofs.age(reference(ageProof), subject), now = this.now();
    if (!object(proof) || proof.subject !== subject
        || !['under13', 'teen', 'adult', 'unknown'].includes(proof.band)
        || !instant(proof.issuedAt) || !instant(proof.expiresAt)
        || proof.issuedAt > now || proof.expiresAt <= now
        || proof.expiresAt - proof.issuedAt > 30 * DAY) fail('invalid_proof');
    return { band: proof.band, issuedAt: proof.issuedAt, expiresAt: proof.expiresAt };
  }
  async register(token, input) {
    fields(input, ['ageProof', 'policyVersion']);
    if (input.policyVersion !== this.policyVersion) fail('policy_changed');
    const who = await this.principal(token, true);
    if (await this.read(who.id)) fail('account_exists');
    const age = await this.verifiedAge(who.subject, input.ageProof);
    const now = this.now();
    return this.save(null, { schema: 1, id: who.id, subject: who.subject,
      policy: this.policyVersion, state: 'active',
      age,
      guardian: null, request: null, requestDay: Math.floor(now / DAY), requestCount: 0 });
  }
  async renew(token, input) {
    fields(input, ['ageProof', 'policyVersion']);
    if (input.policyVersion !== this.policyVersion) fail('policy_changed');
    const who = await this.principal(token, true), previous = await this.read(who.id);
    if (!previous) fail('not_registered');
    const age = await this.verifiedAge(who.subject, input.ageProof);
    if (age.issuedAt <= previous.updatedAt) fail('invalid_proof');
    const next = structuredClone(previous);
    next.state = 'active'; next.policy = this.policyVersion;
    next.age = age;
    next.guardian = null; next.request = null;
    return this.save(previous, next);
  }
  async status(token) {
    const who = await this.principal(token), record = await this.read(who.id);
    if (!record) return { status: 'not_registered', policyVersion: this.policyVersion };
    const result = { ...this.summary(record), policyVersion: this.policyVersion,
      ageProofExpiresAt: record.age.expiresAt };
    if (result.status === 'guardian_required') {
      try { await this.authorize(token); result.status = 'teen_eligible'; }
      catch (error) {
        if (error.message !== 'guardian_required' && error.message !== 'unauthorized') throw error;
        if (record.guardian?.decision === 'deny') result.status = 'guardian_denied';
        else if (record.request?.expiresAt > this.now()) result.status = 'guardian_pending';
      }
    }
    return result;
  }
  async requestGuardian(token) {
    const who = await this.principal(token, true), record = await this.read(who.id);
    if (!this.validAge(record, 'teen')) fail('not_eligible');
    const day = Math.floor(this.now() / DAY);
    const count = record.requestDay === day ? record.requestCount : 0;
    if (!Number.isSafeInteger(count) || count < 0) fail('invalid_account');
    if (count >= 3) fail('rate_limited');
    const secret = randomBytes(32).toString('hex'), requestId = randomUUID();
    const next = structuredClone(record);
    next.requestDay = day; next.requestCount = count + 1;
    // A replacement request invalidates the previous grant and challenge atomically.
    next.guardian = null;
    next.request = { id: requestId, hash: digest(secret), policy: this.policyVersion,
      expiresAt: Math.min(this.now() + DAY, record.age.expiresAt) };
    await this.save(record, next);
    return { childAccountId: who.id, requestId, secret, expiresAt: next.request.expiresAt };
  }
  async decideGuardian(token, input) {
    fields(input, ['childAccountId', 'requestId', 'secret', 'decision', 'policyVersion', 'guardianProof']);
    if (input.policyVersion !== this.policyVersion) fail('policy_changed');
    if (!UUID.test(input.requestId ?? '') || !/^[a-f0-9]{64}$/.test(input.secret ?? '')
        || !['grant', 'deny'].includes(input.decision)) fail('invalid_input');
    const who = await this.principal(token, true), guardian = await this.read(who.id);
    if (!this.validAge(guardian, 'adult') || who.id === input.childAccountId) fail('not_eligible');
    const child = await this.read(input.childAccountId), request = child?.request;
    if (!this.validAge(child, 'teen') || !object(request) || request.id !== input.requestId
        || request.policy !== this.policyVersion || !instant(request.expiresAt)
        || request.expiresAt <= this.now() || !ID.test(request.hash ?? '')
        || !timingSafeEqual(Buffer.from(request.hash, 'hex'), Buffer.from(digest(input.secret), 'hex')))
      fail('invalid_request');
    const proof = await this.proofs.guardian(reference(input.guardianProof), {
      childSubject: child.subject, guardianSubject: who.subject, requestId: request.id,
      policyVersion: this.policyVersion, decision: input.decision });
    if (!object(proof) || proof.childSubject !== child.subject || proof.guardianSubject !== who.subject
        || proof.requestId !== request.id || proof.policyVersion !== this.policyVersion
        || proof.decision !== input.decision || !instant(proof.expiresAt)
        || proof.expiresAt <= this.now() || proof.expiresAt > this.now() + 30 * DAY) fail('invalid_proof');
    const next = structuredClone(child);
    next.request = null;
    next.guardian = { id: who.id, revision: guardian.revision, decision: input.decision,
      policy: this.policyVersion, recordedAt: this.now(),
      expiresAt: Math.min(proof.expiresAt, guardian.age.expiresAt, child.age.expiresAt) };
    await this.save(child, next);
    return { status: input.decision === 'grant' ? 'guardian_granted' : 'guardian_denied' };
  }
  async withdraw(token) {
    const who = await this.principal(token, true), record = await this.read(who.id);
    if (!record) fail('not_registered');
    const next = structuredClone(record); next.state = 'withdrawn'; next.request = null; next.guardian = null;
    return this.save(record, next);
  }
  async revokeGuardian(token, childAccountId) {
    const who = await this.principal(token, true), child = await this.read(childAccountId);
    if (child?.guardian?.id !== who.id) fail('not_eligible');
    const next = structuredClone(child); next.guardian = null; next.request = null;
    await this.save(child, next);
    return { status: 'guardian_revoked' };
  }
  async authorize(token) {
    const who = await this.principal(token), child = await this.read(who.id);
    if (this.validAge(child, 'adult')) return { accountId: who.id, ageBand: 'adult' };
    if (!this.validAge(child, 'teen')) fail('not_eligible');
    const grant = child.guardian;
    if (!object(grant) || grant.decision !== 'grant' || grant.policy !== this.policyVersion
        || !instant(grant.expiresAt) || grant.expiresAt <= this.now()
        || !instant(grant.recordedAt) || grant.recordedAt > this.now()
        || grant.id === child.id) fail('guardian_required');
    const guardian = await this.read(grant.id);
    if (!this.validAge(guardian, 'adult') || guardian.revision !== grant.revision) fail('guardian_required');
    await this.identity.assertActive(guardian.subject, grant.recordedAt);
    return { accountId: who.id, ageBand: 'teen' };
  }
}

// Version-checked writes prevent two tabs/servers from accepting the same challenge.
export const ACCOUNT_CAS = `
local raw = redis.call('GET', KEYS[1])
if raw then
  local ok, record = pcall(cjson.decode, raw)
  if not ok or type(record) ~= 'table' or type(record.revision) ~= 'number' then
    return redis.error_reply('invalid_account')
  end
  if record.revision ~= tonumber(ARGV[1]) then return 0 end
elseif tonumber(ARGV[1]) ~= 0 then return 0 end
redis.call('SET', KEYS[1], ARGV[2], 'EX', 7776000)
return 1
`;

export class RedisAccountRepository {
  constructor(redis, prefix) {
    if (typeof redis?.command !== 'function' || !/^[a-zA-Z0-9:_-]{1,64}:$/.test(prefix ?? ''))
      fail('invalid_account_config');
    this.redis = redis; this.prefix = prefix;
  }
  key(id) { if (!ID.test(id ?? '')) fail('invalid_input'); return `${this.prefix}account:${id}`; }
  async read(id) {
    const raw = await this.redis.command(['GET', this.key(id)]);
    if (raw === null) return null;
    if (typeof raw !== 'string' || raw.length > 8192) fail('invalid_account');
    try { return JSON.parse(raw); } catch { fail('invalid_account'); }
  }
  async compareAndSet(id, revision, record) {
    if (!Number.isSafeInteger(revision) || revision < 0 || record.id !== id
        || record.revision !== revision + 1) fail('invalid_account');
    const value = JSON.stringify(record);
    if (value.length > 8192) fail('invalid_account');
    const result = await this.redis.command(['EVAL', ACCOUNT_CAS, 1, this.key(id), revision, value]);
    if (result !== 0 && result !== 1) fail('invalid_account');
    return result === 1;
  }
}
