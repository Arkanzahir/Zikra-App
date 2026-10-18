#!/usr/bin/env bash
set -euo pipefail
source /workspace/setup-zikra/env.sh
zikra_repo=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
zikra_device=emulator-5558
zikra_avd=Zikra_Recovery_API28
if ! adb devices | awk '{print $1}' | rg -qx "$zikra_device"; then
  if [ ! -d "$ANDROID_AVD_HOME/$zikra_avd.avd" ]; then
    printf 'no\n' | avdmanager create avd -n "$zikra_avd" -k 'system-images;android-28;default;x86_64' --device pixel_2
    cat >> "$ANDROID_AVD_HOME/$zikra_avd.avd/config.ini" <<'AVD'
hw.lcd.width=540
hw.lcd.height=1170
hw.lcd.density=210
hw.ramSize=2048
vm.heapSize=256
hw.keyboard=yes
hw.audioInput=no
skin.dynamic=yes
showDeviceFrame=no
AVD
  fi
  nohup "$ANDROID_HOME/emulator/emulator" "@$zikra_avd" -no-window -no-audio -no-boot-anim -no-snapshot -accel off -gpu swiftshader -cores 2 -memory 2048 -port 5558 > /workspace/setup-zikra/emulator.log 2>&1 < /dev/null &
fi
# Short polls keep readiness visible; a first software boot can take several minutes.
for zikra_poll in $(seq 1 120); do
  if [ "$(adb -s "$zikra_device" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; then break; fi
  if [ "$zikra_poll" -eq 120 ]; then echo 'Emulator boot timed out; inspect emulator.log.' >&2; exit 1; fi
  if [ "$((zikra_poll % 6))" -eq 0 ]; then echo 'Waiting for Android boot…'; fi
  sleep 5
done
adb -s "$zikra_device" root
adb -s "$zikra_device" wait-for-device
python3 - <<'PY'
import os, socket, subprocess, urllib.parse
proxy=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',os.environ.get('https_proxy','')))
if proxy.hostname:
    # Android cannot resolve the container-only proxy name; use its resolved address.
    address=socket.gethostbyname(proxy.hostname)
    subprocess.run(['adb','-s','emulator-5558','shell','settings','put','global','http_proxy',f'{address}:{proxy.port or 80}'],check=True)
PY
# Install the injected CA in this test emulator only. APK trust policy is unchanged.
zikra_ca=/usr/local/share/ca-certificates/environment-proxy-ca.crt
if [ -f "$zikra_ca" ]; then
  zikra_ca_hash=$(openssl x509 -in "$zikra_ca" -subject_hash_old -noout)
  adb -s "$zikra_device" shell mkdir -p /data/local/tmp/zikra-cacerts
  adb -s "$zikra_device" shell cp -r /system/etc/security/cacerts/. /data/local/tmp/zikra-cacerts/
  adb -s "$zikra_device" push "$zikra_ca" "/data/local/tmp/zikra-cacerts/$zikra_ca_hash.0"
  if ! adb -s "$zikra_device" shell mount | rg -q 'tmpfs on /system/etc/security/cacerts'; then
    adb -s "$zikra_device" shell mount -t tmpfs tmpfs /system/etc/security/cacerts
  fi
  adb -s "$zikra_device" shell cp -r /data/local/tmp/zikra-cacerts/. /system/etc/security/cacerts/
  adb -s "$zikra_device" shell chmod 644 "/system/etc/security/cacerts/$zikra_ca_hash.0"
  adb -s "$zikra_device" shell chcon u:object_r:system_file:s0 "/system/etc/security/cacerts/$zikra_ca_hash.0"
fi
adb -s "$zikra_device" shell wm dismiss-keyguard
adb -s "$zikra_device" install -r "$zikra_repo/android/app/build/outputs/apk/debug/app-debug.apk"
adb -s "$zikra_device" shell am start -n com.zikra.app/.SplashActivity
for zikra_ready in $(seq 1 60); do
  zikra_foreground=$(adb -s "$zikra_device" shell dumpsys activity activities | rg 'mResumedActivity' || true)
  if [[ "$zikra_foreground" == *com.zikra.app/.HomeActivity* || "$zikra_foreground" == *com.zikra.app/.OnboardingActivity* ]]; then
    printf '%s\n' "$zikra_foreground"
    exit 0
  fi
  sleep 1
done
echo 'Zikra did not leave splash; inspect Activity/logcat before retry.' >&2
exit 1
