# Audio dan cache peta — 14 November 2026

Kandidat internal **1.9.48/code75**, debug/HOLD. APK lokal:
`D:/Download/Zikra-App-main/Zikra-Polish-v1.9.48-beta-debug.apk`.
49.615.884 byte; SHA256
`7a8d094170d31b2dbd25b5e0e31074b61642b3208e0b269b5ce6b90ab61b6319`.
Tidak memakai HP, mengganti rekaman/desain, deploy backend atau membuka unduhan.
Nama penerbit publik **Zikra** dikonfirmasi pengguna; email dukungan tetap
arkanzahir46@gmail.com. Cakupan usia/retensi/kebijakan final belum disahkan.

## Perubahan dan alasan

- `AudioTransfer` dan `TimedAudioRepository` dahulu menerima semua HTTP2xx.
  Respons206 atau200 dengan Content-Range kini ditolak sebelum publikasi cache.
  Resume tetap per ayat yang sudah lengkap, bukan byte range yang belum diperiksa.
  Regression JVM gagal sebelum perbaikan, kemudian lulus. File parsial dibersihkan;
  file lengkap yang valid dapat digunakan kembali tanpa jaringan.
- Dua peta memakai `OsmMapPolicy` bersama: User-Agent bermakna dengan versi/kontak,
  dua thread, tanpa overshoot, tanpa override/penambahan expiry. Sebelumnya peta
  gempa menambah7hari pada expiry server melalui konfigurasi global osmdroid.
  Cache baru `osm-http-v2` mencegah penggunaan expiry lama yang sudah terpanjang;
  cache lama tidak dihapus. Ini cache tile opsional, bukan data pengguna.
- Sumber utama kebijakan: [OSM tile usage policy](https://operations.osmfoundation.org/policies/tiles/).
  Source osmdroid6.1.20 yang dipakai proyek ditinjau: MAPNIK memakai URL kanonis,
  computeExpirationTime menghormati max-age/Expires dan default7hari bila keduanya
  tidak tersedia. Tidak mengganti provider menjadi Google Maps atau mengaktifkan billing.

## Bukti pada biner final yang sama

| Pemeriksaan | Hasil | Batas |
|---|---|---|
| JVM |207 lulus,0skip,0gagal/error|Termasuk fixture Doa dan Mushaf privat yang tersedia; bukan fixture dibundel|
| lintDebug |0error/890warning|Warning lain tetap perlu triase|
| Native audio |12 lulus|7transfer,5queue/player/download/focus/corrupt/cancel; `native-audio.txt`|
| Native live/peta/UI |6 lulus|Tiga qari dalam1case,teks200%,panelfold,smoke16layar,livewordtiming,petaexpiry; `native-live-map-ui.txt`|
| Update terisi |1 verifikasi lulus|1case seed terpisah pada1.9.41; install-r1.9.48 tanpa uninstall/clear di antaranya|
| Audit APK |Privat0/credential dikenal0|Signature valid dan audio tetap; debug/HOLD; `apk-audit.json`|

**19kasus native pada1.9.48**, ditambah1 persiapan seed pada1.9.41. Hasil32kasus
1.9.47 tetap historis dan tidak dijumlahkan. Versi backend tidak berubah;
125tes backend dari checkpoint sebelumnya, tidak diklaim dijalankan ulang.

`checks.json` mencatat hasil mesin; log asli privat di
`.local/audio-final-20261114/`. Build final `candidate-audio-map-final.txt`
berhasil46detik; dua kelompok native17,907detik dan56,664detik.
JVM memakai `ZIKRA_HADEETHENC_FIXTURE`, `ZIKRA_MUSHAF_AUDIT_DIR`,
`ZIKRA_DOA_AUDIT_FILE`. Fixture lengkap dijalankan ulang untuk menghindari skip
yang tersimpan sebagai hasil task up-to-date. Raw naskah/rekaman tidak masuk Git/APK.

## Detail yang tidak boleh diperluas menjadi klaim lain

- Emulator task API37.1,720×1560,1536MB; isolated ADB5042,loopback5583.
  `-no-audio`: decoder/playback state diuji, bukan kualitas suara yang didengar manusia.
  Uji font200%/folded lulus, tetapi screenshot `ReaderFoldedPlayingSynthetic`
  timeout. Tidak mengklaim screenshot itu berhasil disimpan.
- Tiga qari cocok katalog live: Alafasy7,Husary6,Minshawi9. Masing-masing hanya
  Al-Fatihah1:1; decoder6087/5224/4971ms, timing tervalidasi untuk sampel tersebut.
  `live-reciters.json` hanya metadata; klip sementara dibersihkan test.
  Live reader mencatat2offset kata berbeda dan pindah keayat3.
  Ini bukan validasi lafaz seluruh Quran/semua timing atau izin redistribusi.
- Transfer menguji206,Content-Range200,bodyterputus/truncated,retry,cancel dan
  mempertahankan file valid. HTTP200 tanpa header range masih bergantung pada
  panjang body dan decoder; ini bukan bukti semua bentuk korupsi dapat dideteksi.
- Kebijakan peta diuji melalui konfigurasi/library parser aktual untuk max-age,
  Expires,headerkosong dan konfigurasi ulang. Bukan audit wire semua tile/cache
  provider atau jaminan kapasitas layanan publik.
- Upgrade sintetis memeriksa profil,tugas,Hifz,chat/draf,bacaan terakhir,tema.
  Clear data hanya persiapan emulator disposable sebelum seed; bukan HP.
  Signer sama; belum membuktikan migrasi debug→release.
- ENOSPCfisik/powerloss,Doze/vendor/perizinan/receiver lintas HP,seluruhfont/audio,
  hak/B01/audio,retensi/usia,signer/migrasi,J13/J14/J15 tetap terbuka.
  A–D/E/F/G dan pekerjaan tanpaHP belum boleh dinyatakan selesai seluruhnya.

WIP `PROJECT_HANDOFF.md`, Infaq/line-endingXML, stash audio, konfigurasi privat,
logo dan artwork diterima tetap dipertahankan; audit checkpoint dilakukan sebelum push.

Emulator task dihentikan dan direktori disposable E:/Zikra-AudioFinal-20261114
dibersihkan setelah validasi path dan marker. Hash7konfigurasi privat, WIP handoff,
arsip audio, logo/foreground serta stash diverifikasi tetap. Website cek statis
1.9.48 lulus; screenshot/layout desktop/mobile tetap bukti1.9.47.
