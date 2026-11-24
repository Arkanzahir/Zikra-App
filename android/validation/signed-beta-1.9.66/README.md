# Kandidat signed Zikra 1.9.66

24 November 2026. Kandidat standar dengan code93 berstatus **HOLD**. APK
dibangun dari arsip Git `3cde8f0ff8eb6c30a58e4366c8067a1ac718996c` dan
perubahan versi `android/app/build.gradle` pada checkpoint ini. Build memakai
klien Firebase produksi untuk akses akun 18+. Auth Emulator dan Hapus Akun
nonaktif.

| Pemeriksaan | Hasil |
|---|---|
| Signing dan paket | Sertifikat sama dengan 1.9.65. Audit menemukan 0 kecocokan rahasia yang dipindai dan 0 berkas konfigurasi privat. SHA-256 dua MP3 adzan cocok dengan sumber di [kredit APK](../../app/src/main/assets/bundled-adhan/CREDITS.txt). |
| JVM dan lint | 248 tes release lulus tanpa skip. Lint mencatat 0 error dan 913 warning. |
| Upgrade emulator API 36 | Seed empat nilai baca sintetis pada signed 1.9.65, lalu jalankan `adb install -r` untuk signed 1.9.66. Kedua uji lulus. Nilai baca dan waktu pemasangan awal terjaga. |
| Murottal sesudah upgrade | Dua uji native lulus dalam 20,961 detik. Uji mencakup memutar ulang ayat yang macet dan menjaga unduhan offline saat cache sorotan kata kedaluwarsa. Uji memakai audio sintetis. |
| Unduhan lokal | HEAD dan GET HTTPS loopback mengembalikan 200 dengan sertifikat uji tepercaya, jenis konten APK, ukuran dan SHA-256 sesuai manifest. |
| Website | [Preview tanpa promo](https://zikra-k1pk6yh46-arkanzahir86-3257s-projects.vercel.app/) berstatus READY. Beranda, privasi, dan ketentuan mengembalikan 200 dengan `noindex` saat diperiksa dengan autentikasi. Ketiganya mengalihkan pengunjung anonim ke login. APK dan promo tidak disertakan. |

APK privat tersimpan di `.local/releases/Zikra-v1.9.66-beta-candidate.apk`.
[Manifest kandidat](../../../docs/beta/internal-artifact.json) mencatat ukuran,
SHA-256, dan sertifikatnya. CLI Vercel membuat satu akses otomasi saat uji
halaman. Akses itu dicabut setelah uji. Pengaturan login diverifikasi kembali
dan jumlah akses otomasi kembali ke nol.

Uji HP melalui USB, uji akun hosted pada backend Preview, media pemulihan
signing terpisah, persetujuan konten dan privasi, serta validasi URL unduhan
publik masih tercatat di [persyaratan rilis](../../../docs/beta/PUBLIC_RELEASE_BLOCKERS.md).
