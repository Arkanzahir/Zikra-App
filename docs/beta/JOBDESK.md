## Update14November2026

[Bukti kandidat1.9.47](../../android/validation/beta-final-2026-11-14/README.md) dan [handoff akun](../../ACCOUNT_HANDOFF.md) adalah checkpoint terbaru. Parent tetap terbuka; hanya dua kriteria terarah tambahan dicentang.

# Jobdesk menuju beta Zikra

Checkpoint terbaru: [handoff akun](../../ACCOUNT_HANDOFF.md),
[hasil gabungan](../../android/validation/final-batch-2026-11-13/README.md).
Kandidat1.9.47:202JVM/32native kasus berbeda lulus,4JVMskip,890lintwarning. Backend tidak berubah;125tes lulus pada checkpoint sebelumnya.
Website J15 pratinjau lokal tersedia di `website/dist`, tanpa tautan APK.
Gerbang J14 belum ditutup; jangan menganggap seluruh J02–J15 selesai.

Dibuat 12 November 2026. Acuan source: ca80e85, APK internal1.9.41, HP terakhir
terbukti1.9.39. Ini daftar pelaksanaan turunan IMPLEMENTATION_PLAN.md dan
RELEASE_SCOPE.md, tidak menggantikan cakupan atau menghapus gerbang lama.
Pengguna mengizinkan pelaksanaan urutan terdekat. Checkpoint13November: J01–J03 dikerjakan, bukti di android/validation/quran-sync-integration/README.md; belum semua kriteria terpenuhi. Website J15 mencakup preview lokal sebelum J14, publikasi unduhan sesudah gerbang.

Rute penutupan terbaru: [FINAL_BATCH.md](FINAL_BATCH.md); kandidat internal1.9.46/code73 tetap HOLD. [Operator/dukungan](OPERATOR_RUNBOOK.md) disiapkan, bukan bukti retensi/kapasitas penyedia disahkan.

## Cara melacak

Status: **Antrean**, **Sebagian**, **Menunggu HP**, **Menunggu keputusan/izin**,
**Selesai terverifikasi**, **Ditunda**. Sebagian berarti ada kode/bukti tetapi
kriteria job belum semuanya terpenuhi. Checkbox hanya dicentang setelah bukti
tersimpan, pemeriksaan terkait lulus dan commit/push diverifikasi.

Setiap update mencatat: ID, perubahan, hasil tes/gagal/skip, lokasi bukti,
versi/hash APK bila berubah, commit dan hambatan berikutnya. Tes versi lama
tetap bukti versi lama; jangan otomatis mengklaim paket baru lulus.

Perkiraan70–75% di chat sebelumnya adalah estimasi informal, bukan hitungan
terbukti. Belum ada angka progres rilis resmi. Tidak menghitung jumlah fitur
sebagai persentase keamanan/kesiapan rilis, atau memberi bobot sama untuk tugas
kecil dan penghalang distribusi.

## Urutan dan penanggung jawab

Kerjakan J01→J02→J03 terlebih dahulu tanpa HP. J04–J07 diselesaikan menurut
bukti/izin yang tersedia, lalu J08–J12. J13 memerlukan kesiapan HP; pemeriksaan
harus memakai kandidat final yang sama setelah perubahan terkait stabil.
J14 adalah keputusan gerbang, J15 baru kanal distribusi. Bila izin eksternal
menunggu, lanjut pekerjaan independen berikutnya tanpa mencentang yang menunggu.

Codex menangani implementasi, audit, tes dan bukti. Pengguna menentukan identitas,
izin publikasi/layanan, menyediakan akses melalui konfigurasi privat dan
mengonfirmasi hal yang hanya dapat dirasakan pada HP. Pemilik konten memberi
izin konten; Codex tidak dapat mengesahkan izin atas nama mereka.

| ID | Prioritas / acuan | Pekerjaan | Status checkpoint14November | HP |
| --- | --- | --- | --- | --- |
| J01 | P0 / G03, D03 | Layanan Content Sync server | Selesai terverifikasi: endpoint/cache/enkripsi dan hosted200/401/429 lulus | Tidak |
| J02 | P0 / D03, D06, G03 | Integrasi Quran reader dan unduhan Android | Terintegrasi1.9.47;hosted native,staging,upgrade terisi lulus;gapfailure/audio/HP tetap | Emulator dahulu, konfirmasi akhir J13 |
| J03 | P0 / D03, G03 | Integrasi layout Mushaf, font, timing/audio | Mapping604/font CDN asli lulus;seluruhfont/timing/audio/hak tetap | Emulator dahulu, akhir J13 |
| J04 | P0 / G03 | Sumber dan hak Quran/font/terjemahan/layanan | Sebagian; tiga font bundel lolos audit paket | Tidak, izin eksternal mungkin perlu |
| J05 | P0 / B01 | Kejelasan sumber kitab Hadis lengkap | Menunggu klarifikasi sumber/cakupan | Tidak |
| J06 | P0 / D02, D02a | Hak dan lafaz audio adzan | Sebagian: dua rekaman/hash teridentifikasi; izin belum | Audit tanpa HP; suara akhir J13 |
| J07 | P1 / C02, G03 | Operasional AI dan akses tester | Sebagian; health/auth dan tes tersedia | Mayoritas tidak; kasus akhir J13 |
| J08 | P1 / E03 | Cuaca, alert, doa kontekstual, briefing | Cuaca live/cache,pengaturan,dedupe/izin/lokasi/hari lulus terarah;jobbackground/HP tetap | Mayoritas tidak, lalu J13 |
| J09 | P1 / E04 | Darurat dan masjid terdekat | Intent nomor/draf/rute/fallback lulus dengan monitor;app penerima/HP tetap | Emulator dahulu, aksi akhir J13 |
| J10 | P1 / F01–F04 | Konsistensi UI, aksesibilitas, performa | Panduan,splash/dock/keyboard/teks200% dan smoke16layar lulus;matriks/performaHP tetap | Sebagian emulator; performa akhir HP |
| J11 | P0 / G03 | Privasi, dukungan dan tata kelola | Sebagian; draf/email sudah ada | Tidak |
| J12 | P0 / G04 | Signing, paket dan migrasi distribusi | Sebagian; guard/auditor ada, key final belum | Build tanpa HP; migrasi akhir J13 |
| J13 | P0 / C02, D02, G01, G02 | Uji paket final dan kegagalan fisik | Menunggu HP dan kandidat final | Ya, dikoordinasikan |
| J14 | P0 / G05 | Putusan siap beta terbatas | Antrean; HOLD tetap berlaku | Berdasarkan bukti J01–J13 |
| J15 | Sesudah J14 | Website unduhan dan peluncuran bertahap | Preview lokal desktop/mobile selesai;download/publish menungguJ14 | Uji instal kanal pada HP |

## Kriteria selesai per job

### J01 — Layanan Content Sync server

- [x] Mesin staging/token atomik di memori dan transport server-only dibuat;
  17tes serta bootstrap/incremental produksi terarah dibuktikan.
- [x] Hubungkan endpoint konten dengan autentikasi instalasi/rate limit; secret
  QF hanya di server, tanpa URL upstream bebas dari client.
- [x] Tentukan penyimpanan/cold-start dan cache/refresh; respons ke APK harus
  ringkas, bukan meneruskan snapshot core35MB melalui satu respons serverless.
- [x] Uji tidak berizin/kuota, abort/timeout/paging/tombstone, restart server,
  rollback dan ketersediaan resource; verifikasi deployment jika diaktifkan.

Bukti awal: ../../android/validation/quran-content-sync-2026-11-12/README.md
(dari root repo: android/validation/quran-content-sync-2026-11-12/README.md).
Hasil: endpoint dan receipt uji yang dapat dipakai J02/J03, belum klaim migrasi APK.

Receipt lanjutan: android/validation/quran-sync-integration/README.md dan hosted-proof.json. J02/J03 tidak otomatis selesai.

### J02 — Quran reader dan unduhan

Tambahan: [withdrawal/proses mati/gagal tulis/ruang rendah](../../android/validation/quran-failure-2026-11-14/README.md) teruji terarah pada APK yang sama. Lima kasus ini bukan ENOSPCfisik atau power-loss; kriteria majemuk yang belum lengkap tetap terbuka.

- [x] Hubungkan reader/QuranDownloadService ke data dari Content Sync dengan
  identitas surah/ayat/terjemahan yang sama, tanpa mengubah teks sumber.
- [ ] Terapkan penyimpanan durable/staging dan perpindahan generasi hanya sesudah
  lengkap; file lama, bookmark/progres/pilihan qari dipertahankan sampai pengganti valid.
- [ ] Refresh sesuai kontrak, bedakan umur data dari waktu dibaca; uji pembatalan,
  koneksi putus, korupsi, storage rendah, resource dicabut dan proses mati.

Hasil: migrasi data terisi dan reader/unduhan luring teruji di emulator;
catat batas empat surah bawaan/sumbernya. Tidak menghapus unduhan demi meluluskan tes.

### J03 — Mushaf dan audio

- [ ] Petakan layout Mushaf/word ke ID resource yang benar, terpisah dari Quran
  core/terjemahan; pertahankan navigasi halaman dan bookmark lama.
- [ ] Bedakan cache font CDN, metadata timing dan binary rekaman; periksa kebijakan
  penyimpanan masing-masing. Uji basmalah/nama surah/harakat dan pergantian halaman.
- [ ] Uji keserasian ayat/audio/timing, putus/pulih, pause/stop dan qari yang didukung.

D03 yang sudah dicentang adalah hasil teknis beta terdahulu, bukan bukti migrasi
baru ini selesai. Sorotan Quran UI10 tetap ditunda pengguna; tidak diubah diam-diam.

### J04 — Sumber dan hak

- [x] Tiga font bundel/notice cocok dengan APK1.9.41; pemeriksaan otomatis5tes lulus.
- [ ] Verifikasi akun/atribusi dan implementasi sesuai ketentuan QF terbaru;
  tutup bukti teks/terjemahan/timing/audio serta font unduhan secara terpisah.
- [ ] Tuntaskan tinjauan syarat OSM/Overpass, cuaca/geokode, BMKG, USNO dan Doa
  sesuai cara pemakaian; atribusi terlihat, batas layanan dan penggunaan beta jelas.

Hasil: matriks izin/sumber per aset/layanan dengan bukti primer, tanggal, syarat
serta status. Lisensi kode tidak otomatis melisensikan konten. Tanpa bukti: tetap terbuka.

### J05 — Hadis B01

- [ ] Pastikan edisi, asal terjemahan, penomoran, kelengkapan Bukhari/Muslim dan
  hak distribusi; catat hasil korespondensi/sumber primer.
- [ ] Bila klarifikasi tidak tersedia, siapkan pilihan konkret sumber/cakupan
  untuk keputusan pengguna sebelum mengganti/mengurangi target.
- [ ] Sesudah keputusan dan hak jelas, uji dataset/bookmark/atribusi versi final.

HadeethEnc2.260entri adalah koleksi pilihan opsional, bukan dua kitab lengkap;
B02/B03 teknis dan B04 tidak perlu diulang dari nol. Email tidak dikirim tanpa instruksi.

### J06 — Audio adzan

- [ ] Verifikasi pemilik/kewenangan dan izin APK/AAB atas dua rekaman yang dipilih.
- [ ] Periksa lafaz penuh/identitas rekaman dan kebutuhan atribusi; simpan bukti privat.
- [ ] Pertahankan hash/audio pilihan sampai ada keputusan sah; bila perlu pengganti,
  siapkan opsi lalu minta keputusan pengguna, tanpa mengganti diam-diam.

Hasil: receipt hak/lafaz untuk dipakai J12; gratis diunduh bukan izin distribusi.
Uji suara dan alarm fisik digabung J13, bukan sesi USB berulang untuk tugas yang sama.

### J07 — AI dan akses tester

- [ ] Tutup prosedur menerbitkan/mencabut akses per tester, kuota/kapasitas,
  penanganan penuh/timeout serta pemantauan tanpa log sensitif.
- [ ] Uji percakapan berurutan dan pemulihan slot; data chat/lampiran tetap hanya
  dikirim sesuai izin. Bedakan logika lokal, hosted, dan hasil HP.
- [ ] Gabungkan kasus laptop tidak aktif/internet HP mandiri pada J13.

C03–C09 beserta subset yang dicentang tetap bukti sesuai versi/lingkupnya;
C02 bukan berarti seluruh AI belum bekerja. Jangan mengganti provider/model/paket
atau menambah biaya tanpa kebutuhan dan keputusan yang sesuai.

### J08 — E03 kontekstual

- [ ] Verifikasi cuaca untuk lokasi/zona/waktu pilihan dan keadaan data lama/gagal.
- [ ] Tuntaskan alert/kontrol pengguna/deduplikasi, doa perjalanan/pagi/petang,
  morning briefing sesuai cakupan yang sudah disetujui.
- [x] Uji pergantian hari/lokasi, izin ditolak, opt-out dan tidak ada notifikasi ganda.
  Bukti emulator journal/cooldown/revision/location/permission pada1.9.47;
  bukan klaim delivery fisik Doze/semuaOS.

Hasil: bukti skenario nyata serta batas layanan. E01/E02 tidak diulang tanpa alasan.

### J09 — E04 darurat/masjid

- [ ] Audit nomor/tautan bantuan dan atribusi; fallback lokasi ditolak/hasil
  kosong/gagal/cache harus jelas dan tidak mengarang data komunitas.
- [ ] Uji rute ke masjid, buka Maps, panggilan/pesan melalui tindakan pengguna.
- [ ] Tutup skenario fisik yang benar-benar perlu pada sesi J13.

Tetap OSM sesuai keputusan pengguna, tanpa aktivasi billing Google Maps.
Tidak melakukan panggilan darurat atau mengirim pesan sungguhan sebagai tes.

### J10 — UI dan performa

- [x] Bekukan panduan dari referensi yang diterima: logo/ikon, font, warna,
  ukuran/jarak dan variasi layar yang disengaja. Tidak merombak desain diterima.
- [ ] Audit lintas layar, tema, teks besar/Arab, keyboard/dialog/scroll,
  sentuhan/label aksesibel, keadaan loading/kosong/gagal dengan data nyata.
- [ ] Periksa animasi berhenti saat tidak terlihat, reduced motion/penghemat daya;
  ukur memori/baterai di HP pada J13. Simpan screenshot native per kondisi.

Hasil: matriks layar + cacat yang diperbaiki + bukti perbaikan; F03a selesai subset,
F01–F04 tidak otomatis selesai dari subset tersebut.

### J11 — Privasi/dukungan

- [x] Kontak pengguna dicatat: arkanzahir46@gmail.com; draf retensi berbasis kode ada.
- [ ] Tetapkan identitas penerbit final, cakupan usia/layanan dan kebijakan publik
  yang sesuai implementasi; jangan menjanjikan nol log atau retensi yang tak terbukti.
- [ ] Tetapkan proses dukungan, pencabutan akses/penghapusan data dan penanganan
  insiden; verifikasi retensi penyedia dan salinan cadangan, bukan hanya TTL gateway.

Hasil: dokumen final yang siap ditinjau/diterbitkan, bukan teks placeholder.

### J12 — Paket rilis dan perpindahan dari debug

- [x] Guard signing env-only dan auditor paket ada; kandidat internal53,2MB.
- [ ] Tentukan signer release tetap/cadangan privat dan strategi perpindahan data.
  Signer baru tidak dapat memperbarui debug ber-ID sama; jangan uninstall.
- [ ] Setelah hak audio selesai, bangun signed candidate dengan versi/hash dan
  source terikat; audit rahasia/fixture, FCM, lisensi/aset dan fitur setara.
- [ ] Buktikan unduhan awal <100.000.000byte; unduhan lanjutan opsional dijelaskan.

Hasil: kandidat signed untuk J13, belum publikasi. Identitas/certificate/password
privat tidak masuk Git/APK/log. Membuat key bukan pengganti uji migrasi.

### J13 — Satu sesi HP terkoordinasi

- [ ] Cadangan diverifikasi dan update tanpa uninstall; preservasi data/izin/pilihan
  pengguna dibandingkan sebelum/sesudah pada kandidat final yang sama.
- [ ] Tutup C02 serta D02 idle/hemat baterai, audio focus/Stop/selesai alami dan
  skenario gagal yang masih kurang. Bukti reboot lama tidak diulang tanpa alasan.
- [ ] Smoke lintas fitur G01/G02, izin ditolak, proses/background, putus jaringan,
  storage, tema/teks besar/sensor/peta dan performa sesuai gap yang tersisa.
- [ ] Lepas simulasi daya, pulihkan setting, bersihkan helper/fixture dan periksa
  alarm/data asli sebelum mengembalikan HP. USB putus = belum konklusif.

Pengguna diminta suara/rotasi/hal fisik seperlunya; Codex mengendalikan yang
terukur. Tidak restart, menutup game, mengubah autostart atau mengirim berkas
pribadi tanpa koordinasi. Catat gagal/skip, jangan hanya hasil lulus.

### J14 — Gerbang beta

- [ ] Semua pekerjaan wajib memenuhi kriteria atau perubahan cakupan disahkan
  eksplisit sesuai RELEASE_SCOPE; tidak ada pengurangan diam-diam.
- [ ] Tidak ada bug penghalang/data hilang, izin/hak/kontak/paket/sumber dapat
  ditelusuri; keterbatasan, pemantauan, pemulihan dan penghentian distribusi tersedia.
- [ ] Putusan rilis beta dan bukti paket final dicatat. Rilis publik G05 tidak
  otomatis ditutup hanya karena beta terbatas dapat dimulai.

### J15 — Website dan peluncuran

- [ ] Sesudah J14, susun halaman unduhan, versi/hash, fitur/batas, privasi,
  dukungan dan petunjuk update aman. Jangan menerbitkan debug/unsigned sebagai final.
- [ ] Uji HTTPS/tautan unduh dan instal/update dari kanal nyata; mulai bertahap,
  pantau masalah/kuota dan siapkan penarikan versi bermasalah.
- [ ] Tinjau hak promo/voice-over sebelum publikasi, terpisah dari persetujuan visual.

## Di luar antrean beta sekarang

F05 login Google, F06 backup pengguna/cloud, F07 sinkronisasi antarperangkat,
F08 dashboard admin: tetap ditunda sesuai keputusan sebelumnya. F06 bukan izin
mengabaikan keselamatan migrasi debug→release; J12 harus memiliki solusi aman.
UI10 sorotan Quran dan mode orang tua juga tetap ditunda. Tidak menambah fitur
besar ini sebelum gerbang inti tuntas atau pengguna mengubah prioritas.

## Format update kerja

```text
ID / status sebelumnya → status sekarang:
Perubahan dan hasil yang terlihat:
Verifikasi (lulus / gagal / skip) serta versi/hash:
Bukti:
Commit/push:
Sisa / dependensi:
```

Belum ada tugas implementasi yang dicentang hanya karena jobdesk ini dibuat.
