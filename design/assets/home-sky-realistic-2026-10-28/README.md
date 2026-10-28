# Referensi awan realistis dan langit dinamis — 28 Oktober 2026

Aset dibuat dengan **built-in imagegen**, permintaan latar transparan. Prompt final disimpan utuh di [PROMPT.txt](PROMPT.txt). Tidak menggunakan CLI/API fallback.

- [cloud-bank-v1.png](cloud-bank-v1.png): master asli RGBA 2172 × 724, keluaran generator tanpa perubahan gambar.
- [cloud-bank-v1.webp](cloud-bank-v1.webp): salinan ringan 1024 × 341 untuk calon integrasi.
- [cloud-bank-preview.webp](cloud-bank-preview.webp): salinan 768 × 256 untuk referensi interaktif.
- [manifest.json](manifest.json): ukuran, hash dan pemeriksaan transparansi. Alpha master 0–253 dengan sekitar 62% piksel sepenuhnya transparan. WebP mempertahankan alpha; hanya ukuran/kompresi dioptimalkan.

Referensi menampilkan dua lapis awan yang bergeser perlahan, parallax sentuh/kursor, warna fajar–siang–senja–malam, bintang dan bulan bertekstur. Teks tetap diam. Gerak berhenti ketika layar referensi tidak terlihat dan mengikuti preferensi kurangi animasi. Gerak awan dan parallax dapat dimatikan secara terpisah.

Waktu matahari memakai contoh perhitungan lokal aplikasi tanggal 28 Oktober 2026 di titik awal publik Surabaya, bukan lokasi GPS pengguna. Arah gambar matahari, bintang, dan posisi bulan bersifat ilustratif, bukan arah kompas atau ephemeris bulan. Fase contoh bulan sekitar 95,3% menyusut. Awan dekoratif, bukan laporan cuaca.

Status: **referensi desain dan aset saja**. Belum mengubah resource Android, versi aplikasi, APK, rumus jadwal salat, atau menutup checklist F04/G02. Belum ada bukti pemakaian RAM/baterai pada HP untuk efek ini.

## Pemeriksaan referensi

[Fragmen interaktif](preview-fragment.html), [tampilan siang](preview-day.png), [malam](preview-night.png), dan [hasil browser](preview-checks.json) disimpan bersama aset. Fragmen memerlukan wrapper/runtime visualisasi untuk kontrol dan tampil mandiri; bukan halaman aplikasi Android.

Pemeriksaan akhir Edge headless lulus pada viewport 320/390/736 px: tidak ada overflow/gambar gagal/error JavaScript; urutan posisi matahari, warna senja, hilangnya matahari/bulan siang, munculnya bulan/bintang malam, tekstur bulan, dua lapis awan bergerak saat jam diam, parallax, sakelar gerak dan penggeser jam berfungsi. Mode kurangi animasi menghentikan gerak. Uji awal perubahan preferensi gerak gagal; sinkronisasi diperbaiki menggunakan nilai `matches` dari peristiwa perubahan preferensi, kemudian seluruh pemeriksaan diulang dan lulus. Tidak menjalankan build/emulator untuk referensi ini.
