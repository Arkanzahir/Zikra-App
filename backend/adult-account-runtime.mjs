import { isIP } from 'node:net';
import { initializeApp, getApps, cert } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { createFirebaseIdentity } from './firebase-identity.mjs';
import { AdultAccounts, DELETION_MARKER } from './adult-accounts.mjs';
import { RedisAccountRepository } from './account-eligibility.mjs';
import { createAccountPreAuth } from './account-abuse.mjs';
import { createAdultAccountHandler } from './account-http.mjs';
import { RedisRest, readRedisConfig, adultRecordKey } from './redis-access.mjs';

const PRODUCTION_PROJECT = 'zikra-79b75';
const PROJECT_ID = /^[a-z][a-z0-9-]{4,28}[a-z0-9]$/;

function readServiceAccount(raw, projectId, accountName) {
  let credential;
  try { credential = JSON.parse(raw); } catch { return undefined; }
  const [name, domain, extra] = typeof credential?.client_email === 'string'
    ? credential.client_email.split('@') : [];
  if (credential?.type !== 'service_account' || credential.project_id !== projectId
      || extra !== undefined || domain !== `${projectId}.iam.gserviceaccount.com`
      || !(name === accountName || name?.startsWith(accountName + '-') && /^[a-z0-9]+$/.test(name.slice(accountName.length + 1)))
      || typeof credential.private_key !== 'string'
      || !credential.private_key.startsWith('-----BEGIN PRIVATE KEY-----\n')) return undefined;
  return credential;
}

export function readAdultRuntimeConfig(env) {
  if(env.ZIKRA_ADULT_ACCOUNTS_ENABLED!=='true'||env.FIREBASE_AUTH_EMULATOR_HOST
      ||env.NODE_TLS_REJECT_UNAUTHORIZED==='0')throw Error('account_runtime_unavailable');
  const subjectKey=env.ZIKRA_ACCOUNT_SUBJECT_KEY;
  if(!/^[a-f0-9]{64}$/.test(subjectKey??''))throw Error('account_runtime_unavailable');
  const previewProject = env.ZIKRA_PREVIEW_FIREBASE_PROJECT_ID;
  const isPreview = env.VERCEL === '1' && env.VERCEL_ENV === 'preview';
  if (isPreview ? !PROJECT_ID.test(previewProject ?? '') || previewProject === PRODUCTION_PROJECT
    : previewProject !== undefined) throw Error('account_runtime_unavailable');
  const projectId = isPreview ? previewProject : PRODUCTION_PROJECT;
  const credential = readServiceAccount(env.ZIKRA_AUTH_SERVICE_ACCOUNT_JSON, projectId, 'zikra-auth-reader');
  if (!credential)
    throw Error('account_runtime_unavailable');
  return {subjectKey,credential,projectId};
}

export function vercelAccountAddress(env,req) {
  if(env.VERCEL!=='1')throw Error('access_unavailable');
  // Vercel injects this at its edge. Never use this adapter on a directly exposed Node server.
  const address=req.headers['x-vercel-forwarded-for'];
  if(typeof address!=='string'||!isIP(address))throw Error('access_unavailable');
  return address;
}

// The marker replaces the account record atomically, blocking stale requests and CAS writes.
const BEGIN_DELETION = `
redis.call('SET', KEYS[1], ARGV[1], 'EX', 86400)
redis.call('DEL', KEYS[2], KEYS[3])
return 1`;

export function createAdultServices({auth,redis,prefix,subjectKey,projectId,trustedAddress,deleteUser,now=Date.now}) {
  const identity=createFirebaseIdentity(auth,{projectId,now});
  const repository=new RedisAccountRepository(redis,prefix+'adult:');
  const eraseAccount = deleteUser && (async (id, subject) => {
    const result = await redis.command(['EVAL', BEGIN_DELETION, 3, adultRecordKey(prefix, id),
      prefix + 'usage:' + id, prefix + 'report-rate:' + id, JSON.stringify(DELETION_MARKER)]);
    if (result !== 1) throw Error('account_deletion_unavailable');
    try { await deleteUser(subject); }
    catch (error) {
      if (error?.code !== 'auth/user-not-found') throw Error('account_deletion_unavailable');
    }
  });
  const service=new AdultAccounts({identity,repository,subjectKey,eraseAccount,now});
  const beforeAuth=createAccountPreAuth({redis,prefix:prefix+'adult:',addressKey:subjectKey,trustedAddress});
  return {service,beforeAuth,handler:createAdultAccountHandler({service,beforeAuth})};
}
function deletionAdapter(env, projectId) {
  if (env.ZIKRA_ADULT_ACCOUNT_DELETION_ENABLED !== 'true') return undefined;
  const credential = readServiceAccount(env.ZIKRA_AUTH_DELETER_SERVICE_ACCOUNT_JSON, projectId, 'zikra-auth-deleter');
  if (!credential) return undefined;
  try {
    const name = 'zikra-adult-account-deleter';
    const app = getApps().find(app => app.name === name)
      ?? initializeApp({ projectId, credential: cert(credential) }, name);
    if (app.options.projectId !== projectId) return undefined;
    return subject => getAuth(app).deleteUser(subject);
  } catch { return undefined; }
}

let runtime;
export function getAdultRuntime(env=process.env) {
  const config=readAdultRuntimeConfig(env);
  if(runtime)return runtime;
  const redisConfig=readRedisConfig(env);
  const name='zikra-adult-account-reader';
  const app=getApps().find(app=>app.name===name)??initializeApp({projectId:config.projectId,credential:cert(config.credential)},name);
  if(app.options.projectId!==config.projectId)throw Error('account_runtime_unavailable');
  return runtime=createAdultServices({auth:getAuth(app),redis:new RedisRest(redisConfig),prefix:redisConfig.prefix,
    subjectKey:config.subjectKey,projectId:config.projectId,trustedAddress:req=>vercelAccountAddress(env,req),deleteUser:deletionAdapter(env,config.projectId)});
}
