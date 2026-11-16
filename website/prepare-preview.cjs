const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

const files = [
  {
    source: path.resolve(__dirname, '../.local/promo-no-cloud-20261112/review-0393.jpg'),
    target: 'ai-demo.jpg',
    sha256: '1a16b18096a543eae62f35c337444939825f5704a00439828cbb2be3161d73ed',
  },
  {
    source: path.resolve(__dirname, '../../Zikra-Promo-Lengkap-52s-Tanpa-Awan.mp4'),
    target: 'zikra-promo.mp4',
    sha256: '347d985b0ca9f2c7e12c33c148d2163661c32af0791c05cd631d24ffc4bb2407',
  },
  {
    source: path.resolve(__dirname, '../.local/promo-no-cloud-20261112/review-0267.jpg'),
    target: 'promo-poster.jpg',
    sha256: 'c0a9aaddc5dc18198ccbe10c3496cf40ecb75adb20eec57e4a27a3ef51c2c7c8',
  },
];
const media = path.join(__dirname, 'dist/media');
for (const file of files) {
  if (!fs.existsSync(file.source)) throw new Error(`Local review media missing: ${file.source}`);
  const bytes = fs.readFileSync(file.source);
  const digest = crypto.createHash('sha256').update(bytes).digest('hex');
  if (digest !== file.sha256) throw new Error(`Review media differs from approved version: ${file.target}`);
}
fs.mkdirSync(media, { recursive: true });
for (const file of files) fs.copyFileSync(file.source, path.join(media, file.target));
console.log('Prepared approved 52-second promo for local review only; public distribution remains on HOLD.');
