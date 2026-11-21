# Kandidat signed Zikra 1.9.59 — 21 November 2026

Kandidat internal `.local/releases/Zikra-v1.9.59-account-candidate.apk`. Versi 1.9.59, kode 86, ukuran 38.946.195 byte, target API 36. Build memakai arsip source `ef5a1a3` dengan kenaikan nomor versi pada checkpoint ini. Perubahan lokal di luar commit tidak masuk APK.

- Gradle `:app:assembleRelease :app:testReleaseUnitTest` dengan `-PzikraReleaseAdultAccounts=true`. Seluruh 237 tes JVM release lulus tanpa skip. Scope beta, login, dan akun 18+ aktif pada APK. Auth emulator mati.
- Signer release cocok dengan 1.9.58. Pemindaian APK menemukan nol konfigurasi privat, nol kecocokan kredensial yang dipindai, dan nol MP3 adzan terbundel. [Receipt SHA-256 dan signer](audit.json).
- `adb install -r` memperbarui 1.9.58 ke 1.9.59 di emulator API 36 offline. ID dan direktori data aplikasi serta waktu pemasangan awal cocok. Data sintetis tugas, jadwal, draf AI, dan bookmark hadis cocok pada empat database. Preferensi bookmark dan pemeriksaan integritas SQLite lulus.
- Splash, Study, AI, Hadis, dan layar akun berhasil dibuka. Emulator serta server ADB tes dihentikan setelah verifikasi.

## Uji update di HP

- Cadangan terenkripsi baru disalin ke lokasi privat sebelum update. Ukuran 748.987 byte dan SHA-256 salinan cocok dengan file di HP. Pemulihan dari cadangan baru belum diuji.
- Update langsung melalui ADB dari APK historis 1.9.55/code82 ke signed 1.9.59/code86. Hash APK lama cocok dengan receipt historis. ID aplikasi, direktori data dan waktu instalasi awal cocok setelah update.
- Pemeriksaan baca-saja lolos untuk integritas SQLite pada lima database. Jumlah baris pada sepuluh tabel cocok sebelum dan sesudah update. Isi setiap baris tidak dibandingkan. Helper uji sementara sudah dihapus. Zikra berhasil dibuka lewat launcher.

Rilis tetap HOLD. Akses AI mandiri masih berada di staging terlindungi. Pemilihan MP3 dan suara adzan pada HP masih perlu diuji. [Bukti akun dan staging](../../../docs/beta/ACCOUNT_PRODUCTION_2026-11-21.md).
