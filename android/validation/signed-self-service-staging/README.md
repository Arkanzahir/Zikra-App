# Uji chat mandiri dari APK signed di staging

## Instrumen untuk kandidat signed 1.9.66

Instrumen sekarang memeriksa versi 1.9.66 dan memastikan `AccountSession.hold()`
berhasil menyimpan status sebelum menguji pemblokiran lokal. Build
`:app:assembleDebugAndroidTest` dengan `-PzikraHostedTests=true` lulus.
Sertifikat APK instrumen cocok dengan sertifikat kandidat. Paket privat
`.local/releases/Zikra-v1.9.66-hosted-instrumentation.apk` memiliki SHA-256
`2f63d8f4c5266a56bdaf3bbd3ab6a28d827819a281604da6ec4ab5aa5345b83b`.
Kandidat release tetap cocok dengan manifest internal.

Pemeriksaan proyek `zikra-ai` menemukan proteksi `all_except_custom_domains`
aktif. Proyek Firebase `zikra-account-staging` dengan paket Spark, login email,
dan klien Android telah dibuat. Secret pembaca serta penghapus dipasang khusus
di Preview. Satu akun terverifikasi sintetis sudah tersedia. Izin sementara
`firebaseauth.users.create` dicabut setelah akun dibuat. API IAM memverifikasi
pembaca memiliki `firebaseauth.users.get` dan penghapus memiliki
`firebaseauth.users.delete`, tanpa izin pembuatan pada keduanya. Sebanyak
178 tes backend lulus tanpa skip. Uji hosted masih menunggu Redis terpisah
dan deployment.

## Uji Redis hosted untuk kandidat 1.9.66

Uji integrasi berikutnya memakai Firebase Auth Emulator resmi, Firebase Admin
SDK, handler HTTP Zikra, dan Redis Upstash sementara lewat HTTPS. Akses akun
sebelum verifikasi email ditolak dengan HTTP 401. Setelah verifikasi dan
deklarasi usia, chat tanpa token instalasi mengembalikan HTTP 200 dan jawaban
sintetis `63`. Endpoint Hapus Akun mengembalikan HTTP 200, akun hilang dari
Firebase Auth Emulator, dan penghitung kuota dihapus. Token lama ditolak
oleh endpoint akun dan chat dengan HTTP 401. Login ulang ditolak oleh
Firebase Auth Emulator. Redis kembali kosong setelah pembersihan.

Semua identitas uji sintetis. Handler HTTP dijalankan di loopback dengan
provider jawaban sintetis. APK, Firebase produksi, dan Vercel Preview
tidak dipakai dalam uji integrasi ini.

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

Saat uji awal, kandidat `1.9.64`/code91 berstatus HOLD. APK lokal cocok dengan
ukuran dan SHA-256 yang tercatat untuk versi tersebut.
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
   (akses Automation Protection milik operator), `email`, `password`, `uid`,
   serta `firebase` yang berisi `projectId`, `applicationId`, dan `apiKey`
   dari aplikasi Android di proyek Firebase staging. Instrumen memilih project
   tersebut di emulator. APK kandidat tetap memakai konfigurasi produksinya.
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

Untuk uji penghapusan, siapkan akun baru dengan alamat
`zikra-staging-<id>@example.test` dan pastikan operator menandai email akun
sintetis itu sebagai terverifikasi. Atur JSON fixture dengan
`"allowSyntheticDeletion": true`, dan jalankan
`SignedSelfServiceStagingTest#testSignedChatAndSyntheticAccountDeletion`
pada emulator yang bersih. Metode ini menguji chat dan pencabutan, memanggil
endpoint penghapusan, memeriksa HTTP 401 untuk token lama, lalu memastikan
Firebase menolak login ulang. Skenario penghapusan memakai endpoint melalui
instrumen. Antarmuka Hapus Akun memerlukan build dengan fitur itu aktif.

Kompilasi dari `android`, dengan JDK, SDK, dan cache lokal yang tercatat di
[bukti akun](../adult-accounts-2026-11-15/README.md):

```powershell
./gradlew.bat :app:assembleDebugAndroidTest -PzikraHostedTests=true -PzikraAdultAccounts=true -PzikraAccountSignIn=true --offline --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=512m'
```

APK instrumen dan konfigurasi uji tidak masuk distribusi. Pengujian Hapus Akun
tetap memerlukan izin layanan Firebase yang tercatat di [status rilis](../../../docs/beta/PUBLIC_RELEASE_BLOCKERS.md).
