# Sumber Mushaf dan timing — 21 Oktober 2026

## Quran Foundation

[Panduan font resmi](https://api-docs.quran.com/docs/tutorials/fonts/font-rendering/)
memetakan QCF V2 ke mushaf 1, code_v2 dan metadata halaman: kandidat Madani,
**belum edisi final**. Jangan mencampur font dan layout.

[Developer Terms, 11 Oktober 2026](https://api-docs.quran.com/legal/developer-terms/)
mensyaratkan akun Developer Console aktif dan atribusi QF untuk cache/bundle font
dan gambar dari API/CDN terdokumentasi, terintegrasi aplikasi. Offline data yang
tersedia lewat Content Sync harus menggunakan jalur itu; cache API biasa dibatasi
satu minggu. Cache/unduhan teks lama perlu audit dan rencana migrasi, tanpa
otomatis menghapus data pengguna. Akun aktif/production Content Sync belum
terkonfirmasi pada proyek.

[Dokumentasi audio](https://api-docs.quran.foundation/docs/sdk/javascript/audio/)
membedakan ID recitation per ayat dan chapter-reciter. API resmi memakai kredensial
server; jangan memasukkan secret ke APK. Segmen harus cocok dengan recording API.
Probe Foundation verses/by_page/1 tanpa header mendapat HTTP 400: missing required
headers or invalid request, bukan proxy denial. Tidak mencari/mempublikasikan secret.

Endpoint public legacy recitation 7 / 1:1 masih HTTP 200. [Bukti](providers.json)
menunjukkan hash recording berbeda dari CDN aplikasi. Satu sampel belum membuktikan
PCM, offset/durasi, atau kecocokan seluruh korpus.

## Al Quran Cloud

[Ketentuan, 14 Juni 2026](https://alquran.cloud/terms-and-conditions): hak rekaman
tetap pada qari/pemegang hak; stream/embed/download untuk penggunaan pribadi dan
pendidikan diperbolehkan. Bukan public domain. Provider aplikasi tetap HTTPS
128 kbps ar.alafasy/ar.husary/ar.minshawi. Tidak membundel MP3 ke Git/APK.
Ketentuan ini tidak menyediakan bukti timing cocok dengan ketiga rekaman aplikasi.

## QUL / Tarteel

[Panduan layout](https://qul.tarteel.ai/docs/tutorial-mushaf-layout-end-to-end)
memerlukan page/line/type/word-range dan script/font yang cocok.
[Layout 12](https://qul.tarteel.ai/resources/mushaf-layout/12) adalah Indopak
Qudratullah, **610 halaman/15 baris**, bukan Madinah 604. ID contoh tutorial bukan
keputusan edisi. [Segments](https://qul.tarteel.ai/docs/with-segments) membedakan
timing per ayat/per surah; timestamp contoh bukan data MP3 produksi.
[FAQ](https://qul.tarteel.ai/docs/faq) meminta lisensi spesifik dataset diperiksa.
Versi paket dan izin setiap font/gambar/layout/recording belum terverifikasi;
tidak ada aset QUL yang disalin ke aplikasi.

## Langkah berikutnya

Pulihkan tes Android; konfirmasi akun/akses QF atau paket alternatif berizin;
tetapkan edisi+versi+hash. Periksa halaman awal/tengah/akhir dan Al-Mulk,
basmalah/waqaf, RTL, word-id dan mapping ayat/juz. Verifikasi recording/timing saat
seek, pause, buffering, repeat dan pergantian ayat sebelum membuat sorotan kata.
