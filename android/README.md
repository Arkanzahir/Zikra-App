> **Pilihan doa beta 1.7.2 — 24 Oktober 2026:** [14 bacaan bawaan luring, kategori wajib dan inventaris koleksi](validation/dua-selection-b04-2026-10-24/README.md). Arab/Latin/arti/rujukan tersedia; pagi/petang dan guruh ditambahkan dengan konteks sumber. Koleksi tambahan tetap 225 bacaan, 213 belum ditelaah per entri. Build/lint dan 110 unit lulus/3 dilewati; APK 31,3 MB. B01/B04 masih terbuka; tidak ada emulator/HP atau email pada putaran ini. Source `91c8739`; riwayat jumlah di bawah berlaku untuk versi masing-masing.

> **Reader 1.4.3 — 22 Oktober 2026:** panel audio/gulir dilipat secara bawaan, tetap dapat mengontrol audio; pilihan tersimpan. Judul surat diberi ornamen emas tipis. [Screenshot, lima tes Android dan batas](validation/reader-fold-ornament-2026-10-22/README.md). Emulator ditutup; belum tes HP.

> **Tata letak1.4.2 — 22 Oktober 2026:** Home, Reader Quran dan Study diselaraskan dengan referensi terbaru: urutan salat, judul surat/bismillah, nomor/audio di kiri ayat, player di atas navigasi dan agenda lebih padat. [Screenshot native, tes dan batas](validation/comfortable-layout-2026-10-22/README.md). Enam metode Android lulus; koreksi kosmetik akhir diperiksa ulang terarah.

> **Quran 1.4.1 — 22 Oktober 2026:** mini-player katalog, tombol lanjut mengikuti ayat, dan pencocokan alif Utsmani. Lima tes Android lulus, termasuk sorotan kata/ayat berikut dengan rekaman Alafasy asli. JVM: 93 lulus, 3 dilewati. [Screenshot, build dan batas](validation/quran-catalog-audio-2026-10-22/README.md).

> **Study beranimasi 1.4.0 — 22 Oktober 2026:** aset ilustrasi dibuat dahulu, lalu dipasang ke agenda/fokus/istirahat. Tangan menulis dan siklus cangkir mengikuti timer; 7 tes Android lulus, termasuk jeda, lifecycle dan font 200%. [Screenshot, video, build dan batas](validation/study-layered-motion-2026-10-22/README.md). Emulator ditutup; HP fisik belum diuji.

> **Home/Quran 1.3.2 — 22 Oktober 2026:** ilustrasi masjid baru, proporsi Home dan daftar surah mengikuti referensi lebih dekat. [Screenshot APK, build dan batas](validation/reference-fidelity-2026-10-22/README.md). Study/AI belum setara ilustrasi konsep.

> **Emulator / 1.3.1 — 22 Oktober 2026:** screenshot native Home/Study/AI/Quran, layout fokus lebih ringkas dan tombol tetap terlihat. Gerak break/pause dan font 200%: 4 tes Android lulus. [Bukti, video dan batas](validation/emulator-layout-2026-10-22/README.md). Emulator ditutup setelah tes.

> **Terbaru 1.3.0:** desain mengikuti konsep pilihan pengguna, sorotan kata reader dan jalur backend Featherless. [Validasi/batas](validation/concept-refresh-2026-10-21/README.md). Belum screenshot/tes native HP atau AI online live.

> **Checkpoint lanjutan 20 Oktober 2026:** timer fokus, tenggat/pengingat Study, status retry peta dan transfer ulang audio rusak tersedia pada `feature/map-audio-study-2026-10-20`. SQLite v3 mempertahankan data. [Hasil dan batas](validation/map-audio-study-2026-10-20/README.md). Provider live belum terverifikasi karena proxy 403.

> **Status 20 Oktober 2026:** `main` terkonfirmasi `1045fa7`, termasuk pemutar Quran native/background/notifikasi, unduhan teks-audio per surah, gulir dan latihan rentang Hifz. [Bukti/batas audio](validation/location-quran-2026-10-19/README.md). Perapian netral masjid/lokasi: [tahap 20 Oktober](validation/location-polish-2026-10-20/README.md). Catatan checkpoint lama di bawah bersifat historis bila berbeda.

> **Status 19 Oktober 2026:** lokasi bersama dan masjid OSM native sudah tersedia; onboarding/parallax dipertahankan. [Validasi terbaru](validation/location-mosques-2026-10-19/README.md) · [RECOVERY.md](../RECOVERY.md). Ronde kegunaan/onboarding sebelumnya tetap historis.


# Zikra Android — Java dan XML

Project Android Studio mandiri, aplikasi native Java dengan layout XML. Tanpa Kotlin, Compose, Flutter, React Native, atau WebView. Referensi desain tersedia di [`../design/`](../design/). Implementasi awal dipindahkan dari repositori `Arkanzahir/prototype_zikra`, commit `606dfb634db7839860ade25873c22e14ebd08c87`.

## Melanjutkan dari chat baru

Baca [status terbaru](validation/location-mosques-2026-10-19/README.md) sebelum inventaris historis di bawah. Onboarding memakai tiga ilustrasi 2D statis tanpa jenjang/minat. Reader terjemahan mencakup 114 surah melalui public v4 API, empat surah bawaan dan cache delapan bagian. Mushaf halaman dan offline seluruh korpus otomatis belum tersedia; pemutar murottal/unduhan per surah sudah terpasang, dengan verifikasi rekaman qari live masih terbuka. Home memakai USNO/fallback berlabel; cuaca memakai Open-Meteo, gempa/peringatan tetap BMKG.


## Buka dan jalankan

Buka folder `android` melalui Android Studio → Open. Gunakan JDK 17, Android SDK 35, Build Tools 35.0.0, dan perangkat Android 8.0/API 26 atau lebih baru. Rincian lingkungan tersedia di [BUILDING.md](BUILDING.md).

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
# Dengan emulator/perangkat tersambung:
./gradlew connectedDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Pilihan provider untuk tahap berikutnya dan kondisi integrasinya dicatat di [API_PLAN.md](API_PLAN.md): Featherless, Gemini, Aladhan, Quran.com, BMKG, Open-Meteo dan Android Intent. Daftar tersebut adalah rencana integrasi; fitur yang sudah tersedia dijelaskan di bawah.

## Halaman dan fitur

Jalur Java berikut berada di `app/src/main/java/com/zikra/app/`; layout di `app/src/main/res/layout/`.

| Halaman | Implementasi dan fungsi |
| --- | --- |
| Home | `HomeActivity.java`, `activity_home.xml`: jam zona pilihan, Hijriah, langit dinamis, delapan shortcut, progres ibadah nyata dan navigasi adaptif |
| Protector BMKG | `ProtectorActivity.java`, `activity_protector.xml`: kartu cuaca Pine/sage, tautan informasi BMKG, peta Indonesia, laporan gempa, akses bantuan |
| Detail cuaca | `WeatherActivity.java`, `activity_weather.xml`: hero Pine, kelembapan/angin/jarak pandang, grafik suhu Canvas, slot prakiraan bertanggal |
| Detail gempa | `EarthquakeActivity.java`, `activity_earthquake.xml`: peta, koordinat episentrum, kedalaman/magnitudo/waktu, buka lokasi di aplikasi peta |
| Kiblat | `QiblaActivity.java`, `activity_qibla.xml`, `reading/QiblaCompassView.java`: kompas roset sage, Ka'bah, bearing dari koordinat pilihan, kalibrasi dan akses masjid terdekat |
| Waktu sholat | `PrayerActivity.java`, `PrayerSchedule.java`, `reminders/`: perhitungan harian koordinat/zona pilihan, tab hari/pekan, countdown, alarm dan notifikasi per sholat |
| Lokasi | `LocationActivity.java`, `location/`: GPS satu kali di foreground, pilihan kota online/offline dan zona waktu eksplisit |
| Masjid | `NearbyMosquesActivity.java`, `mosque/`: daftar/peta OSM native, cache 30 menit, jarak garis lurus dan arah ke Google Maps |
| Quran | `QuranActivity.java`, `activity_quran.xml`: katalog/search surah, hero buku native, reader, bookmark ayat, mode malam sensor/manual |
| Doa | `DuaActivity.java`, `activity_dua.xml`: pencarian, kategori, kartu doa, teks Arab/arti, pelafalan TTS Android |
| Study Hub/Diary Hifz | `StudyActivity.java`, `activity_study.xml`, `data/ZikraDbHelper.java`: agenda akademik, target hafalan terpisah dan editor tugas terstruktur |
| Zikra AI | `AiActivity.java`, `activity_ai.xml`, `assistant/`: tampilan Pine/ivory, topik belajar/ibadah/hafalan, percakapan lokal dan input suara Android |
| Profil | `ProfileActivity.java`, `activity_profile.xml`: progres SQLite, bacaan terakhir, Hadis, Kalender, preferensi belajar, pengingat dan bantuan |
| Onboarding | `OnboardingActivity.java`, `activity_onboarding.xml`: tiga adegan 2D statis 3:2, judul dua baris, langkah 01/03, lewati/lanjut dan SharedPreferences; tanpa jenjang/minat |
| Hadis | `HadithActivity.java`, `activity_hadith.xml`: tujuh kutipan pilihan bersumber, pencarian, kategori Bukhari/Muslim dan bookmark |
| Kalender Hijriah | `CalendarActivity.java`, `activity_calendar.xml`, `discovery/HijriCalendarModel.java`: bulan sebelumnya/berikutnya, pilih tanggal, konversi Gregorian dan hari istimewa |
| Navigasi | `NavDock.java`: dock XML utama dan dock ibadah; Activity menggunakan explicit Intent |

Warna dasar Deep Pine `#15332F`, Emerald `#10B981`, background `#F4F6F8`, kartu putih dan CTA `#CF9447`. BMKG menambahkan warna biru/cyan sesuai referensi; halaman ibadah memakai sage dan ivory. Kesamaan setiap piksel di semua perangkat belum diklaim.

## Implementasi Pemrograman Mobile

- **SQLiteOpenHelper:** `ZikraDbHelper` menangani CRUD, transaksi inisialisasi harian dan kalkulasi progres. Hari baru kosong, tanpa tugas atau progres contoh otomatis. Akademik/Hifz tetap dapat diakses lintas tanggal; ibadah mengikuti hari lokal perangkat. Data lama dipertahankan. Tanggal checklist mengikuti zona lokal perangkat.
- **SharedPreferences:** `zikra_preferences` menyimpan surah/ayat terakhir, bookmark hadis, jenjang (`education_level`)/minat onboarding serta peran lama, percakapan lokal, pengingat dan preferensi goyangan.
- **Akselerometer:** dua goyangan membuka pilihan bantuan; listener hanya aktif di foreground. Tombol manual tetap tersedia.
- **Cahaya:** reader Quran menjadi gelap di bawah 10 lux dan terang di atas 15 lux, dengan kontrol manual dan fallback perangkat tanpa sensor.
- **Rotasi:** kompas mengoreksi rotasi layar dan deklinasi magnetik untuk koordinat pilihan; haptik saat sejajar. Pembaruan dibatasi dan nilai stabil tidak menggambar ulang terus-menerus.
- **Networking:** OkHttp Java 3.14.9 menjalankan request dan parsing di background; hasil dikirim ke UI. Request dibatalkan saat layar ditutup/dimuat ulang.
- **Intent implisit:** dialer `tel:112`, WhatsApp, Google Maps; tidak menelepon atau mengirim pesan otomatis.
- **Device services:** doa memakai TTS Arab; tugas dan pendamping offline memakai `RecognizerIntent`, dengan fallback jika provider tidak tersedia.

## Sumber data dan batasan

- BMKG gempa: `https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json`. Cuaca memakai Open-Meteo untuk koordinat/zona pilihan; endpoint cuaca BMKG lama bukan sumber prakiraan aktif.
- Halaman BMKG menampilkan loading, kegagalan, slot kedaluwarsa dan nilai kosong secara eksplisit. Tidak ada suhu/gempa contoh yang ditampilkan sebagai data live. Akses live host BMKG terblokir dari jaringan cloud saat pemeriksaan; validasi respons dilakukan memakai fixture.
- Peta native memakai batas negara **Natural Earth, public domain**, dengan atribusi di `res/raw/protector_map_attribution.txt`. Titik episentrum hanya muncul jika koordinat resmi tersedia dan valid. Peta ringkas bukan navigasi turn-by-turn.
- Quran offline memuat **4 surah lengkap**: Al-Fatihah, Al-Ikhlas, Al-Falaq, An-Nas. Koleksi hadis berisi **7 kutipan pilihan**, bukan seluruh kitab.
- Zikra AI saat ini **pendamping offline berbasis aturan/template**, belum tersambung ke AI generatif online. Input suara tergantung layanan pengenal ucapan perangkat.
- Jadwal sholat memakai **perhitungan matahari harian**, bukan jadwal resmi Kemenag atau feed jadwal online. Koordinat dan zona mengikuti pilihan bersama; Sukolilo (-7,280; 112,790), Asia/Jakarta hanya acuan awal. Asumsi permukaan laut. Metode: Subuh -20°, Isya -18°, Ashar bayangan 1×, matahari terbit/terbenam -0,833°, pembulatan ke atas dan ihtiyati 2 menit untuk sholat (terbit tanpa ihtiyati). Algoritme posisi matahari mengikuti persamaan NOAA; nilai diuji terhadap Astral 3.2 pada empat musim dengan selisih maksimal 1 menit setelah pembulatan/ihtiyati. Cocokkan jadwal dengan masjid setempat.
- Pengingat memakai lima alarm satu kali yang dihitung ulang setelah alarm terkirim, perubahan preferensi, reboot atau perubahan jam. Android 13+ meminta izin notifikasi saat pengguna menyalakan pengingat. Android 12+ menyediakan akses alarm tepat waktu melalui tombol pengaturan; jika akses tidak diberikan, alarm memakai penjadwalan yang dapat ditunda sistem. Bunyi mengikuti channel notifikasi Android, bukan audio adzan. Pengingat awalnya nonaktif sampai pengguna mengaktifkannya.
- Kalender memakai **Umm al-Qura**; awal bulan/hari raya mengikuti otoritas setempat dan dapat berbeda. Tidak mengklaim penetapan rukyat.
- Profil lokal tanpa login/sinkronisasi. Infaq dikeluarkan dari shortcut Home; kode halaman lama masih tersimpan sebagai arsip implementasi.
- Sensor, haptik, pelafalan Arab serta aplikasi tujuan Intent perlu diuji pada ponsel yang mendukungnya; emulator tidak membuktikan akurasi sensor fisik.

## Validasi dan screenshot

Lihat [VALIDATION.md](VALIDATION.md). Screenshot aplikasi native berada di folder `validation/screens/`: [BMKG](validation/screens/ProtectorActivity.png), [Kiblat](validation/screens/QiblaActivity.png), [Quran](validation/screens/QuranActivity.png), [AI offline](validation/screens/AiActivity.png), [Kalender](validation/screens/CalendarActivity.png). Font Plus Jakarta Sans dibundel bersama lisensi OFL pada `res/raw/`.

## Struktur UI dan state

- `res/values/colors.xml` menyimpan warna brand dan varian sage/ivory/lavender; `dimens.xml` menyimpan jarak layar, area sentuh dan ruang dock.
- Layout XML diformat konsisten, dengan area sentuh tombol header minimal 48dp. Source Java menggunakan format yang sama agar mudah ditelusuri.
- Composer AI terpisah dari area scroll percakapan. Dock disembunyikan saat keyboard terbuka, lalu ditampilkan lagi setelah keyboard ditutup.
- Pilihan tanggal dan tab hari/pekan pada Waktu Sholat disimpan melalui `onSaveInstanceState`.
- Header profil mengikuti jenjang/peran onboarding yang tersimpan, dan dialog profil dibersihkan ketika Activity ditutup.

## Penyempurnaan Home — 17 Oktober 2026

- Komposisi mengikuti referensi Home ringkas: lokasi, pencarian Quran, akses jadwal/pengingat, tanggal Hijriah, jam 70sp, countdown dan strip sholat. Status bar dan hero memakai warna Pine yang sama.
- Siluet masjid ditempatkan di bawah jam agar angka tetap jelas; versi terbaru menggunakan aset transparan berdasarkan acuan pengguna.
- Grid tetap 2×4 dengan ikon garis: Al-Qur’an, Doa Harian, Kiblat, Diary Hifz, Zikra AI, Protector BMKG, Study Hub dan Hadis.
- Kartu target tetap memakai 24 segmen dan persentase SQLite. Kartu lanjut bacaan menampilkan surah/ayat terakhir, dengan area scroll di atas dock.
- Dimensi memakai varian layar pendek dan tinggi (`values/` dan `values-h740dp/`) agar tombol checklist mendapat ruang di atas dock. Tinggi hero mengikuti ukuran teks.
- Tanggal Hijriah membuka Kalender. Tombol bel membuka Jadwal Sholat dan preferensi pengingat; layanan darurat tetap tersedia melalui Protector dan gestur goyangan.
- Jam diperbarui pada pergantian menit; indikator sholat hanya diperbarui saat sholat berikutnya berubah.
- Perubahan tahap ini berfokus pada Home. Tampilan halaman lainnya dan keterbatasan data di atas masih berlaku.

### Koreksi tata letak dan ikon sesuai referensi

- Strip sholat dinaikkan, dengan ruang 16dp sebelum kartu shortcut. Urutan setiap kolom menjadi nama waktu, ikon, lalu jam; outline lingkaran aktif dihapus, dan ikon waktu berikutnya diberi aksen amber.
- Ikon waktu dibedakan antara bulan/bintang, terbit, matahari, berawan dan terbenam. Shortcut memakai gaya garis tipis emas yang konsisten; ikon Doa berupa tangan berdoa, Quran berupa buku terbuka dan Diary Hifz berupa topi wisuda.
- Delapan nama dan tujuan shortcut tetap mengikuti Zikra. Pergeseran strip mempertahankan tinggi keseluruhan konten agar tombol checklist tetap mendapat ruang pada layar pendek.

### Kartu shortcut melengkung dan siluet transparan

- Pembungkus shortcut memakai `ui/CurvedShortcutLayout.java`, sebuah `LinearLayout` native dengan path putih yang turun 8dp di tengah tepi atas, mengikuti lengkungan pada referensi. Grid tetap didefinisikan dalam XML.
- Kontur yang sama membatasi ripple anak. Outline elevasi memakai bentuk convex yang kompatibel dengan Android 8, sementara permukaan kartu mempertahankan lengkungan cekung.
- Aset `res/drawable-nodpi/home_mosque_silhouette.png` dibuat dari acuan siluet yang diberikan pengguna, dengan latar dan bukaan jendela transparan serta bidang bawah yang dipangkas. Tint teal dan alpha 0,15 diterapkan melalui XML.
- Aset asli hasil pemrosesan disimpan tanpa modifikasi piksel tambahan. Penempatan memakai `fitCenter` agar proporsi kubah dan menara tetap terjaga.

### Aksen lentera pada hero

- `home_hero_ornaments.xml` menambahkan dua lentera outline dengan ukuran berbeda dan tiga bintang kecil di sisi kiri/kanan. Ornamen vector emas berada di bawah bar lokasi dan di luar area teks tanggal/jam.
- Ornamen memakai alpha 0,18, tidak menerima klik atau fokus, dan tidak dibacakan sebagai elemen aksesibilitas. Layout dan alur navigasi tetap sama.
- Siluet masjid tetap selebar hero dengan proporsi asli. Opasitasnya diperjelas menjadi 0,15 dan posisinya dinaikkan 4dp pada layar tinggi; posisi layar pendek dipertahankan agar tidak bertabrakan dengan jam.

## Penyempurnaan fungsi harian

- Home menampilkan jumlah checklist yang **selesai**, termasuk kondisi tanpa checklist. Diary Hifz menggantikan shortcut Jadwal Sholat; strip waktu dan tombol bel tetap membuka jadwal lengkap.
- Diary Hifz adalah mode target hafalan harian pada SQLite yang sama: tambah/edit/hapus dan centang target, progres khusus hafalan, serta input suara. Ini belum jurnal evaluasi hafalan atau analisis bacaan AI.
- `QuranActivity.EXTRA_RESUME_READING` membuka reader langsung pada surah/ayat tersimpan. Posisi gulir dipulihkan setelah layout siap; binding surah awal tidak diulang oleh callback Spinner.
- `PrayerSchedule` menyediakan snapshot harian immutable dan waktu sholat berikutnya lintas tanggal/zona waktu. Home, daftar harian/pekan dan alarm menggunakan perhitungan yang sama.
- `reminders/PrayerReminders`, `PrayerAlarmReceiver`, dan `PrayerRescheduleReceiver` mengelola channel, izin notifikasi, lima alarm, penerimaan reminder, dan penjadwalan ulang. Kejadian masa depan, kejadian yang terlambat lebih dari 30 menit, dan duplikat tidak diposting sebagai notifikasi baru.

## Penyempurnaan desain Protector — 17 Oktober 2026

- Dashboard mengikuti proporsi referensi Info BMKG: suhu dan ilustrasi berdampingan pada hero biru yang lebih ringkas, kartu peringatan amber, peta Indonesia dan ringkasan laporan gempa, serta bagian bantuan dengan tombol native.
- Detail cuaca memakai hero cyan, ikon berdasarkan deskripsi yang diterima, tiga metrik cuaca, grafik suhu, dan deretan prakiraan bertanggal. Kartu prakiraan didefinisikan dalam `weather_forecast_item.xml`; Java hanya mengikat data dan warna keadaan terpilih.
- Detail gempa memakai peta dengan panel putih bersudut atas membulat. Kartu biru menampilkan waktu WIB, magnitudo dan kedalaman; koordinat menggunakan LS/LU serta BT/BB. Tombol Maps tampil nonaktif saat belum ada koordinat.
- `ProtectorChrome` menyatukan system bar ketiga layar. Warna dan ukuran khusus Protector berada di resource; Home yang sudah disetujui tetap dipertahankan.
- Ini pembaruan desain dengan sumber BMKG yang sudah ada. Migrasi cuaca ke Open-Meteo dan integrasi provider baru pada `API_PLAN.md` belum dilakukan pada tahap ini.


### Onboarding — 17 Oktober 2026

Tiga layar native: hero orbit, jenjang SMP/SMA/SMK/Mahasiswa, dan maksimal tiga minat dari delapan pilihan. Onboarding hanya diwajibkan saat penggunaan pertama dan dapat diedit lewat Profil. Tanpa login; pilihan tersimpan lokal. Screenshot dan hasil dua tes perangkat tersedia pada `VALIDATION.md`.


Onboarding kini memiliki parallax sentuh/tilt, ikon mengambang dan transisi langkah native. Gerak berhenti ketika halaman tidak aktif dan mengikuti pengaturan animasi sistem. Preview: `validation/screens/OnboardingParallax.gif`.

## Lokasi dan peta

Masuk dari label lokasi Home atau Profil → Lokasi. Lokasi perangkat diambil saat diminta; izin perkiraan tetap didukung dan pilihan manual tersedia. Tidak memakai izin lokasi background. GPS bukan fitur hardware wajib. Peta menggunakan osmdroid Java, dengan data/tiles OpenStreetMap; tanpa WebView, Kotlin, Google SDK atau API key. Layanan publik tunduk kebijakan pemakaian; [rincian provider](API_PLAN.md).
