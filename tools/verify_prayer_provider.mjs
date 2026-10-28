// Compare public provider tables with the public endpoints used by the Kemenag timetable page.
import {mkdirSync, writeFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
const output = process.argv[2];
if (!output) throw new Error('Provide an evidence output directory');
mkdirSync(output, {recursive:true});
const fixtures = 'android/app/src/test/resources/prayer';
mkdirSync(fixtures, {recursive:true});
async function json(url) {
  const response = await fetch(url, {signal:AbortSignal.timeout(20000),redirect:'error'});
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}
const officialBase='https://bimasislam.kemenag.go.id/api/sihat/';
const providerBase='https://api.myquran.com/v3/sholat/';
const provinces=(await json(officialBase+'provinsi')).data;
const cities=(await json(providerBase+'kabkota/semua')).data;
const records=[];
const fields=['subuh','terbit','dzuhur','ashar','maghrib','isya'];
for (const [slug,name,province] of [['surabaya','KOTA SURABAYA','JAWA TIMUR'],
    ['makassar','KOTA MAKASSAR','SULAWESI SELATAN'],['jayapura','KOTA JAYAPURA','PAPUA']]) {
  const p=provinces.find(x=>x.name===province);
  if (!p) throw new Error('Missing official province '+province);
  const local=(await json(officialBase+'kabko?'+new URLSearchParams({prov:p.id}))).data;
  const c=local.find(x=>x.name===name), provider=cities.find(x=>x.lokasi===name);
  if (!c || !provider) throw new Error('Missing exact city '+name);
  for (const period of ['2026-10','2026-11']) {
    const [year,month]=period.split('-');
    const official=await json(officialBase+'jadwal?'+new URLSearchParams({prov:p.id,kabko:c.id,thn:year,bln:month}));
    const provided=await json(providerBase+'jadwal/'+provider.id+'/'+period);
    if (official.status!==1 || !provided.status || official.kabko!==name || provided.data.kabko!==name)
      throw new Error('Timetable identity mismatch');
    const expected=Object.keys(official.data).sort(), actual=Object.keys(provided.data.jadwal).sort();
    if (JSON.stringify(expected)!==JSON.stringify(actual)) throw new Error('Date coverage mismatch');
    for (const date of expected) for (const field of fields)
      if (official.data[date][field]!==provided.data.jadwal[date][field]) throw new Error('Time mismatch '+date+' '+field);
    const text=JSON.stringify(provided,null,2)+'\n';
    writeFileSync(`${fixtures}/${slug}-${period}.json`,text);
    records.push({city:name,province,period,days:expected.length,eventsCompared:expected.length*fields.length,
      exactMatch:true,providerUrl:providerBase+'jadwal/'+provider.id+'/'+period,
      officialPage:'https://bimasislam.kemenag.go.id/jadwal-sholat',
      officialEndpoint:officialBase+'jadwal',fixtureSha256:createHash('sha256').update(text).digest('hex'),
      sample:period==='2026-10'?{date:'2026-10-28',times:Object.fromEntries(fields.map(f=>[f,official.data['2026-10-28'][f]]))}:null});
  }
}
const report={checkedAt:new Date().toISOString(),provider:'myQuran v3',
  providerSourceDeclaration:await json(providerBase.slice(0,-1)),
  publicDataOnly:true,privateCoordinatesSent:false,records,
  eventsCompared:records.reduce((n,r)=>n+r.eventsCompared,0),passed:true,
  scope:'Exact equality for six public city-months, not a claim that all dates/regions or provider availability are verified.'};
writeFileSync(output+'/source-comparison.json',JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify({passed:true,months:records.length,events:report.eventsCompared,surabaya:records[0].sample}));
