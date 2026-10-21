# Referensi terpilih → pembaruan native 1.3.0

Pengguna memilih [dua konsep ini](../../../design/approved-2026-10-21/README.md),
meminta langit bergerak, karakter fokus/break, warna teks Quran saat audio, dan
jalur Featherless. Pengguna belum mempunyai backend dan memilih menyiapkan
jalurnya dahulu. Emulator tidak dijalankan lagi karena percobaan sebelumnya
menggunakan sekitar 5 GiB RAM. HP sedang dipakai pengguna.

## Perubahan

- Home: sapaan pagi/siang/sore/malam menurut zona lokasi, tanggal, salat berikutnya
  dan countdown detik; jadwal/metode lokasi tetap memakai sumber yang sudah ada.
  Masjid dan bukit digambar native, awan bergerak pada lapisan terpisah; matahari
  memiliki rotasi sinar dan bulan gerak/rotasi halus. Bentuk bulan tetap mengikuti
  fraksi terang sumber harian, arah/penempatan ilustratif. Data tidak diambil
  dari mockup. Provenans langit tetap dapat dibuka. Logo master dipertahankan.
- Study: strip pekan dan pemilihan tanggal tenggat, ringkasan progres nyata,
  kartu fokus berilustrasi, serta daftar tugas lebih terbaca. Default tetap
  menampilkan semua tugas tersimpan; memilih hari memfilter tenggat hari itu
  (hari ini juga memuat tugas terlambat). Tugas tanpa tenggat tetap tersedia.
  Tekan lama hari untuk kembali ke semua tugas. Ringkasan durasi menunjukkan
  pengaturan timer, **bukan statistik menit fokus contoh**. Tidak mengubah skema
  SQLite, memasukkan tugas demo, atau menandai tugas otomatis selesai.
- Fokus/break: ring menampilkan FocusClock yang sama. Ruangan tetap diam;
  ilustrasi siswa, tangan menulis, gerak kepala/napas dan uap cangkir digambar
  terpisah memakai Canvas. Timer 25/5 atau 50/10, pause/resume/reset dan aturan
  transisi eksplisit tetap dipertahankan. Pengaturan animasi/penghemat daya serta
  pause/window visibility mengendalikan callback. Batas frame adalah sasaran
  kode, bukan hasil ukur performa/baterai HP.
- Quran: hero katalog, Mushaf/unduhan dan dock diperbarui. Reader terjemahan
  mempunyai mode **Ikuti kata • online**, atau sorotan ayat/offline. Rekaman dan
  WordTimeline tetap berasal dari pasangan sumber yang sama, diperiksa hash dan
  durasi decoder. Seluruh urutan kata Arab sumber dicocokkan ke rentang teks
  sebelum ForegroundColorSpan diterapkan; jumlah kata saja tidak cukup. Bila
  ejaan/urutan/timing tidak cocok, hanya ayat berubah warna. Teks sumber tidak
  diganti, diterjemahkan ulang atau diubah susunannya demi mockup. Jeda/seek/
  buffering memakai posisi aktual pemutar. Mushaf QCF V2 beserta ornamen teal
  tetap dipertahankan. Timing offline penuh belum tersedia.
- Pendamping Zikra: hero/orb lebih ringkas, chat lavender, pilihan mode dan status
  loading/gagal/coba lagi. Offline tetap default. Klien online opt-in menuju
  gateway HTTPS; kode akses gateway disimpan AES-GCM/Android Keystore. Provider
  key tidak ada di APK. Respons online/offline diberi label sesuai asalnya dan
  kegagalan online tidak diam-diam menjadi jawaban lokal. Server Node tanpa
  dependency tambahan tersedia di [backend](../../../backend/README.md).

## Hasil aktual

APK: `Zikra-Concept-Refresh-v1.3.0-debug.apk`, versionCode 5, minSdk 26 / target 35.
SHA256: `1A4F0CC9D7DE3C31D47ABCC31906EE9E679937630A18554B93C628E710D98DA2`.
Sertifikat debug sama dengan versi sebelumnya:
`c7888fe2ca40e14698726ed20b2f93947033cef0bdee9cfd127c429d0ea0ca1b`.

- [Build akhir](build.txt): assembleDebug, assembleDebugAndroidTest,
  testDebugUnitTest, lintDebug lulus dalam **32 detik**, satu worker/heap 768 MiB,
  offline, tanpa emulator. Kompilasi tes Android bukan eksekusi tes Android.
- **96 tes JVM lulus, 0 gagal, 0 dilewati**. Snapshot/font Mushaf lokal disertakan
  melalui `ZIKRA_MUSHAF_AUDIT_DIR`; snapshot tidak dimasukkan ke APK atau commit.
  Termasuk pencocokan kata (urutan, penanda waqaf, mismatch) dan kontrak gateway.
- Lint: **0 error/fatal, 648 warning**. Bukan klaim bebas warning.
- [9 tes backend](backend-tests.txt) lulus. Provider dimock; tidak memakai key,
  akun, kuota atau koneksi Featherless asli. Menguji akses, validasi konteks,
  pembatasan request/response, error provider dan rate limit.
- Probe metadata `112:1?words=true&word_fields=text_uthmani` pada API Quran.com
  berhasil dari komputer ini. Ini bukan uji ulang playback/qari Android.
- [Ringkasan terstruktur dan hash sumber APK](result.json).

Build memakai sumber staged terpisah, **tidak menyertakan eksperimen prefetch
audio lama** yang belum dituntaskan. Perubahan lokal lama pada service/cache,
MushafReaderTest, audit timing dan catatan storage tetap dipertahankan.

## Batas dan langkah berikut

Belum ada screenshot atau rekaman native versi 1.3.0. Ilustrasi dibuat ulang
dengan Canvas, bukan bitmap mockup; jangan mengklaim identik piksel, kelancaran,
ukuran font di HP atau animasi/audio live sudah diverifikasi. Tes FocusMotion
disesuaikan untuk pose baru dan berhasil dikompilasi, **belum dijalankan**.

Pasang APK sebagai pembaruan (jangan uninstall/hapus data), lalu periksa Home
siang/malam, Study kosong/bertugas/teks besar, timer fokus→jeda→lanjut→break,
orb/chat/keyboard, sorotan reader online/offline, seek/repeat dan Mushaf.

Featherless **belum aktif live**: belum ada hosting HTTPS, model dan kredensial
server yang dikonfigurasi. Gateway saat ini ditujukan untuk pengujian pribadi
dengan kode akses terpisah; distribusi publik memerlukan otorisasi/kuota per
instalasi. Lihat batas, konfigurasi serta dokumentasi provider pada README backend.
Adzan penuh dan perbaikan provider masjid bukan bagian perubahan tahap ini.
