import{DURATION,PREVIEW_DURATION,stateAt,svgAt}from'./motion-core.mjs';
const $=id=>document.getElementById(id),reduced=$('reduced'),query=matchMedia('(prefers-reduced-motion: reduce)');
let frame=0,raf=0,variant='polished',rate=1;
reduced.checked=query.matches;
function stop(){if(raf)cancelAnimationFrame(raf);raf=0;}
function draw(ms){frame=Math.max(0,Math.min(PREVIEW_DURATION,ms));$('scene').innerHTML=svgAt(frame,variant);$('timeline').value=Math.min(DURATION,frame);$('time').value=(Math.min(DURATION,frame)/1000).toFixed(2).replace('.',',')+' s';window.splashPreview={time:frame,variant,running:!!raf,reduced:reduced.checked,rate,state:stateAt(frame,variant)};}
function play(speed=1){stop();rate=speed;if(reduced.checked){draw(DURATION);return;}const start=performance.now();draw(0);function tick(now){draw((now-start)*rate);if(frame<PREVIEW_DURATION)raf=requestAnimationFrame(tick);else{raf=0;draw(PREVIEW_DURATION);}}raf=requestAnimationFrame(tick);}
$('replay').addEventListener('click',()=>play());$('slow').addEventListener('click',()=>play(.5));reduced.addEventListener('change',()=>play());query.addEventListener('change',event=>{reduced.checked=event.matches;play();});
$('timeline').addEventListener('input',event=>{stop();draw(Number(event.target.value));});
document.querySelectorAll('[data-time]').forEach(button=>button.addEventListener('click',()=>{stop();draw(Number(button.dataset.time));}));
document.querySelectorAll('[data-variant]').forEach(button=>button.addEventListener('click',()=>{variant=button.dataset.variant;document.querySelectorAll('[data-variant]').forEach(item=>item.setAttribute('aria-pressed',String(item===button)));play();}));
document.addEventListener('visibilitychange',()=>{if(document.hidden){stop();draw(frame);}});
window.previewControls={seek(ms){stop();draw(ms);},play,stop(){stop();draw(frame);}};
document.fonts.ready.then(()=>{window.previewReady=true;play();});
