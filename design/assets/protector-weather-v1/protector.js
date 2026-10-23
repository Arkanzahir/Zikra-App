"use strict";
const modes = {
clear:{title:"Cerah",temp:29,cloud:"Tidak ditampilkan",layers:"Langit + matahari + kota",note:"Tanpa lapisan awan dan hujan",rain:0,speed:0,length:0,wind:0,intensity:"Tidak ada"},
partly:{title:"Cerah berawan",temp:29,cloud:"Dua lapisan, kecepatan berbeda",layers:"Langit + matahari + awan terang + kota",note:"Awan terang bergerak perlahan",rain:0,speed:0,length:0,wind:0,intensity:"Tidak ada"},
overcast:{title:"Mendung",temp:27,cloud:"Awan gelap, tanpa hujan",layers:"Langit + awan gelap + kota",note:"Awan tebal menutupi langit",rain:0,speed:0,length:0,wind:0,intensity:"Tidak ada"},
rain:{title:"Hujan ringan",temp:26,cloud:"Awan gelap bergerak perlahan",layers:"Langit + awan gelap + kota + hujan",note:"Hujan ringan · 22 partikel",rain:22,speed:280,length:13,wind:55,intensity:"Ringan"},
heavy:{title:"Hujan deras",temp:24,cloud:"Dua lapisan awan gelap",layers:"Langit + awan gelap + kota + hujan",note:"Hujan deras · 65 partikel",rain:65,speed:550,length:23,wind:130,intensity:"Deras"},
storm:{title:"Badai",temp:23,cloud:"Awan gelap, gerak berlapis",layers:"Langit gelap + awan gelap + kota + hujan",note:"Badai simulasi · 90 partikel",rain:90,speed:730,length:29,wind:220,intensity:"Deras, miring oleh angin (contoh)"},
night:{title:"Malam cerah",temp:25,cloud:"Tidak ditampilkan",layers:"Langit malam + bulan + kota",note:"Malam cerah, tanpa awan",rain:0,speed:0,length:0,wind:0,intensity:"Tidak ada"}
};
const scene=document.getElementById("scene"),canvas=document.getElementById("rain"),ctx=canvas.getContext("2d");
const reduce=window.matchMedia("(prefers-reduced-motion:reduce)");
let mode="partly",motion=!reduce.matches,raf=0,last=0,width=1,height=1,drops=[];
const buttons={};
function text(id,value){const el=document.getElementById(id);if(el)el.textContent=value}
function random(seed){let n=seed>>>0;return()=>{n=(Math.imul(n,1664525)+1013904223)>>>0;return n/4294967296}}
function seedDrops(){const rnd=random(20261022);drops=Array.from({length:modes[mode].rain},()=>({x:rnd()*(width+80)-40,y:rnd()*height,k:.65+rnd()*.65,a:.16+rnd()*.24}))}
function render(){ctx.clearRect(0,0,width,height);const m=modes[mode];for(const p of drops){ctx.strokeStyle="rgba(225,243,249,"+p.a+")";ctx.lineWidth=p.k>1?1:.65;ctx.beginPath();ctx.moveTo(p.x,p.y);ctx.lineTo(p.x-m.wind/m.speed*m.length*p.k,p.y-m.length*p.k);ctx.stroke()}}
function frame(now){raf=0;if(!motion||(document.hidden || !document.getElementById('detail').hidden)||!modes[mode].rain)return;if(!last)last=now;if(now-last>=1000/30){const dt=Math.min((now-last)/1000,.06),m=modes[mode];last=now;for(const p of drops){p.x+=m.wind*p.k*dt;p.y+=m.speed*p.k*dt;if(p.y>height+40){p.y=-40;p.x=(p.x*1.37+59)%(width+80)-40}if(p.x>width+40)p.x=-40}render()}raf=requestAnimationFrame(frame)}
function syncMotion(){if(raf)cancelAnimationFrame(raf);raf=0;last=0;scene.classList.toggle("paused",!motion||(document.hidden || !document.getElementById('detail').hidden));scene.classList.toggle("motion-opt-in",motion);const b=document.getElementById("motion");b.setAttribute("aria-pressed",String(motion));b.textContent=motion?"Jeda gerak":"Putar gerak";text("motion-note",document.hidden?"Tab tidak aktif":!document.getElementById("detail").hidden?"Cuaca dijeda saat membaca peta":motion?"Gerak aktif":"Gerak dijeda");if(motion&&!(document.hidden || !document.getElementById('detail').hidden)&&modes[mode].rain)raf=requestAnimationFrame(frame)}
function selectMode(key){mode=key;scene.dataset.mode=key;const m=modes[key];text("temperature",m.temp);const readings={clear:[31,65],partly:[31,72],overcast:[29,80],rain:[27,88],heavy:[25,92],storm:[24,95],night:[26,78]};text("feels",readings[key][0]+"°");text("humidity",readings[key][1]+"%");text("condition",m.title);text("layers",m.layers);text("layer-note",m.note);text("cloud-motion",m.cloud);text("intensity",m.intensity);for(const [id,b]of Object.entries(buttons))b.setAttribute("aria-pressed",String(id===key));seedDrops();render();syncMotion()}
function resize(){const rect=scene.getBoundingClientRect(),dpr=Math.min(devicePixelRatio||1,2);width=scene.clientWidth;height=scene.clientHeight;canvas.width=Math.round(width*dpr);canvas.height=Math.round(height*dpr);ctx.setTransform(dpr,0,0,dpr,0,0);seedDrops();render()}
for(const [key,m]of Object.entries(modes)){const b=document.createElement("button");b.type="button";b.textContent=m.title;b.setAttribute("aria-pressed",String(key===mode));b.addEventListener("click",()=>selectMode(key));buttons[key]=b;document.getElementById("modes").append(b)}
document.getElementById("motion").addEventListener("click",()=>{motion=!motion;syncMotion()});
reduce.addEventListener("change",e=>{motion=!e.matches;syncMotion()});
document.addEventListener("visibilitychange",syncMotion);
new ResizeObserver(resize).observe(scene);
resize();selectMode("partly");
const overview=document.getElementById("overview"),detail=document.getElementById("detail"),geography=document.getElementById("detail-geography");
const overviewMap=new EarthquakeMap(document.getElementById("overview-geography")), detailMap=new EarthquakeMap(geography,true);
let actualSize=true,report=null;
function showMap(show){overview.hidden=show;detail.hidden=!show;syncMotion();if(show){detailMap.draw();document.getElementById("map-back").focus();}else document.getElementById("expand-map").focus()}
for(const id of ["expand-map","map-open","detail-open"])document.getElementById(id).addEventListener("click",()=>showMap(true));
document.getElementById("map-back").addEventListener("click",()=>showMap(false));
function changeZoom(delta){detailMap.changeZoom(delta);document.getElementById("zoom-in").disabled=detailMap.zoom>=9;document.getElementById("zoom-out").disabled=detailMap.zoom<=3}
document.getElementById("zoom-in").addEventListener("click",()=>changeZoom(1));
document.getElementById("zoom-out").addEventListener("click",()=>changeZoom(-1));
document.getElementById("zoom-reset").addEventListener("click",()=>{detailMap.reset();changeZoom(0)});
function mapType(shake){
 document.getElementById("shakemap-tab").setAttribute("aria-pressed",String(shake));
 document.getElementById("epicenter-tab").setAttribute("aria-pressed",String(!shake));
 const shakeAvailable=Boolean(report?.shakemap)&&document.getElementById("shakemap-image").dataset.failed!=="true";
 document.getElementById("shakemap-image").hidden=!shake||!shakeAvailable;
 document.getElementById("shake-empty").hidden=!shake||shakeAvailable;
 document.querySelector(".zoom-controls").hidden=shake;
 geography.hidden=shake;document.querySelector(".map-caption").hidden=shake;
 document.querySelector(".map-corner").hidden=shake;
 if(!shake)detailMap.draw();
}
document.getElementById("epicenter-tab").addEventListener("click",()=>mapType(false));
document.getElementById("shakemap-tab").addEventListener("click",()=>mapType(true));
document.getElementById("shakemap-image").addEventListener("error",()=>{
 document.getElementById("shakemap-image").dataset.failed="true";
 document.getElementById("shakemap-image").hidden=true;document.getElementById("shake-empty").hidden=document.getElementById("shakemap-tab").getAttribute("aria-pressed")!=="true";
 document.querySelector("#shake-empty p").textContent="Gambar BMKG belum dapat dimuat. Buka sumber BMKG untuk memeriksa laporannya.";
});
for(const b of document.querySelectorAll(".refresh-demo"))b.addEventListener("click",()=>{detailMap.reset();changeZoom(0);mapType(false);selectMode(mode)});
function fitPhone(){const narrow=innerWidth<=760,limitWidth=(narrow?innerWidth-36:390)/376;const scale=actualSize?Math.min(1,limitWidth):Math.max(.45,Math.min(1,limitWidth,(innerHeight-(narrow?100:120))/760));document.documentElement.style.setProperty("--phone-scale",scale.toFixed(4))}
document.getElementById("actual-size").addEventListener("click",()=>{actualSize=!actualSize;const b=document.getElementById("actual-size");b.setAttribute("aria-pressed",String(actualSize));b.textContent=actualSize?"Sesuaikan layar":"Lihat ukuran asli";fitPhone()});
window.addEventListener("resize",fitPhone);
document.addEventListener("keydown",e=>{if(e.key==="Escape"&&!detail.hidden)showMap(false)});
fitPhone();changeZoom(0);

function renderReport(value){
 report=value;overviewMap.setReport(value);detailMap.setReport(value);mapType(false);changeZoom(0);
 const live=Boolean(value), fullTime=live?(value.time?new Intl.DateTimeFormat("id-ID",{dateStyle:"medium",timeStyle:"short",timeZone:"Asia/Jakarta"}).format(new Date(value.time)):value.date+" "+value.clock):"—";
 for(const metrics of document.querySelectorAll(".metrics")){
   const values=live?[value.magnitude,value.depth||"—",fullTime]:["—","—","—"];
   metrics.querySelectorAll("dd").forEach((el,i)=>el.textContent=values[i]);
 }
 document.querySelector(".report-heading .badge").textContent=live?"BMKG":"Contoh";
 document.querySelector(".detail-badge").textContent=live?"Laporan BMKG":"Contoh tampilan";
 document.querySelector(".sheet-heading .badge").textContent=live?"DATA BMKG":"DATA CONTOH";
 document.querySelector(".sheet-location").textContent=live?value.area:"Lokasi mengikuti laporan yang dipilih";
 document.querySelector(".felt").textContent=live?"Dirasakan: "+(value.felt||"Tidak dicantumkan oleh sumber"):"Dirasakan: mengikuti laporan BMKG";
 document.querySelector(".not-live").textContent=live?"Laporan terakhir Indonesia · bukan peringatan lokasi Anda":"Bukan kejadian aktual";
 document.querySelector(".map-caption").textContent=live?(ZikraGeo.valid(value.lat,value.lon)?"Koordinat dari laporan BMKG":"Koordinat belum tersedia · tanpa titik"):"Posisi dan data hanya ilustrasi";
 document.querySelector(".update-note p").textContent=live?"Diambil "+new Intl.DateTimeFormat("id-ID",{timeStyle:"short",timeZone:"Asia/Jakarta"}).format(new Date(value.fetchedAt))+" WIB · waktu kejadian di atas":"Waktu pembaruan akan ditampilkan";
 document.getElementById("use-example").hidden=!live;
 const image=document.getElementById("shakemap-image");image.removeAttribute("src");image.dataset.failed="false";
 if(value?.shakemap) image.src=value.shakemap;
 document.querySelector("#shake-empty p").textContent=live?"Peta guncangan tidak dicantumkan pada laporan ini.":"Pilih “Cek laporan BMKG” untuk memuat peta guncangan dari sumber resminya.";
}
document.getElementById("load-bmkg").addEventListener("click",async()=>{
 const button=document.getElementById("load-bmkg");button.disabled=true;button.textContent="Memuat BMKG…";
 text("data-status","Menghubungi sumber BMKG…");
 try{
   const response=await fetch("/api/earthquake",{signal:AbortSignal.timeout(16000)});
   if(!response.ok)throw Error();const value=await response.json();
   if(!value.area||!Number.isFinite(Number(value.magnitude))||!Number.isFinite(Date.parse(value.fetchedAt)))throw Error();
   renderReport(value);text("data-status","Laporan BMKG termuat. Cuaca tetap contoh. Tidak ada deteksi gempa melalui HP.");
 }catch{ text("data-status",report?"Pembaruan gagal. Laporan sebelumnya tetap ditampilkan; waktu tidak diperbarui.":"Laporan BMKG belum dapat dimuat. Peta masih bertanda contoh."); }
 finally{button.disabled=false;button.textContent="Cek laporan BMKG";}
});
document.getElementById("use-example").addEventListener("click",()=>{renderReport(null);text("data-status","Peta saat ini: titik contoh.");});
