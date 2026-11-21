import { isIP } from 'node:net';
import { initializeApp, getApps, cert } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { createFirebaseIdentity } from './firebase-identity.mjs';
import { AdultAccounts } from './adult-accounts.mjs';
import { RedisAccountRepository } from './account-eligibility.mjs';
import { createAccountPreAuth } from './account-abuse.mjs';
import { createAdultAccountHandler } from './account-http.mjs';
import { RedisRest, readRedisConfig } from './redis-access.mjs';

export function readAdultRuntimeConfig(env) {
  if(env.ZIKRA_ADULT_ACCOUNTS_ENABLED!=='true'||env.FIREBASE_AUTH_EMULATOR_HOST
      ||env.NODE_TLS_REJECT_UNAUTHORIZED==='0')throw Error('account_runtime_unavailable');
  const subjectKey=env.ZIKRA_ACCOUNT_SUBJECT_KEY;
  if(!/^[a-f0-9]{64}$/.test(subjectKey??''))throw Error('account_runtime_unavailable');
  let credential;
  try{credential=JSON.parse(env.ZIKRA_AUTH_SERVICE_ACCOUNT_JSON);}catch{throw Error('account_runtime_unavailable');}
  if(credential?.type!=='service_account'||credential.project_id!=='zikra-79b75'
      ||!/^zikra-auth-reader(?:-[a-z0-9]+)?@zikra-79b75\.iam\.gserviceaccount\.com$/.test(credential.client_email??'')
      ||typeof credential.private_key!=='string'||!credential.private_key.startsWith('-----BEGIN PRIVATE KEY-----\n'))
    throw Error('account_runtime_unavailable');
  return {subjectKey,credential,projectId:credential.project_id};
}

export function vercelAccountAddress(env,req) {
  if(env.VERCEL!=='1')throw Error('access_unavailable');
  // Vercel injects this at its edge. Never use this adapter on a directly exposed Node server.
  const address=req.headers['x-vercel-forwarded-for'];
  if(typeof address!=='string'||!isIP(address))throw Error('access_unavailable');
  return address;
}

export function createAdultServices({auth,redis,prefix,subjectKey,projectId,trustedAddress,now=Date.now}) {
  const identity=createFirebaseIdentity(auth,{projectId,now});
  const repository=new RedisAccountRepository(redis,prefix+'adult:');
  const service=new AdultAccounts({identity,repository,subjectKey,now});
  const beforeAuth=createAccountPreAuth({redis,prefix:prefix+'adult:',addressKey:subjectKey,trustedAddress});
  return {service,beforeAuth,handler:createAdultAccountHandler({service,beforeAuth})};
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
    subjectKey:config.subjectKey,projectId:config.projectId,trustedAddress:req=>vercelAccountAddress(env,req)});
}
