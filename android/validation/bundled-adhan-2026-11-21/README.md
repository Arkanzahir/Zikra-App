# Adzan bawaan Zikra 1.9.60 — 21 November 2026

APK kandidat bertanda tangan di `.local/releases/Zikra-v1.9.60-beta-audio-candidate.apk`. Versi 1.9.60, kode 87. Build dari arsip `ae78bc1` dengan perubahan adzan pada checkpoint ini dan opsi akun 18+ yang sama seperti kandidat 1.9.59. Akses mandiri di produksi tidak diaktifkan oleh build ini.

- [Audit APK](audit.json) lulus. Signer sama dengan 1.9.59. Paket memuat Subuh karya Ahmed Khalaf (CC BY 4.0) dan rekaman Adam-synagda (CC0) untuk Zuhur hingga Isya. Kedua hash cocok dengan kredit audio. Dua MP3 build internal tidak ditemukan.
- JVM release melaporkan 232 lulus, 5 dilewati karena fixture data host, tanpa gagal.
- Emulator Android 36 dengan Wi-Fi dan data seluler dimatikan menerima update langsung dari signed 1.9.59 ke signed 1.9.60. Waktu pemasangan awal tetap sama. Isi database tidak dibandingkan pada putaran ini.
- [11 tes instrumentasi](instrumentation.txt) lulus pada APK bertanda tangan. Alarm Android memicu pemutaran rekaman Subuh bawaan tanpa impor MP3, dan tombol Berhenti pada notifikasi menghentikan pemutaran. Tes juga mencakup pemilihan audio bawaan dan regresi impor serta penghapusan MP3 pilihan pengguna.
- Suara fisik, izin, dan alarm dengan layar terkunci masih perlu diperiksa di HP. Website beta tetap pada preview terlindungi, tanpa tautan unduh APK publik.
