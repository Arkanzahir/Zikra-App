# Validasi pembaruan referensi — 16 Oktober 2026

Build memakai Java 17, AGP 8.7.3, Gradle 8.9, compile/target SDK 35, min SDK 26.

- `assembleDebug`: lulus, APK dihasilkan.
- `testDebugUnitTest`: 14 tes lulus, tanpa kegagalan/error.
- `lintDebug`: lulus tanpa error; peringatan nonfatal terutama string UI dan rekomendasi tampilan.
- `assembleDebugAndroidTest`: lulus.

Tes JVM memeriksa arah/smoothing kompas, waktu prakiraan, countdown sholat serta kalender Hijriah (panjang bulan, konversi weekday, batas rentang dan urutan hari istimewa). Tes perangkat memeriksa SQLite, persistensi Quran, pembukaan layar, navigasi detail BMKG, parser respons resmi dan halaman baru.

Pengujian emulator berjalan pada Android 15/API 35 dengan rendering software tanpa KVM. Kinerja perangkat fisik, sensitivitas goyangan, akurasi kiblat, haptik dan mesin suara Arab belum diklaim terverifikasi. Endpoint BMKG tidak dapat diakses dari jaringan cloud saat pemeriksaan; fixture pengujian tidak dijadikan data live aplikasi.

Cakupan dan batasan produk dicatat di [README.md](README.md). Uji terdahulu terhadap Home mencatat progres SQLite berubah 8/24 (33%) ke 7/24 (29%) melalui UI dan bertahan setelah instal ulang; data demo dipulihkan ke 8/24. Suite terbaru selesai: **26 tes instrumentasi lulus**, tanpa kegagalan. Rincian runner ada di `validation/reference-tests.txt`.

Screenshot yang sudah diperiksa: [Protector BMKG](validation/screens/ProtectorActivity.png), [Kiblat](validation/screens/QiblaActivity.png), [Pendamping offline](validation/screens/AiActivity.png), dan [Kalender Hijriah](validation/screens/CalendarActivity.png). Cuplikan BMKG menampilkan keadaan belum ada data, bukan laporan live.

## Perapian XML dan Java

- Resource warna dan dimensi disatukan; tombol header memakai area sentuh 48dp. Java diformat konsisten, dan atribut XML dipisah agar mudah dibaca.
- Composer AI dipindah ke luar area scroll; ruang dock disesuaikan saat IME terbuka. Pemeriksaan visual: [chat](validation/screens/AiActivity.png) dan [keyboard](validation/screens/AiKeyboard.png).
- Header profil membaca peran onboarding; pilihan tanggal/tab Waktu Sholat dipulihkan setelah Activity dibuat ulang.
- Build APK, lint, dan 14 unit test dijalankan ulang dan lulus.
- Putaran 13 tes UI menemukan satu masalah deteksi keyboard; 12 skenario lain lulus. Setelah perbaikan, ketiga tes regresi (keyboard/composer, rekreasi Waktu Sholat, peran profil) diulang dan seluruhnya lulus. Dengan demikian 13 skenario UI unik telah lulus; suite data/BMKG sebelumnya tidak diulang karena logikanya tidak berubah.
- Log putaran awal dan uji ulang tersedia di `validation/polish-initial-tests.txt` dan `validation/polish-regression-tests.txt`.

## Penyempurnaan Home — 17 Oktober 2026

- `assembleDebug`, `assembleDebugAndroidTest`, dan `lintDebug` lulus setelah pembaruan layout dan resource ukuran layar. APK debug juga lulus verifikasi tanda tangan `apksigner`.
- `testDebugUnitTest`: 14 tes JVM lulus setelah perubahan logika Home; tidak ada kegagalan/error.
- Dua tes `HomeNavigationTest` lulus: shortcut Hadis membuka halaman pencarian hadis, dan tanggal Hijriah membuka kalender yang dapat dinavigasi.
- Putaran navigasi awal berhenti dengan timeout input/fokus jendela. Runner diperbaiki agar menunggu fokus sebelum berinteraksi dan sebelum menutup halaman; putaran ulang lulus. Log awal disimpan di `validation/home-navigation-initial.txt`, hasil ulang di `validation/home-navigation-tests.txt`.
- Smoke Home memeriksa delapan shortcut dan progres database pada emulator API 35: lulus pada 540×1170px/210dpi (sekitar 411dp) dan 480×1040px/213dpi (sekitar 360dp). Kedua screenshot diperiksa untuk memastikan tombol checklist terlihat penuh di atas dock.
- Screenshot Home terbaru: [layar standar](validation/screens/HomeActivity.png) dan [layar 360dp](validation/screens/HomeActivity360.png). Log: `validation/home-build.txt`, `validation/home-standard-screen-tests.txt`, `validation/home-small-screen-tests.txt`.
- Ini validasi khusus pembaruan Home. Suite seluruh halaman tidak dijalankan ulang; keterbatasan data, sensor fisik, AI dan pengingat pada README masih berlaku.

### Koreksi jarak strip dan ikon Home

- `assembleDebug`, `assembleDebugAndroidTest`, dan `lintDebug` lulus setelah susunan strip serta ikon diperbarui. Tanda tangan APK diverifikasi kembali.
- Smoke Home menambahkan pemeriksaan bahwa bagian bawah strip sholat berjarak sekurangnya 12dp dari bagian atas kartu shortcut. Desain menyediakan ruang minimal 16dp, dengan toleransi pemeriksaan pengukuran view.
- Urutan kolom strip menjadi nama waktu → ikon → jam; warna aktif diterapkan ke ikon tanpa outline besar. Ikon shortcut memakai satu gaya emas, termasuk ikon tangan berdoa.
- Screenshot standar dan 360dp pada folder `validation/screens/` diperbarui untuk versi ini. Pengujian ini berfokus pada Home; suite halaman lain tidak diulang.
- Smoke dengan pemeriksaan jarak lulus pada kedua ukuran layar. Log versi ini: `validation/home-spacing-build.txt`, `validation/home-spacing-standard-tests.txt`, dan `validation/home-spacing-small-tests.txt`.

### Pembungkus shortcut melengkung dan aset masjid transparan

- Build APK, APK instrumentasi dan lint lulus setelah `CurvedShortcutLayout` ditambahkan. Tanda tangan APK diverifikasi.
- Aset masjid PNG 2172×724 memiliki mode RGBA dengan piksel latar ber-alpha 0; aset hasil pemrosesan disalin tanpa pengeditan piksel tambahan. Tint dan transparansi tampilan diatur oleh XML.
- Kontur atas turun 8dp di tengah, sementara ukuran grid dan elevasi dipertahankan. Screenshot Home standar dan 360dp diperbarui untuk menilai lengkungan, siluet, jarak strip dan keterlihatan CTA.
- Validasi berfokus pada Home; data dan layanan fitur lain tidak diubah.
- Smoke Home dan pemeriksaan jarak lulus pada lebar sekitar 411dp serta 360dp. Kedua screenshot diperiksa setelah render. Log: `validation/home-curved-build.txt`, `validation/home-curved-standard-tests.txt`, dan `validation/home-curved-small-tests.txt`.
