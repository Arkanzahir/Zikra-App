# Kandidat signed Zikra 1.9.59 — 21 November 2026

Kandidat internal `.local/releases/Zikra-v1.9.59-account-candidate.apk`. Versi 1.9.59, kode 86, ukuran 38.946.195 byte, target API 36. Build memakai arsip source `ef5a1a3` dengan kenaikan nomor versi pada checkpoint ini. Perubahan lokal di luar commit tidak masuk APK.

- Gradle `:app:assembleRelease :app:testReleaseUnitTest` dengan `-PzikraReleaseAdultAccounts=true`. Seluruh 237 tes JVM release lulus tanpa skip. Scope beta, login, dan akun 18+ aktif pada APK. Auth emulator mati.
- Signer release cocok dengan 1.9.58. Pemindaian APK menemukan nol konfigurasi privat, nol kecocokan kredensial yang dipindai, dan nol MP3 adzan terbundel. [Receipt SHA-256 dan signer](audit.json).
- `adb install -r` memperbarui 1.9.58 ke 1.9.59 di emulator API 36 offline. ID dan direktori data aplikasi serta waktu pemasangan awal cocok. Data sintetis tugas, jadwal, draf AI, dan bookmark hadis cocok pada empat database. Preferensi bookmark dan pemeriksaan integritas SQLite lulus.
- Splash, Study, AI, Hadis, dan layar akun berhasil dibuka. Emulator serta server ADB tes dihentikan setelah verifikasi.

Rilis tetap HOLD. HP belum diperbarui. Akses AI mandiri masih berada di staging terlindungi. Uji APK ini di emulator mencakup update offline dan layar, sedangkan verifikasi email dan inferensi staging dicatat terpisah. [Bukti akun dan staging](../../../docs/beta/ACCOUNT_PRODUCTION_2026-11-21.md).
