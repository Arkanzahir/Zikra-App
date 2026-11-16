const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execFileSync } = require('node:child_process');

const repo = path.resolve(__dirname, '..');
const projectFile = path.join(repo, '.local/website-deploy-20261116/project-response.json');
const project = JSON.parse(fs.readFileSync(projectFile, 'utf8').replace(/^\uFEFF/, ''));
if (project.name !== 'zikra-web' || project.ssoProtection?.deploymentType !== 'prod_deployment_urls_and_all_previews') {
  throw new Error('Expected a dedicated protected website project; no upload package prepared.');
}
execFileSync(process.execPath, [path.join(__dirname, 'check.cjs')], { stdio: 'inherit' });
const source = path.join(__dirname, 'dist');
const walk = directory => fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
  const file = path.join(directory, entry.name);
  if (entry.isSymbolicLink()) throw new Error('Symlinks are not accepted in the upload source.');
  return entry.isDirectory() ? walk(file) : [file];
});
const files = walk(source);
const manifest = files.map(file => {
  const relative = path.relative(source, file).replaceAll('\\', '/');
  if (!/^(?:index\.html|privacy\.html|style\.css|assets\/[a-zA-Z0-9_./-]+\.(?:svg|png|ttf|txt)|media\/(?:zikra-promo\.mp4|promo-poster\.jpg|ai-demo\.jpg))$/.test(relative)) {
    throw new Error(`Unexpected upload file: ${relative}`);
  }
  const data = fs.readFileSync(file);
  return { file: relative, bytes: data.length, sha256: crypto.createHash('sha256').update(data).digest('hex') };
});
const totalBytes = manifest.reduce((sum, file) => sum + file.bytes, 0);
if (totalBytes > 20000000) throw new Error('Unexpected preview upload size.');
const stage = fs.mkdtempSync(path.join(repo, '.local/website-preview-upload-'));
for (const file of manifest) {
  const target = path.join(stage, file.file);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.copyFileSync(path.join(source, file.file), target);
}
fs.copyFileSync(path.join(__dirname, 'vercel.json'), path.join(stage, 'vercel.json'));
fs.mkdirSync(path.join(stage, '.vercel'));
fs.writeFileSync(path.join(stage, '.vercel/project.json'), JSON.stringify({
  projectId: project.id, orgId: 'team_jMImrdodMK1NtHYe1ZXuWvRG', projectName: project.name,
}));
const receipt = { stage, project: project.name, environment: 'preview', files: manifest, totalBytes, includesApk: false };
fs.writeFileSync(path.join(repo, '.local/website-deploy-20261116/upload-receipt.json'), JSON.stringify(receipt, null, 2));
console.log(JSON.stringify({ stage, files: files.length, totalBytes, includesApk: false }));
