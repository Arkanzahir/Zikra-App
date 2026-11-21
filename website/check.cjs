const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.join(__dirname, 'dist');
const sourceOnly = process.argv.includes('--source-only');
const missingReviewMedia = [];
for (const name of ['index.html', 'privacy.html']) {
  const html = fs.readFileSync(path.join(root, name), 'utf8');
  assert(html.includes('lang="id"') && html.includes('name="viewport"'), name);
  for (const [, target] of html.matchAll(/(?:href|src|poster)="([^"]+)"/g)) {
    if (/^(?:https?:|mailto:|#)/.test(target)) continue;
    const file = target.split(/[?#]/)[0];
    if (!fs.existsSync(path.resolve(root, file)) && sourceOnly && file.startsWith('media/')) {
      missingReviewMedia.push(file);
      continue;
    }
    if (!fs.existsSync(path.resolve(root, file)) && sourceOnly && file.startsWith('media/')) {
      missingReviewMedia.push(file);
      continue;
    }
    assert(fs.existsSync(path.resolve(root, file)), `${name}: missing ${file}`);
  }
  assert(!/<script|\.apk|https?:\/\//i.test(html), 'No remote resources, scripts or APK links in HOLD preview');
}
const home = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const artifact = JSON.parse(fs.readFileSync(path.join(__dirname, '../docs/beta/internal-artifact.json'), 'utf8'));
assert.equal(artifact.releaseStatus, 'HOLD');
assert.equal(artifact.distributionApproved, false);
assert.equal(artifact.downloadUrl, null);
assert(artifact.bytes < 100000000);
assert(home.includes(`<dt>Kandidat internal</dt><dd>${artifact.version}</dd>`), 'Website version must match audited internal manifest');
assert(home.includes(`<dd>${(artifact.bytes / 1e6).toFixed(1).replace('.', ',')} MB</dd>`), 'Website size must match audited internal manifest');
assert(/<button[^>]*disabled>Unduhan belum dibuka<\/button>/.test(home));
assert(home.includes('arkanzahir46@gmail.com'));
const ids = [...home.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]);
assert.equal(ids.length, new Set(ids).size, 'Unique navigation targets');
for (const [, id] of home.matchAll(/href="#([^"]+)"/g)) assert(ids.includes(id), `Missing anchor: ${id}`);
const css = fs.readFileSync(path.join(root, 'style.css'), 'utf8');
assert(!/https?:\/\/|@import/i.test(css), 'No remote CSS resources');
for (const [, target] of css.matchAll(/url\(['"]?([^'"\)]+)['"]?\)/g)) assert(fs.existsSync(path.resolve(root, target)), `CSS missing ${target}`);
assert(css.includes('prefers-reduced-motion'), 'Respect reduced motion');
assert(!home.includes('class="serif"'), 'One type family; removed italic marketing accents');
const video = home.match(/<video\b[^>]*>/)?.[0];
assert(video && /\bcontrols\b/.test(video) && /\bplaysinline\b/.test(video));
assert(/preload="none"/.test(video) && !/\bautoplay\b/.test(video) && !/\bmuted\b/.test(video));
assert(home.includes('id="transkrip"') && home.includes('Naskah video pengenalan'));
const captionTrack = home.match(/<track kind="captions" src="([^"]+)" srclang="id"/);
assert(captionTrack, 'Indonesian captions missing');
const [header, ...cues] = fs.readFileSync(path.join(root, captionTrack[1]), 'utf8').trim().split(/\r?\n\r?\n/);
assert.equal(header, 'WEBVTT');
const seconds = stamp => Number(stamp.slice(0, 2)) * 60 + Number(stamp.slice(3));
let previousEnd = 0;
const captionText = cues.map(cue => {
  const [timing, ...lines] = cue.split(/\r?\n/);
  const [, start, end] = timing.match(/^(\d{2}:[0-5]\d\.\d{3}) --> (\d{2}:[0-5]\d\.\d{3})$/) ?? [];
  assert(start && end && lines.length && seconds(start) >= previousEnd
    && seconds(start) < seconds(end) && seconds(end) <= 52, 'Invalid caption cue: ' + timing);
  previousEnd = seconds(end);
  return lines.join(' ');
});
const script = fs.readFileSync(path.join(__dirname, '../design/promos/2026-11-12/script-approved.txt'), 'utf8');
const normalize = value => value.trim().replace(/\s+/g, ' ');
assert.equal(normalize(captionText.join(' ')), normalize(script), 'Captions must match approved narration');
assert(home.includes('unduhan beta belum dibuka'));
console.log(`PASS: ${sourceOnly ? 'source-only' : 'local assets'}, navigation, candidate metadata, HOLD download, video controls/no autoplay, transcript and captions`);
if (missingReviewMedia.length) console.log(`Local review media absent (not a playback test): ${missingReviewMedia.join(', ')}`);
