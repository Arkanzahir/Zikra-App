import urllib.request,json,hashlib,tarfile,zipfile,xml.etree.ElementTree as ET
from pathlib import Path
root=Path('/workspace')
(root/'setup-zikra').mkdir(exist_ok=True)
def fetch(url):
 req=urllib.request.Request(url,headers={'User-Agent':'Zikra-Android-Setup/1.0'})
 with urllib.request.urlopen(req,timeout=180) as r:return r.read()
def download(url,path,algorithm,expected):
 if not path.exists() or hashlib.new(algorithm,path.read_bytes()).hexdigest()!=expected:path.write_bytes(fetch(url))
 assert hashlib.new(algorithm,path.read_bytes()).hexdigest()==expected
meta=json.loads(fetch('https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=linux&vendor=eclipse'))[0]
pkg=meta['binary']['package'];archive=root/'setup-zikra/jdk17.tar.gz';download(pkg['link'],archive,'sha256',pkg['checksum'])
(root/'java').mkdir(exist_ok=True)
with tarfile.open(archive) as f:
 folder=f.getmembers()[0].name.split('/')[0]
 if not (root/'java'/folder/'bin/javac').exists():f.extractall(root/'java',filter='data')
metadata=ET.fromstring(fetch('https://dl.google.com/android/repository/repository2-3.xml'))
package=next(x for x in metadata if x.tag.endswith('remotePackage') and x.get('path')=='cmdline-tools;latest')
platform_archive=next(x for x in package.findall('./archives/archive') if x.findtext('host-os')=='linux')
complete=platform_archive.find('complete');url='https://dl.google.com/android/repository/'+complete.findtext('url');checksum=complete.find('checksum')
archive=root/'setup-zikra/cmdline-tools.zip';download(url,archive,checksum.get('type','sha1'),checksum.text)
sdk=root/'android-sdk';target=sdk/'cmdline-tools/latest'
if not (target/'bin/sdkmanager').exists():
 temp=root/'setup-zikra/cmdline-unpack';temp.mkdir(exist_ok=True)
 with zipfile.ZipFile(archive) as f:f.extractall(temp)
 target.parent.mkdir(parents=True,exist_ok=True);(temp/'cmdline-tools').rename(target)
 for p in (target/'bin').iterdir():p.chmod(0o755)
java=root/'java'/folder
env=f'export JAVA_HOME={java}\nexport ANDROID_HOME={sdk}\nexport ANDROID_SDK_ROOT={sdk}\nexport ANDROID_USER_HOME=/workspace/android-user\nexport ANDROID_AVD_HOME=/workspace/android-user/avd\nexport GRADLE_USER_HOME=/workspace/gradle-cache\nexport PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"\n'
(root/'setup-zikra/env.sh').write_text(env)
print('Verified JDK 17 and Android command-line tools installed.')
