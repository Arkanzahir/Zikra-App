import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { once } from 'node:events';
import { AdultAccounts, adultOnDate, ADULT_POLICY } from './adult-accounts.mjs';
import { RedisAccountRepository } from './account-eligibility.mjs';
import { createAdultAccountHandler } from './account-http.mjs';
import { createGateway } from './server.mjs';
import { readAdultRuntimeConfig, vercelAccountAddress } from './adult-account-runtime.mjs';
import { createVercelHandler } from './vercel-handler.mjs';
import { redisFixture } from './test-support/redis-fixture.mjs';

function fixture(repository) {
  let now=Date.parse('2026-11-15T05:00:00Z');const data=new Map(),disabled=new Set();
  const identity={verify:async(token,recent)=>{
    if(!['adult','minor','other'].includes(token)||disabled.has(token))throw Error('unauthorized');
    return {subject:token};
  }};
  const repo=repository??{read:async id=>structuredClone(data.get(id)??null),compareAndSet:async(id,rev,r)=>{
    if((data.get(id)?.revision??0)!==rev)return false;data.set(id,structuredClone(r));return true;
  }};
  const service=new AdultAccounts({identity,repository:repo,subjectKey:'a'.repeat(64),now:()=>now});
  const body=(birthDate='2008-11-15')=>({birthDate,accepted:true,policyVersion:ADULT_POLICY});
  return {service,data,disabled,body,advance:ms=>now+=ms};
}

test('adult date boundary, leap years and strict calendar validation',()=>{
  assert.equal(adultOnDate('2008-11-15','2026-11-15'),true);
  assert.equal(adultOnDate('2008-11-16','2026-11-15'),false);
  assert.equal(adultOnDate('2008-02-29','2026-02-28'),false);
  assert.equal(adultOnDate('2008-02-29','2026-03-01'),true);
  for(const date of ['2008-02-30','2027-01-01','1899-12-31','15-11-2000','2000-1-01','not-date'])
    assert.throws(()=>adultOnDate(date,'2026-11-15'),/invalid_input/);
});
test('email identity is required and clients cannot supply age, subject or assurance',async()=>{
  const f=fixture();
  await assert.rejects(f.service.register('fake',f.body()),/unauthorized/);
  for(const extra of [{age:18},{subject:'adult'},{assurance:'verified'},{accepted:false}])
    await assert.rejects(f.service.register('adult',{...f.body(),...extra}),/invalid_input/);
  await assert.rejects(f.service.register('adult',{...f.body(),policyVersion:'old'}),/policy_changed/);
});
test('adult declaration stores no birth date, digest, email, password or bearer and is explicitly self-declared',async()=>{
  const f=fixture(),result=await f.service.register('adult',f.body('1999-05-12'));
  assert.equal(result.status,'adult_confirmed');assert.equal(result.assurance,'self_declared');
  const record=[...f.data.values()][0],raw=JSON.stringify(record);
  for(const value of ['1999-05-12','birthDate','email','password','token','guardian'])assert.ok(!raw.includes(value));
  assert.equal((await f.service.authorize('adult')).assurance,'self_declared');
  assert.equal((await f.service.status('other')).status,'not_registered');
});
test('under18 remains blocked across retries, withdrawal and attempted adult renewal',async()=>{
  const f=fixture();assert.equal((await f.service.register('minor',f.body('2010-01-01'))).status,'under18_blocked');
  await assert.rejects(f.service.authorize('minor'),/adult_confirmation_required/);
  await assert.rejects(f.service.register('minor',f.body('1990-01-01')),/under18_blocked/);
  await f.service.withdraw('minor',{});
  await assert.rejects(f.service.renew('minor',f.body('1990-01-01')),/under18_blocked/);
});
test('expiration, withdrawal, policy changes and revoked email sessions deny online immediately',async()=>{
  for(const action of ['expired','withdrawn','disabled','policy']){
    const f=fixture();await f.service.register('adult',f.body());
    if(action==='expired')f.advance(30*86400000);
    if(action==='withdrawn')await f.service.withdraw('adult',{});
    if(action==='disabled')f.disabled.add('adult');
    if(action==='policy') [...f.data.values()][0].policy='old';
    await assert.rejects(f.service.authorize('adult'));
  }
});
test('renewal is explicit, current-policy and reauthenticated; corrupt records fail closed',async()=>{
  const f=fixture();await assert.rejects(f.service.renew('adult',f.body()),/not_registered/);
  await f.service.register('adult',f.body());await assert.rejects(f.service.register('adult',f.body()),/account_exists/);
  await f.service.withdraw('adult',{});f.advance(1000);
  assert.equal((await f.service.renew('adult',f.body())).status,'adult_confirmed');
  [...f.data.values()][0].assurance='verified';
  await assert.rejects(f.service.authorize('adult'),/invalid_account/);
});
test('Redis adult registration CAS has one winner and records expire', {skip:!process.env.ZIKRA_REDIS_TEST_PYTHON},async t=>{
  const redis=await redisFixture(t),repo=new RedisAccountRepository(redis,'synthetic:adult:');const f=fixture(repo);
  const both=await Promise.allSettled([f.service.register('adult',f.body()),f.service.register('adult',f.body())]);
  assert.equal(both.filter(x=>x.status==='fulfilled').length,1);
  const id=(await f.service.authorize('adult')).accountId;
  assert.ok(await redis.command(['TTL',repo.key(id)])>0);
  assert.equal((await repo.read(id)).schema,2);
});
test('adult HTTP surface has no guardian routes and accepts no query credentials',async t=>{
  const calls=[];const service=Object.fromEntries(['status','register','renew','withdraw'].map(name=>[name,async()=>{calls.push(name);return{status:name};}]));
  const server=http.createServer(createAdultAccountHandler({service,beforeAuth:async()=>true}));
  server.listen(0,'127.0.0.1');await once(server,'listening');t.after(()=>{server.closeAllConnections();server.close();});
  const base=`http://127.0.0.1:${server.address().port}`,headers={authorization:'Bearer synthetic-32-character-account-token','content-type':'application/json'};
  for(const [path,method,body] of [['','GET',undefined],['/declare','POST',{}],['/renew','POST',{}],['/withdraw','POST',{}]]){
    const response=await fetch(base+'/v1/adult-account'+path,{method,headers,...(body?{body:JSON.stringify(body)}:{})});
    assert.equal(response.status,200);assert.equal(response.headers.get('cache-control'),'no-store');
  }
  for(const path of ['/v1/account/guardian/request','/v1/adult-account?token=secret'])
    assert.equal((await fetch(base+path,{headers})).status,404);
  assert.deepEqual(calls,['status','register','renew','withdraw']);
});

async function gateway(t,gate,upstream=async()=>new Response(JSON.stringify({choices:[{message:{content:'Synthetic answer'}}]})),extra={}){
  const server=createGateway({apiKey:'synthetic-server-key',model:'test/model',clientToken:'synthetic-installation-key-32-characters',adultAccessGate:gate,fetchImpl:upstream,...extra});
  server.listen(0,'127.0.0.1');await once(server,'listening');t.after(()=>{server.closeAllConnections();server.close();});
  return (path='/v1/assistant',account='synthetic-firebase-token-at-least-32-characters')=>fetch(`http://127.0.0.1:${server.address().port}`+path,{method:path==='/v1/capabilities'?'GET':'POST',headers:{authorization:'Bearer synthetic-installation-key-32-characters','content-type':'application/json',...(account?{'x-zikra-account-token':account}:{})},...(path==='/v1/capabilities'?{}:{body:JSON.stringify({messages:[{role:'user',content:'Synthetic'}]})})});
}
const principal={accountId:'a'.repeat(64),revision:1};
test('adult gate protects chat/image/report before provider or report storage; installation alone cannot bypass',async t=>{
  for(const path of ['/v1/assistant','/v1/images','/v1/answer-reports']){
    const send=await gateway(t,async()=>{throw Error('adult_confirmation_required');},()=>assert.fail('provider'),{imageService:{generate:()=>assert.fail('image')},reportAnswer:()=>assert.fail('report')});
    assert.equal((await send(path)).status,403);assert.equal((await send(path,null)).status,401);
  }
});
test('adult gate rechecks immediately before provider and discards late responses after revocation',async t=>{
  for(const revokeAt of [2,3]){
    let checks=0,calls=0;const send=await gateway(t,async()=>{if(++checks>=revokeAt)throw Error('adult_confirmation_required');return principal;},async()=>{calls++;return new Response(JSON.stringify({choices:[{message:{content:'Must not be shown after withdrawal'}}]}));});
    const response=await send();assert.equal(response.status,403);assert.equal(calls,revokeAt===2?0:1);
    assert.deepEqual(await response.json(),{error:'adult_confirmation_required'});
  }
});
test('changed identity/revision and gate outages never return online content',async t=>{
  for(const failure of ['identity','revision','outage']){
    let checks=0;const send=await gateway(t,async()=>{
      if(++checks===1)return principal;
      if(failure==='outage')throw Error('private-admin-details');
      return {...principal,...(failure==='identity'?{accountId:'b'.repeat(64)}:{revision:2})};
    },()=>assert.fail('provider'));
    const response=await send();assert.equal(response.status,failure==='outage'?503:403);
    assert.ok(!(await response.text()).includes('private-admin-details'));
  }
});
test('capabilities disclose the 18+ requirement without sending account credentials upstream',async t=>{
  const send=await gateway(t,()=>assert.fail('not needed for capabilities'));
  const response=await send('/v1/capabilities',null);assert.equal(response.status,200);
  const data=await response.json();assert.equal(data.accountRequired,true);assert.equal(data.minimumAge,18);
});

test('hosting activation fails closed without a configured account identity service',()=>{
 const env={FEATHERLESS_API_KEY:'synthetic-server-only-key',FEATHERLESS_MODEL:'test/model',UPSTASH_REDIS_REST_URL:'https://synthetic.upstash.io',UPSTASH_REDIS_REST_TOKEN:'synthetic-redis-token',ZIKRA_ADULT_ACCOUNTS_ENABLED:'true',ZIKRA_ADULT_ACCOUNTS_REQUIRED:'true'};
 assert.throws(()=>createVercelHandler(env),/Adult account identity service required/);
 assert.throws(()=>createVercelHandler({...env,ZIKRA_ADULT_ACCOUNTS_REQUIRED:'yes'}),/Invalid adult account activation/);
});

test('production account runtime cannot accept emulator, messaging sender or caller-spoofed identity routing',()=>{
 assert.throws(()=>readAdultRuntimeConfig({}),/account_runtime_unavailable/);
 assert.throws(()=>readAdultRuntimeConfig({ZIKRA_ADULT_ACCOUNTS_ENABLED:'true',ZIKRA_ADULT_ACCOUNTS_REQUIRED:'true',FIREBASE_AUTH_EMULATOR_HOST:'127.0.0.1:9099'}));
 const env={ZIKRA_ADULT_ACCOUNTS_ENABLED:'true',ZIKRA_ADULT_ACCOUNTS_REQUIRED:'true',ZIKRA_ACCOUNT_SUBJECT_KEY:'a'.repeat(64),ZIKRA_AUTH_SERVICE_ACCOUNT_JSON:JSON.stringify({type:'service_account',project_id:'zikra-79b75',client_email:'zikra-earthquake-push@zikra-79b75.iam.gserviceaccount.com',private_key:'-----BEGIN PRIVATE KEY-----\nsynthetic'})};
 assert.throws(()=>readAdultRuntimeConfig(env));
 assert.throws(()=>vercelAccountAddress({}, {headers:{'x-vercel-forwarded-for':'127.0.0.1'}}));
 assert.throws(()=>vercelAccountAddress({VERCEL:'1'}, {headers:{'x-forwarded-for':'127.0.0.1'}}));
 assert.throws(()=>vercelAccountAddress({VERCEL:'1'}, {headers:{'x-vercel-forwarded-for':'127.0.0.1, evil'}}));
 assert.equal(vercelAccountAddress({VERCEL:'1'}, {headers:{'x-vercel-forwarded-for':'127.0.0.1'}}),'127.0.0.1');
});

test('verified account header never enters the upstream model request',async t=>{
 const firebaseToken='synthetic-firebase-token-at-least-32-characters';
 let invoked=false;
 const send=await gateway(t,async token=>{assert.equal(token,firebaseToken);return principal;},async(url,options)=>{
  invoked=true;assert.ok(!JSON.stringify(options).includes(firebaseToken));
  assert.equal(options.headers.Authorization,'Bearer synthetic-server-key');
  return new Response(JSON.stringify({choices:[{message:{content:'Synthetic answer'}}]}));
 });
 const response=await send();assert.equal(response.status,200);assert.equal(invoked,true);
});
test('withdrawal before report storage or after image generation denies the result',async t=>{
 for(const path of ['/v1/answer-reports','/v1/images']){
  let checks=0,generated=0;
  const send=await gateway(t,async()=>{if(++checks>=(path.endsWith('images')?3:2))throw Error('adult_confirmation_required');return principal;},()=>assert.fail('chat'),{
   reportAnswer:()=>assert.fail('report must not be stored'),
   imageService:{generate:async()=>{generated++;return{image:'synthetic'};}}
  });
  const response=await send(path);assert.equal(response.status,403);assert.equal(generated,path.endsWith('images')?1:0);
 }
});
