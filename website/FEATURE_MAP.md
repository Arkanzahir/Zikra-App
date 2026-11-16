# Fitur pada website Zikra

16November2026. Katalog menjelaskan fitur produk, bukan tanda seluruh gate rilis selesai. HOLD, tidak ada APK publik atau aktivasi layanan baru.

## Enam panel visual

| Panel | Rujukan implementasi | Batas yang dijelaskan |
|---|---|---|
| Al-Quran | QuranActivity, MushafActivity, DetailActivity | Unduhan teks/font/audio berbeda |
| Zikra AI | AiActivity, assistant/ImageGenerationClient | Riwayat lokal, online berkuota, gambar mengikuti layanan; akun18+ belum diluncurkan |
| Study dan fokus | StudyActivity, WeeklyClassesActivity, FocusTimerActivity | Pengguna menyusun tugas/jadwal sendiri |
| Hifz | FaithActivity pada alur Diary Hifz dan penyimpanan target | Tidak mengklaim deteksi hafalan otomatis |
| Kalender Hijriah | CalendarActivity, discovery/HijriCalendarModel | Perkiraan Umm al-Qura, bukan penetapan resmi |
| Protector | ProtectorActivity, WeatherActivity, EarthquakeActivity, EarthquakeAlertsActivity | Internet/pembaruan; gempa bukan prediksi/peringatan dini |

## Dua belas penjelasan tambahan

| Topik | Rujukan implementasi | Catatan |
|---|---|---|
| Doa | DuaActivity, DuaLibraryActivity | Bacaan/arti/simpanan |
| Hadis | HadithActivity, HadithReaderActivity | HadeethEnc untuk cakupan publik; Gading tetap ditunda |
| Kiblat | QiblaActivity | Sensor, posisi dan kalibrasi perangkat |
| Salat/adzan | PrayerActivity, prayer | Per-waktu/izin Android/MP3 lokal biasa dan Subuh |
| Masjid | NearbyMosquesActivity | Jarak garis lurus, bukan rute |
| Ibadah hari ini | FaithActivity, HomeActivity | Catatan pengguna |
| Pengingat harian | ContextualActivity | Terpisah dari adzan; pembatasan baterai |
| Profil dan simpanan | ProfileActivity | Profil lokal, bookmark/bacaan/aktivitas |
| Cadangan | migration/BackupActivity | Enkripsi lokal, verifikasi, bukan cloud sync |
| Tema | BaseActivity dan pilihan tampilan HomeActivity | Tema aplikasi berbeda dari langit Home |
| Infaq kampus | InfaqActivity | Panduan/rute; bukan pemroses pembayaran |
| Akun18+ | account/AdultAccountActivity dan WIP runtime | Diberi label sedang disiapkan; Google login/cloud/wali belum tersedia |

Ikon delapan pintasan berasal dari home_shortcut_ref_*.xml, dikonversi keSVG dengan path dan transformasi grup yang sama. Tidak memakai ikon generik baru. Label tautan tetap terbaca tanpa gambar.

Foto panel Hifz berasal dari android/validation/beta-final-2026-11-14/screens/QualityHifz.png. Kalender dari android/validation/final-batch-2026-11-13/screens/final-CalendarActivity-light.png. CuplikanAI dari frame0393 promo final lokal; mengikuti pengabaianGit media. Tidak merekam ulang APK atau mengklaim screenshot adalah source58 terkini.
