import test from 'node:test';
import assert from 'node:assert/strict';
import { loadTafsir } from './tafsir.mjs';
import { selectedPrompt } from './assistant-contract.mjs';

const data = (chapter,items) => Response.json({code:200,data:{nomor:chapter,tafsir:items}});
test('tafsir rejects wrong chapter, missing/duplicate verses and empty markup before any summary', async () => {
  const source={ayat:1,teks:'Synthetic source, not scripture.'};
  for(const [chapter,fetchImpl] of [[10,async()=>data(11,[source])],[12,async()=>data(12,[])],
    [13,async()=>data(13,[source,source])],[14,async()=>data(14,[{ayat:1,teks:'<p></p><div></div>'}])]]){
    await assert.rejects(loadTafsir({chapter,number:1},fetchImpl),/source_unavailable/);
  }
});
test('tafsir preserves word boundaries, marks bounded excerpts and never splits a surrogate pair', async () => {
  const source=await loadTafsir({chapter:15,number:1},async()=>data(15,[{ayat:1,teks:'<p>First paragraph</p><p>Second paragraph</p>'}]));
  assert.match(source.text,/paragraph\s+Second/);assert.equal(source.truncated,false);
  const long=await loadTafsir({chapter:16,number:1},async()=>data(16,[{ayat:1,teks:'x'.repeat(5999)+'😀tail'}]));
  assert.equal(long.text.length,5999);assert.equal(long.truncated,true);assert.equal(long.url,'https://equran.id/tafsir/16');
});
test('tafsir has fixed HTTPS origin, bounded body and observes cancellation even for cached data', async () => {
  let calls=0;
  const source=await loadTafsir({chapter:17,number:1},async(url,options)=>{
    calls++;assert.equal(url,'https://equran.id/api/v2/tafsir/17');assert.equal(options.redirect,'error');
    return data(17,[{ayat:1,teks:'Synthetic public source only.'}]);
  });
  const abort=new AbortController();abort.abort();
  await assert.rejects(loadTafsir({chapter:17,number:1},async()=>{calls++;},abort.signal));assert.equal(calls,1);
  await assert.rejects(loadTafsir({chapter:18,number:1},async()=>new Response('x'.repeat(2*1024*1024+1))),/source_too_large/);
  const prompt=selectedPrompt({context:{},taskProposals:false},source);
  assert.match(prompt,/Ayat yang diminta persis 17:1/);assert.match(prompt,/membahas ayat lain sebagai konteks/);
});
