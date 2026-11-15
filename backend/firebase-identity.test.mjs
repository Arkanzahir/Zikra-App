import test from 'node:test';
import assert from 'node:assert/strict';
import { createFirebaseIdentity } from './firebase-identity.mjs';

const now = 1800000000000, token = 'synthetic-identity-token-for-tests-only';
const claims = { uid: 'synthetic-owner', sub: 'synthetic-owner', aud: 'zikra-testing',
  iss: 'https://securetoken.google.com/zikra-testing', email_verified: true,
  exp: now / 1000 + 1000, auth_time: now / 1000 - 30 };
function fixture(change = {}) {
  const calls = [];
  const auth = {
    verifyIdToken: async (...args) => { calls.push(args); return { ...claims, ...change }; },
    getUser: async uid => ({ uid, disabled: false, emailVerified: true,
      tokensValidAfterTime: new Date(now - 60000).toUTCString() })
  };
  return { auth, calls, identity: createFirebaseIdentity(auth, { projectId: 'zikra-testing', now: () => now }) };
}
test('Firebase identity uses Admin revocation checks and exposes no email/JWT', async () => {
  const { identity, calls } = fixture();
  assert.deepEqual(await identity.verify(token, true), { subject: 'synthetic-owner' });
  assert.deepEqual(calls, [[token, true]]);
  await identity.assertActive('synthetic-owner', now);
});
test('Firebase identity rejects wrong project, unverified email, expiry and stale reauthentication', async () => {
  for (const change of [{ aud: 'other' }, { iss: 'other' }, { sub: 'other' }, { uid: '' },
    { email_verified: false }, { exp: now / 1000 }, { auth_time: now / 1000 + 1 },
    { auth_time: now / 1000 - 301 }, { auth_time: undefined }]) {
    await assert.rejects(fixture(change).identity.verify(token, true), /unauthorized/);
  }
  await fixture({auth_time: now / 1000 - 1000}).identity.verify(token);
  await assert.rejects(fixture().identity.verify('short'), /unauthorized/);
  const f = fixture(); f.auth.verifyIdToken = async () => { throw new Error('provider-private-error'); };
  await assert.rejects(f.identity.verify(token), error => error.message === 'unauthorized');
});
test('guardian account disablement, email change and session revocation invalidate an old grant', async () => {
  for (const change of [{ disabled: true }, { emailVerified: false }, { uid: 'other' },
    { tokensValidAfterTime: new Date(now + 10000).toUTCString() }, { tokensValidAfterTime: undefined }]) {
    const f = fixture(), original = f.auth.getUser;
    f.auth.getUser = async uid => ({ ...await original(uid), ...change });
    await assert.rejects(f.identity.assertActive('synthetic-owner', now), /unauthorized/);
  }
});
