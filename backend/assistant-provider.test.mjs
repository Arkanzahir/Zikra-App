import test from 'node:test';
import assert from 'node:assert/strict';
import {once} from 'node:events';
import {createGateway} from './server.mjs';
import {readProviderConfig} from './config.mjs';

const env={ZIKRA_AI_PROVIDER:'cloudflare',CLOUDFLARE_ACCOUNT_ID:'a'.repeat(32),CLOUDFLARE_API_TOKEN:'synthetic-cloudflare-token',
  CLOUDFLARE_CHAT_MODEL:'@cf/qwen/qwen3.8-27b',CLOUDFLARE_VISION_MODEL:'@cf/qwen/qwen3.8-27b',ZIKRA_ASSISTANT_FREE_PLAN_CONFIRMED:'true'};
const token='synthetic-installation-access-code-32';
async function gateway(t,fetchImpl,options={}){
 const server=createGateway({...readProviderConfig(env),clientToken:token,fetchImpl,...options});
 server.listen(0,'127.0.0.1');await once(server,'listening');
 t.after(()=>new Promise(resolve=>{server.closeAllConnections();server.close(resolve);}));
 const url=`http://127.0.0.1:${server.address().port}/v1/assistant`;
 return (body={messages:[{role:'user',content:'Pesan sintetis'}]},provider='cloudflare')=>fetch(url,{method:'POST',
  headers:{'Content-Type':'application/json',Authorization:'Bearer '+token,...(provider?{'X-Zikra-Provider':provider}:{})},body:JSON.stringify(body)});
}

test('Cloudflare fails closed on missing Free confirmation, identity, model or key; no legacy fallback',()=>{
 const valid=readProviderConfig({...env,FEATHERLESS_API_KEY:'unused-old-key-123456',FEATHERLESS_MODEL:'old/model'});
 assert.equal(valid.provider,'cloudflare');assert.equal(valid.apiKey,env.CLOUDFLARE_API_TOKEN);
 for(const changes of [{ZIKRA_AI_PROVIDER:'other'},{CLOUDFLARE_ACCOUNT_ID:'https://evil.test'},{CLOUDFLARE_API_TOKEN:''},
  {CLOUDFLARE_CHAT_MODEL:'https://evil.test'},{ZIKRA_ASSISTANT_FREE_PLAN_CONFIRMED:'false'}])assert.throws(()=>readProviderConfig({...env,...changes}));
});

test('old or different-provider client cannot forward data to a new recipient or consume quota',async t=>{
 let acquired=0;
 const send=await gateway(t,()=>assert.fail('must not forward'),{accessMode:'installation',accessStore:{
  identify:async()=> 'a'.repeat(36),acquire:async()=>{acquired++;throw Error('must not acquire');}}});
 for(const provider of [null,'featherless']){
  const reply=await send(undefined,provider);assert.equal(reply.status,409);
  assert.deepEqual(await reply.json(),{error:'provider_changed',provider:'cloudflare'});
 }
 assert.equal(acquired,0);
});

test('explicit provider mismatch also blocks an operator rollback before forwarding',async t=>{
 const send=await gateway(t,()=>assert.fail('must not forward'),{provider:'featherless',model:'test/model',visionModel:undefined});
 const reply=await send();assert.equal(reply.status,409);
 assert.deepEqual(await reply.json(),{error:'provider_changed',provider:'featherless'});
});

test('Cloudflare request uses exact endpoint, inline messages, bounded low reasoning, no storage or provider fallback',async t=>{
 const send=await gateway(t,async(url,request)=>{
  assert.equal(url,`https://api.cloudflare.com/client/v4/accounts/${env.CLOUDFLARE_ACCOUNT_ID}/ai/v1/chat/completions`);
  assert.equal(request.redirect,'error');assert.equal(request.headers.Authorization,'Bearer '+env.CLOUDFLARE_API_TOKEN);
  const body=JSON.parse(request.body);assert.equal(body.model,env.CLOUDFLARE_CHAT_MODEL);
  assert.equal(body.reasoning_effort,'low');assert.equal(body.max_completion_tokens,512);assert.equal(body.store,false);
  assert.equal(body.user,undefined);assert.equal(body.metadata,undefined);assert.equal(body.messages[1].content,'Pesan sintetis');
  return Response.json({choices:[{message:{content:'Balasan sintetis'}}]});
 });
 const reply=await send();assert.equal(reply.status,200);
 assert.deepEqual(await reply.json(),{reply:'Balasan sintetis',provider:'cloudflare'});
});

test('provider daily allocation is distinct from busy capacity and upstream details never leak',async t=>{
 for(const [code,error]of [[3036,'provider_quota_exceeded'],[3040,'provider_busy']]){
  const send=await gateway(t,()=>Response.json({errors:[{code,message:'sensitive upstream details'}]},{status:429}));
  const reply=await send();assert.equal(reply.status,429);
  assert.deepEqual(await reply.json(),{error});assert.ok(Number(reply.headers.get('retry-after'))>0);
 }
});

test('daily quota and temporary concurrency preserve separate wire errors',async t=>{
 for(const [error,expected]of [['quota_exceeded','daily_quota_exceeded'],['rate_limited','rate_limited'],['try_later','try_later']]){
  const send=await gateway(t,()=>assert.fail('provider must not run'),{accessMode:'installation',accessStore:{
   identify:async()=> 'a'.repeat(36),acquire:async()=>({allowed:false,status:429,error})}});
  const reply=await send();assert.equal(reply.status,429);assert.deepEqual(await reply.json(),{error:expected});
 }
});

test('Cloudflare HTTP 200 errors cannot coexist with a displayed successful answer',async t=>{
 const send=await gateway(t,()=>Response.json({success:false,errors:[{code:5004,message:'private detail'}],
  choices:[{message:{content:'must not display'}}]}));
 const reply=await send();assert.equal(reply.status,502);assert.deepEqual(await reply.json(),{error:'provider_unavailable'});
});

test('distributed lease is released before success or provider-error response becomes visible',async t=>{
 for(const status of [200,503]){
  let held=false,releaseStarted,finishRelease;
  const started=new Promise(resolve=>{releaseStarted=resolve;});
  const gate=new Promise(resolve=>{finishRelease=resolve;});
  t.after(()=>finishRelease());
  const send=await gateway(t,()=>status===200
    ?Response.json({choices:[{message:{content:'Synthetic reply'}}]})
    :Response.json({error:'synthetic'},{status}),{accessMode:'installation',accessStore:{
      identify:async()=> 'a'.repeat(36),
      acquire:async()=>{
        if(held)return {allowed:false,status:429,error:'try_later'};
        held=true;return {allowed:true,lease:'synthetic-lease'};
      },
      release:async()=>{releaseStarted();await gate;held=false;}
    }});
  let delivered=false;
  const first=send().then(response=>{delivered=true;return response;});
  await started;await new Promise(resolve=>setTimeout(resolve,30));
  assert.equal(delivered,false,'A response must not finish while its slot is still held');
  finishRelease();const response=await first;await response.json();
  assert.equal(response.status,status);assert.equal(held,false);
  const second=await send();assert.equal(second.status,status);await second.json();
 }
});

test('rejection diagnostics distinguish own queue from upstream without request or credential data',async t=>{
 const events=[];
 const send=await gateway(t,()=>assert.fail('upstream must not run'),{onRejection:event=>events.push(event),
  accessMode:'installation',accessStore:{identify:async()=> 'a'.repeat(36),
    acquire:async()=>({allowed:false,status:429,error:'try_later'})}});
 const reply=await send();assert.equal(reply.status,429);await reply.json();
 assert.deepEqual(events,[{event:'assistant_rejected',status:429,code:'try_later'}]);
 const upstreamEvents=[];
 const busy=await gateway(t,()=>Response.json({errors:[{code:3040,message:'private upstream detail'}]},{status:429}),
  {onRejection:event=>upstreamEvents.push(event)});
 const response=await busy();assert.equal(response.status,429);await response.json();
 assert.deepEqual(upstreamEvents,[{event:'assistant_rejected',status:429,code:'provider_busy'}]);
});

test('diagnostic callback failures cannot break rejection responses',async t=>{
 const send=await gateway(t,()=>Response.json({errors:[{code:3040}]},{status:429}),
  {onRejection:()=>{throw Error('synthetic logger outage');}});
 const reply=await send();assert.equal(reply.status,429);
 assert.deepEqual(await reply.json(),{error:'provider_busy'});
});
