> **Status terbaru — 18 Oktober 2026:** instance diganti; perubahan lokal sesi sebelumnya tidak tersedia. Pemulihan tahap ini mencakup Study/Hifz, target surah/ayat, editor dan navigasi bersama. Reader native masih 4 surah; target lain membuka Quran.com eksternal. Baca [RECOVERY.md](../RECOVERY.md) dan [validasi terbaru](validation/agenda/README.md) sebelum catatan historis di bawah. Setup baru: [tools/cloud](../tools/cloud/README.md).

# Validasi pembaruan referensi — 16 Oktober 2026

## Hasil terbaru: desain BMKG — 17 Oktober 2026

- Build `assembleDebug`, APK instrumentasi dan `lintDebug` lulus; 18 unit test lulus. Log: `validation/bmkg-design-verified-build.txt` dan `validation/bmkg-design-layout-final-build.txt`.
- 15 tes perangkat BMKG lulus pada Android 9/API 28: binding data ketiga layar, navigasi detail, kondisi peta tanpa koordinat, parser respons, dan smoke dashboard. Log: `validation/bmkg-design-regression-tests.txt`.
- Tiga tes binding/render diulang pada konfigurasi sekitar 360dp dan lulus. Smoke screenshot dashboard juga lulus. Log: `validation/bmkg-design-small-tests.txt` dan `validation/bmkg-design-actual-screen-tests.txt`.
- Uji awal API 35 terhenti karena emulator tidak memberikan fokus jendela dan menampilkan layar hitam; pengujian dipindah ke API 28. Compile/target SDK aplikasi tetap 35. Putaran API 28 pertama menemukan timestamp `+00:00` tidak terbaca pada implementasi lama `Instant.parse`; fallback `OffsetDateTime` memperbaikinya dan seluruh tes ulang lulus. Log awal dipertahankan pada `validation/bmkg-design-initial-tests.txt` serta `validation/bmkg-design-fixture-retry-tests.txt`.
- Screenshot data contoh diberi label **DATA UJI · BUKAN DATA LIVE**: [dashboard](validation/screens/ProtectorDesignFixture.png), [cuaca](validation/screens/WeatherDesignFixture.png), [gempa](validation/screens/EarthquakeDesignFixture.png). Screenshot versi 360dp memakai akhiran `360`; PNG menangkap display fisik 540×1170 yang menampilkan viewport logis 480×1040/213dpi. Helper memakai `screencap` agar Android lama tidak memotong tangkapan pada batas logis.
- Tampilan loading tanpa data contoh juga dicatat pada `ProtectorActivity.png`, `WeatherActivity.png` dan `EarthquakeActivity.png`. Akses data live BMKG belum diklaim terverifikasi. Integrasi API baru, sensor fisik, Kiblat dan Quran tidak termasuk pembaruan tahap ini.

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

### Ornamen lentera dan penegasan siluet

- Build debug, APK instrumentasi dan lint lulus setelah ornamen vector ditambahkan.
- Dua lentera outline dan tiga bintang ditempatkan di sisi hero, di bawah bar lokasi; tanggal, jam, kontrol header serta tinggi konten dipertahankan. Ornamen tidak menerima klik/fokus dan dikecualikan dari aksesibilitas.
- Siluet masjid menggunakan alpha 0,15. Pada varian layar tinggi, margin bawah naik 4dp untuk menaikkan gambar tanpa mengubah skalanya; varian layar pendek mempertahankan posisi sebelumnya.
- Screenshot Home pada folder `validation/screens/` diperbarui untuk pemeriksaan keseimbangan dekorasi serta keterlihatan teks dan CTA.
- Smoke Home serta pemeriksaan jarak strip/kartu lulus pada sekitar 411dp dan 360dp. Screenshot kedua ukuran diperiksa; tanda tangan APK juga diverifikasi. Log: `validation/home-lantern-build.txt`, `validation/home-lantern-standard-tests.txt`, `validation/home-lantern-small-tests.txt`.

## Fungsi harian — 17 Oktober 2026

- `assembleDebug`, `assembleDebugAndroidTest`, dan `lintDebug` lulus. **18 tes JVM lulus**, tanpa kegagalan/error. Log build: `validation/daily-build.txt`.
- **9 tes instrumentasi terarah lulus** pada emulator Android 15/API 35: lima alarm mengikuti sakelar utama/per sholat, receiver memposting notifikasi asli dan mengabaikan duplikat/kejadian masa depan, lanjut ayat tersimpan, Diary Hifz, Hadis, kalender, jadwal harian, pemulihan tanggal/tab setelah rekreasi, dan delapan shortcut/progres Home. Log: `validation/daily-regression-tests.txt`.
- Putaran awal berhenti saat reader kehilangan fokus jendela. Binding surah awal yang berulang lewat callback Spinner dihapus; uji lanjut bacaan tersendiri dan suite sembilan tes kemudian lulus. Log awal dan uji ulang tersimpan pada `validation/daily-initial-tests.txt` dan `validation/daily-reading-tests.txt`.
- Tes lanjut bacaan menyimpan Al-Falaq ayat 4, membuka reader langsung, memeriksa label bookmark serta posisi gulir. Screenshot: [reader](validation/screens/QuranResume.png). Mode sensor gelap pada emulator terlihat dalam cuplikan tersebut.
- Diary Hifz memfilter kategori hafalan dan menghitung progres kategori sendiri; shortcut ke mode ini dan label progres diverifikasi. Screenshot: [Diary Hifz](validation/screens/DiaryHifz.png).
- Jadwal diuji terhadap perhitungan independen **Astral 3.2** untuk 15 Januari, 15 April, 15 Juli dan 17 Oktober 2026: Subuh -20°, Isya -18°, Ashar bayangan 1×, terbit/terbenam geometris -0,833°, tanpa refraksi tambahan, pembulatan ke atas dan ihtiyati 2 menit (kecuali terbit). Semua enam waktu berbeda maksimal satu menit pada tanggal fixture. Urutan waktu diuji sepanjang 366 hari tahun 2028; perpindahan setelah Isya, batas tepat waktu dan representasi zona waktu juga lulus.
- Pengujian notifikasi memberikan izin lewat UiAutomation dan mengirim broadcast ke receiver terdaftar. Keberadaan PendingIntent alarm dan pengiriman notifikasi diperiksa memakai layanan Android; tes tidak menunggu waktu sholat, memulai ulang emulator, atau membuktikan ketepatan alarm pada ponsel dalam mode hemat daya.
- Smoke Home dan pemeriksaan jarak strip/kartu juga lulus pada lebar sekitar 360dp. Screenshot [layar 360dp](validation/screens/HomeActivity360.png) diperiksa; CTA terlihat penuh di atas dock. Log: `validation/daily-small-screen-tests.txt`.
- Screenshot Home diambil setelah frame siap; pengambilan otomatis awal yang hanya menampilkan background diganti. Palet dan ornamen Home dipertahankan. Screenshot [Home standar](validation/screens/HomeActivity.png) dan [jadwal](validation/screens/PrayerActivity.png) diperiksa. APK lulus verifikasi tanda tangan `apksigner`.
- Jadwal merupakan hasil perhitungan lokasi tetap Sukolilo, **bukan feed resmi Kemenag**. Ini validasi perubahan fungsi harian; suite semua halaman, sensor fisik dan koneksi BMKG tidak diulang.


## Kiblat — 17 Oktober 2026

Dial dipadatkan sesuai referensi dengan rosette sage, penanda derajat, indeks arah ponsel, dan Ka’bah di tengah. Tata letak menggunakan XML dengan ukuran dial adaptif tinggi layar. Tombol membuka panduan kalibrasi, bukan mengklaim mengkalibrasi sensor secara otomatis. Status direset ketika halaman dibuka kembali; indikator sejajar dan haptik hanya aktif pada akurasi sensor minimal medium.

Validasi satu putaran: `assembleDebug`, `lintDebug`, `assembleDebugAndroidTest`, dan 4 `CompassMathTest` lulus. Smoke test `NativeFlowTest#testQiblaInflatesWithoutPhysicalSensor` lulus (1 tes, emulator Android 9/API 28). Screenshot diperiksa sekali: [QiblaActivity.png](validation/screens/QiblaActivity.png). Sensor dan getaran pada ponsel fisik belum diuji. Build memakai compile/target SDK 35.

Log: `validation/qibla-build.txt` dan `validation/qibla-device-test.txt`.


## Onboarding — 17 Oktober 2026

Tiga langkah native Java/XML mengikuti struktur referensi Blend wrk dengan warna Zikra: hero orbit vektor dan bulan sabit, pilihan SMP/SMA/SMK/Mahasiswa, lalu delapan chip minat (maksimal tiga). Tidak membutuhkan login. Pilihan jenjang disimpan sebagai `education_level`, minat sebagai `learning_interests`; profil menampilkan jenjang baru dan tetap mendukung peran lama. Tombol lanjut pada langkah jenjang aktif setelah memilih; Lewati tetap tersedia. Pilihan yang belum selesai dipertahankan melalui `onSaveInstanceState`.

Satu putaran validasi: `assembleDebug`, `lintDebug`, `assembleDebugAndroidTest` lulus. Dua tes perangkat lulus pada emulator Android 9/API 28: penyimpanan jenjang/minat, batas tiga minat, mengganti pilihan, kembali antar langkah, selesai dari mode edit, serta kompatibilitas peran profil lama. Log: `validation/onboarding-build.txt` dan `validation/onboarding-device-tests.txt`. Tidak ada pengujian ulang suite seluruh aplikasi. Rekreasi Activity dan jalur Lewati ke Home tidak diuji secara terpisah pada tahap ini.

Screenshot diperiksa: [Welcome](validation/screens/OnboardingWelcome.png), [Jenjang](validation/screens/OnboardingEducation.png), [Minat](validation/screens/OnboardingInterests.png). Pilihan SMA/SMK dan minat pada gambar adalah hasil interaksi tes. Mode Mushaf Quran belum diimplementasikan pada tahap onboarding ini.


## Parallax onboarding — 17 Oktober 2026

Hero memakai tiga lapisan kedalaman, orbit elips, medali bulan sabit, kartu ikon pastel dengan gradien/bayangan, gerak mengambang, dan respons drag serta sensor gravitasi (fallback akselerometer). Frame dibatasi sekitar 30 fps. Callback frame dan listener sensor dihentikan saat halaman tersembunyi, kehilangan fokus, Activity pause, atau view dilepas. Animasi mengikuti `ValueAnimator.areAnimatorsEnabled()`. Perpindahan langkah memakai fade/slide 260 ms; hero dipusatkan agar ruang kosong lebih seimbang.

`assembleDebug`, `assembleDebugAndroidTest`, dan `lintDebug` lulus. Satu putaran berisi dua tes perangkat lulus pada API 28: gambar orbit berubah saat animasi aktif dan berhenti ketika meninggalkan welcome; pilihan jenjang/minat tetap tersimpan dan dibatasi tiga. Pemeriksaan visual menemukan alpha paint terbawa ke gradien; reset alpha diperbaiki, lalu hanya build/lint diulang. Suite perangkat tidak diulang karena perubahan terakhir hanya reset paint sebelum shader. Log tersedia pada `validation/onboarding-parallax-build.txt`, `validation/onboarding-parallax-final-build.txt`, dan `validation/onboarding-parallax-tests.txt`.

[Preview gerak native](validation/screens/OnboardingParallax.gif) direkam dari emulator dengan gesture drag; [screenshot](validation/screens/OnboardingWelcome.png). Pengaturan animator emulator dikembalikan setelah rekaman. Tilt pada ponsel fisik dan reduced-motion tidak diuji secara terpisah pada tahap ini.
