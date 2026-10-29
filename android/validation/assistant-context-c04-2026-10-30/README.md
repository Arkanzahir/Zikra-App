# C04 — Konteks kelas dan tindakan AI, beta 1.9.28

30 Oktober 2026, lanjutan sesi 29 Oktober. Pengguna mengizinkan kelanjutan C04.
**Selesai teknis beta terarah**, dengan data sintetis di emulator dan backend
Vercel/Featherless nyata. HP tidak disentuh; seluruh C/A–D/publik belum selesai.

## Perilaku dan batas data

- Consent setiap pengiriman kini memiliki pilihan jadwal 7 hari kalender mulai
  hari ini di zona lokasi pilihan. Seluruh enam pilihan awalnya mati; batal,
  retry atau percakapan baru tidak mewariskan izin pengiriman sebelumnya.
- Jadwal dibaca dari WeeklyClassStore yang sama dengan editor dan Home.
  Maksimal 10 kelas berurut tanggal/jam; jumlah yang tidak disertakan terlihat.
  Hanya judul, tanggal, jam mulai/selesai, rentang dan zona waktu dikirim.
  Guru, ruang, SKS, pengingat, ID/revisi kelas serta koordinat tidak diserialisasi.
  Kosong berarti belum ada kelas tersimpan, bukan data contoh atau akses penuh.
- Preview merupakan snapshot yang dikirim. Perubahan store setelah preview
  tidak diam-diam mengganti payload. Mengubah pilihan membentuk preview baru.
  Semua data yang dipilih diproses layanan Zikra dan penyedia AI sesuai consent.
- Backend menolak field tersembunyi, tanggal palsu/di luar rentang, zona invalid,
  jam/urutan/bentrok invalid, karakter kontrol dan batas yang dilampaui. Nama
  kelas diperlakukan sebagai data tak tepercaya, bukan instruksi. Pilihan kelas
  tidak memberi izin mengedit kelas atau menambah/menyelesaikan tugas otomatis.
- Proposal tetap hanya tambah/edit tugas Study yang diizinkan dan ditinjau.
  Tugas/revisi lama, transaksi receipt, double-tap, restart dan completed/stale
  diuji ulang. Tidak ada perubahan skema database atau key provider di APK.
- Konteks salat sekarang membawa penanda tabel acuan tersimpan atau hitungan
  lokal cadangan dari Day yang sama dengan Home/alarm. Tidak mengubah jadwal
  salat. Backend tetap menerima format APK lama tanpa field source/classes.

Dialog consent/review memakai area gulir terbatas agar judul dan tombol tetap
terjangkau. Teks privasi diringkas, tombol Kirim/Simpan tidak mengecilkan font
sistem. [Consent nyata](consent-live.jpg), [usulan kelas nyata](class-reply-live.jpg),
[sentuhan consent](consent-touch.jpg), [tinjauan tugas](task-review-touch.jpg),
[font200 consent](consent-font200.jpg), [font200 tinjauan](task-review-font200.jpg).
JPEG diperkecil; dua gambar font200 adalah crop dialog Android tanpa retouch,
mengeluarkan Toast dari kasus uji sebelumnya. Belum audit seluruh ukuran/tema.

## Tadabbur dan telaah sampel

Temuan lama pada [1:2](../assistant-hosted-emulator-2026-10-25/README.md) ditelaah:
petikan membahas Basmalah pada ayat sebelumnya sebagai konteks. Prompt kini
menegaskan nomor ayat dan membedakan konteks ayat lain dari isi yang diminta.
Backend menolak nomor/sumber tak cocok, entri ganda, teks kosong sesudah
markup dibersihkan dan body terlalu besar; kutipan dibatasi serta tidak
memotong pasangan surrogate. Gagal sumber tidak dialihkan ke tafsir rekaan.

[Dokumentasi EQuran](https://equran.id/apidev/v2) menyebut Kemenag sebagai sumber.
Ini atribusi penyedia, bukan pengesahan independen seluruh konten. Dua respons
hosting final, **1:2 dan 113:1**, ditelaah terhadap petikan: pujian/pemeliharaan
Allah serta perintah berlindung kepada Tuhan. Kekeliruan Basmalah lama tidak
muncul pada sampel 1:2 yang baru. [Balasan/hash/penilaian](hosted-and-editorial.json),
[layar1:2](tafsir-1-2-live.jpg), [layar113:1](tafsir-113-1-live.jpg).
Petikan mentah tetap privat untuk menghindari duplikasi teks panjang.
Ini pemeriksaan dua keluaran teramati, bukan jaminan setiap ayat atau keluaran
model selanjutnya benar. Ringkasan model tetap dibedakan dari sumber; penelaahan
konten menyeluruh dan gerbang keagamaan/publik tetap G03/B01.

## Pembuktian

[Ringkasan](checks.json), [69 tes backend](backend-tests.tap),
[13 eksekusi native akhir/10 metode unik](native-results.json):

- 163 unit Android lulus, 4 skip, 0 gagal; build app/helper/unit/lint berhasil,
  lint 0 error/862 warning. Unit kelas mencakup lintas minggu/tahun, zona,
  pembatasan/omitted, metadata privat, snapshot dan jadwal kosong.
- 69 backend lulus tanpa skip: kontrak/HTTP kelas, input tersembunyi, proposal,
  source provenance, abort/batas sumber dan regresi akses/kuota/Redis/Lua.
  Redis/Lua memakai fakeredis/Lupa lokal, bukan akses Redis produksi oleh suite.
- 3 native C04 pada font normal, 3 pada font200, 6 regresi assistant, 1 metode
  hosted (3 HTTP inference: kelas + 2 sumber). Input sentuhan nyata memeriksa
  checkbox, gulir, Kirim dan Simpan; tombol tidak terpotong/ellipsized.
- Alur hosted memakai APK → HTTPS Vercel → Featherless tanpa interceptor.
  Usulan kelas tidak menulis tugas atau jadwal sebelum ditinjau. Kasus native
  terkontrol memakai interceptor penyedia; tidak dihitung sebagai provider nyata.

[Riwayat percobaan](native-attempts.json) tetap menyimpan kegagalan awal fixture:
tes lama hanya menurunkan user_version tabel v5 ke v3 sehingga kolom ganda;
fixture dibangun ulang sesuai skema v3, kode migrasi produksi tidak diubah.
Tes klik programatis awal tidak membuktikan akses font besar: pemeriksaan
visual menemukan tombol sulit dijangkau, diperbaiki lalu diuji sentuhan nyata.
Runtime Python venv lama tidak dapat dijalankan; suite Redis akhir memakai
runtime bundel dengan dependensi fixture yang sudah tersedia. Bukan skip palsu.

[Deployment/manifest hash](deployment.json) dan [health200/401](hosted-health.json)
mengikat kode yang diunggah ke deployment Vercel READY; alias produksi dicek.
Allowlist memasukkan class-context.mjs dan menolak file privat/uji. Hanya kode
diupdate; env/key/akses instalasi/plan tidak diganti dan tidak membuat layanan
berbayar. Kebijakan kuota, HTTPS, sumber tetap dan no-store tetap berlaku.

## Paket dan preservasi

APK lokal `D:\Download\Zikra-App-main\Zikra-AI-v1.9.28-beta-debug.apk`, code55,
**45.617.459 byte / 45,6 MB**, di bawah100.000.000 byte.
SHA256 `439c0c2cded30d38c97a4d31ef5918d7fa8398cbf2264791640d11f6d2346ab8`.
Signer debug tetap `c7888fe2ca40e14698726ed20b2f93947033cef0bdee9cfd127c429d0ea0ca1b`.
[Audit APK](apk.json) mendekompresi seluruh entry: 0 nilai privat yang diketahui,
kedua audio cocok pilihan terakhir; APK/helper/key/MP3/backup tidak masuk Git.

[Preservasi](preservation.json): 32 berkas internal asli emulator dipulihkan
cocok hash; tiga berkas input/marker baru dihapus. Pengaturan/viewport kembali,
helper/gambar uji dibersihkan, emulator ditutup. [WIP/arsip/env/aset](protected-state.json)
terjaga; stash audio tetap. Tidak mengirim data pengguna nyata ke provider.
HP terakhir diuji agen pada1.9.25; tidak mengklaim APK1.9.28 sudah dipasang.

C02 laptop mati, C07, D02/D02a, B01, E/F/G tetap mengikuti checklist. Font Arab
reader belum diubah; UI09 dicatat untuk kelanjutan. Lampu/sky/splash/dock dan
desain/ikon yang disetujui tidak diganti. [Receipt](receipt.json) mengikat APK,
source dan bukti; commit receipt hanya dokumentasi, remote diperiksa saat push.
