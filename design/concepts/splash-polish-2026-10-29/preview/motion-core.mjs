export const DURATION=800;
export const PREVIEW_DURATION=1200;
export const PARTS={"crescent":"M40.5 7.6C27.3 3.3 14 12.7 14 26.9C14 35.7 18.2 41.8 24.5 45.3C27.9 46 30.3 47.9 32 50.4C33.9 47.9 36.6 46 40.3 45.3C47.4 41.6 51.9 34.7 52 26.8C49.3 36.7 41.4 42.1 32.6 39.7C23.7 37.3 19.3 28.8 21.8 20.2C24.1 12.3 31.8 7.4 40.5 7.6Z","spark":"M38.3 19.4C39.3 24.1 40.5 25.3 45.2 26.3C40.5 27.3 39.3 28.5 38.3 33.2C37.3 28.5 36.1 27.3 31.4 26.3C36.1 25.3 37.3 24.1 38.3 19.4Z","bookLeft":"M12.8 48.4C21.5 47.4 28 49.8 31 55.2V59.6C26.8 56.3 20.2 55.1 10.8 56.2L12.8 48.4Z","bookRight":"M51.2 48.4C42.5 47.4 36 49.8 33 55.2V59.6C37.2 56.3 43.8 55.1 53.2 56.2L51.2 48.4Z"};
const tracks={
  shift:[[0,58],[420,58],[780,0],[800,0]],
  word:[[0,0],[500,0],[800,1]],
  wordX:[[0,-8],[500,-8],[800,0]],
  bookScale:[[0,1],[90,.78],[360,1]],
  bookAngle:[[0,0],[90,11],[380,0]],
  bookY:[[0,0],[90,-1.8],[380,0]],
  moonAlpha:[[0,1],[90,.73],[340,1]],
  moonAngle:[[0,0],[100,-4],[380,0]],
  trace:[[0,0],[60,0],[360,1]],
  traceAlpha:[[0,0],[100,.8],[360,.65],[480,0]],
  starScale:[[0,1],[120,.70],[340,1.12],[520,1]],
  starAlpha:[[0,1],[120,.65],[340,1]],
  glint:[[0,0],[180,0],[340,.27],[600,0]],
  halo:[[0,.015],[340,.035],[800,.015]]
};
function cubic(t,a,b){return 3*(1-t)*(1-t)*t*a+3*(1-t)*t*t*b+t*t*t;}
function easing(x){let a=0,b=1;for(let i=0;i<18;i++){let t=(a+b)/2;if(cubic(t,.22,.36)<x)a=t;else b=t;}return cubic((a+b)/2,1,1);}
function value(track,time){if(time<=track[0][0])return track[0][1];for(let i=1;i<track.length;i++)if(time<=track[i][0]){const prev=track[i-1],next=track[i];return prev[1]+(next[1]-prev[1])*easing((time-prev[0])/(next[0]-prev[0]));}return track.at(-1)[1];}
export function stateAt(ms,variant='polished'){
 const t=Math.max(0,Math.min(DURATION,Number(ms)||0));
 if(variant==='current'){
  const old=(start,end)=>value([[0,0],[start,0],[end,1]],t);
  return {shift:58*(1-old(72,296)),word:old(172,344),wordX:-8*(1-old(172,344)),bookScale:1,bookAngle:0,bookY:0,moonAlpha:1,moonAngle:0,trace:0,traceAlpha:0,starScale:1,starAlpha:1,glint:0,halo:.015};
 }
 return Object.fromEntries(Object.entries(tracks).map(([name,track])=>[name,value(track,t)]));
}
const n=value=>Number(value.toFixed(5));
export function svgAt(ms,variant='polished'){
 const s=Object.fromEntries(Object.entries(stateAt(ms,variant)).map(([k,v])=>[k,n(v)]));
 const page=(side,pivot,sign)=>`<path d="${PARTS[side]}" fill="#fff" transform="translate(0 ${s.bookY}) translate(${pivot} 55) rotate(${sign*s.bookAngle}) scale(${s.bookScale} 1) translate(${-pivot} -55)"/>`;
 return `<svg xmlns="http://www.w3.org/2000/svg" width="390" height="844" viewBox="0 0 390 844" role="img" aria-label="Preview gerak logo Zikra">
 <defs><radialGradient id="ambientGradient"><stop stop-color="#D5E7DE"/><stop offset="1" stop-color="#D5E7DE" stop-opacity="0"/></radialGradient><radialGradient id="sparkGradient"><stop stop-color="#FFF4D4"/><stop offset="1" stop-color="#FFF4D4" stop-opacity="0"/></radialGradient></defs>
 <rect width="390" height="844" fill="#15332F"/>
 <circle cx="195" cy="422" r="130" fill="url(#ambientGradient)" opacity="${s.halo}"/>
 <g transform="translate(80 366)"><g transform="translate(${s.shift} 0)"><g transform="scale(1.75)">
 <path d="${PARTS.crescent}" fill="#fff" opacity="${s.moonAlpha}" transform="rotate(${s.moonAngle} 32 27)"/>
 <path d="${PARTS.crescent}" fill="none" stroke="#fff" stroke-width=".65" stroke-linecap="round" stroke-dasharray="160 160" stroke-dashoffset="${n(160*(1-s.trace))}" opacity="${s.traceAlpha}" transform="rotate(${s.moonAngle} 32 27)"/>
 <circle cx="38.3" cy="26.3" r="12" fill="url(#sparkGradient)" opacity="${s.glint}"/>
 <path d="${PARTS.spark}" fill="#fff" opacity="${s.starAlpha}" transform="translate(38.3 26.3) scale(${s.starScale}) translate(-38.3 -26.3)"/>
 ${page('bookLeft',31,1)}${page('bookRight',33,-1)}
 </g></g><g opacity="${s.word}" transform="translate(${s.wordX} 0)"><text x="120" y="72" fill="#fff" font-family="Plus Jakarta Sans" font-size="44" font-weight="600" letter-spacing="-.88">zikra</text></g></g>
 </svg>`;
}
