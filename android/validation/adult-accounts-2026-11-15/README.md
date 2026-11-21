# Akun AI 18+ — checkpoint internal15 November 2026

Source1.9.58/code85, debug emulator. HOLD; belum signed release58, belum dipasang diHP, dan layanan akun belum dideploy ke backend produksi. Tidak mengubah migrasi/dataHP 55. Pengguna memilih AI online 18+ dahulu, alur wali ditunda. Fitur lokal tidak otomatis menjadi18+.

## Bukti yang benar-benar ada

-175backend tests lulus,0skip;237JVM lulus,0skip. Lint0error/912warning. npm audit dependency pada run ini0vulnerability. [Receipt](proof.json), [ringkasan backend](backend-summary.txt).
-5native lulus dalam12,314detik menggunakan emulator AndroidAPI36, Firebase Auth Emulator resmi, Admin SDK, Redis fixture nyata/Lua dan jawaban AI sintetis. [Ringkasan](native-summary.txt). Tidak mengirim email produksi atau data pribadi.
- Kasus: pendaftaran/email terverifikasi/deklarasi→HTTP→AIstub→withdraw/local hold/server403; under18 diblokir; signupAndroid under18 ditahan sebelum membuat akunFirebase; endpoint/token tidak diteruskan ke host lain atau produksi dari buildemulator; recreation mengosongkanDOB/checkbox.
- Build debug44.924.550byte. Bukan APK yang boleh ditawarkan ke publik. Tidak ada audit APK release58 atau pemasanganHP 58.
- Tes lokal Node22.17.0; Vercel target24.x. Tidak mengklaim runtimeNode24 diuji lokal.

## Implementasi

Server memverifikasi tokenFirebase/revocation/current disabled dan email, mensyaratkan login baru≤5menit untuk declare/renew/withdraw. Usia dihitung pada zonaAsia/Jakarta. DOB diproses untuk permintaan, tidak disimpan dalam record. MetadataUID/HMAC/ageband/policy/revision/timestamp mempunyai TTL90hari sejak perubahan; konfirmasi berlaku30hari. Assurance adalah self_declared, bukan identitas/usia terverifikasi. Penolakan under18 bukan jaminan anti-pemalsuanDOB.

X-Zikra-Account-Token hanya ke backend pertama; tidak diteruskan ke model. Gate chat/gambar/laporan diperiksa ulang sebelum operasi dan sesudah jawaban tertentu. Android mengikat permintaan padaUID/revision dan menolak callback lama. Pencabutan membuat local hold persisten sebelumRPC. Kegagalan penyimpanan commit/localdisk+network+restart belum diuji dengan fault injection; jangan klaim tanpa celah.

## Rollout belum selesai

`ZIKRA_ADULT_ACCOUNTS_ENABLED` mengaktifkan endpoint akun; `ZIKRA_ADULT_ACCOUNTS_REQUIRED` mewajibkan akun padaAI. Pengguna menyetujui keyAuth reader/privat Vercel dan deploy layanan akun dahulu, kewajibanAI menyusul setelah APK siap. Belum dilakukan. Seluruh accountflagreleaseAndroid masihfalse; jangan mengunci pengguna lama lebih dahulu.

Firebase service account zikra-auth-reader dengan Firebase Authentication Viewer sudah dibuat melalui konsol. Satu key dibuat, tetapi berkas privat belum berhasil ditemukan/diambil. Jangan membuat key baru atau menganggap konfigurasinya siap tanpa memeriksa inventory. Jangan memakai FCM sender, testsubject key, emulator atau TLSoff untuk produksi. [Langkah tersisa](../../../docs/beta/CURRENT_STATUS.md).

Proses utama emulator Android5592/ADB5048, Firebase Auth Emulator, serta server fixture9098 dihentikan saat serah-terima16November. Testpackage/marker dibersihkan dan font emulator dipulihkan2.0. HP/defaultADB5037 tidak disentuh. Localwebpreview tidak menjadi dependensi hosting online.

## Reproduksi tanpaHP

Backend: jalankan dari root repo dengan D:/node.exe. Agar20tesRedis tidak dilewati, set PYTHONPATH ke .local/vercel-test-venv/Lib/site-packages dan ZIKRA_REDIS_TEST_PYTHON ke PythonbundledCodex, lalu `D:/node.exe --test backend/*.test.mjs`. Jangan memakai kredensialRedisproduksi untuk fixture.

Android memerlukan JAVA_HOME=C:/Users/Lenovo/.jdks/jbr-17.0.14, ANDROID_HOME=E:/AndroidSDK, GRADLE_USER_HOME=E:/.gradle. Fixturelokal: ZIKRA_HADEETHENC_FIXTURE=.local/hadeethenc-audit-20261112/HadeethEnc.com_id-v1.22.0.csv, ZIKRA_MUSHAF_AUDIT_DIR=.local/mushaf-audit, ZIKRA_DOA_AUDIT_FILE=.local/equran-doa-current.json (gunakan pathabsolut). Perintah yang dipakai:

```powershell
./android/gradlew.bat -p android :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug -PzikraPublicBetaScope=true -PzikraAccountSignIn=true -PzikraAccountEmulator=true -PzikraAdultAccounts=true --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=512m'
```

Untuk menjalankan `AdultAccountFlowEmulatorTest`, mulai Firebase Auth Emulator untuk proyek `demo-zikra-accounts` pada `127.0.0.1:9099`. Set `FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099` dan arahkan `ZIKRA_REDIS_TEST_PYTHON` ke Python yang memiliki `fakeredis[lua]`. Jalankan `node backend/test-support/android-account-server.mjs` dari root repo. Server sintetis hanya mendengar pada `127.0.0.1:9098`. Gunakan build debug dengan opsi Auth Emulator dan akun 18+ aktif. Buat marker `files/account-emulator-authorized` melalui `run-as com.zikra.app` hanya di emulator milik pengujian. Pengujian ini memakai data sintetis dan terbatas pada emulator lokal.
