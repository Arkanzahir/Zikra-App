# Pemulihan audio Murottal yang macet

Laporan pengguna pada 24 November 2026: audio berhenti terdengar pada satu
ayat dan baru kembali setelah menekan Berikutnya. Sebelumnya batas waktu
berlaku saat memuat audio. Setelah decoder siap, posisi yang terus diam dapat
membiarkan pemutar menunggu tanpa batas.

Pemutar kini memantau posisi decoder saat pengguna meminta pemutaran. Setelah
15 detik tanpa perubahan posisi, status menyebut nomor ayat yang macet dan
menawarkan Putar untuk memuat ulang ayat itu. Rentang, qori, dan pengulangan
tetap dipertahankan. Kemajuan audio, mulai memutar lagi, dan seek mengatur ulang
pengukuran. Perpindahan antrean normal tetap mengikuti callback selesai.

- 24 tes JVM paket audio lulus tanpa skip, termasuk tiga tes batas kemacetan,
  kemajuan posisi, jeda dan seek.
- Dua tes Android API 36 lulus dalam 28,217 detik pada build debug. Satu membuat
  decoder berhenti tanpa callback error. Ayat kedua tetap dipilih dan tombol
  Putar memuat ulang ayat kedua. Satu lagi menguji tombol Berikutnya dan
  tindakan mengikuti ayat pada tampilan baca.

Kedua tes memakai WAV sintetis. Perbaikan mendeteksi posisi yang terhenti.
Rekaman yang sunyi meski posisi terus maju membutuhkan pemeriksaan rekaman dan
rute audio. Surah, ayat, qori, dan sumber audio kejadian pengguna belum diuji.
Belum ada APK signed baru atau pemasangan di HP. Beta publik tetap HOLD.
