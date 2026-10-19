# Setup cloud Java/XML

Repo utama `/workspace/Zikra-App`, project `android/`; `prototype_zikra` hanya referensi lama. Task sudah terisolasi: **jangan buat worktree kecuali pengguna meminta**. Baca `RECOVERY.md`, README, BUILDING, API_PLAN dan VALIDATION terbaru; periksa status dan instruksi AGENTS. Jangan reset/pull atau mengubah source, dependency, lockfile untuk setup.

```bash
cd /workspace/Zikra-App
bash tools/cloud/install.sh
source /workspace/setup-zikra/env.sh
bash tools/cloud/start-emulator.sh
```

`install.sh` memasang JDK 17 dan Android command-line tools Linux dari metadata resmi dengan checksum arsip; SDK 35/build-tools 35.0.0/platform-tools/emulator serta image AOSP API 28. Gradle wrapper/dependency tetap milik repo. Arsip/JDK/SDK/cache/CA lokal berada di `/workspace`, bukan Git. CA proxy ditambahkan ke salinan truststore; verifikasi TLS/checksum tetap aktif. Host/port proxy diambil dari binding proses, tanpa menyalin kredensial. `local.properties` ignored dibuat hanya bila belum ada. Tidak ada service web/backend untuk build.

ADB terbaru memerlukan `~/.android` writable. Symlink ke `/workspace/android-user/adb` dibuat hanya jika belum ada; **HOME tidak diubah**. Pada instance ini pembuatan symlink di home memerlukan dan sudah mendapat persetujuan sandbox. Bila instance baru membatasi lokasi itu, selesaikan akses direktori tersebut; jangan menyamarkannya dengan mengganti HOME atau menyatakan pengujian perangkat sudah berhasil.

`start-emulator.sh` menggunakan AVD `Zikra_Recovery_API28`, port 5558, ANGLE + SwiftShader (`-gpu swangle -feature -Vulkan`) tanpa KVM. Ia menggunakan ulang device tersebut bila sudah ada, memeriksa `sys.boot_completed`, memasang proxy/CA hanya di emulator AOSP uji, memasang APK dan menjalankan entry Splash. Peluncuran pertama diarahkan aplikasi ke onboarding baseline; fokus OnboardingActivity diperiksa pada instance ini. Jangan hentikan emulator pengguna lain. Proses berjalan tidak ikut tersimpan oleh snapshot; jalankan startup lagi setelah pemulihan instance. Frame/fokus dan log aplikasi tetap perlu diperiksa: PID atau boot flag bukan bukti alur UI berhasil. Cold boot pertama dapat lambat dan System UI emulator dapat ANR; diagnosis log sebelum mengulang tes.

Pinterest pin `https://id.pinterest.com/pin/1110911433096315275/` diperiksa ulang pada 18 Oktober: HTTP 200, judul “Minimal App Design for Self Care”. Akses HTML tidak membuktikan animasi/video diputar atau desain telah dianalisis. Gunakan referensi lokal yang benar-benar tersedia; file `/workspace/setup-zikra/pinterest-reference.jpg` dari instance lama tidak ikut pulih.

Verifikasi satu putaran sesuai perubahan. Hasil terkini ada di `android/validation/location-mosques-2026-10-19/README.md`; laporan recovery lama bersifat historis. Tidak perlu mengulang build setelah perubahan dokumentasi. Jalankan suite tambahan hanya untuk perubahan/kekhawatiran relevan. Jangan menyamakan emulator API 28 dengan pengujian runtime API 35 atau HP fisik. Tidak perlu API key untuk build/metadata katalog publik; akses Quran Foundation Content Sync production belum ada.

Konfigurasi environment harus menyimpan **install_script**, **start_skill** dan kedua repository dengan HEAD aktual. Penyimpanan draft bukan publikasi snapshot. Tinjau/simpan lalu publish melalui pengaturan environment bila tool menyatakan `requires_publish`. Snapshot yang belum diuji pada task baru tidak boleh diklaim sudah bisa dipulihkan. Simpan pekerjaan kode dalam commit dan backup terpisah; jangan hanya bergantung pada draf/snapshot.

Pada ronde 19 Oktober, SystemUI/renderer EGL emulator SwiftShader awal mengalami ANR setelah pergantian ukuran layar. Backend ANGLE/SwiftShader dipakai untuk pemulihan dengan Vulkan guest dimatikan. Ini tetap rendering software, bukan bukti performa HP. Pastikan Launcher mendapat fokus setelah boot dan postboot dexopt selesai sebelum install/resize. Bila dialog SystemUI ANR terlihat, diagnosis lalu pulihkan dialog/proses uji; jangan menganggap Activity yang resumed berarti window sudah siap. Startup kini berhenti cepat bila log launcher berisi FATAL/PANIC. Jika lock AVD menunjuk PID zombie, periksa tidak ada instance aktif; cadangkan hanya lock basi sebelum mengulang, tanpa menghapus userdata.

Ronde lokasi menemukan satu root proxy yang belum dipasang pada emulator, sehingga HTTPS layanan langsung gagal meskipun host berhasil. `install.sh`/`start-emulator.sh` kini memuat seluruh sertifikat CA publik sesi dari `/usr/local/share/ca-certificates/*.crt`, bukan hanya satu nama file. Trust host/JDK/emulator tetap terpisah; tidak menyertakan CA cloud atau kebijakan trust permisif ke APK. Langkah import emulator diuji langsung, dan kedua skrip lulus `bash -n`.
