import { createHmac } from 'node:crypto';

export const ADULT_POLICY = 'adult-beta-2026-11-v1';
const DAY = 86400000, ID = /^[a-f0-9]{64}$/;
const exact = (value, keys) => value && typeof value === 'object' && !Array.isArray(value)
  && Object.keys(value).length === keys.length && keys.every(key => Object.hasOwn(value, key));
const fail = code => { throw new Error(code); };

export function adultOnDate(birthDate, today) {
  if (typeof birthDate !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(birthDate)
      || !/^\d{4}-\d{2}-\d{2}$/.test(today)) fail('invalid_input');
  const born = new Date(birthDate + 'T00:00:00Z');
  if (!Number.isFinite(born.getTime()) || born.toISOString().slice(0, 10) !== birthDate
      || birthDate < '1900-01-01' || birthDate > today) fail('invalid_input');
  const years = Number(today.slice(0, 4)) - Number(birthDate.slice(0, 4));
  return years > 18 || years === 18 && today.slice(5) >= birthDate.slice(5);
}

/** Verified email plus an explicit age declaration; never represented as identity verification. */
export class AdultAccounts {
  constructor({ identity, repository, subjectKey, now = Date.now, policyVersion = ADULT_POLICY }) {
    if (typeof identity?.verify !== 'function' || typeof repository?.read !== 'function'
        || typeof repository?.compareAndSet !== 'function' || !ID.test(subjectKey ?? '')
        || !/^[A-Za-z0-9._-]{1,64}$/.test(policyVersion)) fail('invalid_account_config');
    Object.assign(this, { identity, repository, subjectKey, now, policyVersion });
  }
  id(subject) {
    if (typeof subject !== 'string' || !subject || subject.length > 128) fail('unauthorized');
    return createHmac('sha256', this.subjectKey).update('adult-account:v1:').update(subject).digest('hex');
  }
  async principal(token, recent = false) {
    const { subject } = await this.identity.verify(token, recent);
    return { subject, id: this.id(subject) };
  }
  async read(id) {
    const record = await this.repository.read(id);
    if (record === null) return null;
    if (!exact(record, ['schema','id','subject','revision','state','policy','assurance',
        'ageBand','declaredAt','updatedAt','expiresAt']) || record.schema !== 2 || record.id !== id
        || this.id(record.subject) !== id || !Number.isSafeInteger(record.revision) || record.revision < 1
        || !['active','withdrawn','under18'].includes(record.state)
        || !['adult','under18'].includes(record.ageBand) || record.assurance !== 'self_declared'
        || !/^[A-Za-z0-9._-]{1,64}$/.test(record.policy)
        || !['declaredAt','updatedAt','expiresAt'].every(key => Number.isSafeInteger(record[key]) && record[key] >= 0)
        || record.updatedAt < record.declaredAt || record.updatedAt > this.now() || record.declaredAt > this.now()
        || record.expiresAt <= record.declaredAt || record.expiresAt - record.declaredAt > 30 * DAY
        || (record.state === 'under18') !== (record.ageBand === 'under18' && record.state !== 'withdrawn')) fail('invalid_account');
    return record;
  }
  summary(record) {
    let status = record.state === 'under18' ? 'under18_blocked'
      : record.state === 'withdrawn' ? 'withdrawn'
      : record.policy !== this.policyVersion ? 'policy_changed'
      : record.expiresAt <= this.now() ? 'confirmation_expired' : 'adult_confirmed';
    return { status, minimumAge: 18, assurance: 'self_declared', policyVersion: this.policyVersion,
      expiresAt: record.expiresAt, revision: record.revision };
  }
  async save(previous, record) {
    record.revision = (previous?.revision ?? 0) + 1; record.updatedAt = this.now();
    if (!await this.repository.compareAndSet(record.id, previous?.revision ?? 0, record)) fail('account_conflict');
    return this.summary(record);
  }
  async status(token) {
    const who = await this.principal(token), record = await this.read(who.id);
    return record ? this.summary(record) : { status: 'not_registered', minimumAge: 18,
      assurance: 'self_declared', policyVersion: this.policyVersion };
  }
  async register(token, input) { return this.declare(token, input, false); }
  async renew(token, input) { return this.declare(token, input, true); }
  async declare(token, input, renewal) {
    if (!exact(input, ['birthDate','accepted','policyVersion']) || input.accepted !== true) fail('invalid_input');
    if (input.policyVersion !== this.policyVersion) fail('policy_changed');
    const who = await this.principal(token, true), previous = await this.read(who.id);
    if (previous?.state === 'under18' || previous?.ageBand === 'under18') fail('under18_blocked');
    if (!renewal && previous) fail('account_exists');
    if (renewal && !previous) fail('not_registered');
    const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Jakarta',
      year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(this.now()));
    const adult = adultOnDate(input.birthDate, today), now = this.now();
    // Birth date is used only for this calculation; neither it nor its digest is persisted.
    return this.save(previous, { schema: 2, id: who.id, subject: who.subject, revision: 0,
      state: adult ? 'active' : 'under18', policy: this.policyVersion, assurance: 'self_declared',
      ageBand: adult ? 'adult' : 'under18', declaredAt: now, updatedAt: now, expiresAt: now + 30 * DAY });
  }
  async withdraw(token, input) {
    if (!exact(input, [])) fail('invalid_input');
    const who = await this.principal(token, true), previous = await this.read(who.id);
    if (!previous) fail('not_registered');
    return this.save(previous, { ...previous, state: 'withdrawn' });
  }
  async authorize(token) {
    const who = await this.principal(token), record = await this.read(who.id);
    if (!record || this.summary(record).status !== 'adult_confirmed') fail('adult_confirmation_required');
    return { accountId: who.id, revision: record.revision, assurance: 'self_declared' };
  }
}
