# Self-service accounts: integration contract

Status: local foundation only; no public registration endpoint or Firebase provider activated.
The existing signed APK1.9.55 and installation gateway are unchanged. This is not a
complete age/guardian flow or a determination of legal compliance.

## Implemented and tested

`firebase-identity.mjs` accepts an official Firebase Admin Auth instance. It calls
`verifyIdToken(token, true)` for revocation checks; requires the configured project,
verified email and valid expiry. Sensitive changes require authentication within
five minutes. Guardian eligibility also checks the parent's current disabled/email
state and `tokensValidAfterTime` against the recorded grant.

`account-eligibility.mjs` keeps account state outside Android backup. It requires
trusted age and guardian-proof adapters; no client age boolean is accepted. Missing,
unknown, under13, expired, withdrawn or old-policy eligibility is denied. A teen
requires a separate eligible adult and a proof bound to both subjects, request,
decision and policy. Challenges expire after at most24hours; replacement invalidates
the preceding grant/challenge. Three requests per account per UTCday. A challenge
is consumed once with Redis version-checked Lua. Current account/guardian state
must be checked for each protected request, not just at login or token issue.

## Data and explicit limits

The stored record contains a keyed pseudonymous ID, Firebase subject ID, age band,
proof validity times, policy version, revision, and optional guardian/challenge
state. It stores no email, date of birth, identity photo, raw proof or bearer token.
The one-time challenge secret is returned once; only its hash is stored.
Redis records have a90day expiry from the latest mutation. This is a provisional
implementation limit, **not** an approved privacy promise or renewal policy. Proof
validity is independently limited to30days; expired records cannot grant access.
Raw Firebase identifiers remain personal data despite keyed Redis names.

Tests use synthetic proof/identity adapters, not a real age verification provider.
The Firebase Admin adapter is contract-tested with a mock official Auth instance;
no real signup/email/revocation E2E is established. Lua concurrency is run locally
with the project's existing Redis fixture. No account data or email was created in
production; no new privileges or costs were activated. The modules are intentionally
absent from the Vercel deployment allowlist until integration is complete.

## Required next integration

1. Select a proportionate, supported age/guardian assurance method and review the
   service risk/privacy requirements. A verified email proves address control; it
   does not prove age or parental responsibility. Do not substitute a checkbox or
   a unit-test adapter. No identity uploads are authorized by this implementation.
2. Initialize official Firebase Admin Auth with a dedicated least-privilege account
   after authorization; enable the chosen sign-in provider, delivery and abuse
   controls. Do not expand the FCM sender's permissions silently. Never accept
   client-supplied `subject`, `band` or guardian receipt as trusted proof.
3. Add bounded authenticated enrollment/withdrawal endpoints and Android account
   screens, plus the guardian completion surface. Consume one-time secrets via
   request bodies, not server-logged URL query parameters. Add account-level and
   pre-auth abuse limits before exposing endpoints.
4. Gate all account-backed AI/chat/image/report/content requests using fresh
   eligibility. Do not fall back to an installation token if an account is denied.
   Re-check queued work before provider submission; discard late replies after
   withdrawal. Test revocation across processes and account deletion/erasure.
5. Provide accessible renewal, consent history, guardian withdrawal and account
   deletion. Current foundation deliberately rejects duplicate registration after
   withdrawal; re-consent/renewal is not yet implemented. Existing local history
   does not automatically upload or become an account backup.
6. Test real email/guardian/Android flows with authorized test accounts, document
   retention and recovery, then review public activation. HOLD remains in effect.

Primary references: [Firebase token verification](https://firebase.google.com/docs/auth/admin/verify-id-tokens),
[Firebase session management](https://firebase.google.com/docs/auth/admin/manage-sessions).
