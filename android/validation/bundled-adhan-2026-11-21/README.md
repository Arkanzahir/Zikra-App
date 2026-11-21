# Adzan bawaan Zikra 1.9.60 — 21 November 2026

APK kandidat bertanda tangan di `.local/releases/Zikra-v1.9.60-beta-audio-candidate.apk`. Versi 1.9.60, kode 87. Build dari arsip `ae78bc1` dengan perubahan adzan pada checkpoint ini dan opsi akun 18+ yang sama seperti kandidat 1.9.59. Akses mandiri di produksi tidak diaktifkan oleh build ini.

- [Audit APK](audit.json) lulus. Signer sama dengan 1.9.59. Paket memuat Subuh karya Ahmed Khalaf (CC BY 4.0) dan rekaman Adam-synagda (CC0) untuk Zuhur hingga Isya. Kedua hash cocok dengan kredit audio. Dua MP3 build internal tidak ditemukan.
- JVM release melaporkan 232 lulus, 5 dilewati karena fixture data host, tanpa gagal.
- Emulator Android 36 dengan Wi-Fi dan data seluler dimatikan menerima update langsung dari signed 1.9.59 ke signed 1.9.60. Waktu pemasangan awal tetap sama. Isi database tidak dibandingkan pada putaran ini.
- [11 tes instrumentasi](instrumentation.txt) lulus pada APK bertanda tangan. Alarm Android memicu pemutaran rekaman Subuh bawaan tanpa impor MP3, dan tombol Berhenti pada notifikasi menghentikan pemutaran. Tes juga mencakup pemilihan audio bawaan dan regresi impor serta penghapusan MP3 pilihan pengguna.
## Uji update pada HP Android 36

- Update ADB dari signed 1.9.59/code86 ke 1.9.60/code87 berhasil. Hash APK terpasang cocok dengan kandidat. Sertifikat, ID aplikasi, direktori data, dan waktu pemasangan awal cocok. Isi database tidak dibandingkan pada update ini.
- Cadangan terenkripsi 748.987 byte dari sesi 1.9.59 tersedia dan hash salinannya cocok dengan file di HP. Izin notifikasi dan alarm tepat waktu tetap aktif setelah update.
- Pengguna memutar pratinjau Subuh dari menu Dengarkan adzan di HP, mendengar suaranya, lalu menghentikannya. Layanan adzan teramati aktif saat pratinjau dan berhenti setelahnya.
- Pembatasan pemasangan di HP membatalkan paket alat uji terpisah. Paket tersebut tidak terpasang. Uji alarm otomatis dengan layar terkunci masih terbuka. Website beta tetap pada preview terlindungi, tanpa tautan unduh APK publik.
