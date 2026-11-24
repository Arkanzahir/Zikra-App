# Uji chat mandiri dari APK signed di staging

Kandidat `1.9.64`/code91 tetap HOLD. APK kandidat lokal cocok dengan ukuran dan
SHA-256 dalam [manifest internal](../../../docs/beta/internal-artifact.json).
APK instrumen terpisah berhasil dibangun dan sertifikatnya cocok dengan
kandidat. Permintaan metadata staging `dpl_68Cae68VFhkZHE7jUexdwKDHY8pS` melalui
sesi CLI Vercel mendapat HTTP 403. Tes hosted masih menunggu akses staging dan
akun sintetis terverifikasi. Hasil ini belum membuktikan percakapan hosted.

Instrumen memakai `AssistantConnection`, `AccountSession.interceptor()` dan
`OnlineAssistantClient` dari APK signed. Interceptor khusus instrumen hanya
meneruskan chat yang sudah divalidasi klien ke preview HTTPS `zikra-ai` dan
menyertakan kredensial akses preview melalui header resmi Vercel. Produksi tetap
menjadi host dalam APK kandidat. Pendaftaran dan pencabutan dipanggil melalui
HTTP di staging, jadi tes ini tidak mencakup layar UI pengelolaan akun.

1. Siapkan preview terlindungi yang sudah disetujui pemilik. Pastikan konfigurasi
   self-service aktif. Pakai akun Firebase sintetis milik pengujian dengan email
   terverifikasi dan UID yang belum didaftarkan ke preview. Siapkan cara
   menghapus akun dan data fixture Redis sebelum menjalankan uji.
2. Pakai emulator Android 36 baru. Cocokkan SHA-256 APK kandidat dengan manifest.
   Pasang APK kandidat dan APK instrumen dengan sertifikat yang sama. Paket
   instrumen `com.zikra.app.test` menargetkan `com.zikra.app`.
3. Siapkan JSON privat berisi `preview` (URL unik deployment), `previewAccess`
   (akses Automation Protection milik operator), `email`, `password`, dan `uid`.
   Simpan di luar Git. Transfer hanya ke emulator uji, ke
   `/data/local/tmp/zikra-signed-self-service-staging.json`, dengan mode `600`
   untuk shell. Jangan kirim melalui argumen instrumentation atau log.
4. Jalankan satu metode pada serial emulator eksplisit:

   ```powershell
   adb -s <serial-emulator-uji> shell am instrument -w -e class 'com.zikra.app.assistant.SignedSelfServiceStagingTest#testSignedChatWithoutInstallationAndWithdrawal' com.zikra.app.test/android.test.InstrumentationTestRunner
   ```

5. Hasil yang diperlukan adalah chat dengan jawaban sintetis `63` memakai token
   Firebase tanpa header token instalasi, penolakan lokal tanpa permintaan chat
   tambahan setelah `hold()`, dan HTTP 403 `adult_confirmation_required` dari
   preview untuk token yang sama setelah pencabutan server. Catat status tanpa
   konten akun. Hapus JSON dari emulator, hapus akun dan data fixture miliknya,
   lalu lepaskan instrumen atau tutup emulator uji.

Kompilasi dari `android`, dengan JDK, SDK, dan cache lokal yang tercatat di
[bukti akun](../adult-accounts-2026-11-15/README.md):

```powershell
./gradlew.bat :app:assembleDebugAndroidTest -PzikraHostedTests=true -PzikraAdultAccounts=true -PzikraAccountSignIn=true --offline --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=512m'
```

APK instrumen privat disimpan di `.local/releases/Zikra-Signed-Self-Service-Staging-Instrumentation.apk`.
SHA-256 `821efcaae33926843e091e3474a1b4a5481b7121c27b00931da018daf7eeb1a6`.
APK instrumen dan konfigurasi uji tidak masuk distribusi. Pengujian Hapus Akun
tetap memerlukan izin layanan Firebase yang tercatat di [status rilis](../../../docs/beta/PUBLIC_RELEASE_BLOCKERS.md).
