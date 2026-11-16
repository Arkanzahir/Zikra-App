# AkunAI18+ aktif sebagai pekerjaan —16November2026

Kontrak terbaru: [status/gap](../../docs/beta/CURRENT_STATUS.md), [bukti](../../android/validation/adult-accounts-2026-11-15/README.md). Source58 mempunyai AdultAccounts, runtimefirebase-admin14.5.1, route/v1/adult-account serta interceptorAndroid.175backend/237JVM/5native demo lulus. Belum deploy akun keproduksi atau signedAPK58; releaseflagsfalse. FirebaseAuthreader role sudahdibuat, filekeybelumdidapat. DeklarasiDOB self_declared bukan assuranceidentitas; noDOBpersist, metadata90hari sejakperubahan, konfirmasi30hari. ENABLED dahulu, REQUIRED hanya setelahAPKsiap. Alurwali di bawah **historis/ditunda**, jangan mengaktifkannya untukbeta18+ tanpa keputusanbaru.

---

# Self-service accounts: integration contract

Status: local HTTP integration and internal Android authentication implemented. Firebase Email/Password is enabled on the approved Spark project; account/age/guardian endpoints are not deployed publicly.
The existing signed APK1.9.55 on the phone and installation gateway are unchanged. Source/internal debug is1.9.56; release account flags remain false. This is not a
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
real production email delivery/revocation E2E is not established. Android SDK signup, verification, login/logout and password reset are tested with the official Firebase Auth emulator and a demo project. Lua concurrency is run locally
with the project's existing Redis fixture. The tests created no account data or email in production. The user approved activation of the real Email/Password provider on Spark and separately approved Require/minimum12password policy; both settings were visibly verified. No billing upgrade or new server credentials/roles were activated. The modules are intentionally
absent from the Vercel deployment allowlist until integration is complete.

## Required next integration

1. Select a proportionate, supported age/guardian assurance method and review the
   service risk/privacy requirements. A verified email proves address control; it
   does not prove age or parental responsibility. Do not substitute a checkbox or
   a unit-test adapter. No identity uploads are authorized by this implementation.
2. The client provider is enabled, but still initialize official Firebase Admin Auth with a dedicated least-privilege account
   after authorization; enable the chosen sign-in provider, delivery and abuse
   controls. Do not expand the FCM sender's permissions silently. Never accept
   client-supplied `subject`, `band` or guardian receipt as trusted proof.
3. HTTP handler and internal Android sign-in screens are implemented. Wire them to hosting and real trusted proof adapters, and build the guardian completion surface. Consume one-time secrets via
   request bodies, not server-logged URL query parameters. The implemented Redis pre-auth gate requires a trusted hosting address resolver and uses20/IP/minute,500global/minute and10,000global/day. Do not trust a caller-supplied forwarding header. Account guardian challenges have their own3/day limit.
4. Gate all account-backed AI/chat/image/report/content requests using fresh
   eligibility. Do not fall back to an installation token if an account is denied.
   Re-check queued work before provider submission; discard late replies after
   withdrawal. Test revocation across processes and account deletion/erasure.
5. Provide accessible renewal, consent history, guardian withdrawal and account
   deletion. Duplicate registration remains rejected; a separate reauthenticated renewal with a fresh proof now exists and invalidates old guardian linkage. The UI for renewal/deletion and complete account erasure are still missing. Existing local history
   does not automatically upload or become an account backup.
6. Test real email/guardian/Android flows with authorized test accounts, document
   retention and recovery, then review public activation. HOLD remains in effect.

Primary references: [Firebase token verification](https://firebase.google.com/docs/auth/admin/verify-id-tokens),
[Firebase session management](https://firebase.google.com/docs/auth/admin/manage-sessions).

## HTTP and Android test scope

`account-http.mjs` exposes only the handler factory; it is not in Vercel rewrites or the deploy allowlist. Requests are bounded16KB/8seconds, require bearer authentication and no-store responses; errors are allowlisted. `account-abuse.mjs` uses atomic Redis limits and a keyed address digest. `status` is owner-scoped. Renewal requires fresh trusted evidence/current policy and invalidates old guardian linkage. Full backend run158passed/0skip.

Android debug flags `zikraAccountSignIn` and `zikraAccountEmulator` defaultfalse; both are forciblyfalse for release. The local emulator branch uses a named Firebase app with project `demo-zikra-accounts`, so it does not reuse production messaging configuration. Debug-only network rules allow HTTP solely to10.0.2.2 for the emulator; release does not include those rules. Never distribute this debug emulator APK as the beta release. Account screens use FLAG_SECURE, do not save password view state, clear passwords on stop and show the signed-in email. Firebase owns its private session persistence; that session is excluded from Zikra local migration by its explicit allowlist. No account login implies AI eligibility or upload of local chat/task data.
