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
