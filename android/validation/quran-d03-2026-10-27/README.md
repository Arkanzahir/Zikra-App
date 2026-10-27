# D03 — Quran, Mushaf dan audio, 27 Oktober 2026

**Selesai teknis beta1.9.13/code40.** Audit identitas/pemetaan 114 surah dan
6.236 ayat, pemutar native, mengikuti audio/gulir mandiri, sampel rekaman asli,
unduhan/perbaikan berkas dan bookmark dibuktikan terarah. Ini bukan sertifikasi
editorial/fonetik seluruh Quran atau penyelesaian gerbang HP/publik.

Source `d7ac65a161b0da4b9371d5d1c79c0901cc628ae9`, kandidat lokal
`Zikra-Quran-v1.9.13-beta-debug.apk`, **41.200.618byte** (<100.000.000), SHA256
`740faa1804667311cd59a64b201ecae370bed83d89862bdd94bcc933c299994c`.
Signer debug sama dengan1.9.12; schema main5/chat1/kelas2. Kandidat sebelumnya
tetap utuh. Checkpoint lokal, belum push baru atau signed release publik.

## Perbaikan

- Permintaan timing/audio memakai tiket generasi sebelum masuk antrean worker.
  Batal/close mencegah tahap HTTP berikutnya dan publikasi cache dari pekerjaan
  lama, termasuk saat dibatalkan sebelum worker mulai. Tidak menahan lock selama
  jaringan; setiap panggilan terdaftar/dibatalkan sendiri, anggaran jaringan
  gabungan40detik, maksimum tiga rekaman sementara. Close memang sudah dijalankan
  di worker sebelum perubahan ini; dugaan awal UI menunggu close dikoreksi.
- Notifikasi unduhan memakai7500, terpisah dari Dzuhur7302. Migrasi hanya
  membatalkan7302 tanpa tag yang kanalnya `quran_downloads`; notifikasi salat
  tetap ada. Kanal dan pilihan pengguna tidak dihapus.
- Pembaruan notifikasi pemutar disatukan, status terakhir dibaca saat dikirim;
  promosi foreground tidak diulang selama masih aktif. Kontrol/MediaSession
  tetap segera, refresh kartu biasanya dalam satu detik. Stop/destroy menghapus
  callback agar kartu tidak muncul lagi. Keandalan tombol Jeda/Putar setelah
  kontrol cepat diuji.
- Decoder yang menolak unduhan lokal memberi jalur **Audio offline → Unduh /
  lanjutkan**. `AudioTransfer` memvalidasi decoder, mengganti berkas rusak melalui
  `.part` dan rename; perbaikan/penghapusan audio tidak menghapus bookmark.
  Tidak mengunduh ulang otomatis atau menghapus koleksi pengguna secara massal.

## Audit sumber dan hasil

| Pemeriksaan | Hasil terarah |
| --- | --- |
| Catalog Quran.com dibanding metadata Al Quran Cloud |114 ID/jumlah surah cocok, total6.236|
| Teks sumber|190 halaman API berukuran total4.079.394byte: key/nomor/urutan/paginasi, Arab dan terjemahan resource33 tidak kosong; semua lolos `QuranPage.parse` produksi di Android|
| URL audio global|6.236 nomor berturut-turut untuk masing-masing tiga edisi; bukan18.708 rekaman yang diunduh/didengar|
| Timing Quran.com|ID7 Alafasy,6 Husary,9 Minshawi Murattal. Rekaman1:1 masing-masing didekode dan di-seek ke kata2/4/1, pause/resume dan hash/durasi asli dicatat|
| CDN unduhan|Enam MP3 asli:1:1/global1 dan114:6/global6.236 untuk ketiga qari. Transfer nyata, decode, reuse tanpa akses HTTP dan posisi pemutar file lokal maju; hash/durasi dicatat|
| Reader/Mushaf|Sorotan reader mengikuti audio asli1:2→1:3; Mushaf pindah ayat/halaman, font sampel, zoom/malam/teks200%; seek/pause mengikuti posisi decoder|
| Offline/kegagalan|Perangkat tanpa Wi-Fi/data untuk kelompok pemutar sintetis, rentang/repeat/background/focus, partial file, decoder rusak/perbaikan, unduhan lanjut, gulir mandiri/sentuhan manual, bookmark pulih|

**139 unit lulus/4 skip,25 metode native unik lulus,lint0 error/780 warning.**
Empat skip merupakan snapshot host opsional: satu katalog Doa, satu dokumen
Mushaf dan dua metrik font;
tidak dihitung lulus. Sampel Mushaf native dan audit190 halaman dicatat terpisah.
Audio sintetis hanya menguji mesin/state, dilabeli pada screenshot terkait;
tidak disajikan sebagai rekaman Quran. Emulator API37 memakai `-no-audio`, sehingga
decode/posisi nyata bukan bukti kualitas suara terdengar.

Sumber primer:
[chapter API](https://api.quran.com/api/v4/chapters?language=id),
[metadata pembanding](https://api.alquran.cloud/v1/meta),
[recitation ID](https://api.quran.com/api/v4/resources/recitations?language=id),
[edisi audio CDN](https://api.alquran.cloud/v1/edition/format/audio),
[dokumentasi CDN](https://alquran.cloud/cdn).
API edisi CDN menyebut `ar.minshawi` bertipe `translation`, sedangkan dokumentasi
CDN resmi mencantumkannya sebagai Minshawi Murattal128kbps. Perbedaan metadata
dicatat, bukan digunakan untuk menganggap rekaman CDN identik dengan rekaman
timing Quran.com. Timing selalu diikat ke URL/byte/hash rekaman yang diputar.

## Bukti dan pengulangan

- [Receipt APK/source/hash](release.json),[manifest190 halaman dan metadata](corpus-manifest.json),
  [unit](unit-results.json),[lima uji pembatalan/cache](TEST-com.zikra.app.audio.TimedAudioRepositoryTest.xml),
  [native final](native-results.json),[preservasi](preservation.json),[audit privat](audit.json).
- [Rekaman timing nyata](Mushaf-Word-Audio.json),[sampel CDN nyata](D03-Cdn-Audio.json),
  [gambar runtime](screenshots),[build](build-final-v2.txt),[lint/harness akhir](build-harness-final.txt).
- Jalankan `node tools/audit-quran-d03.mjs .local/quran-d03-audit` dari root repo.
  Raw snapshot disimpan lokal, dua request halaman bersamaan, ukuran/timeout
  dibatasi; tidak membundel corpus baru ke APK/Git. Direktori yang sudah berisi
  snapshot dipakai ulang; gunakan direktori baru untuk pengambilan segar.
- Corpus native memakai `QuranCorpusAuditTest` dengan snapshot privat pada
  external-files/validation/d03-corpus. Kelompok native memerlukan isolasi data
  dan pemulihan pengaturan; jangan menjalankannya pada data HP utama tanpa
  cadangan. Gradle dan emulator dijalankan bergantian.

Putaran awal tetap dicatat pada `attempt-1-results.json`, `attempt-2-results.json`
dan `attempt-3-results.json`. Dua acuan tes lama diperbarui: tombol opsi sudah
berada di header, dan dock reader memang terlihat pada desain yang disetujui;
pengganti memeriksa bookmark terpilih setelah recreation. Kasus notifikasi
Jeda/Putar yang tidak konsisten mendorong coalescing/pembatasan refresh produksi.
Tes migrasi yang memeriksa posting/cancel secara langsung kadang mendahului
transaksi Android; harness akhir menunggu kanal aktif sebelum memigrasi dan
menunggu penghapusan selesai. Kegagalan terdahulu tidak dihitung sebagai lulus. Raw kegagalan tersedia pada
`attempt-1-offline-failure.txt`, `attempt-1-reader-failure.txt`,
`attempt-2-notification-state-failure.txt` dan
`attempt-3-notification-migration-failure.txt`.

## Preservasi dan batas

Seluruh10 berkas asli emulator cocok hash setelah pemulihan, tetapi baseline
tersebut0byte: **bukan bukti upgrade HP berisi**. Font/tema/ukuran/jaringan,
DND/ringer, izin notifikasi/exact alarm dan pengaturan terkait dipulihkan;
helper dihapus/emulator ditutup. HP tidak disentuh. Folder bukti eksternal
sebelumnya dipindah sementara dan dikembalikan.

WIP PROJECT_HANDOFF, stash audio/arsip A03, konfigurasi privat, Home/AI/logo,
APK dan audio asli dipertahankan. Nilai rahasia privat yang diketahui diperiksa
pada source/evidence dan seluruh entry APK; bukan audit seluruh sejarah Git.
Dua audio adzan pengguna tetap disuplai sebagai aset debug privat; sumber/lafaz
dan izin distribusinya tetap gerbang D02/G03. Optimasi prefetch dalam stash
ditinjau tetapi tidak digabung: perubahan lock saja belum menangani pembatalan
bertahap/tiket antrean dan belum mempunyai bukti benchmark yang diperlukan.

Sorotan kata tersedia hanya saat timing cocok dengan rekaman/durasi/hash;
audio CDN offline memakai sorotan ayat. Paket offline reader per ayat berbeda
dari font/halaman Mushaf; cache font dibatasi dan Mushaf tidak dijanjikan sebagai
paket604 halaman luring. Empat surah bawaan tetap memakai teks lokal yang
dibedakan dari source Quran.com. Audit190 halaman memeriksa identitas dan
kelengkapan, bukan penelaahan manusia setiap karakter/terjemahan atau pelafalan
semua rekaman. Tidak mengubah teks agama dari perkiraan model.

Uji HP ditunda sesuai instruksi terbaru. D01/D02/D02a, D06 upgrade berisi,
C02/C04/C07, B01, D05/E/F/G dan rilis publik tetap mengikuti checklist masing-masing.
