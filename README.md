# Zikra Smart Islamic Student Companion

> **"Harmonisasi Ibadah, Waktu Belajar, dan Kecerdasan Buatan"**  
> Aplikasi asisten cerdas all-in-one untuk pelajar & mahasiswa Muslim Indonesia dalam menyeimbangkan rutinitas ibadah harian dan produktivitas akademis.

---

## Mulai di sini untuk sesi chat baru

**Pembaruan: 17 Oktober 2026.** Repositori aplikasi utama adalah **[Arkanzahir/Zikra-App](https://github.com/Arkanzahir/Zikra-App)**, project Android Studio di **`android/`**. `Arkanzahir/prototype_zikra` adalah referensi prototipe web lama; jangan memindahkan pengembangan kembali ke React. Checkpoint kode sebelum pembaruan dokumentasi ini: **`65666c2`** (onboarding parallax).

Baca README ini, [dokumentasi Android](./android/README.md), [rencana API](./android/API_PLAN.md), [cara build](./android/BUILDING.md), lalu [validasi terakhir](./android/VALIDATION.md). PRD dan mockup adalah target produk; kondisi implementasi aktual dan urutan pekerjaan di bawah menjadi acuan melanjutkan sesi.

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
| Onboarding | Tiga layar: hero orbit parallax sentuh/tilt, SMP/SMA-SMK/Mahasiswa, maksimal 3 dari 8 minat; SharedPreferences dan edit lewat Profil | Uji tilt serta reduced-motion pada perangkat fisik; belum mempersonalisasi rekomendasi konten secara otomatis |
| Quran | Search/katalog, 4 surah offline lengkap, teks Arab/arti, bookmark/lanjut ayat, auto-dark sensor/manual | **Prioritas berikutnya:** desain katalog/reader, 114 surah, mode Mushaf asli, audio dan cache |
| Study Hub / Diary Hifz | CRUD checklist tugas/ibadah SQLite, progres harian, kategori hafalan dan voice-to-task | Kalender pekanan, timeline, deadline/prioritas, timer fokus dan statistik; Hifz belum evaluasi bacaan AI |
| Waktu sholat | Kalkulasi matahari harian Sukolilo, daftar hari/pekan, countdown, preferensi dan notifikasi/alarm | Integrasi Aladhan; belum audio adzan; validasi izin dan ketepatan alarm pada HP |
| Doa / Hadis / Kalender | Pencarian doa dan TTS, 7 kutipan hadis bersumber/bookmark, navigasi kalender Umm al-Qura | Rapikan desain dan tambah konten terverifikasi bertahap; bukan koleksi hadis lengkap |
| Zikra AI | Chat lokal berbasis aturan, input suara, tampilan awal lavender | Orb holografik/voice UI, backend Featherless, Gemini untuk scan soal; belum AI generatif atau vision |
| Profil | Data lokal, progres, bacaan terakhir, jenjang/minat, pengingat dan pengaturan | Rapikan susunan halaman dan kontrol preferensi sesuai fitur baru; belum akun/cloud sync |

### Urutan pekerjaan berikutnya

1. **Quran — tahap aktif berikutnya.** Buka acuan Suhayel dan reader elite terlebih dahulu. Rapikan daftar surah, search, header Arab, tipografi, ayat dan terjemahan. Integrasikan katalog 114 surah dan reader lengkap menggunakan API pilihan setelah endpoint, autentikasi, lisensi dan resource terjemahan/audio diverifikasi. Pertahankan empat surah offline sebagai fallback yang jelas. Simpan surah/ayat terakhir dan bookmark dengan nomor yang stabil; cache unduhan agar tetap bisa dibaca offline. Tangani loading, gagal jaringan dan pagination di background. Tambahkan navigasi Surah/Juz/Halaman/Hizb hanya ketika data dan tujuannya benar-benar tersedia.
2. **Mode Mushaf dan audio Quran.** Pengguna secara eksplisit meminta dua pilihan: **Mushaf / Terjemahan**. Rincian wajib ada di bawah; mode Mushaf belum diimplementasikan. Tambahkan murottal Mishary, play/pause, progres audio dan opsi auto-scroll yang sesuai bacaan setelah sumber audio siap.
3. **Study Hub dan Diary Hifz.** Ikuti kalender pekanan, streak, kartu pastel dan circular focus timer dari referensi Habit Tracker; gunakan timeline ala Structured. Hubungkan jadwal belajar, tugas dan waktu sholat tanpa bentrok. Kembangkan target hafalan dengan progres yang tersimpan; jangan menyebutnya pemeriksa hafalan AI sebelum fitur tersebut tersedia.
4. **Doa, Hadis, Kalender, Waktu Sholat dan Profil.** Terapkan bahasa visual referensi Isha Manzoor: kartu doa/kutipan, kategori dan pencarian, bel pengingat, kalender yang terbaca dan profil ringkas. Tambah konten hanya dengan sumber/atribusi. Integrasikan Aladhan memakai jadwal tanggal/lokasi/metode yang sama untuk Home, halaman jadwal dan alarm. Lokasi GPS adalah pengembangan berikutnya, bukan kemampuan yang sudah ada.
5. **Lengkapi layanan Protector dan Zikra AI.** Untuk Protector: Open-Meteo, cache bertimestamp, kondisi offline dan ambang pengingat prakiraan yang jelas. Untuk AI: desain orb holografik dan chat/voice mengikuti referensi, lalu backend Java untuk Featherless dan Gemini vision. API key tidak boleh berada di aplikasi/APK/Git. Scan soal memakai alur kamera/pilih gambar dan respons pembimbing belajar setelah backend tersedia.

Selesaikan satu tahap menjadi hasil yang dapat dicoba sebelum memperluas pekerjaan. Jangan memulai ulang Home, BMKG, Kiblat atau onboarding dari nol hanya karena berpindah chat. Masukan desain baru dari pengguna tetap dapat mengubah prioritas.

### Persyaratan mode Mushaf

- **Mode Terjemahan:** ayat Arab dengan nomor ayat, terjemahan Indonesia yang bersumber, bookmark dan tindakan per ayat, sesuai referensi reader.
- **Mode Mushaf:** halaman Arab dengan susunan halaman/baris, penanda ayat, surah dan juz yang mengikuti **edisi mushaf yang dipilih secara konsisten**. Gunakan data halaman/layout dan font/aset yang sesuai serta berlisensi. Sekadar menyembunyikan terjemahan lalu membiarkan TextView membungkus teks tidak memenuhi kebutuhan ini.
- Sediakan perpindahan halaman dengan navigasi RTL yang benar, lompat halaman/surah/juz, pembesaran yang nyaman dan penyimpanan halaman terakhir. Perpindahan mode tetap berada pada ayat yang sama melalui pemetaan metadata halaman.
- Verifikasi sumber teks Utsmani, penomoran ayat, layout, tanda waqaf dan atribusi sebelum menyatakan mode Mushaf lengkap. Referensi Tarteel digunakan sebagai arah interaksi; pengenalan/koreksi hafalan AI belum termasuk kemampuan saat ini.
- Murottal berasal dari rekaman reciter yang sah. TTS doa yang sudah ada tidak menggantikan audio Quran.

### Peta referensi desain

| Bagian / inspirasi pengguna | Aset yang harus dibuka |
| --- | --- |
| Onboarding Blend wrk: orbit, kartu jenjang, chip minat; pengguna kemudian meminta parallax lebih modern | [Blend wrk](./design/ref_onboarding_blendwrk.png), [E-learning](./design/ref4_dribbble_elearning_onboarding.png), [preview implementasi bergerak](./android/validation/screens/OnboardingParallax.gif) |
| Home dan Quran Suhayel Ahmed Nasim: hero teal, jam besar, strip sholat, kartu target, katalog surah | [Quran app](./design/ref2_dribbble_quran_app.png), [Home target](./design/ref_suhayel_home_target.png) |
| Reader Quran: header kaligrafi, Mushaf/Terjemahan, tindakan ayat, audio/auto-scroll | [Reader elite](./design/ref_quran_elite.jpg), [Mockup Quran Zikra](./design/zikra_quran_screen_1789529356744.jpg) |
| Ibadah/Doa/Hadis/Kalender/Profil — Isha Manzoor | [Islamic app](./design/ref1_pinterest_islamic_app.png), [Ibadah dan profil](./design/zikra_ibadah_profile_1789529427700.jpg) |
| Study / habit tracker — Thao Lee; timeline Structured | [Habit tracker](./design/ref5_pinterest_habit_tracker_lightmode.png), [Study Zikra](./design/zikra_study_tasks_1789529378583.jpg), [Structured](https://structured.app) |
| Zikra AI: orb holografik, voice, chat bersih | [AI voice](./design/ref3_pinterest_ai_voice_assistant.png), [Chat Zikra](./design/zikra_ai_chat_1789529366764.jpg) |
| BMKG — Naufal/Fajar | [Naufal](./design/ref6_dribbble_bmkg_naufal.png), [Fajar](./design/ref7_dribbble_bmkg_fajar.png), [Protector elite](./design/ref_protector_elite.jpg) |
| Kiblat — Rayhan Raju | [Kompas](./design/ref8_dribbble_qibla_rayhan.png) |

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
Home, BMKG, Kiblat, dan onboarding parallax sudah dikerjakan. Berikutnya Quran:
katalog/reader sesuai referensi lokal, 114 surah, dua mode Mushaf/Terjemahan,
bookmark/lanjut baca, cache dan murottal sesuai roadmap README.
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

## Desain & Mockup UI

Dokumentasi lengkap desain UI/UX dan filosofi identitas branding:
- 📄 [Dokumentasi Mockup Desain](./design/design_mockup.md)
- 📋 [Product Requirement Document (PRD)](./ZIKRA_PRD.md)

---

## 🚀 Status Proyek
- [x] Product Requirement Document (PRD)
- [x] UI/UX Mockups & Brand Identity
- [x] Implementasi awal Android native Java/XML
- [ ] Penyempurnaan tampilan dan kelengkapan fitur Android
- [ ] Backend & AI Integration
