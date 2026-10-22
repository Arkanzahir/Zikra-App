# Protector: menyelaraskan tata letak dengan referensi pengguna

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
