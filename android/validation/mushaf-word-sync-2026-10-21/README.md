# Sorotan kata Mushaf — 21 Oktober 2026

Sorotan emas kini mengisi glyph kata dari kanan ke kiri mengikuti posisi decoder
asli. Ayat aktif tetap berlatar sage. Layout, font, basmalah, medali dan hit target
tidak digeser. Jeda membekukan posisi; seek menunggu decoder selesai sebelum
menampilkan kata baru. Gap timing, buffering, error dan Stop menghapus sorotan kata.
Perpindahan ayat mengikuti halaman saat opsi Ikuti ayat aktif; sentuhan manual
tetap menjeda mengikuti audio seperti sebelumnya.

## Sumber audio dan timing yang dipasangkan

- [Dokumentasi ayah-recitation](https://api-docs.quran.com/docs/content_apis_versioned/4.0.0/list-ayah-recitation/)
  mendefinisikan endpoint per ayat dan field segments/duration. Runtime memakai
  public v4 `api.quran.com`, sama dengan loader halaman yang sudah ada, tanpa token.
- [Daftar recitations aktual](https://api.quran.com/api/v4/resources/recitations):
  Alafasy = 7, Husary = 6, Minshawi Murattal = 9. Ini ID rekaman per ayat,
  bukan ID audio per surah. Pemetaan ditetapkan setelah memeriksa data sumber.
- URL file dan empat kolom `[index0, posisi1, mulaiMs, akhirMs]` diambil dari satu
  record `recitations/{id}/by_ayah/{surah}:{ayat}?fields=segments,duration`.
  URL relatif diselesaikan ke verses.quran.foundation; URL mirrors.quranicaudio.com
  memakai HTTPS. Host lain/redirect ditolak dan memakai jalur fallback.
- Jumlah/urutan kata diverifikasi terhadap metadata ayat, tanpa medali akhir ayat.
  Timing harus mencakup semua kata secara berurutan, tanpa overlap. File yang
  dirujuk diunduh, dihitung SHA256, lalu file itu sendiri diputar oleh MediaPlayer.
  WordTimeline dikaitkan ke SHA tersebut, surah/ayat dan durasi decoder. Selisih
  durasi metadata lebih dari 1500 ms atau segmen melebihi durasi asli mematikan
  sorotan; tidak merenggangkan atau menebak timestamp.

Audio Mushaf memakai pasangan rekaman/timing Quran.com tersebut. Reader biasa dan
unduhan lama tetap memakai provider lama. Bila sumber gagal atau timing tidak
lengkap, audio lama tetap dapat diputar dengan sorotan ayat dan keterangan bahwa
sorotan kata belum tersedia. Tidak memasangkan timing ke rekaman lama hanya
berdasarkan kesamaan nama qari.

Maksimal tiga file audio sementara, masing-masing maksimal 8 MiB, berada di cache
privat. Timing hanya di memori; tidak ada database Quran, font, rekaman, secret
atau token dibundel. Cache dibersihkan saat service dihancurkan; sisa sesi yang
terhenti dibersihkan pada inisialisasi berikutnya. Tidak mengubah unduhan pengguna.
Pergantian ayat/qari membatalkan request lama dengan pemeriksaan generasi callback.
Pengambilan audio memiliki tenggat total 45 detik. Frame sorotan membaca posisi
MediaPlayer saat UI aktif, tanpa memakai waktu animasi sebagai jam audio.

## Bukti dan batas

[Identitas rekaman aktual](actual-audio.json) memuat SHA256 dan durasi yang dibaca
decoder untuk tiga qari pada 1:1. Screenshot berasal dari APK native pada AVD
khusus Zikra_Mushaf_Test API 37, 1080×2400 / 420 dpi:

- [Alafasy](screens/Mushaf-Word-ar.alafasy.png),
  [Husary](screens/Mushaf-Word-ar.husary.png),
  [Minshawi](screens/Mushaf-Word-ar.minshawi.png): seek ke tengah kata kedua lalu
  dijeda; sebagian glyph berwarna emas sesuai posisi audio, bukan gambar mockup.
- [Mode malam](screens/Mushaf-Word-Night.png).
- [Ayat multibaris Al-Mulk 67:5](screens/Mushaf-Word-Multiline.png): seek ke kata
  terakhir, gold berada pada baris terakhir ayat; nomor ayat tetap teal.
- [Lanjut halaman 2 → 3](screens/Mushaf-Word-Next-Page.png): audio 2:5 diulang dua
  kali, kemudian 2:6 dibuka dan mendapat timing sendiri pada halaman berikutnya.

Belum audit seluruh 6236 ayat/tiga qari, pemeriksaan fonetik manual seluruh
rekaman atau pengujian HP fisik. Ketersediaan timing mengikuti sumber. Mushaf
tetap Android 12+ dan online; unduhan lama tidak otomatis mendapat timing offline.
Backend terautentikasi/Content Sync dan paket offline penuh belum diintegrasikan.

## Pengujian final

- [Build/lint/JVM](build.txt): assembleDebug, assembleDebugAndroidTest, lintDebug,
  testDebugUnitTest lulus. XML: **87 tes JVM, 0 gagal/error/skip**. Tujuh tes baru
  mencakup parsing tuple, cakupan kata, pemetaan qari, hash/durasi mismatch, metadata
  ayat salah, gap posisi, host sumber dan penolakan timing tumpang tindih/tidak lengkap.
- [Mushaf native](device-mushaf-api37.txt): **7 metode unik lulus, 42.625 detik**.
  Audio asli 1:1 ketiga qari; seek kata 2 → 4 → 1 saat dijeda dan posisi/fraction
  tetap selama 250 ms, lalu resume. 67:5 diperiksa sampai kata terakhir di baris
  berikutnya. 2:5 diulang, lalu 2:6 membuka halaman berikutnya; Stop membersihkan
  gold. Suite juga mencakup navigasi, font, zoom/malam dan perpindahan mode.
- [Regresi audio lama](device-regression-api37.txt): **3 metode unik lulus,
  22.328 detik**: rentang Hifz/pengulangan, tombol notifikasi/fokus audio/background
  layar mati, serta file offline rusak. Ketiganya memakai WAV sintetis sebagai
  fixture platform, **bukan bukti audio Quran atau pelafalan asli**.
- [Gagal awal notifikasi](device-regression-initial.txt): tes mengambil notifikasi
  sebelum tombol berubah ke Jeda. Tes kini menunggu label tombol yang sesuai
  sebelum mengirim PendingIntent; assertion pause/resume tetap diperiksa.
  [Percobaan berikutnya](device-regression-permission.txt) berhasil menjeda tetapi
  gagal menunggu notifikasi Putar. Izin POST_NOTIFICATIONS emulator ternyata
  nonaktif; pengujian final mengaktifkannya sementara dan memulihkan kondisi awal.
- Build pernah berhenti karena JVM kehabisan memori native, sebelum tes berjalan.
  Final memakai satu worker, heap 768 MiB/SerialGC dan berhasil. Tidak mengubah
  konfigurasi Gradle proyek atau menutup aplikasi pengguna.

APK: `D:/Download/Zikra-App-main/Zikra-Mushaf-WordSync-20261021-debug.apk`.
SHA256: `5DCA3A4863039747ADD4D1DFC985BA3DDB5BEDB88FF827DD89480F5807906D12`.
