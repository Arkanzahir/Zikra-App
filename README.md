> **Status terbaru — 18 Oktober 2026:** Study/Hifz sudah tersedia pada branch `recovery/study-hifz-refresh`. Branding satu master dan onboarding taman tiga cerita tanpa jenjang/minat kini **dibangun ulang dari referensi chat**, dengan parallax native. Build/lint dan tiga tes terkait lulus. Home siang/malam, posisi matahari dan fase bulan USNO/fallback juga sudah dibangun ulang serta diuji. Reader native 114 surah, pagination, bookmark dan cache terbatas juga sudah dibangun ulang; cuaca masih tahap berikutnya. Sumber asli versi lokal lama belum ditemukan. Baca [catatan pembangunan ulang](android/validation/rebuilt/README.md) dan [RECOVERY.md](RECOVERY.md).

# Zikra Smart Islamic Student Companion

> **"Harmonisasi Ibadah, Waktu Belajar, dan Kecerdasan Buatan"**  
> Aplikasi asisten cerdas all-in-one untuk pelajar & mahasiswa Muslim Indonesia dalam menyeimbangkan rutinitas ibadah harian dan produktivitas akademis.

---

## Mulai di sini untuk sesi chat baru

**Pembaruan: 17 Oktober 2026.** Repositori aplikasi utama adalah **[Arkanzahir/Zikra-App](https://github.com/Arkanzahir/Zikra-App)**, project Android Studio di **`android/`**. `Arkanzahir/prototype_zikra` adalah referensi prototipe web lama; jangan memindahkan pengembangan kembali ke React. Checkpoint kode sebelum pembaruan dokumentasi ini: **`65666c2`** (onboarding parallax).

Baca dan **buka gambar di [galeri referensi visual](./design/README.md)** terlebih dahulu; sesi baru perlu melihat file gambarnya, bukan hanya membaca label referensi.

Baca README ini, [dokumentasi Android](./android/README.md), [rencana API](./android/API_PLAN.md), [cara build](./android/BUILDING.md), lalu [validasi terakhir](./android/VALIDATION.md). PRD memuat target produk; kondisi implementasi aktual dan urutan pekerjaan di bawah menjadi acuan melanjutkan sesi.

### Aturan proyek dan cara bekerja

- **Java + layout XML 100% untuk aplikasi Android.** Komponen Canvas native boleh untuk kompas, grafik dan ilustrasi. Tidak memakai Kotlin, Compose, Flutter, React Native, atau WebView.
- Pertahankan identitas Zikra: Pine `#15332F`, Emerald `#10B981`, background `#F4F6F8`, kartu putih, CTA Amber `#CF9447` / Camel `#E5A853`. BMKG boleh biru/cyan; halaman ibadah sage/ivory. Ikon garis harus konsisten, jarak rapi, tombol mudah disentuh dan dock tidak menutup konten.
- Ikuti aset referensi pengguna di `design/`; boleh memperbaiki desain agar modern dan rapi. Home yang sudah diterima dipertahankan, ubah hanya jika diperlukan oleh fitur atau diminta pengguna.
- **Utamakan fungsi berjalan dan build lulus.** Kerjakan per tahap; jangan menghabiskan waktu pada penyempurnaan kecil. Jalankan satu putaran pemeriksaan yang relevan setelah perubahan selesai; ulang hanya jika ada kegagalan, perubahan baru atau masalah yang belum terselesaikan. Perubahan dokumentasi tidak membutuhkan build aplikasi.
- Komunikasi dalam bahasa Indonesia, langsung ke inti, maksimal 2–3 poin ringkas; tanpa pembuka/penutup panjang. Tetap beri pembaruan singkat saat pekerjaan berlangsung. `$caveman` adalah gaya komunikasi yang diminta; skill tersebut tidak tersedia pada sesi sebelumnya.
- **Tanpa login untuk tahap sekarang.** Profil, checklist, pilihan onboarding dan bookmark disimpan lokal. Login/sinkronisasi cloud baru dipertimbangkan pada tahap tersendiri.
- Infaq/Donasi **tidak diprioritaskan dan tidak ditampilkan di Home**. Delapan shortcut: Al-Quran, Doa Harian, Kiblat, Diary Hifz, Zikra AI, Protector BMKG, Study Hub, Hadis. Jadwal sholat dibuka lewat strip waktu atau tombol bel.
- Gunakan checkout yang sudah ada, periksa `git status` dan pertahankan perubahan pengguna. Pada sesi sebelumnya identitas Git lokal yang dikonfirmasi pengguna adalah **Arkanzahir `<arkanzahir86@gmail.com>`**; jangan mengubah konfigurasi global atau menulis ulang riwayat yang sudah dipush.

### Status nyata saat pindah sesi

| Bagian | Yang sudah ada | Yang masih perlu dikerjakan |
| --- | --- | --- |
| Home | Hero jam/Hijriah/countdown, siluet transparan, lentera, kartu melengkung 2×4, dock, progres 24 tick dari SQLite, lanjut bacaan | Pertahankan komposisi; hubungkan sumber Quran/jadwal online saat integrasinya siap |
| Protector BMKG | Dashboard biru, detail cuaca cyan, detail gempa, peta native, parsing BMKG background, loading/error dan bantuan Intent | Validasi data live dari jaringan yang mengizinkan; integrasi Open-Meteo, cache data dan pengingat cuaca belum selesai |
| Kiblat | Dial roset, Ka'bah, penanda derajat, acuan 294° Sukolilo, rotasi sensor, panduan kalibrasi, haptik bersyarat akurasi | Uji arah/getaran pada ponsel fisik; lokasi masih tetap Sukolilo |
| Onboarding | Tiga layar: hero orbit parallax sentuh/tilt, SMP/SMA-SMK/Mahasiswa, maksimal 3 dari 8 minat; SharedPreferences dan edit lewat Profil | **Redesign menjadi prioritas sekarang:** pengguna belum puas dengan kualitas visual orbit/parallax. Uji tilt/reduced-motion fisik masih diperlukan; rekomendasi otomatis belum ada |
| Quran | Search/katalog, 4 surah offline lengkap, teks Arab/arti, bookmark/lanjut ayat, auto-dark sensor/manual | Setelah redesign onboarding: katalog/reader, 114 surah, mode Mushaf asli, audio dan cache |
| Study Hub / Diary Hifz | CRUD checklist tugas/ibadah SQLite, progres harian, kategori hafalan dan voice-to-task | Kalender pekanan, timeline, deadline/prioritas, timer fokus dan statistik; Hifz belum evaluasi bacaan AI |
| Waktu sholat | Kalkulasi matahari harian Sukolilo, daftar hari/pekan, countdown, preferensi dan notifikasi/alarm | Integrasi Aladhan; belum audio adzan; validasi izin dan ketepatan alarm pada HP |
| Doa / Hadis / Kalender | Pencarian doa dan TTS, 7 kutipan hadis bersumber/bookmark, navigasi kalender Umm al-Qura | Rapikan desain dan tambah konten terverifikasi bertahap; bukan koleksi hadis lengkap |
| Zikra AI | Chat lokal berbasis aturan, input suara, tampilan awal lavender | Orb holografik/voice UI, backend Featherless, Gemini untuk scan soal; belum AI generatif atau vision |
| Profil | Data lokal, progres, bacaan terakhir, jenjang/minat, pengingat dan pengaturan | Rapikan susunan halaman dan kontrol preferensi sesuai fitur baru; belum akun/cloud sync |

### Urutan pekerjaan berikutnya — setelah pemulihan 18 Oktober

Study/Hifz sudah diperbaiki pada branch pemulihan; lihat `RECOVERY.md` untuk status nyata. Langkah berikutnya: pulihkan reader Quran native 114 surah sebelum Mushaf/audio; lalu branding satu master dan onboarding cerita tanpa jenjang/minat, serta Home siang/malam/parallax. Cuaca Open-Meteo/cache dan header fitur lain perlu dipulihkan terpisah. Kode baseline untuk bagian itu belum mewakili hasil yang disebut pada percakapan lama.

Ikuti arahan terbaru pengguna dan kerjakan satu tahap yang dapat dicoba sebelum memperluas pekerjaan. Jangan menganggap semua fitur sudah aman. Catatan status di atas adalah baseline publik historis, bukan daftar fitur lama yang berhasil dipulihkan.

### Persyaratan mode Mushaf

- **Mode Terjemahan:** ayat Arab dengan nomor ayat, terjemahan Indonesia yang bersumber, bookmark dan tindakan per ayat, sesuai referensi reader.
- **Mode Mushaf:** halaman Arab dengan susunan halaman/baris, penanda ayat, surah dan juz yang mengikuti **edisi mushaf yang dipilih secara konsisten**. Gunakan data halaman/layout dan font/aset yang sesuai serta berlisensi. Sekadar menyembunyikan terjemahan lalu membiarkan TextView membungkus teks tidak memenuhi kebutuhan ini.
- Sediakan perpindahan halaman dengan navigasi RTL yang benar, lompat halaman/surah/juz, pembesaran yang nyaman dan penyimpanan halaman terakhir. Perpindahan mode tetap berada pada ayat yang sama melalui pemetaan metadata halaman.
- Verifikasi sumber teks Utsmani, penomoran ayat, layout, tanda waqaf dan atribusi sebelum menyatakan mode Mushaf lengkap. Referensi Tarteel digunakan sebagai arah interaksi; pengenalan/koreksi hafalan AI belum termasuk kemampuan saat ini.
- Murottal berasal dari rekaman reciter yang sah. TTS doa yang sudah ada tidak menggantikan audio Quran.

### Peta referensi desain

**Logo utama Zikra tetap tersedia:** [desain logo dan ikon aplikasi](./design/branding/README.md). Pertahankan identitas bulan sabit, buku dan spark.

**[Buka galeri gambar lengkap di GitHub](./design/README.md).** Folder referensi sudah dibersihkan: hanya gambar yang cocok dengan acuan pengguna di chat yang dipertahankan. Mockup Zikra, logo alternatif dan prompt desain lama sudah dihapus; jangan menggunakannya kembali. Referensi nama/link tanpa file baru ditandai terpisah.

| Bagian / inspirasi pengguna | Aset yang harus dibuka |
| --- | --- |
| Onboarding Blend wrk: orbit, kartu jenjang dan chip minat; pengguna meminta redesign yang lebih modern | [Referensi pengguna](./design/references/onboarding-blend-wrk.png) |
| Home dan Quran Suhayel: hero teal, jam, strip sholat, kartu target, katalog dan reader | [Home/Quran](./design/references/home-quran-suhayel.png), [Detail Home](./design/references/home-target-suhayel.png) |
| Ibadah/Doa/Hadis/Kalender/Profil — Isha Manzoor | [Referensi pengguna](./design/references/ibadah-isha-manzoor.png) |
| Zikra AI: orb holografik, voice dan chat | [Referensi pengguna](./design/references/ai-voice-reference.png) |
| Protector BMKG: dashboard, detail cuaca dan gempa biru/cyan | [Referensi tiga layar pengguna](./design/references/bmkg-user-reference.png) |
| Kiblat — Rayhan Raju | [Referensi pengguna](./design/references/qibla-rayhan-raju.png) |
| Study/Habit Tracker — Thao Lee; timeline Structured | Pengguna memberi nama inspirasi; belum ada file gambar pengganti yang terkonfirmasi. Lihat [catatan referensi](./design/README.md#referensi-berbentuk-namalink-tanpa-file-baru) |

Benchmark tambahan pengguna: [Pillars](https://pillars.app) untuk pengalaman ibadah dan [Tarteel](https://tarteel.ai) untuk arah interaksi Quran. Acuan ini bukan permintaan menyalin seluruh fitur benchmark.

**Referensi baru yang belum berhasil dilihat:** [Pinterest pin 1110911433096315275](https://id.pinterest.com/pin/1110911433096315275/). Jangan mengarang isi pin tersebut. Akses dari sesi cloud sebelumnya mendapat proxy `403`; UI environment menampilkan Unrestricted tetapi sesi melaporkan Restricted. Publish pengaturan tertahan konflik draf **“Environment changed” / `stale_base`**. Ini masalah konfigurasi platform yang terpisah dari build aplikasi. Gunakan aset lokal yang tersedia sambil menangani akses; screenshot/video pin dapat menjadi acuan bila pengguna memberikannya.

### API dan cara memverifikasi hasil

Provider pilihan: **Quran.com/Quran Foundation**, **Aladhan**, **BMKG**, **Open-Meteo**, **Featherless AI**, **Gemini**, serta Android Intent. [API_PLAN.md](./android/API_PLAN.md) memuat endpoint, status integrasi dan prasyarat. Utamakan akses publik/gratis yang benar-benar tersedia; verifikasi model, autentikasi, kuota dan ketentuan provider saat implementasi. Jangan mengklaim integrasi live hanya karena desain sudah selesai atau fixture berhasil.

Toolchain: **JDK 17**, Gradle **8.9**, AGP **8.7.3**, SDK/target **35**, minimum **26**. Pada clone baru ikuti [BUILDING.md](./android/BUILDING.md); path `/workspace` dan SDK milik instance sebelumnya tidak dijamin tersedia.

```sh
cd android
./gradlew assembleDebug lintDebug
# Bila logika berubah, tambahkan unit/instrumentation test yang relevan,
# bukan menjalankan seluruh suite berulang untuk setiap perubahan visual.
```

APK berada di `android/app/build/outputs/apk/debug/app-debug.apk`. Bukti pemeriksaan per tahap terdapat di [VALIDATION.md](./android/VALIDATION.md). Uji perangkat terbaru dijalankan pada emulator **API 28**, walau aplikasi dibangun dengan SDK 35. Sensor fisik, haptik, TTS, Intent eksternal dan alarm perlu pengujian HP; jangan mengklaimnya sudah terbukti dari emulator. Screenshot berlabel **DATA UJI** bukan data live.

### Teks pembuka yang dapat disalin ke chat baru

```text
Lanjutkan Arkanzahir/Zikra-App, project Android di android/. Baca README root,
android/README.md, API_PLAN.md, BUILDING.md dan VALIDATION.md lebih dahulu.
Checkpoint implementasi: 65666c2; gunakan HEAD terbaru untuk dokumentasi/perubahan berikutnya.
Home, BMKG, Kiblat, dan versi awal onboarding parallax sudah dikerjakan.
PRIORITAS SEKARANG: redesign onboarding; pengguna belum puas dengan visualnya.
Buka design/README.md dan lihat langsung gambar pengguna di design/references/.
Jangan memakai mockup/prompt lama yang sudah dihapus.
Sesudah onboarding, lanjutkan Quran: katalog/reader, 114 surah, mode
Mushaf/Terjemahan, bookmark, cache dan murottal sesuai roadmap README.
Tetap Java + XML, tanpa login/Infaq dahulu. Jangan mengarang isi pin Pinterest
yang belum terbuka. Kerjakan bertahap, cepat dan rapi; cukup satu putaran uji
relevan lalu ulang hanya bila ada masalah. Jawab singkat dalam bahasa Indonesia.
```

---

## Implementasi Android Saat Ini

Source aplikasi native **Java + layout XML** tersedia di [`android/`](./android/). Buka folder tersebut melalui **Android Studio → Open**, gunakan JDK 17 dan Android SDK 35, lalu jalankan pada Android 8.0/API 26 atau lebih baru.

- [Petunjuk build dan menjalankan aplikasi](./android/BUILDING.md)
- [Halaman, fitur yang tersedia, dan batasannya](./android/README.md)
- [Hasil pengujian](./android/VALIDATION.md)
- [Screenshot aplikasi](./android/validation/screens/)

Implementasi awal dipindahkan dari `Arkanzahir/prototype_zikra` pada commit `606dfb634db7839860ade25873c22e14ebd08c87`. Home memakai header ringkas, aksen lentera, siluet masjid transparan, kartu shortcut melengkung, serta progres SQLite. Diary Hifz menggantikan shortcut Jadwal Sholat; jadwal tetap dibuka dari strip waktu atau tombol bel. Lanjut bacaan langsung membuka ayat tersimpan. Jadwal kini dihitung harian untuk Sukolilo dan pengingat memakai notifikasi/alarm Android dengan izin pengguna. Penyempurnaan halaman lain masih bertahap; Quran memuat empat surah dan AI memakai respons lokal.

Daftar fitur berikut merupakan target produk dan belum seluruhnya tersedia dalam aplikasi.

## Fitur Utama

1. **Smart Ibadah & Prayer Orbit (Fusion UI)**
   - Visualisasi dinamis orbit 5 waktu sholat harian (inspirasi *Pillars*).
   - Pengingat sholat adzan dan hitung mundur waktu berikutnya.
   - Tracker sholat mingguan & streak ibadah.

2. **Mushaf Digital & Ngaji AI**
   - Al-Quran digital 30 juz lengkap dengan audio murottal.
   - Fitur tanya tafsir kontekstual per ayat via Zikra AI.
   - Progress bar khatam Quran.

3. **Study & Academic Planner**
   - Integrasi jadwal kuliah / sekolah harian (inspirasi *Structured*).
   - Task manager dengan deadline tracker & tingkat prioritas.
   - Sinkronisasi otomatis jeda waktu sholat agar jadwal belajar tidak bertabrakan.

4. **Zikra AI Assistant**
   - Chatbot asisten kontekstual yang memahami jadwal akademik & ibadah.
   - Fitur vision: scan & tanya penyelesaian soal PR/tugas.
   - Panduan doa dan fiqih harian.

5. **Zikra Protector**
   - Notifikasi bencana & cuaca darurat terintegrasi (BMKG).
   - Rekomendasi doa kontekstual sesuai situasi.

---

## Referensi Desain

Referensi visual pengguna dan kebutuhan produk:
- 📄 [Galeri referensi pengguna](./design/README.md)
- 📋 [Product Requirement Document (PRD)](./ZIKRA_PRD.md)

---

## 🚀 Status Proyek
- [x] Product Requirement Document (PRD)
- [x] Referensi gambar pengguna dipusatkan di `design/references/`
- [ ] Redesign onboarding sesuai harapan visual pengguna
- [x] Implementasi awal Android native Java/XML
- [ ] Penyempurnaan tampilan dan kelengkapan fitur Android
- [ ] Backend & AI Integration
