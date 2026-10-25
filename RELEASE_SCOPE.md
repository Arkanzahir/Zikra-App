# A02 — Cakupan penyelesaian Zikra

Keputusan: **24 Oktober 2026**. Pengguna mendelegasikan penetapan cakupan: “yang penting selesai saja dulu silahkan kamu tentukan dlu saja”. Rincian di bawah adalah keputusan pengerjaan berdasarkan delegasi tersebut, bukan klaim pengguna telah memilih setiap detail secara terpisah. Acuan pekerjaan: [implementation plan](IMPLEMENTATION_PLAN.md), [audit A01](android/validation/release-audit/FEATURE_MATRIX.md), [PRD](ZIKRA_PRD.md).

Tujuan pertama adalah **rilis publik pertama Zikra yang lengkap dalam cakupan tertulis ini**, Android Java/XML, tanpa menambah desain/fitur baru di luar daftar selama penyelesaian. Beta adalah tahap pembuktian menuju tujuan itu, bukan pengganti rilis publik. Versi pengembangan yang sekarang 1.6.2 tidak diubah oleh keputusan dokumen ini; nomor versi distribusi final ditentukan pada G04 agar pembaruan Android tetap naik.

## Wajib untuk rilis publik pertama

| Modul | Hasil yang harus dapat digunakan | Pekerjaan / PRD |
| --- | --- | --- |
| Home, onboarding, lokasi, Profil | Onboarding jelas; jenjang pelajar/mahasiswa/umum dan nama lokal; lokasi/zona konsisten; jadwal, bacaan terakhir dan progres memakai data asli; pengaturan tersimpan | A03–A04, D01/D04/D06, F01–F03; F1.1/F3.1 |
| Salat dan Kiblat | Metode jadwal dinyatakan dan divalidasi; kota/GPS/manual; countdown; pengingat lokal luring terjadwal 7 hari dengan penjadwalan ulang; sakelar utama dan pilihan **adzan penuh / notifikasi biasa tanpa adzan / getar / mati** per salat, stop suara dan audio focus; mode senyap sesuai jam kelas; Kiblat dengan status sensor/kalibrasi. Diperjelas pengguna 25 Oktober, tugas D02a | D01–D02/D02a, G02; F1.1–F1.4 |
| Quran | 114 surah/30 juz, teks Arab/Indonesia dan transliterasi opsional bersumber; per ayat serta Mushaf pada perangkat yang mendukung; bookmark/bacaan terakhir; audio, seek/repeat/rentang/background; sorotan dan mengikuti audio saat timing cocok; gulir baca mandiri; paket unduhan/luring yang jelas | A03–A04, D03/D06; F2.1–F2.3 |
| Ibadah dan Hifz | Checklist/jurnal, streak harian/mingguan berdasar catatan; target Hifz/rentang dan latihan; target khatam 30/90/365 hari dengan progres yang dicatat pengguna | D05–D06; F1.3/F2.4 |
| Hadis | Dua koleksi **Bukhari dan Muslim** dari edisi/sumber yang tervalidasi, Arab/Indonesia, nomor/rujukan, cari nomor/kata, reader, bookmark/resume dan unduhan luring per koleksi. Label lengkap hanya sesudah cakupan sumber dibuktikan; tujuh kutipan tidak cukup | B01–B03 |
| Doa dan dzikir | Kategori belajar, makan, tidur, rumah, perjalanan, hujan/petir, musibah dan pagi/petang; Arab/Latin/arti/rujukan yang diperiksa, cari dan simpan luring. Jumlah final mengikuti entri yang lolos validasi, tidak dipaksakan sama dengan angka dokumentasi API. TTS dilabeli TTS, bukan rekaman qari | B04, E03 |
| Study | CRUD tugas, mata pelajaran/kuliah, deadline/prioritas/urutan; jadwal kelas mingguan; alarm kelas H-30/H-15 dan tenggat H-1/Hari-H; fokus/break/pause serta animasi; pulih background; pilihan jeda fokus saat adzan aktif tanpa menandai tugas selesai otomatis | D02/D04/D06, F04; F3.1–F3.4 |
| AI teks | Offline berbasis aturan dilabeli jelas; online Featherless melalui hosting HTTPS permanen, kuota/akses per instalasi tanpa meletakkan provider key di APK; error/batal/retry jelas; konteks nama/tugas/jadwal/salat hanya dengan izin; usulan tambah/edit tugas perlu tinjauan dan disimpan sekali; input suara bila recognizer tersedia | C01–C06; F5.1–F5.3/F5.5 |
| AI foto dan dokumen (tambahan 25 Oktober) | + pada composer untuk Kamera/Galeri/Dokumen; preview/hapus sebelum Kirim, tidak mengunggah otomatis. Foto melalui model vision yang aksesnya diuji; PDF/DOCX/TXT sesuai parser dan batas ukuran/halaman nyata, izin minimal dan gagal/batal jelas. Dukungan dokumen scan diuji dan batas pembacaan dijelaskan. Ini menggantikan penundaan Vision lama; belum implementasi atau selesai | C07, UI04, G03 |
| Tadabbur | Alur tanya makna dari ayat dengan sumber tafsir terverifikasi dan rujukan; batas jawaban jelas ketika sumber tidak tersedia. Jangan membuat kutipan keagamaan dari perkiraan model | C04, G03; F2.5 |
| Protector | Prakiraan kota dan animasi sesuai kondisi; BMKG episentrum/Shakemap/angka/waktu dari satu kejadian; waktu pembaruan/stale/error; push kejadian baru ketika aplikasi ditutup, deduplikasi dan detail ID yang tepat; panduan serta kontak/masjid dengan tindakan pengguna | E01–E04, C02/G02; F4.1–F4.2 |
| Pengingat kontekstual | Briefing pagi pilihan pengguna dari jadwal/tugas/cuaca/salat; doa perjalanan dari jadwal berangkat; dzikir pagi/petang; cuaca ekstrem memakai sumber/ambang yang dinyatakan. Semua dapat dimatikan, deduplikasi dan tidak menjanjikan prediksi gempa | D04/E03/B04; F3.5/F4.2–F4.4 |
| Desain dan kualitas | Pertahankan referensi visual yang disetujui; tata letak/font konsisten, Arab terbaca, teks besar/keyboard/izin/error terjangkau; animasi berhenti di background dan mengikuti pengaturan sistem; pengukuran HP untuk RAM/baterai | F01–F04, G01–G02 |
| Paket distribusi | Pembaruan mempertahankan data; signed release/hash/source cocok, atribusi/privasi/dukungan; **unduhan awal <100.000.000 byte** setiap versi. Audio/paket besar opsional, tampilkan ukuran dan hapus unduhan/cache | D06, G03–G05 |

Pencarian Hadis harus membedakan seluruh koleksi dari paket di perangkat. Semua data pengguna lokal tetap dapat dipakai tanpa akun. Akses AI/push dapat memiliki identitas instalasi teknis yang bisa dicabut; ini bukan akun pengguna/sinkronisasi profil.

## Beta tertutup sebelum publik

Beta boleh dibagikan untuk menguji hasil per modul setelah syarat dasarnya terpenuhi:

- Jalur instal baru/onboarding/lokasi/Home, Quran baca/audio, Study/tugas, Hadis/Doa dan Profil stabil; tidak merusak data pada pembaruan. A03–A04 dan kasus relevan B/D/F/G harus mempunyai bukti.
- Data contoh diberi label; sumber konten harus lolos B01/B04 sebelum distribusi paket, termasuk dalam beta. Jangan membagikan provider key, token gateway bersama, atau konfigurasi pengembang dalam APK.
- Modul AI/push yang belum memiliki hosting diuji privat dan dinyatakan belum tersedia; tombol publik tidak berpura-pura berfungsi. Rilis publik tetap menunggu hasil wajib di tabel, tanpa menghapus fiturnya dari cakupan.
- Kondisi gagal/izin ditolak/luring tidak menyebabkan crash atau statistik palsu. Keterbatasan qari/timing/Mushaf/recognizer/sensor disebut sesuai hasil pemeriksaan.
- Setiap APK beta memiliki versi, hash, source checkpoint, daftar skenario lulus dan kekurangan. Emulator membantu pemeriksaan; G02 dan publik tetap memerlukan HP fisik.

## Ditunda ke pembaruan setelah rilis pertama

| Fitur | Keputusan dan alasan | Jejak pekerjaan |
| --- | --- | --- |
| Akun/login, cloud backup dan sinkronisasi antar-HP | Ditunda agar model lokal stabil lebih dahulu. Tidak otomatis mengirim profil, jadwal, tugas dan bookmark ke server. Wajib menjaga data lokal/migrasi untuk rilis pertama; belum menjanjikan pemulihan ketika HP hilang | D06 bagian lokal tetap wajib; perlu rencana akun/identitas/konflik/hapus akun tersendiri saat dimulai |
| Infaq/pembayaran | Di luar navigasi rilis pertama; jangan menyambungkan pembayaran atau membuat klaim donasi. Source panduan lama dipertahankan sampai keputusan pengembangan berikutnya; jangan hapus data/draft sebagai efek keputusan ini | Temuan Infaq A01; bukan kebutuhan inti PRD |
| XP/badge, keuangan anak kos, mode Ramadan khusus | Tetap post-MVP sesuai PRD. Target khatam umum dan Pomodoro tetap wajib; tidak ditunda bersama fitur ini | Backlog PRD fase 2 |
| Koleksi Hadis tambahan dan variasi ilustrasi provinsi | Dua koleksi dan keluarga aset yang disetujui lebih dahulu; tambah setelah sumber dan kinerja dasar andal | B01–B03/F04 untuk baseline; ekspansi sesudah rilis |

Ini adalah penundaan berdasarkan kewenangan yang diberikan pengguna pada 24 Oktober, bukan checkbox selesai. Kebutuhan tetap dapat dilacak di PRD/plan dan dibuka lagi pada fase yang disebut.

## Bersyarat terhadap kemampuan perangkat/layanan

- Mushaf QCF halaman dipakai pada Android yang didukung (saat ini 12+); perangkat lain tetap mendapat reader per ayat, tanpa tombol mati yang membingungkan. Luring menyebut paket yang benar-benar tersimpan.
- Sorotan kata hanya untuk rekaman dan timing cocok; jika tidak, gunakan sorotan ayat yang benar dan status yang jujur. D03 wajib menguji sumber yang ditawarkan sebagai didukung.
- Dikte suara dan sensor Kiblat memiliki keadaan tidak tersedia/izin ditolak serta fallback mengetik/petunjuk arah; tidak ada janji mendukung sensor yang tidak dimiliki HP.
- Notifikasi bergantung izin dan batas sistem; jadwal/layanan dipulihkan sesuai kemampuan Android. Push gempa tetap wajib, tetapi bukan jaminan datang sebelum guncangan atau deteksi dari sensor HP.
- Konten/peta/cuaca/provider memerlukan pemeriksaan sumber, penggunaan dan kegagalan. Bila suatu kandidat tidak memenuhi syarat, ganti sumber atau eskalasi keputusan cakupan; jangan menandai fitur wajib selesai karena API pernah merespons.

## Urutan kerja dan definisi selesai

1. **A03 → A04 terlebih dahulu**, sesuai koreksi pengguna 24 Oktober setelah keputusan cakupan: rapikan WIP audio dan buktikan integrasi data. WIP dipertahankan terpisah bila belum lolos; keamanan diperhatikan selama pengerjaan. Penelitian B01 yang sudah ada tetap disimpan.
2. **B01**, lanjut validasi/pilih sumber Hadis setelah A04; tulis keputusan serta bukti. Bila terhambat sumber, laporkan bagian yang belum terbukti dan kerjakan persiapan yang tidak bergantung izin; jangan mengimpor distribusi secara diam-diam.
3. **B02 → B03 → B04**, lengkapkan konten dan penyimpanan dengan uji terarah. Setiap item ditutup sendiri; jangan menggabungkan seluruh tahap menjadi satu klaim selesai.
4. **C → D → E** mengikuti dependensi plan. C02 menyiapkan hosting saat konfigurasi konkret siap; keputusan yang membutuhkan akun hosting/biaya dibawa kepada pengguna pada titik itu.
5. **F → G**, penyelarasan menyeluruh dan uji beta/HP sebelum signed release publik. Kerapian dasar/error penting diperbaiki dalam modul yang sedang dikerjakan.

“Selesai proyek rilis pertama” berarti seluruh syarat wajib di dokumen ini dan G05 terpenuhi. Jumlah centang, screenshot konsep, respons API, build berhasil atau total ukuran APK saja tidak memenuhi definisi itu. Akun dan backlog yang masih ditunda tetap dicatat sebagai rencana setelah rilis; bukan diam-diam dianggap sudah terimplementasi.

**A02 selesai sebagai keputusan cakupan.** Belum ada fitur runtime baru atau perubahan versi APK dalam dokumen ini.

## Perubahan keputusan 25 Oktober — Home dan lampiran AI

Pengguna menyetujui Home Al-Aqsa siang/malam dan meminta foto/file ditambahkan, sehingga penundaan Vision pada keputusan 24 Oktober tidak lagi berlaku untuk kebutuhan lampiran ini. AI otomatis memakai ketersediaan internet dan hasil layanan, dengan status jelas dan opsi panduan offline terbatas. Persetujuan Home bukan bukti runtime; C07, C03 dan UI04 tetap terbuka. [Keputusan terbaru](design/concepts/home-approved-ai-auto-attachments-2026-10-25.md).

Permintaan lanjutan menambahkan riwayat beberapa chat lokal (C08) serta kreasi gambar online (C09). Keduanya tetap antrean dengan kriteria uji di implementation plan; rendering gambar memerlukan layanan/model/akses/biaya yang dibuktikan sebelum tersedia. [Konsep dan batas](design/concepts/ai-history-creation-review-2026-10-25-v4.md). Koreksi Home 1.9.1 sudah mempunyai [bukti native](android/validation/home-reference-match-2026-10-25/README.md), tetapi bukan bukti implementasi fitur AI tersebut atau penutupan UI04/F/G.

**Status terbaru 1.9.5/sourcea5d070a:** C08 riwayat lokal sudah selesai dengan bukti
migrasi/native/consent; menggantikan status antrean C08 di atas. AI otomatis dan
lampiran foto/PDF/DOCX/TXT sudah diterapkan serta diuji ke provider lewat Vercel,
tetapi C03/C07 tetap terbuka untuk gerbang yang tersisa. C09 generasi gambar belum
tersedia, Gemini API baru kandidat tanpa aktivasi biaya. [Bukti dan batas](android/validation/ai-home-refresh-2026-10-25/README.md).
Ini checkpoint beta, tidak mengubah cakupan wajib rilis publik atau menutup C/F/G.
