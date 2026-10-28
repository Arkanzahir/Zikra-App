# Referensi awan realistis dan langit dinamis — 28 Oktober 2026

Aset dibuat dengan **built-in imagegen**, permintaan latar transparan. Prompt final disimpan utuh di [PROMPT.txt](PROMPT.txt). Tidak menggunakan CLI/API fallback.

- [cloud-bank-v1.png](cloud-bank-v1.png): master asli RGBA 2172 × 724, keluaran generator tanpa perubahan gambar.
- [cloud-bank-v1.webp](cloud-bank-v1.webp): salinan ringan 1024 × 341 untuk calon integrasi.
- [cloud-bank-preview.webp](cloud-bank-preview.webp): salinan 768 × 256 untuk referensi interaktif.
- [manifest.json](manifest.json): ukuran, hash dan pemeriksaan transparansi. Alpha master 0–253 dengan sekitar 62% piksel sepenuhnya transparan. WebP mempertahankan alpha; hanya ukuran/kompresi dioptimalkan.

Referensi menampilkan dua lapis awan yang bergeser perlahan, parallax sentuh/kursor, warna fajar–siang–senja–malam, bintang dan bulan kecil dengan detail permukaan halus. Teks tetap diam. Gerak berhenti ketika layar referensi tidak terlihat dan mengikuti preferensi kurangi animasi. Gerak awan dan parallax dapat dimatikan secara terpisah.

Waktu matahari memakai contoh perhitungan lokal aplikasi tanggal 28 Oktober 2026 di titik awal publik Surabaya, bukan lokasi GPS pengguna. Arah gambar matahari, bintang, dan posisi bulan bersifat ilustratif, bukan arah kompas atau ephemeris bulan. Fase contoh bulan sekitar 95,3% menyusut. Awan dekoratif, bukan laporan cuaca.

Status: **referensi desain dan aset saja**. Belum mengubah resource Android, versi aplikasi, APK, rumus jadwal salat, atau menutup checklist F04/G02. Belum ada bukti pemakaian RAM/baterai pada HP untuk efek ini.

## Pemeriksaan referensi

[Fragmen interaktif](preview-fragment.html), [tampilan siang](preview-day.png), [malam](preview-night.png), dan [hasil browser](preview-checks.json) disimpan bersama aset. Fragmen memerlukan wrapper/runtime visualisasi untuk kontrol dan tampil mandiri; bukan halaman aplikasi Android.

Pemeriksaan akhir Edge headless lulus pada viewport 320/390/736 px: tidak ada overflow/gambar gagal/error JavaScript; urutan posisi matahari, warna senja, hilangnya matahari/bulan siang, munculnya bulan/bintang malam, tekstur bulan, dua lapis awan bergerak saat jam diam, parallax, sakelar gerak dan penggeser jam berfungsi. Mode kurangi animasi menghentikan gerak. Uji awal perubahan preferensi gerak gagal; sinkronisasi diperbaiki menggunakan nilai `matches` dari peristiwa perubahan preferensi, kemudian seluruh pemeriksaan diulang dan lulus. Tidak menjalankan build/emulator untuk referensi ini.

## Koreksi visual dari masukan pengguna

Gambar siang dan malam sebelumnya memiliki siluet kubah yang berbeda dan ditumpuk; alpha gambar malam meninggalkan kubah siang terlihat di belakangnya. Referensi kini hanya memakai **satu gambar masjid** untuk seluruh waktu. Warna/kecerahan berubah dan lampu digambar terpisah di dalam fasad, tanpa gambar kubah kedua. Seluruh kelompok masjid/lampu bergerak bersama ketika parallax aktif. Resource Android lama belum diubah oleh koreksi referensi ini.

Bulan dikurangi dari 23 menjadi 19 px; bayangan bola, noise dan pola kawah berulang dihapus, diganti permukaan hampir rata dengan bercak halus dan batas fase lembut. Ini ilustrasi, bukan foto atau ephemeris baru. Pilihan jam tersimpan dipertahankan; default terkini 08.45 agar hitung mundur mudah dibandingkan dengan masukan pengguna.

Pemeriksaan browser diulang setelah koreksi: tepat satu gambar/siluet masjid dengan geometri identik pada 04.50, 11.39, 16.50, 17.23, 18.00 dan 20.30; lampu fasad hanya aktif saat gelap, bulan malam dan gerak tetap berfungsi. Screenshot siang/malam dan hasil pemeriksaan diperbarui. Seluruh pemeriksaan terarah lulus; belum persetujuan visual pengguna atau implementasi APK.

## Hitung mundur dipulihkan

Hitung mundur yang terlewat saat merapikan referensi kembali tampil di bawah nama salat tujuan, dengan format `HH:MM:SS`. Nilainya berasal dari jam simulasi dan contoh jadwal yang sama, bukan angka tetap; ia berhenti saat simulasi dijeda dan berubah ketika penggeser/putar simulasi digunakan. Pada waktu salat tepat, tujuan beralih ke salat berikutnya; sesudah Isya menuju contoh Subuh besok.

[Screenshot 08.45](preview-countdown.png) menunjukkan `02:30:00` menuju Zuhur11.15. Lima kasus selisih/batas (08.45,11.14,11.15,18.37,20.30), perubahan saat playback, keterbacaan pada tiga lebar layar dan pemeriksaan langit sebelumnya lulus di browser. Ini tetap referensi; tidak mengubah penghitung/jadwal pada APK.
