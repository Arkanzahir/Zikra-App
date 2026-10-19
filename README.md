> **Pembaruan terbaru — 19 Oktober 2026 (WIB):** onboarding diperjelas dengan pengantar, judul lebih kuat dan paragraf manfaat pada tiga ilustrasi statis; komposisi teks lebih seimbang. Build/lint dan lima skenario onboarding unik lulus, termasuk layout scoped font 200%. [Screenshot dan batas pemeriksaan](android/validation/onboarding-copy-2026-10-19/README.md) · [Rencana tahap berikutnya](design/IMPROVEMENT_PLAN.md). Ronde sebelumnya: [22 screenshot, 33 tes JVM dan 47 metode emulator unik](android/validation/quality-2026-10-19/README.md). [Android Studio](android/BUILDING.md) · [Status kemampuan](RECOVERY.md).

# Zikra Smart Islamic Student Companion

> **"Harmonisasi Ibadah, Waktu Belajar, dan Kecerdasan Buatan"**  
> Aplikasi asisten cerdas all-in-one untuk pelajar & mahasiswa Muslim Indonesia dalam menyeimbangkan rutinitas ibadah harian dan produktivitas akademis.

---

## Mulai di sini untuk sesi chat baru

**Pembaruan: 19 Oktober 2026 (WIB).** Repositori aplikasi utama adalah **[Arkanzahir/Zikra-App](https://github.com/Arkanzahir/Zikra-App)**, project Android Studio di **`android/`**. `Arkanzahir/prototype_zikra` adalah referensi prototipe web lama; jangan memindahkan pengembangan kembali ke React. Gunakan HEAD terbaru branch **`recovery/study-hifz-refresh`**; sumber asli lokal lama belum ditemukan, dan fitur yang hilang sudah dibangun ulang dari referensi chat.

Baca dan **buka gambar di [galeri referensi visual](./design/README.md)** terlebih dahulu; sesi baru perlu melihat file gambarnya, bukan hanya membaca label referensi.

Baca README ini, [dokumentasi Android](./android/README.md), [rencana API](./android/API_PLAN.md), [cara build](./android/BUILDING.md), lalu [validasi terakhir](./android/VALIDATION.md). PRD memuat target produk; kondisi implementasi aktual dan urutan pekerjaan di bawah menjadi acuan melanjutkan sesi.

### Aturan proyek dan cara bekerja

- **Java + layout XML 100% untuk aplikasi Android.** Komponen Canvas native boleh untuk kompas, grafik dan ilustrasi. Tidak memakai Kotlin, Compose, Flutter, React Native, atau WebView.
- Pertahankan identitas Zikra: Pine `#15332F`, Emerald `#10B981`, background `#F4F6F8`, kartu putih, CTA Amber `#CF9447` / Camel `#E5A853`. Cuaca/gempa memakai Pine/sage yang sekeluarga dengan halaman lainnya. Ikon garis harus konsisten, jarak rapi, tombol mudah disentuh dan dock tidak menutup konten.
- Ikuti aset referensi pengguna di `design/`; boleh memperbaiki desain agar modern dan rapi. Home yang sudah diterima dipertahankan, ubah hanya jika diperlukan oleh fitur atau diminta pengguna.
- **Utamakan fungsi berjalan dan build lulus.** Kerjakan per tahap; jangan menghabiskan waktu pada penyempurnaan kecil. Jalankan satu putaran pemeriksaan yang relevan setelah perubahan selesai; ulang hanya jika ada kegagalan, perubahan baru atau masalah yang belum terselesaikan. Perubahan dokumentasi tidak membutuhkan build aplikasi.
- Komunikasi dalam bahasa Indonesia, langsung ke inti, maksimal 2–3 poin ringkas; tanpa pembuka/penutup panjang. Tetap beri pembaruan singkat saat pekerjaan berlangsung. `$caveman` adalah gaya komunikasi yang diminta; skill tersebut tidak tersedia pada sesi sebelumnya.
- **Tanpa login untuk tahap sekarang.** Profil, checklist, pilihan onboarding dan bookmark disimpan lokal. Login/sinkronisasi cloud baru dipertimbangkan pada tahap tersendiri.
- Infaq/Donasi **tidak diprioritaskan dan tidak ditampilkan di Home**. Delapan shortcut: Al-Quran, Doa Harian, Kiblat, Diary Hifz, Zikra AI, Protector BMKG, Study Hub, Hadis. Jadwal sholat dibuka lewat strip waktu atau tombol bel.
- Gunakan checkout yang sudah ada, periksa `git status` dan pertahankan perubahan pengguna. Pada sesi sebelumnya identitas Git lokal yang dikonfirmasi pengguna adalah **Arkanzahir `<arkanzahir86@gmail.com>`**; jangan mengubah konfigurasi global atau menulis ulang riwayat yang sudah dipush.

### Status nyata saat pindah sesi

| Bagian | Yang sudah ada | Yang masih perlu dikerjakan |
| --- | --- | --- |
| Home | Jam/Hijriah/jadwal Sukolilo, branding master kiri atas, langit siang/malam, USNO/fallback berlabel, parallax dan animasi masuk, delapan shortcut, progres SQLite/dock | Uji kelancaran/baterai di HP; pilihan lokasi masih backlog |
| Protector BMKG | Open-Meteo untuk prakiraan/cache 10 menit–6 jam, BMKG untuk gempa/peringatan, loading/error/sumber, header Pine bersama Study/Hifz | Lokasi masih Sukolilo; belum notifikasi cuaca otomatis |
| Kiblat | Dial roset, Ka'bah, penanda derajat, acuan 294° Sukolilo, rotasi sensor, panduan kalibrasi, haptik bersyarat akurasi | Uji arah/getaran pada ponsel fisik; lokasi masih tetap Sukolilo |
| Onboarding | Tiga adegan ilustrasi 2D statis 3:2 memenuhi lebar bagian atas, pengantar kecil/judul 32sp/paragraf manfaat, blok teks seimbang, kembali kiri dan langkah kanan; wordmark `zikra` tanpa titik dan simbol master kiri atas | Cocokkan visual pada HP; tanpa animasi/parallax/jenjang/minat; preferensi lama tetap disimpan |
| Quran | Katalog/reader native 114 surah; pencarian Arab tanpa harakat, ukuran Arab/terjemahan tersimpan, lompat ayat tervalidasi, terang/malam/otomatis; Utsmani/arti resource 33, bagian 50 ayat, bookmark/resume, cache delapan bagian dan empat surah bawaan | Mushaf halaman, audio dan offline penuh belum tersedia |
| Study Hub / Diary Hifz | Study akademik dan target Hifz tetap dapat diakses lintas tanggal; ibadah khusus hari ini melalui Waktu Sholat. Hari baru kosong; tidak membuat tugas/progres contoh. CRUD/SQLite v2 mempertahankan data lama, target 114 surah/rentang, draft editor/dikte dan reader native | Belum timer fokus/deadline/statistik atau evaluasi bacaan AI; checklist ibadah bukan pencatatan otomatis |
| Waktu sholat | Kalkulasi matahari harian Sukolilo, daftar hari/pekan, countdown, preferensi dan notifikasi/alarm | Integrasi Aladhan; belum audio adzan; validasi izin dan ketepatan alarm pada HP |
| Doa / Hadis / Kalender | Pencarian doa dan TTS, 7 kutipan hadis bersumber/bookmark, navigasi kalender Umm al-Qura | Tambah konten terverifikasi bertahap dan uji suara pada perangkat; bukan koleksi hadis lengkap |
| Zikra AI | Panduan rutinitas offline berlabel jelas, percakapan lokal dan input suara perangkat | Belum AI generatif, pemecah soal khusus, vision atau penilaian bacaan; Featherless/Gemini belum dihubungkan |
| Profil | Avatar/profil lokal, progres, bacaan terakhir, Tentang memakai logo master, membuka ulang cerita Zikra | Tanpa akun/cloud sync; uji izin/pengingat pada HP |

### Ronde kegunaan — 19 Oktober 2026

Riset memakai acuan pengguna, [panduan aksesibilitas Android](https://developer.android.com/guide/topics/ui/accessibility/apps), [ukuran teks](https://www.w3.org/WAI/WCAG22/Understanding/resize-text.html) dan kontras. Prioritas implementasi: tindakan yang jelas, target sentuh, teks yang bisa membesar, status kosong/loading/error yang jujur, serta data rencana yang tetap berguna besok. Logo master `design/branding/zikra-logo-primary.jpg` tidak digambar ulang; simbol bulan, buku dan spark tetap sama. Titik hanya dihapus dari teks wordmark.

Lanjutkan pemeriksaan terarah perubahan ronde ini, kemudian uji pada HP fisik dan cocokkan hasil visual dengan pengguna. Mushaf halaman, murottal, pilihan lokasi dan AI generatif tetap backlog tersendiri. Lokasi acuan jadwal/cuaca/Kiblat masih **Sukolilo, Surabaya**; aplikasi belum menyesuaikan kota pengguna. Jangan menyebut semua fitur optimal atau kode lama telah ditemukan.

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
Lanjutkan Arkanzahir/Zikra-App, native Java/XML di android/.
Gunakan checkout/branch recovery/study-hifz-refresh yang sudah ada; jangan reset.
Baca RECOVERY.md, README, android/validation/rebuilt/README.md dan BUILDING.md.
Onboarding 2D statis tanpa titik pada wordmark, Home astronomi/parallax,
Quran native114 dengan ukuran teks/lompat ayat, Open-Meteo/cache,
serta rencana akademik/Hifz lintas tanggal tersedia pada sumber terbaru.
Hari baru mulai kosong; jangan membuat ulang progres contoh.
Kode asli lokal lama belum ditemukan; jangan menyebut implementasi baru identik.
Prioritas: uji HP fisik dan cocokkan visual dengan pengguna. Mushaf/audio,
unduhan penuh, pilihan lokasi dan backend AI belum tersedia.
Ikuti galeri referensi design/ dan master crescent+book+spark; tanpa login/Infaq.
Simpan checkpoint kode ke GitHub; main jangan digabung otomatis.
Satu putaran uji relevan, ulang hanya karena perubahan/kegagalan nyata.
Jawab ringkas dalam bahasa Indonesia.
```

---

## Implementasi Android Saat Ini

Source aplikasi native **Java + layout XML** tersedia di [`android/`](./android/). Buka folder tersebut melalui **Android Studio → Open**, gunakan JDK 17 dan Android SDK 35, lalu jalankan pada Android 8.0/API 26 atau lebih baru.

- [Petunjuk build dan menjalankan aplikasi](./android/BUILDING.md)
- [Halaman, fitur yang tersedia, dan batasannya](./android/README.md)
- [Hasil pengujian](./android/VALIDATION.md)
- [Screenshot aplikasi](./android/validation/screens/)

**Catatan historis implementasi awal:** source dipindahkan dari `Arkanzahir/prototype_zikra` pada commit `606dfb634db7839860ade25873c22e14ebd08c87`. Versi itu memakai lentera, contoh checklist dan empat surah lokal. Status implementasi sekarang mengikuti tabel di atas; Quran kini mempunyai jalur native 114 surah online dengan empat surah bawaan, sedangkan AI tetap panduan lokal berbasis aturan.

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
- [x] Onboarding ilustrasi 2D statis mengikuti arahan terbaru; evaluasi HP/pengguna masih diperlukan
- [x] Implementasi awal Android native Java/XML
- [ ] Penyempurnaan tampilan dan kelengkapan fitur Android
- [ ] Backend & AI Integration
