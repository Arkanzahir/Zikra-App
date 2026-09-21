# 📱 PRD — ZIKRA App
## Product Requirements Document

> **Nama Produk:** Zikra (Zikra App / Zikra.ai)  
> **Tagline:** *Your Smart Islamic & Study Companion* (Teman Ibadah & Belajar Pintar)  
> **Versi:** 2.0 (Updated from MAZA Concept)  
> **Tanggal:** September 2026  
> **Owner:** Arkan (ITS Surabaya)  
> **Status:** Approved / Ready for Implementation  

---

## 1. 🎯 Product Overview

### 1.1 Visi
> **"Menyelaraskan prestasi belajar dunia dan ketenangan ibadah akhirat dalam satu genggaman — dipandu oleh asisten AI yang cerdas, santun, dan relevan."**

### 1.2 Misi & Makna Nama
* **Zikra (ذِكْرَى)** secara harfiah bermakna *Pengingat / Peringatan*. Sebagaimana Al-Quran disebut sebagai *Adz-Dzikr*, aplikasi Zikra hadir sebagai pengingat komprehensif bagi pelajar Muslim Indonesia: pengingat waktu sholat, pengingat tilawah Al-Quran, pengingat tugas & jadwal sekolah/kuliah, serta pengingat keselamatan darurat bencana.
* Menyatukan 4 kebutuhan yang selama ini terpecah di aplikasi berbeda (Muslim Pro / jadwal kuliah / todo list / BMKG) menjadi 1 ekosistem terpadu.

### 1.3 Value Proposition & Masalah yang Diselesaikan
| Pain Point Pelajar Muslim | Kondisi Saat Ini | Solusi Terintegrasi Zikra |
|---|---|---|
| Aplikasi ibadah kaku dan terpisah dari rutinitas belajar | Harus bolak-balik buka 3–4 app berbeda | 1 dashboard: Sholat, Ngaji, Jadwal Pelajaran, dan Tugas |
| Sering bablas waktu sholat saat ngerjain tugas / kuliah | Notif sholat sering di-mute / diabaikan | Smart Pomodoro yang otomatis pause saat adzan + audio adzan merdu |
| AI Islam generik & tidak tahu jadwal pengguna | Jawaban kaku layaknya mesin pencari | **Zikra AI** tahu nama, kampus/sekolah, jadwal tugas, dan waktu sholat |
| Peringatan gempa sering terlambat atau harus buka app BMKG manual | Terlambat antisipasi bahaya | Push alert otomatis BMKG realtime (≥ M5.0) dengan nada darurat |
| Niat ngaji ada, tapi tidak pernah konsisten | Sulit ukur progres | Target khatam terukur, streak harian, dan AI pendamping tafsir ayat |

---

## 2. 👥 Target Pengguna & Persona

### 2.1 Segmentasi Pengguna
1. **Siswa SMP/SMA (13–18 tahun):** Butuh pengingat jam pelajaran, jadwal PR/tugas, ulangan, bimbingan soal (AI Vision), dan kebiasaan ibadah awal.
2. **Mahasiswa (18–24 tahun) — *Primary Focus*:** Butuh manajemen SKS, jadwal dosen, deadline laporan/tugas besar, alarm kuliah H-30 menit, dan keseimbangan ibadah di perantauan/kosan.
3. **Pekerja Muda / Fresh Graduate:** Memerlukan rutinitas ibadah teratur di sela kesibukan kerja harian.

### 2.2 Persona Utama: Arkan
* **Profil:** Mahasiswa Teknik Informatika semester 5 di Surabaya.
* **Kebutuhan:** 
  * Jadwal kuliah padat dan tugas sering bentrok.
  * Ingin sholat tepat waktu dan tidak melewatkan tilawah harian.
  * Butuh teman ngobrol cerdas yang bisa dimintai tolong: *"Zik, hari ini ada tugas apa aja yang mepet?"* atau *"Zik, ayat ini maksudnya apa ya?"*.

---

## 3. 📦 Core Features (5 Pilar Utama MVP)

---

### 🕌 PILAR 1: SMART IBADAH
Fokus: Memastikan sholat tepat waktu dengan fleksibilitas tinggi bagi pelajar.

* **F1.1 — Jadwal Sholat Berbasis GPS & Kemenag RI:**
  * Sinkronisasi otomatis via GPS (dengan fallback pilihan kota manual se-Indonesia).
  * Standar perhitungan hisab: **Kemenag RI** (Metode Aladhan API: `method=11`).
  * Widget hitung mundur ke waktu sholat berikutnya.
* **F1.2 — Sistem Notifikasi & Audio Adzan Real:**
  * **Offline Local Notifications:** Jadwal sholat dihitung & dijadwalkan secara lokal di HP untuk 7 hari ke depan (tetap berbunyi meski kuota habis/tanpa sinyal).
  * **Audio Adzan Terintegrasi:** Opsi suara adzan lokal/Madinah/Makkah berdurasi penuh atau nada notifikasi pendek (bisa diatur per waktu sholat).
  * Pengaturan fleksibel: On/Off per waktu sholat, mode getar saat jam kuliah/sekolah.
* **F1.3 — Streak & Jurnal Ibadah:**
  * Tombol 1-tap "Sudah Sholat" untuk mencatat kepatuhan waktu.
  * Visualisasi streak harian & mingguan untuk memotivasi konsistensi.
* **F1.4 — Kompas Kiblat Presisi:**
  * Deteksi arah kiblat real-time menggunakan magnetometer sensor smartphone.

---

### 📖 PILAR 2: NGAJI & TADABBUR (AL-QURAN)
Fokus: Akses Al-Quran yang ringan, nyaman dibaca, didukung audio dan bimbingan AI.

* **F2.1 — Mushaf Digital 30 Juz:**
  * Teks Arab standar Kemenag / Madinah yang jernih.
  * Terjemahan resmi Bahasa Indonesia (Kemenag).
  * Transliterasi Latin (opsional toggle on/off).
  * Mode baca: per Surah atau per Halaman (Mushaf view).
  * Caching lokal: Surat yang sudah pernah dibuka bisa dibaca secara offline tanpa kuota.
* **F2.2 — Audio Murottal:**
  * Streaming dan download audio per ayat / per surah.
  * Qori pilihan (Mishary Rashid Al-Afasy, dll).
  * Kontrol audio lengkap: Play, Pause, Repeat Ayat (membantu hafalan).
* **F2.3 — Bookmark & Last Read Auto-Save:**
  * Tombol "Lanjutkan Membaca" langsung dari Home Screen.
* **F2.4 — Target Khatam & Tracker:**
  * Pilihan durasi target: 30 Hari (Ramadan/khusus), 3 Bulan, atau 1 Tahun.
  * Progress bar ayat & juz yang telah diselesaikan.
* **F2.5 — AI Tadabbur & Tanya Tafsir:**
  * Tombol di setiap ayat: *"Tanya Makna ke Zikra"*.
  * Zikra AI memberikan intisari tafsir dan konteks relevansinya dengan kehidupan nyata pelajar secara ringkas dan mudah dipahami.

---

### 📚 PILAR 3: ASISTEN PELAJAR & STUDY PLANNER
Fokus: Pengatur jadwal sekolah/kuliah dan tugas yang tidak bikin overwhelmed.

* **F3.1 — Setup Level Belajar (Onboarding):**
  * Pilihan kategori: Siswa SMP, Siswa SMA, Mahasiswa, atau Umum.
  * Menyesuaikan istilah UI (misal: "Mata Pelajaran & Ruang Kelas" vs "Mata Kuliah, SKS, Dosen, & Ruang").
* **F3.2 — Manajemen Jadwal Rutin:**
  * Input jadwal mingguan dengan mudah.
  * Tampilan ringkasan jadwal hari ini langsung di Home Screen.
* **F3.3 — Smart Class Alarm (H-30 / H-15 Menit):**
  * Notifikasi persiapan sebelum jam masuk kelas dimulai.
  * Notifikasi lokal offline: tidak bergantung koneksi server.
* **F3.4 — Task & Assignment Tracker (Deadline Manager):**
  * Manajemen tugas: Judul, Mata Pelajaran/Kuliah, Tanggal Deadline, Prioritas (Low, Med, High).
  * Sorting otomatis berdasarkan deadline paling mendesak.
  * Notifikasi H-1 dan Hari-H sebelum deadline tugas.
* **F3.5 — Morning Briefing Pintar (Jam 06.30 / 07.00):**
  * Ringkasan otomatis di pagi hari: jadwal kuliah hari ini, tugas yang mendekati deadline, prakiraan cuaca, dan waktu sholat Dzuhur.

---

### 🛡️ PILAR 4: ZIKRA PROTECTOR (PENGINGAT DOA & KESELAMATAN KONTEKSTUAL)
Fokus: Sistem perlindungan diri dan adab berdoa kontekstual yang menghubungkan data realtime (cuaca/gempa/jadwal) dengan amalan sunnah seorang Muslim.

* **F4.1 — Peringatan Dini Gempa & Tuntunan Doa (BMKG Realtime):**
  * Terkoneksi ke feed resmi BMKG Indonesia (Gempa M ≥ 5.0).
  * Ringtone khusus nada darurat agar pengguna langsung waspada dan mencari perlindungan.
  * Tampilan popup darurat: **Panduan cepat keselamatan/evakuasi** + **Tuntunan doa saat gempa & musibah** (*Inna lillahi wa inna ilaihi raji'un...*).
* **F4.2 — Smart Weather & Doa Cuaca Ekstrem (Open-Meteo):**
  * Deteksi otomatis cuaca hujan lebat, badai, atau petir di lokasi GPS pengguna.
  * Notifikasi cerdas + bacaan doa:
    * Saat hujan turun: Doa *Allahumma shayyiban nafi'an* (lengkap dengan audio & terjemahan).
    * Saat petir kencang: Doa mendengar petir & memohon perlindungan.
* **F4.3 — Safe Commute (Doa Bepergian ke Sekolah/Kampus):**
  * Pemicu otomatis dari jadwal kuliah/sekolah: Dikirimkan 15 menit sebelum waktu berangkat di pagi hari.
  * Notifikasi ramah: *"Hati-hati di jalan ya! Jangan lupa baca doa keluar rumah & naik kendaraan 🤲"* (Bismillahi tawakkaltu 'alallah...).
* **F4.4 — Dzikir Perlindungan (Al-Ma'tsurat Pagi & Petang):**
  * Pengingat ringan setelah Subuh dan menjelang Maghrib untuk membaca ayat-ayat perlindungan harian (Ayat Kursi + Al-Mu'awwidzatain) sebagai benteng diri pelajar.

---

### 🤖 PILAR 5: ZIKRA AI (THE BRAIN)
Fokus: AI bukan sekadar chatbot pasif di pojokan, melainkan **otak penggerak** yang menghubungkan seluruh fitur.

* **F5.1 — Persona & Tone of Voice:**
  * Panggilan: **Zikra** (atau sapaan akrab: *"Zik"*).
  * Karakter: Santun, suportif, cerdas, solutif, menggunakan Bahasa Indonesia santai (khas anak muda/pelajar), tidak menggurui.
* **F5.2 — Konteks Pengguna Terpadu (Context-Aware):**
  * Zikra AI dibekali memori kontekstual: mengetahui nama pengguna, jenjang pendidikan, jadwal kuliah hari ini, daftar tugas yang belum selesai, dan waktu sholat terdekat.
* **F5.3 — Shortcut & Agentic Action:**
  * Pengguna bisa memerintahkan Zikra AI lewat percakapan untuk mengelola aplikasi:
    * *"Zik, masukin PR Matematika halaman 45 deadline hari Kamis jam 8 malam ya"* ➔ langsung terinput ke database Task.
    * *"Zik, hari ini ada kelas apa aja?"* ➔ membaca data jadwal dan menyajikan ringkasan.
    * *"Zik, adzan Ashar jam berapa?"* ➔ mengambil data jadwal sholat GPS hari ini.
* **F5.4 — Vision / Scan PR (Study Helper):**
  * Foto soal ujian/PR ➔ Zikra AI menganalisis gambar dan membimbing langkah-langkah penyelesaian konsepnya (bukan sekadar kasih contekan instan).
* **F5.5 — Voice Input:**
  * Fitur mikrofon untuk input suara cepat saat pengguna sedang di jalan atau terburu-buru.

---

## 4. 💡 Fitur Unggulan Fase 2 (Post-MVP)

1. **Smart Pomodoro + Adzan Pause (Fitur Signature):**
   * Timer fokus belajar (25 menit belajar / 5 menit istirahat).
   * Otomatis menghentikan timer belajar dan memutar adzan jika waktu sholat tiba di tengah sesi fokus.
2. **Gamifikasi Ibadah & Belajar:**
   * Poin kebaikan (XP), badge pencapaian (misal: *Subuh Warrior*, *Khatam Hunter*, *Dean's List*).
3. **Pencatatan Keuangan & Hutang Anak Kos (Warisan Maza Bot):**
   * Catat pengeluaran harian dan piutang/hutang antar teman kosan via chat bot.
4. **Ramadan Mode:**
   * Imsak countdown, alarm sahur interaktif, dan target ibadah khusus bulan suci.

---

## 5. 🛠️ Tech Stack & Architecture

### 5.1 Mobile Client
* **Framework:** **React Native** dengan **Expo (Managed Workflow)**.
* **State Management:** **Zustand** (ringan, performa tinggi, minim boilerplate).
* **Navigation:** **React Navigation v6** (Bottom Tabs + Stack Navigation).
* **Local Storage & Offline Caching:** `@react-native-async-storage/async-storage` + `expo-file-system`.
* **Notifikasi & Alarm:** `expo-notifications` (menjadwalkan Local Notifications tanpa kuota internet).
* **Audio Engine:** `expo-av` (untuk pemutaran murottal dan audio file adzan).
* **Sensor & Lokasi:** `expo-location` (GPS sholat/cuaca) & `expo-sensors` (kompas kiblat).

### 5.2 Backend API & Database
* **Runtime & Framework:** **Node.js** + **Express.js**.
* **Database:** **MongoDB Atlas** (Cloud M0 Free Tier).
* **Authentication:** JWT (JSON Web Token) + Password Hashing (bcrypt).
* **Realtime & Push Engine:** Expo Server SDK (untuk push notification gempa darurat BMKG).
* **Scheduler:** `node-cron` (monitoring BMKG tiap 2 menit, daily recap generator).
* **Hosting:** **Railway** (free credits tier) atau Render.

### 5.3 AI & External Integrations (100% Free Tiers)
* **LLM Engine:** **Groq API** (`llama-3.3-70b-versatile` & `llama-3.2-11b-vision-preview` untuk vision/scan soal).
* **Jadwal Sholat:** **Aladhan API** (REST API gratis, parameter Kemenag RI).
* **Al-Quran API:** **Quran.com API v4** (Teks Arab, terjemahan Kemenag, dan audio qori).
* **Gempa Bumi:** **BMKG Open Data API** (`data.bmkg.go.id/DataMKG/TEWS/autogempa.json`).
* **Prakiraan Cuaca:** **Open-Meteo API** (akurat, gratis tanpa API key).

---

## 6. 🗄️ Skema Database (MongoDB Model Blueprint)

```javascript
// 1. User Schema
User {
  _id: ObjectId,
  name: String,
  email: String,
  passwordHash: String,
  educationLevel: 'SMP' | 'SMA' | 'KULIAH' | 'UMUM',
  institution: String,        // Contoh: "ITS Surabaya"
  location: {
    city: String,
    latitude: Number,
    longitude: Number
  },
  preferences: {
    adzanAudio: 'madinah' | 'makkah' | 'short_beep',
    adzanMute: [String],      // Contoh: ['subuh', 'isya']
    classAlarmAdvanceMin: 30, // H-30 menit
    morningBriefingTime: "06:30"
  },
  createdAt: Date
}

// 2. Schedule Schema (Jadwal Pelajaran/Kuliah)
Schedule {
  _id: ObjectId,
  userId: ObjectId,
  subjectName: String,        // "Basis Data"
  dayOfWeek: 1-7,             // 1 = Senin, dst.
  startTime: "08:00",
  endTime: "10:30",
  room: "Ruang IF-105",
  lecturer: "Dr. Bambang"
}

// 3. Task Schema (Tugas & PR)
Task {
  _id: ObjectId,
  userId: ObjectId,
  title: String,              // "Laporan Praktikum SQL"
  subjectName: String,
  deadline: Date,
  priority: 'LOW' | 'MEDIUM' | 'HIGH',
  isCompleted: Boolean,
  completedAt: Date
}

// 4. QuranProgress Schema
QuranProgress {
  userId: ObjectId,
  lastSurah: Number,
  lastAyah: Number,
  targetDays: Number,         // Target khatam (misal 30)
  totalAyahRead: Number,
  streakDays: Number,
  lastReadDate: Date
}

// 5. PrayerLog Schema (Streak Ibadah)
PrayerLog {
  userId: ObjectId,
  date: "YYYY-MM-DD",
  prayers: {
    subuh: Boolean,
    dzuhur: Boolean,
    ashar: Boolean,
    maghrib: Boolean,
    isya: Boolean
  }
}
```

---

## 7. 📱 UI/UX & Design System

### 7.1 Palet Warna (Modern Emerald & Dark Slate)
* **Primary Emerald:** `#10B981` (Segar, modern, Islami kontemporer)
* **Primary Dark:** `#064E3B` (Hijau pinus mewah)
* **Accent Gold:** `#F59E0B` (Aksen prestasi & bintang)
* **Background Dark:** `#0F172A` (Slate 900 — tidak pekat total, sangat nyaman di mata)
* **Surface Card:** `#1E293B` (Slate 800 — kontras lembut)
* **Text Primary:** `#F8FAFC` (Putih terang berkejelasan tinggi)
* **Text Secondary:** `#94A3B8` (Abu-abu netral)

### 7.2 Struktur Navigasi (5 Tab Utama)
1. **🏠 Home:** Ringkasan terintegrasi (Sholat berikutnya, Morning Briefing, Jadwal Kuliah hari ini, Tugas terdekat, Cuaca).
2. **📖 Quran:** Mushaf 30 juz, audio murottal, progress khatam, dan tombol AI Tadabbur.
3. **📚 Study:** Kalender jadwal pelajaran/kuliah, list tugas aktif dengan countdown deadline.
4. **🤖 Zikra AI:** Tab percakapan langsung dengan AI, dukungan kirim foto soal (Vision), dan voice chat.
5. **⚙️ Profil & Ibadah:** Tracker streak sholat, arah kiblat, setting notifikasi adzan & alarm kelas.

---

## 8. 🚀 Tahapan Eksekusi (Implementation Roadmap)

```
┌─────────────────────────────────────────────────────────────┐
│ FASE 1: Project Setup & Core Backend (Hari 1–3)             │
│ • Inisialisasi Expo Project & Folder Architecture           │
│ • Setup Express.js API & MongoDB Connection                 │
│ • Integrasi Groq AI Service & Parser Tool                   │
├─────────────────────────────────────────────────────────────┤
│ FASE 2: Modul Ibadah & Al-Quran (Hari 4–8)                  │
│ • Integrasi Aladhan API + Kompas Kiblat                     │
│ • Local Notification Scheduler & Engine Audio Adzan         │
│ • Mushaf Quran.com API, Player Audio & Caching              │
├─────────────────────────────────────────────────────────────┤
│ FASE 3: Modul Pelajar & Study Assistant (Hari 9–12)         │
│ • CRUD Jadwal Kuliah & Local Alarm H-30 Menit               │
│ • CRUD Task/PR & Deadline Notification                      │
│ • Morning Briefing Generator                                │
├─────────────────────────────────────────────────────────────┤
│ FASE 4: BMKG Alert & Integrasi Zikra AI (Hari 13–16)        │
│ • BMKG Poller Service + Emergency Push Trigger              │
│ • Chat Interface Zikra AI dengan System Prompt Kontekstual   │
│ • AI Function Calling (Buat tugas & jadwal lewat chat)      │
├─────────────────────────────────────────────────────────────┤
│ FASE 5: Polishing, UI Aesthetics & Testing (Hari 17–20)     │
│ • Dark Theme Styling & Micro-animations                     │
│ • Offline Mode & Error Handling                             │
│ • Build APK via Expo EAS                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. 🛡️ Non-Functional Requirements & Security
* **Offline Resilience:** App tetap berfungsi menampilkan jadwal sholat, alarm kelas, dan mushaf surat favorit tanpa koneksi internet.
* **Privasi Ketat:** Lokasi pengguna hanya disimpan di perangkat lokal (atau cache terenkripsi) untuk keperluan hisab sholat dan cuaca. Tidak ada pelacakan pihak ketiga.
* **Respon Cepat:** Waktu buka app (cold start) < 2 detik. Response time chat AI < 1.5 detik menggunakan Groq LPU inference.
