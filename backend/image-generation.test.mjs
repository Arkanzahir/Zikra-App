import test from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {deflateSync} from 'node:zlib';
import {readImageConfig,validateImageRequest,validatedImage,createImageService,IMAGE_MODEL,ImageError} from './image-generation.mjs';
import {createGateway} from './server.mjs';
const config={account:'a'.repeat(32),token:'synthetic-cloudflare-token-for-testing'};
function png(){
 const crc=data=>{let c=0xffffffff;for(const v of data){c^=v;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return(c^0xffffffff)>>>0;};
 const chunk=(type,data)=>{const b=Buffer.concat([Buffer.from(type),data]),size=Buffer.alloc(4),sum=Buffer.alloc(4);size.writeUInt32BE(data.length);sum.writeUInt32BE(crc(b));return Buffer.concat([size,b,sum]);};
 const header=Buffer.alloc(13);header.writeUInt32BE(64,0);header.writeUInt32BE(64,4);header[8]=8;header[9]=2;
 return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',deflateSync(Buffer.alloc(64*(64*3+1),0))),chunk('IEND',Buffer.alloc(0))]).toString('base64');
}
const image=png(),body=()=>({prompt:'A synthetic watercolor landscape',requestId:randomUUID()});
test('image configuration is opt-in, requires Free-plan confirmation, rejects unsafe credentials',()=>{
 assert.equal(readImageConfig({}),null);
 const env={ZIKRA_IMAGES_ENABLED:'true',ZIKRA_IMAGES_FREE_PLAN_CONFIRMED:'true',CLOUDFLARE_ACCOUNT_ID:config.account,CLOUDFLARE_API_TOKEN:config.token};
 assert.deepEqual(readImageConfig(env),config);
 for(const patch of [{ZIKRA_IMAGES_FREE_PLAN_CONFIRMED:'false'},{CLOUDFLARE_ACCOUNT_ID:'../bad'},{CLOUDFLARE_API_TOKEN:'bad\r\nheader'}])assert.throws(()=>readImageConfig({...env,...patch}));
});
test('only an explicit prompt and a fresh UUID can reach image inference',()=>{
 assert.equal(validateImageRequest(body()).prompt,'A synthetic watercolor landscape');
 for(const patch of [{prompt:''},{prompt:'a'.repeat(1001)},{requestId:'bad'},{attachments:[]},{context:{name:'private'}},{url:'https://example.test/image'}])assert.throws(()=>validateImageRequest({...body(),...patch}),ImageError);
});
test('image output is bounded canonical base64 with fixed provider and safe dimensions',()=>{
 const parsed=validatedImage(image);assert.equal(parsed.width,64);assert.equal(parsed.mime,'image/png');assert.equal(parsed.provider,'cloudflare');
 for(const bad of ['https://example.test/photo',image+'\n','x'.repeat(2_700_000),'YWJj'])assert.throws(()=>validatedImage(bad),ImageError);
 const huge=Buffer.from(image,'base64');huge.writeUInt32BE(60000,16);assert.throws(()=>validatedImage(huge.toString('base64')),ImageError);
});
test('persistent quota/deduplication is checked before the provider; failures never fallback or retry',async()=>{
 let calls=0;const redis={command:async()=>[1]};let sent;
 const service=createImageService({config,redis,prefix:'test:',fetchImpl:async(url,options)=>{calls++;sent={url,options};return Response.json({success:true,result:{image}});}});
 const result=await service.generate(body(),randomUUID());assert.equal(result.data,image);assert.equal(calls,1);
 assert.equal(sent.url,'https://api.cloudflare.com/client/v4/accounts/'+config.account+'/ai/run/'+IMAGE_MODEL);
 assert.deepEqual(Object.keys(JSON.parse(sent.options.body)).sort(),['prompt','steps']);assert.equal(JSON.parse(sent.options.body).steps,4);assert.equal(sent.options.redirect,'error');
 for(const grant of [[0,'image_user_quota'],[0,'image_global_quota'],[-1],['unexpected']]){
  redis.command=async()=>grant;await assert.rejects(()=>service.generate(body(),randomUUID()),ImageError);assert.equal(calls,1);
 }
 redis.command=async()=>{throw new Error('Redis down');};await assert.rejects(()=>service.generate(body(),randomUUID()),/image_access_unavailable/);assert.equal(calls,1);
});
test('provider quota, malformed image, oversized response and timeout stay explicit errors',async()=>{
 for(const response of [()=>new Response('{}',{status:429}),()=>Response.json({success:true,result:{image:'bad'}}),()=>Response.json({success:false}),()=>new Response('x'.repeat(2_800_001),{status:200})]){
  const service=createImageService({config,redis:{command:async()=>[1]},prefix:'test:',fetchImpl:async()=>response()});await assert.rejects(()=>service.generate(body(),randomUUID()),ImageError);
 }
 const controller=new AbortController();controller.abort();
 const service=createImageService({config,redis:{command:async()=>[1]},prefix:'test:',fetchImpl:async()=>{throw new Error('Aborted');}});
 await assert.rejects(()=>service.generate(body(),randomUUID(),controller.signal),/image_timeout/);
});
test('HTTP image capability is authenticated, disabled by default, and isolated from text/attachments',async t=>{
 const token='synthetic-gateway-access-code-image-tests';let calls=0;
 const server=createGateway({apiKey:'synthetic-provider-key-images',model:'test/model',clientToken:token,imageService:{generate:async(value)=>{calls++;validateImageRequest(value);return validatedImage(image);}}});
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));t.after(()=>new Promise(resolve=>server.close(resolve)));
 const base='http://127.0.0.1:'+server.address().port;
 assert.equal((await fetch(base+'/v1/capabilities')).status,401);
 const capabilities=await fetch(base+'/v1/capabilities',{headers:{Authorization:'Bearer '+token}});assert.equal((await capabilities.json()).imageGeneration,true);assert.equal(calls,0);
 const generate=await fetch(base+'/v1/images',{method:'POST',headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:JSON.stringify(body())});assert.equal(generate.status,200);assert.equal((await generate.json()).generated,true);assert.equal(calls,1);
 const refused=await fetch(base+'/v1/images',{method:'POST',headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:'x'.repeat(4097)});assert.equal(refused.status,413);assert.equal(calls,1);
 const disabled=createGateway({apiKey:'synthetic-provider-key-images',model:'test/model',clientToken:token});await new Promise(resolve=>disabled.listen(0,'127.0.0.1',resolve));t.after(()=>new Promise(resolve=>disabled.close(resolve)));
 const unavailable=await fetch('http://127.0.0.1:'+disabled.address().port+'/v1/images',{method:'POST',headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:JSON.stringify(body())});assert.equal(unavailable.status,503);assert.equal((await unavailable.json()).error,'image_unavailable');
});
