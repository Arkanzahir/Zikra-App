# A01 — Audit fitur Zikra

24 Oktober 2026. Baseline source **`5884b52074e24334223109cba3826b9876b3248b`**, aplikasi **1.6.2 / code 16**. Acuan: [PRD](../../../ZIKRA_PRD.md) dan [checklist aktif](../../../IMPLEMENTATION_PLAN.md).

Pembaruan sesudah audit ini: [A02 menetapkan cakupan](../../../RELEASE_SCOPE.md), [A03 menyimpan WIP audio terpisah](../baseline-a03-2026-10-24/README.md), dan [A04/1.6.3 memperbaiki integrasi zona/tanggal/bacaan serta membuktikan buka ulang proses](../integration-a04-2026-10-24/README.md). Tabel di bawah tetap snapshot A01; jangan menjadikan gap kalender WIB atau status A02/A03/A04 yang historis sebagai status terbaru.

Audit ini membaca source, manifest, model penyimpanan, rute antarmenu dan laporan pemeriksaan terdahulu. Tidak menjalankan ulang APK/emulator atau menguji HP. Bukti historis tetap terbatas pada versi, perangkat dan skenario laporan masing-masing. Screenshot konsep bukan bukti implementasi. Tidak ada fitur yang disertifikasi **teruji rilis** oleh audit ini.

## Arti status

- **Belum ada:** belum ditemukan implementasi kebutuhan tersebut pada baseline.
- **Sebagian:** ada subset, tetapi kebutuhan PRD belum terpenuhi seluruhnya.
- **Tersedia:** implementasi ditemukan; belum ada bukti runtime memadai untuk perilaku yang disebut.
- **Teruji terarah:** subset perilaku mempunyai bukti native/backend terdahulu; batasnya ditulis.
- **Teruji rilis:** seluruh syarat cakupan rilis, integrasi dan HP terpenuhi. **Belum diberikan.**

Status subset tidak menaikkan status keseluruhan fitur. Contoh: notifikasi salat tersedia tidak berarti adzan penuh tersedia; minggu pada kalender bukan jadwal kelas mingguan.

## Bukti yang digunakan

| Kode | Laporan | Batas pemakaian bukti |
| --- | --- | --- |
| HARIAN | [Halaman harian](../daily-native-2026-10-24/README.md) | Empat pemeriksaan terarah Hadis/Doa/Profil/onboarding 1.6.0; bukan seluruh konten |
| PROFIL | [Latar Profil](../profile-background-2026-10-24/README.md) | Profil 1.6.1; bukan akun/sinkronisasi |
| DOA | [Daftar Doa](../dua-list-2026-10-24/README.md) | Daftar/cari/buka 1.6.2; TTS tidak dibuktikan pada putaran ini |
| QURAN | [Katalog/audio](../quran-catalog-audio-2026-10-22/README.md), [reader lipat](../reader-fold-ornament-2026-10-22/README.md), [Mushaf](../mushaf-word-sync-2026-10-21/README.md) | Ada rekaman Alafasy asli dan sorotan terarah; bukan seluruh ayat/qari/Android |
| STUDY | [Gerak fokus/break](../study-layered-motion-2026-10-22/README.md), [deadline/timer](../map-audio-study-2026-10-20/README.md) | Timer, gerak, lifecycle dan tenggat; bukan semua alarm saat HP tidur |
| PROTECTOR | [Protector native](../protector-native-2026-10-23/README.md) | Cuaca/peta/refresh native; bukan push saat aplikasi ditutup |
| LOKASI | [Perapian lokasi](../location-polish-2026-10-20/README.md), [masjid](../location-mosques-2026-10-19/README.md) | Skenario lokasi/hasil/fallback terdahulu; sensor HP tetap perlu diuji |
| DATA | [Recovery](../recovery/README.md), [quality](../quality-2026-10-19/README.md) | Riwayat migrasi/fitur dasar; batas laporan lama yang sudah diperbaiki tidak dijadikan status terbaru |
| AI | [Fondasi gateway](../concept-refresh-2026-10-21/README.md) | Tes gateway dengan provider simulasi. Catatan handoff 22 Oktober menyebut respons provider dan gateway lokal 200; belum hosting permanen atau AI online dari APK di HP |
| UKURAN | [Ukuran APK](../apk-size-2026-10-24.md) | Debug 1.6.2 sebesar 31.216.429 byte; bukan ukuran semua fitur mendatang atau RAM |

## Seluruh kebutuhan inti PRD

| ID | Aktual dan status | Bukti/source utama | Kesenjangan yang harus ditutup | ID lanjutan |
| --- | --- | --- | --- | --- |
| F1.1 | **Sebagian**: lokasi GPS/manual, hitung salat lokal dan countdown | [PrayerSchedule](../../app/src/main/java/com/zikra/app/PrayerSchedule.java), LOKASI | Belum integrasi Aladhan method 11 yang disebut PRD. Hisab lokal tidak boleh disebut feed resmi Kemenag. Validasi metode, lintang, tanggal dan zona | A04, D01 |
| F1.2 | **Sebagian**: alarm lokal per salat berikutnya, toggle dan notifikasi/getar | [PrayerReminders](../../app/src/main/java/com/zikra/app/reminders/PrayerReminders.java), DATA | Bukan jadwal 7 hari penuh; belum adzan durasi penuh/pilihan suara/mode kelas/jeda Pomodoro karena adzan. Exact alarm dapat jatuh ke inexact bila izin tidak tersedia; HP tidur/reboot perlu dibuktikan | D02, G02 |
| F1.3 | **Sebagian**, checklist manual teruji terarah | [SQLite](../../app/src/main/java/com/zikra/app/data/ZikraDbHelper.java), DATA | Belum streak mingguan dan pencatatan ketepatan waktu salat | D05 |
| F1.4 | **Tersedia**: bearing Kiblat, rotation vector, koreksi deklinasi dan status sensor | [Qibla](../../app/src/main/java/com/zikra/app/QiblaActivity.java), LOKASI | Presisi/kalibrasi di HP fisik belum dibuktikan | D01, G02 |
| F2.1 | **Sebagian**, reader/Mushaf teruji terarah: 114 surah, Arab/Indonesia, halaman dan unduhan teks | [Repository](../../app/src/main/java/com/zikra/app/reading/QuranRepository.java), [unduhan](../../app/src/main/java/com/zikra/app/audio/QuranDownloadService.java), QURAN | Mushaf QCF V2 Android 12+ dan membutuhkan font/data. Cache otomatis terbatas; bukan seluruh korpus selalu luring. Transliterasi Latin belum ditemukan; sumber/terjemahan dan pemetaan penuh perlu audit | D03, G03 |
| F2.2 | **Teruji terarah**: stream, pilihan qari, play/pause/seek/repeat/rentang, background dan download per surah | [Audio service](../../app/src/main/java/com/zikra/app/audio/QuranAudioService.java), QURAN | Timing/warna/gulir untuk semua ayat dan qari belum diaudit. Seek/repeat/offline rusak/telepon di HP belum tuntas. WIP audio belum masuk baseline | A03, D03, G02 |
| F2.3 | **Teruji terarah**: bacaan terakhir, penanda posisi dan lanjut Home/katalog/reader | [QuranActivity](../../app/src/main/java/com/zikra/app/QuranActivity.java), QURAN | Penanda posisi bukan bukti koleksi multi-bookmark pribadi. Periksa konsistensi ayat/halaman dan pulih setelah pembaruan | A04, D03, D06 |
| F2.4 | **Belum ada** target khatam 30/90/365 hari | [Model tugas](../../app/src/main/java/com/zikra/app/data/ZikraDbHelper.java) | Diary Hifz/rentang ayat tidak setara tracker khatam | D05 |
| F2.5 | **Belum ada** alur tafsir/tadabbur per ayat dengan sumber terverifikasi | [QuranActivity](../../app/src/main/java/com/zikra/app/QuranActivity.java), [AI](../../app/src/main/java/com/zikra/app/AiActivity.java) | Perlu sumber tafsir, konteks ayat, rujukan dan alur tanya; chat umum bukan tafsir | C04, G03 |
| F3.1 | **Sebagian/legacy**: Profil membaca preferensi jenjang lama; onboarding aktif tiga ilustrasi | [Onboarding](../../app/src/main/java/com/zikra/app/OnboardingActivity.java), HARIAN | Tidak ada pemilihan jenjang aktif dan penyesuaian model kelas/SKS/dosen yang lengkap | A02, D04, F02 |
| F3.2 | **Belum ada** model jadwal kelas rutin mingguan | [SQLite](../../app/src/main/java/com/zikra/app/data/ZikraDbHelper.java), [Study](../../app/src/main/java/com/zikra/app/StudyActivity.java) | Week strip/agenda tugas bukan recurrence kelas; Home belum ringkasan jadwal kelas | D04 |
| F3.3 | **Belum ada** alarm kelas H-30/H-15 | [StudyReminders](../../app/src/main/java/com/zikra/app/study/StudyReminders.java) | Pengingat tenggat tugas tidak setara alarm kelas | D04, G02 |
| F3.4 | **Sebagian**, task CRUD/tenggat/pengingat teruji terarah | [Study](../../app/src/main/java/com/zikra/app/StudyActivity.java), STUDY | Schema belum field mata pelajaran/prioritas; alarm satu dueAt, belum pasangan H-1/Hari-H. Audit urutan deadline dan pemulihan | D04, D06 |
| F3.5 | **Belum ada** briefing pagi terjadwal | [Manifest](../../app/src/main/AndroidManifest.xml), [Study](../../app/src/main/java/com/zikra/app/StudyActivity.java) | Perlu model jadwal, ringkasan tugas/cuaca/salat, izin dan pengiriman terjadwal | A02, D04, E03 |
| F4.1 | **Sebagian**, fetch laporan BMKG dan peta teruji terarah | [BMKG](../../app/src/main/java/com/zikra/app/safety/BmkgClient.java), PROTECTOR | Belum pemantau server/push, nada darurat dan deep link ID kejadian tetap. Laporan gempa nasional terbaru bukan prediksi atau jaminan peringatan sebelum guncangan | E01, E02, E04 |
| F4.2 | **Sebagian**, Open-Meteo dan animasi sesuai kode cuaca; Doa hujan lokal | [Cuaca](../../app/src/main/java/com/zikra/app/safety/OpenMeteoClient.java), PROTECTOR, DOA | Prakiraan bukan pengukuran langsung. Belum alert background/cuaca ekstrem yang memicu Doa, pelafalan/TTS perlu pemeriksaan perangkat | B04, E03 |
| F4.3 | **Belum ada** pemicu doa perjalanan berdasarkan jadwal berangkat | [Manifest](../../app/src/main/AndroidManifest.xml), [StudyReminders](../../app/src/main/java/com/zikra/app/study/StudyReminders.java) | Bergantung jadwal, paket doa dan alarm pengguna | B04, D04, E03 |
| F4.4 | **Belum ada** paket/pengingat dzikir pagi-petang lengkap | [Dua](../../app/src/main/java/com/zikra/app/DuaActivity.java), [Manifest](../../app/src/main/AndroidManifest.xml) | Empat Doa lokal bukan paket Al-Ma'tsurat; cakupan/rujukan dan pengingat belum ada | B04, E03 |
| F5.1 | **Sebagian**: chat lokal berbasis aturan; jalur online dan persona server tersedia | [OfflineAssistant](../../app/src/main/java/com/zikra/app/assistant/OfflineAssistant.java), [gateway](../../../backend/server.mjs), AI | Mode lokal bukan AI generatif. Belum layanan produksi selalu hidup dan bukti APK ke provider dari HP | C01–C03, C05, F02 |
| F5.2 | **Sebagian**: aturan lokal membaca jumlah tugas/bacaan terakhir; online mengirim chat pilihan | [AI](../../app/src/main/java/com/zikra/app/AiActivity.java), [gateway](../../../backend/server.mjs) | Belum konteks nama/jenjang/jadwal/tugas lengkap/waktu salat terpadu dengan kontrol privasi | C04 |
| F5.3 | **Belum ada** tindakan database dari chat | [OfflineAssistant](../../app/src/main/java/com/zikra/app/assistant/OfflineAssistant.java), [gateway](../../../backend/server.mjs) | Shortcut navigasi tidak sama dengan tool action. Perlu review usulan, validasi, simpan sekali dan pembatalan | C04 |
| F5.4 | **Belum ada** Vision/scan PR | [AI](../../app/src/main/java/com/zikra/app/AiActivity.java), [gateway](../../../backend/server.mjs) | Model/kontrak sekarang teks; scope rilis belum disepakati. Jangan mengklaim foto bisa diproses | A02, C06 |
| F5.5 | **Tersedia** dikte Android | [AI](../../app/src/main/java/com/zikra/app/AiActivity.java), [Study](../../app/src/main/java/com/zikra/app/StudyActivity.java) | Bergantung recognizer HP; belum bukti lintas perangkat/izin/ketiadaan layanan, tidak otomatis luring | C06, G02 |

## Seluruh layar terdaftar dan sambungannya

Inventaris manifest: **21 activity, 2 service dan 4 receiver**. [Inventaris otomatis source baseline](SOURCE_INVENTORY.json) berisi hash source, tujuan Intent eksplisit dan referensi activity dari source lain. Referensi statis membantu meninjau sambungan; bukan bukti semua tombol telah diketuk. NavDock menghubungkan Home, Quran, AI, Study dan Profil.

| Layar | Rute/isi aktual | Status dan sisa pekerjaan |
| --- | --- | --- |
| SplashActivity | Launcher → Home | Tersedia; cold start/upgrade G01–G02 |
| OnboardingActivity | Home instalasi pertama; Profil edit/pengenalan | Teruji terarah HARIAN; tiga cerita/skip, bukan setup jenjang lengkap. D04/F02 |
| HomeActivity | Delapan shortcut, lokasi, jadwal, checklist dan lanjut Quran | Teruji terarah pada laporan layout; audit konsistensi seluruh data A04/G01. Parallax/dekorasi bukan bukti akurasi astronomi |
| QuranActivity | NavDock/Home/Profil/Study → katalog dan reader, player | Teruji terarah QURAN; D03, panel/audio/luring dan font/ukuran HP F03 |
| MushafActivity | Quran → halaman/juz/surah/QCF | Teruji terarah QURAN; Android 12+, font/timing/data dan luring penuh terbatas. D03 |
| StudyActivity | NavDock/Home/Profil/Prayer → Study, Ibadah, Diary Hifz | Teruji terarah STUDY/DATA; D04/D05. Hifz di layar ini, bukan activity tersendiri |
| FocusTimerActivity | Home/Study → fokus, jeda, break | Teruji terarah STUDY; belum integrasi adzan dan pengukuran baterai HP. D02/F04 |
| AiActivity | NavDock/Home → offline, opsi gateway dan dikte | Sebagian; gateway lokal pernah menjawab, belum hosting/HP online. C01–C06 |
| ProtectorActivity | Home/Profil → cuaca, gempa, panduan/kontak | Teruji terarah PROTECTOR; E01–E04, belum push ketika ditutup |
| WeatherActivity | Protector → detail cuaca kota pilihan | Tersedia dengan riwayat pemeriksaan data; alert kontekstual belum ada. E03/A04 |
| EarthquakeActivity | Protector → episentrum/Shakemap/detail | Teruji terarah PROTECTOR; audit laporan-pin-waktu dan kegagalan data E01 |
| HadithActivity | Home/Profil → filter kitab/cari tujuh kutipan | Teruji terarah HARIAN; bukan dua kitab lengkap. B01–B03 |
| HadithReaderActivity | Hadis → baca, ukuran, simpan, salin/bagikan | Teruji terarah HARIAN; ID stabil dan pembaruan dataset B03 |
| DuaActivity | Home/Profil → kategori/cari/buka empat Doa/TTS | Teruji terarah DOA; konten baru/TTS B04, styling keseluruhan F02 |
| ProfileActivity | NavDock/Home → nama/progres/pengaturan/shortcut | Teruji terarah PROFIL; tidak ada akun/sinkronisasi. Ruang simpanan menuju modul umum, bukan perpustakaan bookmark terpadu. A02/D06 |
| PrayerActivity | Home/Profil → jadwal/toggle/lokasi/Kiblat/Ibadah | Sebagian; D01–D02/G02, adzan penuh belum ada |
| QiblaActivity | Home/Prayer → arah/lokasi/masjid | Tersedia; presisi sensor HP belum dibuktikan D01 |
| LocationActivity | Home/Profil/Prayer/Kiblat/Protector → lokasi GPS/manual | Teruji terarah LOKASI; audit lintas menu/zona A04 |
| NearbyMosquesActivity | Kiblat/Profil → daftar/peta/arah OSM | Teruji terarah LOKASI; komunitas tidak menjamin semua masjid. Kegagalan layanan/atribusi E04 |
| CalendarActivity | Home/Profil → kalender Hijriah | Tersedia; HijrahDate pendekatan, bukan keputusan hari besar resmi. Tanggal masih Asia/Jakarta, perlu audit bila kota zona lain A04 |
| InfaqActivity | Manifest dan source ada; referensi masuk dari source activity lain tidak ditemukan | Tersedia sebagai panduan statis/tautan luar, belum jalur menu aktif yang terbukti atau pembayaran. Putuskan scope A02 sebelum menghubungkan/meniadakan |

## Konten yang benar-benar ada dan kandidat perluasan

| Konten | APK 1.6.2 | Hasil hitung kandidat 24 Oktober 2026 | Status |
| --- | --- | --- | --- |
| Doa | **4** entri lokal | **227** entri Doa/dzikir EQuran | Respons 200, ID unik 1–227. Dokumentasi menyebut 228; perbedaan belum dijelaskan |
| Hadis | **7** kutipan lokal, seluruh koleksi digabung | Bukhari **6.638** + Muslim **4.930** = **11.568** entri | Dua dataset kandidat, belum diimpor. Bukan 11.568 riwayat unik atau seluruh kitab Hadis |

Hitungan dilakukan pada JSON sumber, bukan mengira dari nomor terakhir. [Metadata ukuran/hash/nomor sumber](CONTENT_COUNTS.json) menyimpan bukti tanpa menyalin seluruh isi. Bukhari memiliki 6.638 nomor unik dalam rentang 1–7.008, dengan **370 nomor tidak muncul**. Penyebab/edisi belum diaudit; ini tidak membuktikan otomatis ada 370 teks hilang. Muslim baru dihitung jumlah barisnya; ID unik/rentang belum diperiksa. Field `id` pada Hadith API berisi terjemahan Indonesia, bukan ID unik.

Sumber primer: [EQuran endpoint](https://equran.id/api/doa), [dokumentasi EQuran](https://equran.id/apidev/doa), [repository Hadith API](https://github.com/gadingnst/hadith-api), [Bukhari JSON](https://raw.githubusercontent.com/gadingnst/hadith-api/master/books/bukhari.json), [Muslim JSON](https://raw.githubusercontent.com/gadingnst/hadith-api/master/books/muslim.json). Host `api.hadith.gading.dev` tidak berhasil diakses dari lingkungan pemeriksaan (timeout/TLS), sementara raw GitHub berhasil; bukan kesimpulan layanan mati untuk semua pengguna.

**B01/B04 tetap terbuka:** sumber/edisi, ketepatan rujukan, hak penggunaan terjemahan/redistribusi dan kelengkapan belum disahkan. Lisensi kode tidak otomatis mengesahkan seluruh isi. Jangan memasang target “kitab lengkap” berdasarkan angka ini saja. [Usulan pengiriman konten](../../../design/CONTENT_DELIVERY_PLAN.md) belum diimplementasikan; tidak ada hosting konten produksi baru atau perubahan isi APK pada A01.

## Kesenjangan lintas fitur dan rilis

1. **Baseline/reproduksi (A03):** working tree memuat WIP QuranAudioService, TimedAudioRepository, MushafReaderTest dan dua tool audit. APK 1.6.2 dibangun dari snapshot stabil tanpa WIP audio tersebut. Audit source menggunakan commit baseline, bukan mengklaim WIP sudah terdistribusi. PROJECT_HANDOFF/draft desain lokal juga dipertahankan.
2. **Integrasi (A04):** lokasi bersama, bacaan terakhir, tugas/progres perlu pemeriksaan pulih lintas layar; kalender masih zona Jakarta. A01 hanya menemukan gap, tidak mencentang pemeriksaan runtime A04.
3. **Data/akun (A02/D06):** SQLite v3 menyimpan tugas, Hifz, checklist dan tenggat; preferensi menyimpan profil/lokasi/bacaan. Belum akun/cloud sync, pemulihan pindah HP atau aturan konflik. Migrasi historis tidak menjamin seluruh versi mendatang.
4. **Layanan (C/E/G03):** gateway Node/Featherless perlu hosting, autentikasi/kuota per pengguna dan pemeriksaan konfigurasi; token bersama hanya jalur pribadi. Quran, OSM/Overpass, geocoding, BMKG dan Open-Meteo memakai layanan luar dengan failure/atribusi yang harus ditinjau. Belum server push gempa/cuaca.
5. **Desain/performa (F01–F04):** desain native dan gerak telah berubah, tetapi kesesuaian semua layar, font besar, aksesibilitas dan baterai/RAM HP belum disahkan. Ilustrasi cuaca/kota bukan foto terverifikasi kota; fase bulan bersifat pendekatan dekoratif.
6. **Distribusi (G01–G05):** APK debug 31,2 MB di bawah batas unduhan 100 MB saat ini; belum signed release/hasil toko, beta lintas HP, layanan permanen atau gerbang rilis publik. Ukuran konten luring setelah instalasi dan RAM dinilai terpisah.

Stack aktif adalah Android Java/XML + SQLite + gateway Node/Featherless. Stack Expo/Mongo/provider lama di PRD bukan instruksi migrasi. XP/badge, keuangan dan Ramadan tetap backlog post-MVP yang sudah dipisahkan PRD. Integrasi Pomodoro/adzan tetap kebutuhan terbuka bila cakupan rilis memasukkannya.

## Hasil A01 dan langkah berikutnya

**A01 selesai sebagai audit dokumentasi:** 23 kebutuhan inti dipetakan ke aktual/bukti/gap/ID lanjutan; 21 layar dan sambungan diperiksa; jumlah konten dibedakan dari kandidat. Tidak ada centang kesiapan rilis atau kenaikan versi APK.

Berikutnya **A02 — putuskan cakupan beta dan publik**. Rekomendasi awal untuk dibahas: beta dapat menguji modul lokal yang sudah tersedia dengan batas tertulis; rilis publik tetap membutuhkan seluruh fitur wajib yang disepakati, konten tervalidasi, layanan dan HP. Akun/sinkronisasi, Vision, briefing, tindakan AI, serta posisi Infaq belum diputuskan; jangan menganggapnya dibatalkan atau ditunda tanpa keputusan pengguna. B01–B04 baru dilanjutkan setelah A02 sesuai dependensi plan.
