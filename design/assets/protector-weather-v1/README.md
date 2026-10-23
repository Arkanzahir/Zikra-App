# Paket aset cuaca Protector — v1

**Revisi 23 Oktober:** font isi Roboto Condensed, layar 356×740, peta geografis interaktif dan pilihan membaca laporan/shakemap BMKG. **Masih pratinjau browser, belum perubahan APK.** [Rincian dan batas verifikasi](LAYOUT.md).

**Revisi tata letak 22 Oktober:** preview.html kini menampilkan Protector lengkap mengikuti acuan pengguna, dengan header ringkas, peta, laporan, navigasi serta detail peta. [Perubahan, font dan verifikasi](LAYOUT.md). Laboratorium aset awal dipindahkan ke [asset-lab.html](asset-lab.html); tiga PNG sumber tetap sama.

Aset asli dari built-in image_gen, 22 Oktober 2026. Tiga PNG RGBA dengan alpha nyata; source pack untuk tahap desain. **Belum dipasang di APK** dan tidak mengubah data API atau fitur peta.

## Lihat pratinjau

Jalankan `python serve_preview.py --port 8770` dari folder ini, lalu buka http://127.0.0.1:8770/preview.html. Tidak ada akses GPS, suara, kredensial atau API berbayar. Peta mengambil tile OpenStreetMap hanya untuk area yang terlihat, memakai cache normal browser serta atribusi terlihat. Tombol **Cek laporan BMKG** membaca endpoint publik tetap melalui server loopback. Mode awal tetap bertanda contoh; cuaca selalu contoh. `asset-lab.html` tetap dapat digunakan tanpa data gempa.

- Cerah: langit dan matahari; awan benar-benar tidak terlihat.
- Berawan: awan terang bergerak perlahan dalam dua lapisan.
- Mendung: awan gelap; belum ada hujan.
- Hujan ringan: 22 partikel, pendek dan lebih lambat.
- Hujan deras: 65 partikel, lebih panjang dan cepat.
- Badai: 90 partikel dan arah angin miring; contoh visual, bukan bukti kejadian.
- Malam cerah: bulan, tanpa awan.

Hujan digambar dengan satu canvas, maksimal30fps; tidak memerlukan sprite/video. Awan menggunakan dua posisi yang memakai ulang gambar sama. Efek berhenti saat tab tidak aktif; tombol jeda dan prefers-reduced-motion dihormati. Lapisan awan tersembunyi tidak dianimasikan. Tidak ada kilatan otomatis.

## Aset

| Aset | Tujuan | Resolusi sumber |
|---|---|---|
| city-foreground.png | Kota umum tanpa landmark/masjid, langit transparan | 1774×887 |
| clouds-light.png | Lapisan awan terang | 1942×809 |
| clouds-storm.png | Lapisan awan gelap untuk mendung/hujan/badai | 1942×809 |

Lihat [manifest.json](manifest.json) untuk ukuran, hash, rentang alpha dan perkiraan ukuran decode. Total sumber PNG sekitar4.05MiB; **ini bukan ukuran APK atau hasil profiling RAM**. Sumber belum diperkecil/dioptimalkan WebP. Preview memuat ketiga gambar untuk perpindahan mode yang cepat; implementasi Android nanti hanya mendecode aset yang diperlukan sesuai ukuran tampilan. Gambar asli disimpan untuk proses optimasi berikutnya.

Aset kota bersifat ilustrasi umum, bukan foto Surabaya atau bangunan berlokasi pasti. Tempat dalam preview adalah contoh label. Masjid tetap milik desain Home; aset ini khusus Protector.

## Sambungan Android nanti

Layer: sky gradient → sun/moon → moving clouds → city → tint/fog → procedural rain → readability scrim → weather text. Animasi hanya di area header; tidak menimpa peta/tombol informasi. Gunakan satu clock terikat lifecycle; stop ketika Activity tidak terlihat atau motion policy dimatikan.

Sumber cuaca aplikasi sekarang adalah Open-Meteo, dengan kode WMO. Rancangan mapping mengikuti [dokumentasi resminya](https://open-meteo.com/en/docs):
- 0 cerah tanpa awan; 1 sedikit awan; 2 berawan sebagian; 3 mendung.
- 51/53/55 gerimis; 61/63/65 hujan ringan/sedang/lebat; 80/81/82 showers dengan intensitas berbeda.
- 95 thunderstorm tidak otomatis berarti hujan palingderas. 97 heavy thunderstorm jika provider mengirimkannya. 96/99 termasuk hail; jangan sembunyikan keterangan aslinya.
- Kondisi 45/48 kabut, salju/beku dan kode tidak didukung perlu fallback tersendiri, bukan dipaksa cerah.
- Kecepatan angin dapat mengubah kemiringan gerak; kerapatan hujan mengikuti kode/intensitas yang tersedia. Gambar petir tidak membuktikan posisi atau frekuensi sambaran.
- Tidak memetakan kode WMO ke kode prakiraan BMKG begitu saja. Data gempa/peta tetap memakai sumber dan jalur BMKG sendiri.
- Hasil pembacaan kode sekarang pada WeatherCode.java belum memiliki97. Saat integrasi perlu memperbarui validasi parser, label, ikon dan tes terkait bersama-sama, bukan sekadar mengganti latar.

Ini rencana integrasi, **belum implementasi atau pengujian cuaca langsung**. Cache kedaluwarsa/koneksi gagal harus diberi label; jangan menjadikan ilustrasi sebagai jaminan kondisi aktual atau peringatan resmi.

## Verifikasi tahap aset

- Alpha ketiga PNG diperiksa, upper-left transparan.
- Preview diperiksa langsung di satu tab browser: cerah, awan, hujan, badai, malam dan kontrol jeda/putar.
- preview.js lolos pemeriksaan sintaks Node.
- Screenshot browser di preview-clear.png, preview-cloudy.png, preview-storm.png. Screenshot tidak membuktikan laju frame atau angka penggunaanRAM.
- Tidak menjalankan emulator, Gradle, atau mengakses HP.
- Tidak mengklaim pengujian API, baterai, profiling perangkat atau APK baru.

Prompt lengkap: [PROMPTS.md](PROMPTS.md).
