"use strict";
const modes = {
clear:{title:"Cerah",temp:29,cloud:"Tidak ditampilkan",layers:"Langit + matahari + kota",note:"Tanpa lapisan awan dan hujan",rain:0,speed:0,length:0,wind:0,intensity:"Tidak ada"},
partly:{title:"Berawan",temp:28,cloud:"Dua lapisan, kecepatan berbeda",layers:"Langit + matahari + awan terang + kota",note:"Awan terang bergerak perlahan",rain:0,speed:0,length:0,wind:0,intensity:"Tidak ada"},
overcast:{title:"Mendung",temp:27,cloud:"Awan gelap, tanpa hujan",layers:"Langit + awan gelap + kota",note:"Awan tebal menutupi langit",rain:0,speed:0,length:0,wind:0,intensity:"Tidak ada"},
rain:{title:"Hujan ringan",temp:26,cloud:"Awan gelap bergerak perlahan",layers:"Langit + awan gelap + kota + hujan",note:"Hujan ringan · 22 partikel",rain:22,speed:280,length:13,wind:55,intensity:"Ringan"},
heavy:{title:"Hujan deras",temp:24,cloud:"Dua lapisan awan gelap",layers:"Langit + awan gelap + kota + hujan",note:"Hujan deras · 65 partikel",rain:65,speed:550,length:23,wind:130,intensity:"Deras"},
storm:{title:"Badai",temp:23,cloud:"Awan gelap, gerak berlapis",layers:"Langit gelap + awan gelap + kota + hujan",note:"Badai simulasi · 90 partikel",rain:90,speed:730,length:29,wind:220,intensity:"Deras, miring oleh angin (contoh)"},
night:{title:"Malam cerah",temp:25,cloud:"Tidak ditampilkan",layers:"Langit malam + bulan + kota",note:"Malam cerah, tanpa awan",rain:0,speed:0,length:0,wind:0,intensity:"Tidak ada"}
};
const scene=document.getElementById("scene"),canvas=document.getElementById("rain"),ctx=canvas.getContext("2d");
const reduce=window.matchMedia("(prefers-reduced-motion:reduce)");
let mode="clear",motion=!reduce.matches,raf=0,last=0,width=1,height=1,drops=[];
const buttons={};
function text(id,value){document.getElementById(id).textContent=value}
function random(seed){let n=seed>>>0;return()=>{n=(Math.imul(n,1664525)+1013904223)>>>0;return n/4294967296}}
function seedDrops(){const rnd=random(20261022);drops=Array.from({length:modes[mode].rain},()=>({x:rnd()*(width+80)-40,y:rnd()*height,k:.65+rnd()*.65,a:.16+rnd()*.24}))}
function render(){ctx.clearRect(0,0,width,height);const m=modes[mode];for(const p of drops){ctx.strokeStyle="rgba(225,243,249,"+p.a+")";ctx.lineWidth=p.k>1?1:.65;ctx.beginPath();ctx.moveTo(p.x,p.y);ctx.lineTo(p.x-m.wind/m.speed*m.length*p.k,p.y-m.length*p.k);ctx.stroke()}}
function frame(now){raf=0;if(!motion||document.hidden||!modes[mode].rain)return;if(!last)last=now;if(now-last>=1000/30){const dt=Math.min((now-last)/1000,.06),m=modes[mode];last=now;for(const p of drops){p.x+=m.wind*p.k*dt;p.y+=m.speed*p.k*dt;if(p.y>height+40){p.y=-40;p.x=(p.x*1.37+59)%(width+80)-40}if(p.x>width+40)p.x=-40}render()}raf=requestAnimationFrame(frame)}
function syncMotion(){if(raf)cancelAnimationFrame(raf);raf=0;last=0;scene.classList.toggle("paused",!motion||document.hidden);scene.classList.toggle("motion-opt-in",motion);const b=document.getElementById("motion");b.setAttribute("aria-pressed",String(motion));b.textContent=motion?"Jeda gerak":"Putar gerak";text("motion-note",document.hidden?"Tab tidak aktif":motion?"Gerak aktif":"Gerak dijeda");if(motion&&!document.hidden&&modes[mode].rain)raf=requestAnimationFrame(frame)}
function selectMode(key){mode=key;scene.dataset.mode=key;const m=modes[key];text("temperature",m.temp);text("condition",m.title);text("layers",m.layers);text("layer-note",m.note);text("cloud-motion",m.cloud);text("intensity",m.intensity);for(const [id,b]of Object.entries(buttons))b.setAttribute("aria-pressed",String(id===key));seedDrops();render();syncMotion()}
function resize(){const rect=scene.getBoundingClientRect(),dpr=Math.min(devicePixelRatio||1,2);width=rect.width;height=rect.height;canvas.width=Math.round(width*dpr);canvas.height=Math.round(height*dpr);ctx.setTransform(dpr,0,0,dpr,0,0);seedDrops();render()}
for(const [key,m]of Object.entries(modes)){const b=document.createElement("button");b.type="button";b.textContent=m.title;b.setAttribute("aria-pressed",String(key===mode));b.addEventListener("click",()=>selectMode(key));buttons[key]=b;document.getElementById("modes").append(b)}
document.getElementById("motion").addEventListener("click",()=>{motion=!motion;syncMotion()});
reduce.addEventListener("change",e=>{motion=!e.matches;syncMotion()});
document.addEventListener("visibilitychange",syncMotion);
new ResizeObserver(resize).observe(scene);
resize();selectMode("clear");
