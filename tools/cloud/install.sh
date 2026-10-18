#!/usr/bin/env bash
# Local toolchain only: no reset/pull, no source/dependency edits, no credentials.
set -euo pipefail
zikra_repo=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
mkdir -p /workspace/setup-zikra /workspace/android-user/adb /workspace/gradle-cache
python3 "$zikra_repo/tools/cloud/bootstrap.py"
source /workspace/setup-zikra/env.sh
cp "$JAVA_HOME/lib/security/cacerts" /workspace/setup-zikra/cacerts
zikra_ca=/usr/local/share/ca-certificates/environment-proxy-ca.crt
if [ -f "$zikra_ca" ]; then
  keytool -importcert -noprompt -alias zikra-cloud-proxy -file "$zikra_ca" \
    -keystore /workspace/setup-zikra/cacerts -storepass changeit
fi
cat >> /workspace/setup-zikra/env.sh <<'ZIKRA_ENV'
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Djavax.net.ssl.trustStore=/workspace/setup-zikra/cacerts"
ZIKRA_ENV
source /workspace/setup-zikra/env.sh
python3 - <<'PY'
import os, pathlib, urllib.parse, shlex
proxy = urllib.parse.urlparse(os.environ.get('HTTPS_PROXY', os.environ.get('https_proxy', '')))
path = pathlib.Path('/workspace/gradle-cache/gradle.properties')
lines = path.read_text().splitlines() if path.exists() else []
keys = ('systemProp.http.proxyHost=', 'systemProp.http.proxyPort=', 'systemProp.https.proxyHost=', 'systemProp.https.proxyPort=')
lines = [line for line in lines if not line.startswith(keys)]
if proxy.hostname:
    for protocol in ('http', 'https'):
        lines += [f'systemProp.{protocol}.proxyHost={proxy.hostname}', f'systemProp.{protocol}.proxyPort={proxy.port or 80}']
path.write_text('\n'.join(lines)+'\n')
path.chmod(0o600)
values = {'ZIKRA_SDK_PROXY_HOST': proxy.hostname or '', 'ZIKRA_SDK_PROXY_PORT': str(proxy.port or 80)}
pathlib.Path('/workspace/setup-zikra/sdk-proxy.sh').write_text(''.join(f'export {k}={shlex.quote(v)}\n' for k,v in values.items()))
PY
source /workspace/setup-zikra/sdk-proxy.sh
zikra_proxy_args=()
if [ -n "$ZIKRA_SDK_PROXY_HOST" ]; then
  zikra_proxy_args=(--proxy=http --proxy_host="$ZIKRA_SDK_PROXY_HOST" --proxy_port="$ZIKRA_SDK_PROXY_PORT")
fi
python3 -c 'from pathlib import Path; Path("/workspace/setup-zikra/licenses-input").write_text("y\n"*100)'
sdkmanager --sdk_root="$ANDROID_HOME" "${zikra_proxy_args[@]}" --licenses < /workspace/setup-zikra/licenses-input
sdkmanager --sdk_root="$ANDROID_HOME" "${zikra_proxy_args[@]}" 'platforms;android-35' 'build-tools;35.0.0' platform-tools emulator 'system-images;android-28;default;x86_64'
# Preserve HOME. Latest ADB uses ~/.android even when ANDROID_USER_HOME is set.
if [ ! -e "$HOME/.android" ] && [ ! -L "$HOME/.android" ]; then
  ln -s /workspace/android-user/adb "$HOME/.android"
fi
if [ ! -w "$HOME/.android" ]; then
  echo 'ADB needs a writable ~/.android directory; keep HOME unchanged.' >&2
  exit 1
fi
# local.properties is ignored. Preserve pre-existing local properties.
if [ ! -e "$zikra_repo/android/local.properties" ]; then
  printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$zikra_repo/android/local.properties"
fi
java -version
javac -version
"$zikra_repo/android/gradlew" -p "$zikra_repo/android" assembleDebug assembleDebugAndroidTest lintDebug --console=plain
