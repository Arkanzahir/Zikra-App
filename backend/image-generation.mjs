import {ConfigError} from './config.mjs';
import {imageDimensions} from './attachments.mjs';

export const IMAGE_MODEL='@cf/black-forest-labs/flux-1-schnell';
export const IMAGE_USER_DAILY=3, IMAGE_GLOBAL_DAILY=20, IMAGE_MAX_BYTES=2_000_000;
const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
export class ImageError extends Error {constructor(message,status){super(message);this.status=status;}}
export function readImageConfig(env=process.env){
  if(env.ZIKRA_IMAGES_ENABLED!=='true')return null;
  if(!/^[a-f0-9]{32}$/.test(env.CLOUDFLARE_ACCOUNT_ID??'')||! /^[A-Za-z0-9._~-]{20,2048}$/.test(env.CLOUDFLARE_API_TOKEN??''))
    throw new ConfigError('Invalid Cloudflare image configuration.');
  // Deliberate opt-in only after the operator verifies the Free plan; no paid fallback.
  if(env.ZIKRA_IMAGES_FREE_PLAN_CONFIRMED!=='true')throw new ConfigError('Cloudflare Free plan confirmation required.');
  return {account:env.CLOUDFLARE_ACCOUNT_ID,token:env.CLOUDFLARE_API_TOKEN};
}
export const RESERVE_IMAGE=`
local clock=redis.call('TIME')
local day=math.floor(tonumber(clock[1])/86400)
if redis.call('EXISTS',KEYS[3])==1 then return {-1} end
local function count(key)
 local v=redis.call('HMGET',key,'day','count')
 if not v[1] and not v[2] then return 0 end
 if not tonumber(v[1]) or tonumber(v[1])<0 or tonumber(v[1])%1~=0 or not tonumber(v[2]) or tonumber(v[2])<0 or tonumber(v[2])%1~=0 then return nil end
 if tonumber(v[1])~=day then return 0 end
 return tonumber(v[2])
end
local global=count(KEYS[1])
local user=count(KEYS[2])
if not global or not user then return redis.error_reply('invalid_image_usage') end
if global>=20 then return {0,'image_global_quota'} end
if user>=3 then return {0,'image_user_quota'} end
redis.call('HSET',KEYS[1],'day',day,'count',global+1)
redis.call('HSET',KEYS[2],'day',day,'count',user+1)
redis.call('EXPIRE',KEYS[1],172800)
redis.call('EXPIRE',KEYS[2],172800)
redis.call('SET',KEYS[3],'attempted','EX',172800)
return {1}
`;
export function validateImageRequest(body){
  if(!body||typeof body!=='object'||Array.isArray(body)||Object.keys(body).some(k=>!['prompt','requestId'].includes(k))
      ||typeof body.prompt!=='string'||body.prompt.trim().length<3||body.prompt.length>1000||/[\p{Cc}]/u.test(body.prompt.replace(/[\n\r\t]/g,''))||!uuid.test(body.requestId))
    throw new ImageError('invalid_image_request',400);
  return {prompt:body.prompt.trim(),requestId:body.requestId};
}
async function jsonBounded(response,signal){
  const chunks=[];let size=0;
  for await(const chunk of response.body){if(signal?.aborted)throw new ImageError('image_timeout',504);size+=chunk.length;if(size>2_800_000){await response.body.cancel?.().catch(()=>{});throw new ImageError('invalid_image_response',502);}chunks.push(chunk);}
  try{return JSON.parse(Buffer.concat(chunks).toString('utf8'));}catch{throw new ImageError('invalid_image_response',502);}
}
export function validatedImage(data){
  if(typeof data!=='string'||data.length>2_670_000||data.length%4!==0||! /^[A-Za-z0-9+/]+={0,2}$/.test(data))throw new ImageError('invalid_image_response',502);
  const bytes=Buffer.from(data,'base64');if(bytes.length<33||bytes.length>IMAGE_MAX_BYTES||bytes.toString('base64')!==data)throw new ImageError('invalid_image_response',502);
  const mime=bytes[0]===255&&bytes[1]===216?'image/jpeg':'image/png';
  let width,height;try{[width,height]=imageDimensions(bytes,mime);}catch{throw new ImageError('invalid_image_response',502);}
  if(width<64||height<64||width>1536||height>1536||width*height>2_100_000)throw new ImageError('invalid_image_response',502);
  return {provider:'cloudflare',model:'flux-1-schnell',generated:true,mime,width,height,data};
}
export function createImageService({config,redis,prefix,fetchImpl=fetch}){
  if(!config)return null;
  return {async generate(body,identity,signal){
    const request=validateImageRequest(body);
    if(!uuid.test(identity))throw new ImageError('image_unavailable',503);
    let grant;try{grant=await redis.command(['EVAL',RESERVE_IMAGE,3,prefix+'images:global',prefix+'images:user:'+identity,prefix+'images:request:'+identity+':'+request.requestId]);}
    catch{throw new ImageError('image_access_unavailable',503);}
    if(Array.isArray(grant)&&grant[0]===-1)throw new ImageError('image_already_attempted',409);
    if(Array.isArray(grant)&&grant[0]===0&&['image_user_quota','image_global_quota'].includes(grant[1]))throw new ImageError(grant[1],429);
    if(!Array.isArray(grant)||grant.length!==1||grant[0]!==1)throw new ImageError('image_access_unavailable',503);
    let response;
    try{response=await fetchImpl('https://api.cloudflare.com/client/v4/accounts/'+config.account+'/ai/run/'+IMAGE_MODEL,{method:'POST',redirect:'error',signal,headers:{Authorization:'Bearer '+config.token,'Content-Type':'application/json'},body:JSON.stringify({prompt:request.prompt,steps:4})});}
    catch{throw new ImageError(signal?.aborted?'image_timeout':'image_provider_unavailable',signal?.aborted?504:502);}
    if(!response.ok){await response.body?.cancel();throw new ImageError(response.status===429?'image_provider_quota':response.status===401||response.status===403?'image_unavailable':'image_provider_unavailable',response.status===429?429:502);}
    let value;try{value=await jsonBounded(response,signal);}catch(error){if(signal?.aborted)throw new ImageError('image_timeout',504);throw error;}
    if(value.success!==true)throw new ImageError('image_provider_unavailable',502);
    return validatedImage(value.result?.image);
  }};
}
