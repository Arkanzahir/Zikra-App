import { createPrivateKey, sign } from 'node:crypto';
import { boundedJson, MAX_ALERT_AGE_MS } from './earthquake-feed.mjs';

export function readPushConfig(env = process.env) {
  let account;
  try { account = JSON.parse(env.ZIKRA_FIREBASE_SERVICE_ACCOUNT_JSON); } catch { throw new Error('push_not_configured'); }
  if (account.type !== 'service_account' || !/^[a-z][a-z0-9-]{4,61}[a-z0-9]$/.test(account.project_id ?? '')
      || !/^[a-zA-Z0-9._-]+@[a-z0-9-]+\.iam\.gserviceaccount\.com$/.test(account.client_email ?? '')
      || typeof account.private_key !== 'string' || account.private_key.length > 10000) throw new Error('push_not_configured');
  const key = createPrivateKey(account.private_key);
  if (key.asymmetricKeyType !== 'rsa' || key.asymmetricKeyDetails.modulusLength < 2048) throw new Error('push_not_configured');
  const topic = env.ZIKRA_QUAKE_TOPIC ?? 'zikra-earthquake-beta-v1';
  if (!['zikra-earthquake-beta-v1', 'zikra-earthquake-v1'].includes(topic)) throw new Error('invalid_push_topic');
  return { project: account.project_id, email: account.client_email, key, topic };
}

export function createFcmSender(config, { fetchImpl = fetch, clock = Date.now } = {}) {
  let cached;
  async function accessToken() {
    const now = clock();
    if (cached && cached.until > now + 60000) return cached.token;
    const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
    const unsigned = encode({ alg: 'RS256', typ: 'JWT' }) + '.' + encode({ iss: config.email,
      scope: 'https://www.googleapis.com/auth/firebase.messaging', aud: 'https://oauth2.googleapis.com/token',
      iat: Math.floor(now / 1000), exp: Math.floor(now / 1000) + 3600 });
    const assertion = unsigned + '.' + sign('RSA-SHA256', Buffer.from(unsigned), config.key).toString('base64url');
    const body = new URLSearchParams({ grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer', assertion });
    const value = JSON.parse(await boundedJson(await fetchImpl('https://oauth2.googleapis.com/token',
      { method: 'POST', redirect: 'error', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body,
        signal: AbortSignal.timeout(8000) }), 16384));
    if (typeof value.access_token !== 'string' || !/^[A-Za-z0-9._~+\/-]{16,4096}$/.test(value.access_token)
        || !Number.isInteger(value.expires_in) || value.expires_in < 60 || value.expires_in > 3600) throw new Error('push_auth_unavailable');
    cached = { token: value.access_token, until: now + value.expires_in * 1000 }; return cached.token;
  }
  const send = async event => {
    const token = await accessToken();
    const ttl = Math.min(900, Math.floor((event.at + MAX_ALERT_AGE_MS - clock()) / 1000));
    if (ttl < 1) throw new Error('expired_push');
    const message = { topic: config.topic, data: { schema: '1', event_id: event.id, quake_report: event.snapshot },
      android: { priority: 'HIGH', ttl: `${ttl}s`, restricted_package_name: 'com.zikra.app' } };
    // Once this request starts, a timeout is ambiguous: the caller must not resend it automatically.
    const response = await fetchImpl(`https://fcm.googleapis.com/v1/projects/${config.project}/messages:send`,
      { method: 'POST', redirect: 'error', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ message }), signal: AbortSignal.timeout(10000) });
    if (!response.ok) {
      await response.body?.cancel();
      throw Object.assign(new Error('push_rejected'), { notAccepted: response.status >= 400 && response.status < 500 });
    }
    const value = JSON.parse(await boundedJson(response, 16384));
    if (typeof value.name !== 'string' || !value.name.startsWith(`projects/${config.project}/messages/`)) throw new Error('invalid_push_receipt');
    return value.name;
  };
  send.prepare = accessToken;
  return send;
}
