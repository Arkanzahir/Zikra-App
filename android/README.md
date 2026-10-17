# Zikra Android — Java dan XML

Project Android Studio mandiri, aplikasi native Java dengan layout XML. Tanpa Kotlin, Compose, Flutter, React Native, atau WebView. Referensi desain tersedia di [`../design/`](../design/). Implementasi awal dipindahkan dari repositori `Arkanzahir/prototype_zikra`, commit `606dfb634db7839860ade25873c22e14ebd08c87`.

## Buka dan jalankan

Buka folder `android` melalui Android Studio → Open. Gunakan JDK 17 atau 21, Android SDK 35, Build Tools 35.0.0, dan perangkat Android 8.0/API 26 atau lebih baru. Rincian lingkungan tersedia di [BUILDING.md](BUILDING.md).

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
# Dengan emulator/perangkat tersambung:
./gradlew connectedDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Halaman dan fitur

Jalur Java berikut berada di `app/src/main/java/com/zikra/app/`; layout di `app/src/main/res/layout/`.

| Halaman | Implementasi dan fungsi |
| --- | --- |
| Home | `HomeActivity.java`, `activity_home.xml`: jam WIB 70sp, Hijriah, siluet masjid, delapan shortcut, 24 tick progres SQLite, dock melayang |
| Protector BMKG | `ProtectorActivity.java`, `activity_protector.xml`: kartu cuaca biru, tautan peringatan resmi, peta Indonesia, laporan gempa, akses bantuan |
| Detail cuaca | `WeatherActivity.java`, `activity_weather.xml`: hero cyan, kelembapan/angin/jarak pandang, grafik suhu Canvas, slot prakiraan bertanggal |
| Detail gempa | `EarthquakeActivity.java`, `activity_earthquake.xml`: peta, koordinat episentrum, kedalaman/magnitudo/waktu, buka lokasi di aplikasi peta |
| Kiblat | `QiblaActivity.java`, `activity_qibla.xml`, `reading/QiblaCompassView.java`: kompas roset sage, Ka'bah, 294° Sukolilo, kalibrasi dan masjid kampus |
| Waktu sholat | `PrayerActivity.java`, `activity_prayer.xml`: tanggal pekanan, tab hari/pekan, countdown dan preferensi pengingat per sholat |
| Quran | `QuranActivity.java`, `activity_quran.xml`: katalog/search surah, hero buku native, reader, bookmark ayat, mode malam sensor/manual |
| Doa | `DuaActivity.java`, `activity_dua.xml`: pencarian, kategori, kartu doa, teks Arab/arti, pelafalan TTS Android |
| Study Hub/Hafalan | `StudyActivity.java`, `activity_study.xml`, `data/ZikraDbHelper.java`: CRUD tugas/ibadah, kategori Hafalan, progres harian dan voice-to-task |
| Zikra AI | `AiActivity.java`, `activity_ai.xml`, `assistant/`: tampilan lavender, topik belajar/ibadah/hafalan, percakapan lokal dan input suara Android |
| Profil | `ProfileActivity.java`, `activity_profile.xml`: progres SQLite, bacaan terakhir, Hadis, Kalender, preferensi belajar, pengingat dan bantuan |
| Onboarding | `OnboardingActivity.java`, `activity_onboarding.xml`: sambutan orbit native, pilih peran dan minat, pilihan disimpan; tampil pada peluncuran pertama |
| Hadis | `HadithActivity.java`, `activity_hadith.xml`: tujuh kutipan pilihan bersumber, pencarian, kategori Bukhari/Muslim dan bookmark |
| Kalender Hijriah | `CalendarActivity.java`, `activity_calendar.xml`, `discovery/HijriCalendarModel.java`: bulan sebelumnya/berikutnya, pilih tanggal, konversi Gregorian dan hari istimewa |
| Navigasi | `NavDock.java`: dock XML utama dan dock ibadah; Activity menggunakan explicit Intent |

Warna dasar Deep Pine `#15332F`, Emerald `#10B981`, background `#F4F6F8`, kartu putih dan CTA `#CF9447`. BMKG menambahkan warna biru/cyan sesuai referensi; halaman ibadah memakai sage dan ivory. Kesamaan setiap piksel di semua perangkat belum diklaim.

## Implementasi Pemrograman Mobile

- **SQLiteOpenHelper:** `ZikraDbHelper` menangani CRUD, transaksi inisialisasi harian dan kalkulasi progres. Hari pertama memuat 24 contoh dengan 8 selesai: 33%, bukan angka 35% yang dipatok. Hari berikutnya memulai 24 default belum selesai; riwayat tetap tersimpan. Tanggal checklist mengikuti zona lokal perangkat.
- **SharedPreferences:** `zikra_preferences` menyimpan surah/ayat terakhir, bookmark hadis, peran/minat onboarding, percakapan lokal, pengingat dan preferensi goyangan.
- **Akselerometer:** dua goyangan membuka pilihan bantuan; listener hanya aktif di foreground. Tombol manual tetap tersedia.
- **Cahaya:** reader Quran menjadi gelap di bawah 10 lux dan terang di atas 15 lux, dengan kontrol manual dan fallback perangkat tanpa sensor.
- **Rotasi:** kompas mengoreksi rotasi layar dan deklinasi magnetik untuk Sukolilo; haptik saat sejajar. Pembaruan dibatasi dan nilai stabil tidak menggambar ulang terus-menerus.
- **Networking:** OkHttp Java 3.14.9 menjalankan request dan parsing di background; hasil dikirim ke UI. Request dibatalkan saat layar ditutup/dimuat ulang.
- **Intent implisit:** dialer `tel:112`, WhatsApp, Google Maps; tidak menelepon atau mengirim pesan otomatis.
- **Device services:** doa memakai TTS Arab; tugas dan pendamping offline memakai `RecognizerIntent`, dengan fallback jika provider tidak tersedia.

## Sumber data dan batasan

- BMKG gempa: `https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json`; cuaca Keputih/Sukolilo: `https://api.bmkg.go.id/publik/prakiraan-cuaca?adm4=35.78.09.1001`.
- Halaman BMKG menampilkan loading, kegagalan, slot kedaluwarsa dan nilai kosong secara eksplisit. Tidak ada suhu/gempa contoh yang ditampilkan sebagai data live. Akses live host BMKG terblokir dari jaringan cloud saat pemeriksaan; validasi respons dilakukan memakai fixture.
- Peta native memakai batas negara **Natural Earth, public domain**, dengan atribusi di `res/raw/protector_map_attribution.txt`. Titik episentrum hanya muncul jika koordinat resmi tersedia dan valid. Peta ringkas bukan navigasi turn-by-turn.
- Quran offline memuat **4 surah lengkap**: Al-Fatihah, Al-Ikhlas, Al-Falaq, An-Nas. Koleksi hadis berisi **7 kutipan pilihan**, bukan seluruh kitab.
- Zikra AI saat ini **pendamping offline berbasis aturan/template**, belum tersambung ke AI generatif online. Input suara tergantung layanan pengenal ucapan perangkat.
- Jadwal sholat memakai **referensi prototipe**, belum perhitungan astronomi atau layanan jadwal harian. Preferensi pengingat tersimpan; alarm/notifikasi otomatis belum dijadwalkan.
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
- Header profil mengikuti peran onboarding yang tersimpan, dan dialog profil dibersihkan ketika Activity ditutup.

## Penyempurnaan Home — 17 Oktober 2026

- Komposisi mengikuti referensi Home ringkas: lokasi, pencarian Quran, akses jadwal/pengingat, tanggal Hijriah, jam 70sp, countdown dan strip sholat. Status bar dan hero memakai warna Pine yang sama.
- Siluet masjid vector digambar ulang dengan kubah, menara dan lengkungan pintu; ditempatkan di bawah jam agar angka tetap jelas.
- Grid tetap 2×4 dengan ikon garis: Al-Qur’an, Doa Harian, Kiblat, Jadwal Sholat, Zikra AI, Protector BMKG, Study Hub dan Hadis.
- Kartu target tetap memakai 24 segmen dan persentase SQLite. Kartu lanjut bacaan menampilkan surah/ayat terakhir, dengan area scroll di atas dock.
- Dimensi memakai varian layar pendek dan tinggi (`values/` dan `values-h740dp/`) agar tombol checklist mendapat ruang di atas dock. Tinggi hero mengikuti ukuran teks.
- Tanggal Hijriah membuka Kalender. Tombol bel membuka Jadwal Sholat dan preferensi pengingat; layanan darurat tetap tersedia melalui Protector dan gestur goyangan.
- Jam diperbarui pada pergantian menit; indikator sholat hanya diperbarui saat sholat berikutnya berubah.
- Perubahan tahap ini berfokus pada Home. Tampilan halaman lainnya dan keterbatasan data di atas masih berlaku.

### Koreksi tata letak dan ikon sesuai referensi

- Strip sholat dinaikkan, dengan ruang 16dp sebelum kartu shortcut. Urutan setiap kolom menjadi nama waktu, ikon, lalu jam; outline lingkaran aktif dihapus, dan ikon waktu berikutnya diberi aksen amber.
- Ikon waktu dibedakan antara bulan/bintang, terbit, matahari, berawan dan terbenam. Shortcut memakai gaya garis tipis emas yang konsisten; ikon Doa berupa tangan berdoa, Quran berupa buku terbuka dan Jadwal berupa jam alarm.
- Delapan nama dan tujuan shortcut tetap mengikuti Zikra. Pergeseran strip mempertahankan tinggi keseluruhan konten agar tombol checklist tetap mendapat ruang pada layar pendek.
