# Uji chat mandiri dari APK signed di staging

## Uji Redis hosted untuk kandidat 1.9.66

Pada 24 November 2026, kontrak akun pada backend lokal lulus terhadap database
Upstash gratis yang terpisah dan kedaluwarsa otomatis setelah 72 jam. Uji
menggunakan REST HTTPS, identitas dan handler Firebase sintetis, serta Lua
aplikasi. Registrasi dan akuisisi izin chat tanpa token instalasi lulus.
Percobaan penghapusan yang gagal meninggalkan penanda yang memblokir akses
lama. Percobaan berikutnya berhasil dan otorisasi lama ditolak. Uji juga
memastikan data milik identitas sintetis lain tidak ikut terhapus.

Pemeriksaan `SCAN` setelah pembersihan mengembalikan nol kunci. Kredensial
hanya dipakai dalam proses uji. Produksi, data pribadi, dan HP tidak digunakan.
Pengujian ini memverifikasi Redis hosted dan kontrak backend lokal. Uji
menyeluruh memakai APK signed 1.9.66, backend Preview Vercel, dan Firebase
nyata masih terbuka.

## Riwayat instrumen kandidat 1.9.64

Kandidat `1.9.64`/code91 tetap HOLD. APK kandidat lokal cocok dengan ukuran dan
SHA-256 dalam [manifest internal](../../../docs/beta/internal-artifact.json).
APK instrumen terpisah berhasil dibangun dan sertifikatnya cocok dengan
kandidat. Pada 24 November 2026, Vercel CLI 66.6.0 berhasil membaca proyek dan
deployment `dpl_68Cae68VFhkZHE7jUexdwKDHY8pS`. Deployment tersebut berlabel
Production. Daftar deployment dan variabel Preview untuk `zikra-ai` kosong.
Preview terlindungi, Redis terpisah, dan akun sintetis terverifikasi masih
diperlukan. Hasil ini belum membuktikan percakapan hosted.

## Regresi lokal 24 November 2026

Source `0e2293c` diuji sebelum perubahan dokumen ini. Semua 177 tes backend
lulus tanpa skip dengan fixture Redis/Lua. Percobaan pertama di sandbox gagal
pada akses direktori sementara Windows untuk tes backup. Pengulangan dengan
akses yang diizinkan lulus lengkap.

Build debug untuk Auth Emulator dan APK instrumentation berhasil dipasang ke
AVD API 36 baru. Fixture memakai Firebase Auth Emulator dan Redis/Lua sintetis
di loopback dengan jawaban AI sintetis. Sepuluh tes lulus dalam 27,356 detik.

| Suite | Lulus | Cakupan |
|---|---:|---|
| `AdultAccountFlowEmulatorTest` | 5 | Chat tanpa token instalasi, HTML 504 dan percobaan ulang, pencabutan, batas usia serta host token |
| `AccountAuthEmulatorTest` | 3 | Pendaftaran dan verifikasi, masuk ulang, reset sandi lokal, pembatalan hapus akun dan penolakan login setelah hapus |
| `AccountScreenNativeTest` | 2 | Persetujuan, validasi input dan penyimpanan sandi |

Dua startup awal belum mencapai readiness HTTP. Run akhir memakai modul Auth
Emulator Firebase yang dipasang lokal dan probe loopback tanpa proxy. ADB
offline pulih melalui server uji terpisah. APK terpasang dalam run ini adalah
build debug. Tidak ada akun, kuota, sumber daya hosting, atau data produksi yang
diubah. Uji signed ke Preview tetap terbuka.

CLI Vercel Marketplace menampilkan hanya plan berbayar untuk Upstash Redis
tambahan di akun ini. [Harga dan batas Upstash](https://upstash.com/pricing/redis).
Paket sumber Preview privat `zikra-ai-preview-source-0e2293c.zip` berisi 38
berkas terlacak dari allowlist Vercel. Paket tidak memuat env atau kredensial.

## Uji hosted

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
