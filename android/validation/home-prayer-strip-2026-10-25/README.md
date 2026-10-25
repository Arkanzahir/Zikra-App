# UI04 — Panel jadwal salat tanpa sambungan abu-abu

25 Oktober 2026, source `a24ab66`, kandidat **1.9.2 beta/code29**. Pengguna membandingkan [potongan acuan](reference-crop.png) dengan [hasil 1.9.1 yang ditolak](rejected-191-crop.png).

Penyebab visual: warna panel lama `#AA082F32` hanya 67% opak, sedangkan panel melintasi batas ilustrasi dan permukaan cream. Latar yang berbeda menghasilkan garis horizontal dan bagian bawah abu-abu, seolah dua lapisan. Radius16dp, pembatas1dp dan warna aktif camel juga berbeda dari acuan.

Koreksi: satu gradient gelap opak, radius12dp/border0,5dp, pembatas0,5dp lebih pendek/redup, label mendekati putih, jam12sp, sorotan emas pucat dan titik3dp. Panel tetap menumpang batas hero; tidak lagi meneruskan warna latar di bawahnya. Perhitungan jam tidak diubah untuk menyalin angka contoh pada mockup.

- [Hasil siang sesuai rasio acuan](native/sky-day-reference.png), [malam](native/sky-crescent-reference.png), [bulan penuh](native/sky-full-moon-reference.png).
- [Font200% siang](native/sky-day-200.png), [malam](native/sky-crescent-200.png).
- [Checks/log](checks.json): build debug/helper dan lint lulus, **0 error/721 warning**. Tiga eksekusi satu metode native sky yang sudah tersedia pada font100%/200% dan rasio acuan lulus. Guard menunggu frame benar-benar tampil. Tidak menambah tes yang menyalin konfigurasi warna; pengujian fungsional delapan eksekusi versi sebelumnya tetap historis, tidak dihitung ulang sebagai putaran ini.
- [Preservasi](preservation.json): 10 file asli emulator pulih cocok hash, font/ukuran dikembalikan, marker/helper dihapus, emulator ditutup. HP tidak diubah; [audit](audit.json) tidak menemukan nilai rahasia yang diketahui pada cakupan pemindaian.

[Receipt APK/source/signature](release.json), [file perubahan](source-files.json). `Zikra-Home-v1.9.2-beta-debug.apk` **34.401.029 byte**, SHA-256 `a2aa80180f0f909e0d3b1af2724cc432b65de5e098ed9bb50cd0dbdd09ea8ef2`; <100MB. APK1.9.1 dipertahankan, sertifikat debug sama, SQLite4. Snapshot waktu/fase dan lokasi adalah fixture sintetis untuk perbandingan visual; tidak memverifikasi astronomi/baterai/HP atau AI.

UI04 keseluruhan tetap terbuka. Permintaan baru untuk ikon pintasan adalah tahap preview terpisah; ikon baru belum diterapkan pada APK1.9.2. C/B01/D02a/F/G serta rilis publik tidak ditutup oleh koreksi panel ini.
