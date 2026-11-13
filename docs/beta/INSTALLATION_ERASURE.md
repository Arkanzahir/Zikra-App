# Penghapusan metadata instalasi — operator Zikra

14 November 2026. Implementasi lokal teruji; bukan penghapusan data pengguna
produksi dan bukan janji penghapusan seluruh log/cadangan penyedia. Tidak ada
endpoint penghapusan publik baru. Tidak perlu membagikan token operator ke APK.

## Ruang lingkup

`backend/installation-erasure-admin.mjs` menyediakan tiga langkah untuk satu
UUIDv4 instalasi yang sudah diverifikasi operator. Lingkungan privat harus memuat
konfigurasi Upstash yang sah. ID, hash, token dan isi Redis tidak dicetak.
Tidak membaca isi percakapan pengguna. Semua perubahan memakai satu Lua atomik.

1. `inspect ID`: baca status dan jumlah key lingkup instalasi, tanpa mengubah data.
2. `prepare ID --confirm-installation ID`: cabut akses dan catat waktu permintaan.
   Ulangi inspeksi setelah minimal120detik; persiapan ulang tidak mereset waktu.
3. `erase ID --confirm-installation ID`: hanya setelah masa tunggu dan tidak ada
   lease aktif instalasi. Index instalasi↔hash harus cocok. Jika belum siap,
   exit2; operator tidak memaksa dengan menghapus key manual.

Jalankan memakai Node dari direktori backend. `ID` adalah nilai persis dari
registry privat, bukan email, nama, UUID buatan baru atau ID yang dikirim tanpa
verifikasi. Argumen konfirmasi harus sama dengan target. `inspect` tidak menerima
argumen mutasi. Kesalahan/config/network dicatat tanpa nilai sensitif.

Yang dihapus: pasangan registry/index, counter AI, lease instalasi yang sudah
tidak aktif, counter gambar pengguna, deduplikasi request gambar instalasi dan
rate Quran instalasi. Maksimal1000key request gambar; scan berbatas dan validasi
selesai sebelum penghapusan. Bila batas terlampaui atau registry rusak, berhenti.

Yang dipertahankan: counter/kapasitas bersama, snapshot Quran bersama, metadata
gempa/push bersama, instalasi lain, cadangan privat, log platform, data chat/berkas
di HP dan data penyedia. Global lease yang sudah kedaluwarsa mengikuti mekanisme
expiry/pembersihan normal; tidak mengosongkan kapasitas global secara massal.

## Verifikasi permintaan dan bukti

- Identifikasi pemohon/instalasi melalui kanal privat yang sudah digunakan;
  jangan minta API key, token akses penuh, PIN, dokumen identitas atau ekspor chat
  melalui email. Jika kepemilikan tidak dapat dibuktikan, jangan menghapus.
- Catat nomor kasus, lingkup yang diminta, keputusan, waktu persiapan dan hasil
  agregat secara privat. Tidak menjanjikan SLA atau durasi retensi baru tanpa
  keputusan penerbit. Prosedur verifikasi manusia masih perlu diterapkan pada
  kasus beta nyata; tujuh tes lokal bukan bukti verifikasi identitas produksi.
- Setelah erase berhasil, inspect harus `missing`; token lama tidak berizin.
  Request yang sudah berjalan tidak dapat ditarik kembali dari penyedia.
  Masa tunggu melampaui lease normal, tetapi bukan jaminan pembatalan seluruh
  pekerjaan platform. Periksa kejadian terlambat bila sedang menangani insiden.
- Pisahkan penanganan log/cadangan penyedia dan salinan dukungan. Backup registry
  lama tidak boleh mengaktifkan ulang akses: alat restore yang ada memulihkan
  seluruh akses dalam keadaan revoked. Jangan restore database produksi massal
  sebagai cara memenuhi satu permintaan penghapusan.

## Bukti terarah

`backend/installation-erasure.test.mjs`: tujuh tes lulus dengan Lua nyata pada
fakeredis/Lupa lokal, tanpa credential/jaringan produksi. Mencakup validasi,
inspeksi read-only, revokasi/tunggu, scope instalasi, lease, registry rusak,
kapasitas berbatas dan penolakan CLI tanpa konfirmasi. Suite backend keseluruhan:
134lulus/0skip. Bukan bukti run administratif terhadap Upstash produksi.
