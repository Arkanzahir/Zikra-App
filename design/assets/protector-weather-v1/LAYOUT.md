# Protector: menyelaraskan tata letak dengan referensi pengguna

## Revisi 3 — 23 Oktober 2026

**Status: pratinjau browser. APK dan layar native Protector belum berubah.**

- Ukuran logical layar menjadi 356×740, hero278px dengan overlap14px, map148px dan nav51px. Mengurangi ruang vertikal berlebih dibanding revisi2.
- Judul Protector tetap Georgia Bold25px. Isi memakai Roboto Condensed, suhu Roboto58px. Font OFL disertakan dari repositori resmi Google Fonts. Ini pendekatan bentuk/hierarki; identitas font dalam gambar generatif tidak diketahui, sehingga tidak diklaim identik.
- Label isi dibesarkan dan ruang antarelemen dirapatkan. Screenshot lama layout-*.png tetap merupakan revisi2; jangan menyebutnya bukti revisi3.
- Tiga PNG cuaca tidak diubah. Kota masih ilustrasi umum; bukan foto kota atau komposisi persis pada referensi. Animasi cuaca tetap contoh dan dijeda saat peta detail dibuka.

### Peta dan laporan

Peta kini memakai tile geografis OpenStreetMap Standard dengan atribusi terlihat, hanya untuk viewport yang dibuka pengguna. Tidak melakukan unduhan massal atau prefetch. Fallback Natural Earth digambar dengan proyeksi Web Mercator yang sama, sehingga marker tidak bergeser saat tile termuat. Detail mendukung zoom3–9, reset, geser dan tombol panah.

Mode awal **contoh**. Tombol **Cek laporan BMKG** mengambil endpoint publik tetap melalui serve_preview.py di loopback. Tidak meminta lokasi atau kredensial pengguna. Proxy membatasi ukuran262144byte, timeout12detik dan cache60detik. Magnitudo divalidasi, latitude/longitude harus finite dan dalam rentang; koordinat tidak valid menghilangkan marker. Waktu pengambilan dipisahkan dari waktu kejadian. Kegagalan pembaruan tidak mengubah laporan lama menjadi data baru.

Peta guncangan memakai gambar BMKG yang sesuai dengan field Shakemap laporan itu; nama file diizinkan secara ketat dan host tetap static.bmkg.go.id. Jika tidak ada/gagal, UI menampilkan kondisi belum tersedia. Bukan overlay guncangan buatan atau deteksi gempa oleh HP. Ini laporan terakhir Indonesia, bukan peringatan untuk lokasi pengguna.

### Verifikasi revisi 3

- Node: sintaks tiga skrip, proyeksi round-trip, arah utara/selatan/timur, koordinat terbalik dan titik invalid lolos.
- Python: lima tes parser lolos, termasuk timestamp tanpa zona, magnitudo rusak dan nama shakemap yang mencoba pindah host/path.
- Browser: empat tile overview termuat. Laporan BMKG yang diterima saat pemeriksaan menunjukkan kejadian22Oktober2026,17:57:49WIB, M5.2, kedalaman120km, koordinat-3.11,102.14. Bukan klaim gempa baru hari ini. Shakemap resmi 20261022175749.mmi.jpg termuat, lebar612px.
- Browser setelah reload: zoom5→6 memuat skala tile yang sesuai; reset dan geser60px melalui tombol panah menggeser marker60px pada proyeksi yang sama.
- Geometri DOM: content dan screen tidak overflow pada viewport1265×720; detail map353px dan sheet255px. Font selesai dimuat. Ini pemeriksaan geometri, bukan bukti visual pixel-identical.
- Pengambilan screenshot revisi3 melalui API browser gagal (Unable to capture screenshot), termasuk metode alternatif terdokumentasi. Karena itu belum ada bukti screenshot terbaru atau perbandingan visual final yang dapat dinyatakan lulus.
- Tidak menjalankan emulator, build Gradle atau pengujian Android. Belum ada APK baru, profiling RAM/FPS/baterai, maupun pengujian peta native terbaru.

Jalankan: `python serve_preview.py --port 8770`, lalu buka http://127.0.0.1:8770/preview.html.

---

Catatan revisi2 di bawah bersifat historis.

22 Oktober 2026. Halaman browser, bukan implementasi Android atau bukti data live.

## Masalah dan hasil

Preview awal adalah laboratorium aset, sehingga hanya menampilkan header cuaca dan banyak kontrol. Pengguna meminta tata letak serta font mengikuti [gambar acuannya](../../references/protector-layout-user-2026-10-22.png). preview.html kini menjadi layar Protector lengkap; preview lama tersedia di asset-lab.html.

- Layar logical356×780 di dalam frame. Hero290px termasuk statusbar; bidang krem menumpuk14px di bawah hero. Diikuti heading peta, map151px, laporan tiga kolom, pembaruan42px, dua pintasan, atribusi dan navigasi54px.
- Hero memuat judul kiri, refresh kanan, chip lokasi, caption, suhu58px, kondisi16px, label contoh dan dua metrik bawah.
- Georgia Bold24px untuk judul Protector. Plus Jakarta Sans (font proyek dengan OFL terlampir) untuk label, angka, heading isi dan navigasi. Font source176288byte. Georgia memakai font sistem dengan fallback serif; tidak mendistribusikan font Windows. Gambar generatif tidak menyediakan metadata font, sehingga ini pilihan pendekatan, bukan klaim fontidentik.
- Cloud yang terang ditempatkan lebih ke kanan supaya judul kiri tidak kehilangan kontras. Warna serta susunan tetap sama saat modecuaca berubah; clear0 tidak memiliki awan.
- Kontrol mode/motion berada di luar layar contoh. Tampilan fit hanya memperkecil keseluruhan frame; tombol Lihat ukuran asli memperlihatkan ukuran logical untuk menilai keterbacaan.
- Tiga PNG sumber cuaca tetap tidak berubah; hash tetap tercatat pada manifest.json.

## Interaksi

Perbesar/ketuk peta/Detail gempa membuka detail dengan tombol kembali. Zoom+/- dan reset bekerja untuk contoh geografi. Segmen peta guncangan menampilkan kondisi belumtersedia, karena belum ada laporan BMKG live atau shakemap. Sumber BMKG membuka dokumentasi publik.

Saat detail peta tampil, clock cuaca dijeda. Kembali ke overview melanjutkan mengikuti pilihan motion. Jeda/putar, variasi cuaca, reduced-motion dan background policy dipertahankan.

Navigasi lima item dan dua pintasan adalah contoh tata letak, belum navigasi aplikasi Android. Lokasi, suhu, humidity dan episentrum semuanya fixture; tidak memakai GPS/API.

## Peta

map-overview.svg dan map-detail.svg adalah representasi vektor dari protector_indonesia.geojson yang sudah dibundel dalam aplikasi, Natural Earth50m/publicdomain. Detail memakai cakupan portrait tersendiri supaya Java/Bali serta label tidak terpotong karena cover dari gambar landscape. Ini geografi overview tanpa detail jalan; berbeda dari ilustrasi pada gambar acuan. MAP-ATTRIBUTION.txt disertakan. Pin110.7E/9.3S hanyalah contoh visual, bukan laporan aktual atau lokasi pengguna.

## Verifikasi

- preview.js dan protector.js lolos node --check.
- Browser: overview, mode hujan, pause/putar, ukuranasli/fit, detailmap, zoom/reset, segmen shakemap kosong dan kembali diperiksa. Screenshot: layout-overview.png, layout-rain.png, layout-map.png.
- Satu tab yang sudah ada dipakai. Tidak menjalankan emulator, Gradle, atau mengakses HP.
- Belum ada pengukuran native RAM/FPS/baterai atau pengujian Android. Tidak ada APK baru.
