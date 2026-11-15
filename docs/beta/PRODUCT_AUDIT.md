# Audit produk dan kesiapan Zikra — 15 November 2026

## A. Ringkasan dan batas

**Keputusan: HOLD.** A–D/E/F/G belum selesai seluruhnya. Audit ini melanjutkan
checkpoint `9527c1f`/1.9.51, bukan kembali ke baseline1.7.6. Brief pengguna
15November dipahami sebagai audit bertahap dengan perbaikan terarah, tanpa
rewrite, perubahan provider, billing, publikasi atau penambahan fitur besar.
SHA256 brief: `fd3031a1bccb2e0daf87954681440622f9e2b4f0094e3e245ba937ba256cf1e6`.

Ini **audit putaran pertama, bukan sertifikasi seluruh source/produk**. Dokumen
aktif, alur lampiran/startup, batas gateway, manifest/build dan bukti terdahulu
ditinjau. Bukti historis tidak dihitung sebagai tes kandidat terbaru. Bagian yang
memerlukan akun/perangkat/penelaah konten ditandai belum terverifikasi.
Referensi kerja: [jobdesk](JOBDESK.md), [status](STATUS.json),
[panduan desain](../design/APP_GUIDE.md), [cakupan](../../RELEASE_SCOPE.md).

Tidak ada persentase atau skor numerik kesiapan: belum ada cakupan/pembobotan
yang membenarkan angka. Penilaian di bawah kualitatif dan dapat ditelusuri.

## B. Scorecard dan inventaris fungsi

`Fully implemented` hanya berlaku pada subset dengan syarat dan bukti lengkap;
bukan semua kondisi perangkat. `Not verified` tidak berarti rusak.

| Area | Status fungsi | Bukti/risiko yang menentukan |
|---|---|---|
| Home/jam/jadwal/lokasi | Partially implemented | Ada jadwal/cache/fallback berlabel; perbandingan semua kota/tanggal dan lokasi remaja belum tuntas |
| Tema/dock/splash/lampu/gerak | Partially implemented | Implementasi native dan bukti versi lama ada; audit seluruh keadaan dan baterai HP belum lengkap |
| Quran reader/terjemahan/Mushaf | Partially implemented | J02 teknis, pemetaan dan pemulihan terarah terbukti; seluruh604font/qari/timing/hak belum |
| Unduhan/cache Quran | Partially implemented | Manifest/cancel/staging/low-space terarah; ENOSPC/power-loss nyata belum |
| Adzan/Stop/pilihan waktu | Partially implemented | Bukti fisik historis tersedia; D02/D02a, hak rekaman dan sisa kondisi daya tetap terbuka |
| Kiblat | Partially implemented | Sensor, kemiringan dan pembandingan acuan HP pernah lulus; bukan sertifikasi semua sensor/perangkat |
| Masjid/peta/navigasi | Partially implemented | OSM/Overpass/cache/fallback tersedia; layanan eksternal dan penerima intent menyeluruh belum |
| Doa | Partially implemented | Katalog, pencarian/simpanan tersedia; finalisasi sumber/lafaz/seluruh konten mengikuti J04–J06 |
| Hadis | Partially implemented | B02–B04 teknis/editorial; B01 dua kitab lengkap/hak masih menunggu. HadeethEnc bukan pengganti diam-diam |
| Study/tugas/jadwal/fokus | Partially implemented | Persistensi lokal/alarm/sesi tersedia; matriks gangguan dan lintas perangkat belum lengkap |
| Hifz/ibadah harian | Partially implemented | Target/jurnal/progres lokal; audit teks besar/kondisi terisi lintas hari belum menyeluruh |
| Kalender Hijriah | Partially implemented | Perkiraan kalender berlabel; bukan keputusan rukyat resmi; seluruh ukuran/perangkat belum diperiksa |
| Profil/pengaturan | Partially implemented | Ruang pribadi/tema/akses fitur tersedia; tidak mengklaim ada akun cloud |
| AI chat/Markdown/riwayat | Partially implemented | Online hosted dan riwayat lokal; consent antrean1.9.51 teruji; evaluasi keselamatan menyeluruh belum |
| AI offline | Partially implemented | Panduan aturan lokal; bukan model bahasa offline setara layanan online |
| Kamera/foto/PDF/DOCX/TXT | Partially implemented | Batas1berkas8MB/PDF3halaman/teks12.000; pilihan lampiran bukan indeks RAG permanen |
| Gambar AI | Partially implemented | Jalur terpisah/konfirmasi/kuota; tidak menjanjikan kemampuan setara GPT/Gemini |
| Cuaca/Protector/gempa | Partially implemented | Open-Meteo/BMKG/cache/push opt-in ada; bukan peringatan dini atau jaminan waktu kirim |
| Pengingat kontekstual/bantuan | Partially implemented | Defaultoff, dedupe/revisi/draft; Doze/penerima eksternal HP masih terbuka |
| Cadangan lokal terenkripsi | Partially implemented | Core/lintas signer fixture1.9.50; SAF/perangkat/signer final belum. Restore Android9+ |
| Login/sinkronisasi/admin | Planned | F05–F08 sengaja ditunda; tidak menambahkannya demi skor audit |
| Website unduhan | Planned | Pratinjau lokal tersedia, pengguna menunda, download tetap dinonaktifkan |

Arsitektur: AndroidJava/XML, SQLite/SharedPreferences dan penyimpanan privat;
gateway NodeHTTP/Vercel, registry/kuota Upstash, WorkersAI, FCM/cron. Tidak ada
framework backend Express/Next; panduan keamanan framework tersebut tidak
dipaksakan ke server ini. Tinjauan ini bukan audit dependensi/CVE menyeluruh.

## C. Temuan terurut

| ID | Prioritas | Temuan, dampak dan tindakan |
|---|---|---|
| PA01 | P1 sebelum undangan remaja | Belum ada proses usia/wali dan kontrol lokasi remaja yang terverifikasi; basicconsent bukan wali. Lanjut J11, jangan mengumpulkan KTP/DOB/wajah secara asal |
| PA02 | P1 sebelum distribusi | B01, hak Quran/audio/layanan/promo belum seluruhnya ditutup; jangan menyamakan file yang diberikan pengguna dengan hak distribusi |
| PA03 | P1 sebelum Play | `android/app/build.gradle` masih compile/target35; kebutuhan pengajuan baru sekarang API36. Migrasi SDK/toolchain dan uji behavior Android16 wajib, bukan hanya mengganti angka |
| PA04 | P1 sebelum Play | `AiActivity` dan paket assistant belum menyediakan pelaporan konten ofensif di dalam aplikasi. Butuh alur kirim minimal, persetujuan data, pembatasan/retensi, penerimaan laporan dan triase; tautan email eksternal saja belum membuktikan syarat terpenuhi |
| PA05 | P1 sebelum distribusi | Kandidat masih debug; signer final, pemindahan data pengguna, sesi J13 dan keputusan J14 belum tuntas |
| PA06 | P2 privasi | PDF renderer dibersihkan normal tetapi bisa tertinggal setelah proses mati. Perbaikan1.9.52 membatasi cleanup salinan lama milik renderer; lihat D |
| PA07 | P2 operasional | Retensi log/drain/backup akun belum dibuktikan. Probe GET15November mendapat403; jangan menyalin retensi paket historis sebagai fakta akun saat ini |
| PA08 | P2 maintenance | Handoff menyebut51 di atas tetapi49 di bagian kandidat/prompt dan meminta implementasi backup yang sudah tersedia. Konsolidasikan satu status aktif dan tautkan bukti sejarah |
| PA09 | P2 observabilitas | Belum ada SLO hasil beta, pengukuran latensi/biaya/failurerate lintas layanan dan baseline baterai HP yang lengkap; jangan mengklaim siap skala publik |
| PA11 | P2 aksesibilitas, diperbaiki subset | Screenshot320dp/teks200% menunjukkan label dock rapat. Batas autosize minimum khusus dock disesuaikan8–12sp dan ellipsize penjaga; isi halaman tetap skala penuh. Tes mengharuskan seluruh label tanpa ellipsis/keluar tab pada ukuran yang diuji |
| PA10 | P2 kesiapan | Lint894warning, kompatibilitas API26/27/28/35/36/OEM dan audit semua keadaan UI belum tuntas; triase warning berdasarkan dampak, jangan menghapus warning dengan suppress massal |

Tidak ditemukan bukti eksploitasi pengguna pada temuan ini. Ini daftar gap yang
terkonfirmasi, bukan klaim tidak ada temuan lain. Referensi keamanan rinci tetap
[PRIVACY_SECURITY_REVIEW](PRIVACY_SECURITY_REVIEW.md), tidak dibuat laporan saingan.

## D. Perbaikan pada putaran ini

- PA06: kepemilikan salinan PDF renderer dipisahkan; handle menutup/menghapus
  salinan pada sukses/gagal. Startup worker memindai maksimal256entri cache,
  hanya nama renderer berupa angka, file biasa, lebih tua24jam, bukan file aktif.
  Symlink/direktori/nama lain tidak ditelusuri. Berkas asli, chat, lampiran
  tersimpan dan kamera yang mungkin masih digunakan tidak dihapus.
- Cleanup oportunistik, bukan janji penghapusan tepat24jam atau secure erase.
  Aplikasi harus dibuka; scan dibatasi dan kegagalan filesystem dapat menunda.
  Kamera setelah proses mati masih perlu kebijakan tersendiri yang melindungi
  URI capture tertunda. Tidak mengklaim seluruh retensi cache selesai.
- PA08: handoff aktif dirapikan; Python Redis memakai runtime yang berfungsi,
  bukan executable venv lama yang sudah rusak. Ini perbaikan dokumentasi.

[Bukti kandidat1.9.52 dan batas](../../android/validation/product-audit-2026-11-15/README.md).
Tidak mengubah jadwal, audio, keluarga font, artwork, tema, provider, kuota atau produksi.
PA11 hanya mengubah sizing label navigasi; penerimaan desain lain tetap.

## E. Visual dan pengalaman pengguna

Pertahankan identitas/logo dan delapan ikon fitur yang disetujui; alias ikon
konsisten lintas halaman. PlusJakarta untuk UI, AmiriQuran untuk reader,
QCF untuk Mushaf memang memiliki fungsi berbeda. Tidak meratakan semuanya
ke satu font atau mendesain ulang halaman yang sudah diterima.

Matriks review: Home/AI/Study-Hifz/Profil/Kalender/pengingat, light/dark,
kosong/terisi/loading/gagal, 320dp/teks200%, keyboard/dialog, RTL/harakat,
dock/sentuhan dan animasi yang tidak terlihat. Bukti versi lama pada folder
validation tetap sah hanya untuk subset/versinya. Hasil kandidat terbaru dan
screenshot aktual dicatat di receipt; source XML saja bukan bukti visual.

## F. Verifikasi dan pekerjaan QA berikutnya

Perubahan kode putaran ini memerlukan unit guard cleanup dan native renderPDF/
symlink/startup. Buildawal helper native gagal karena PdfDocument bukan
AutoCloseable; helper diperbaiki dengan finally, kegagalan awal tetap dicatat.
Angka tes kandidat/lint/APK berasal dari receipt, bukan penjumlahan sejarah.
Backend tidak berubah;134tes14November adalah hasil terdahulu, bukan runbaru.

Sisa: SAF batal/password/grant/restoreHP; actual ENOSPC/powerloss; Android26–36
dan OEM; pengingat saatidle serta cuaca latar belakang; sensor/performa/baterai.
Tidak mengulang uji adzan identik tanpa perubahan atau alasan baru.

Rencana beta setelah gate:20–50penguji bertahap5→20→50, termasuk kebutuhan
aksesibilitas dan perangkat rendah/menengah. Remaja baru diundang setelah J11.
Catat build/hash, OS/perangkat, alur dan hasil; jangan kumpulkan isi chat/GPS
sebagai telemetri bawaan. Ambang internal yang **diusulkan, belum terukur**:
nol kehilangan data/blocker, keberhasilan alur inti≥95% dari percobaan tercatat,
catat p50/p95latensi serta error per provider. Evaluasi sebelum memperluas cohort.

AIeval: himpunan sintetis berversi untuk belajar, soal ambigu, ayat/hadis,
foto buram/PDF, dokumen berinstruksi berbahaya, permintaan berbahaya, data
pribadi, offline/429/503/timeout/revoke. Nilai akurasi/sumber, penolakan aman,
Markdown dan ketepatan tindakan. Penelaah manusia khusus konten agama; hasil
snapshot model bukan jaminan seluruh jawaban. Jangan upload dokumen pengguna.

## G. Yang belum dapat ditutup

Usia/wali/lokasi, retensi akun, hak/B01/audio, PA03/PA04, signer/migrasiHP,
J13/J14, seluruh matriks UI/performa dan uji beta nyata. Login/admin/cloud dan
UI10 tetap tertunda. [Jobdesk](JOBDESK.md) menjadi checklist implementasi;
audit ini tidak mencentang parent hanya karena subset tersedia.

## H. Gerbang Google Play

Ditinjau15November2026; penerapan akun/negara/jenislayanan harus dicek lagi
saat submit. Bukan nasihat hukum atau persetujuan Google.

| Pemeriksaan | Status | Alasan |
|---|---|---|
| Target API pengajuan baru | FAIL | target35, perlu36; tenggat perpanjangan1Nov2026 sudah lewat |
| AI reporting/safeguards | FAIL | Tidak ada alur laporan in-app yang terbukti; keselamatan model belum dievaluasi menyeluruh |
| Signed release/AAB/Play App Signing | BLOCKED | Signer/keputusan/paket final belum tersedia |
| Ukuran unduhan internal <100MB | PASS | Receipt kandidat mengukur APK; bukan ukuran download AAB hasil Play |
| DataSafety/SDK inventory/retensi | BLOCKED | Draf ada, belum dicocokkan seluruh akun/SDK aktual |
| Privacy URL publik dan in-app | BLOCKED | Draf ada; situs ditunda dan belum ada URL final aktif |
| Audience13+/wali/lokasi/kelayakan layanan | BLOCKED | J11 belum tuntas; rating bukan pengganti verifikasi usia |
| Exact alarm/FGS/notification declarations | NOT TESTED | Manifest ada, penerimaan deklarasi Play belum diuji |
| Hak konten/aset/audio dan listing akurat | BLOCKED | B01/hak sumber masih terbuka |
| Content rating/app access/ads declarations | NOT TESTED | Belum mengisi/menyerahkan Console |
| Closed testing untuk akun personal baru | NOT TESTED | Jenis/tanggal akun belum diverifikasi; jika berlaku12tester terus14hari |
| Login/cloud-account deletion | N/A | Fitur akun cloud belum diimplementasikan; penghapusan instalasi/data tetap perlu |
| Pre-launch report/Android Vitals | NOT TESTED | Belum ada hasil kandidat di Play |

Sumber primer:
[TargetAPI](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en),
[AI](https://support.google.com/googleplay/android-developer/answer/13985936),
[DataSafety](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en),
[privacy dan review](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en),
[testing akun personal](https://support.google.com/googleplay/android-developer/answer/14151465).
Ketentuan disarikan terbatas; baca sumber saat finalisasi.

## I. Infrastruktur dan model bisnis — asumsi, belum produk berbayar

Pertahankan backend/server-only key, identitas instalasi, batas harian dan lease
distribusi; kuota vendor bersama bukan kuota setiap pengguna. Free plan bukan
SLA kapasitas publik. Provider/model contoh env bukan bukti konfigurasi aktif.
Tidak mengaktifkan paket, model, pembayaran atau logging tambahan pada audit ini.

Dokumentasi [WorkersAI pricing](https://developers.cloudflare.com/workers-ai/platform/pricing/)
15November: alokasi10.000neurons/hari; penggunaan lebih memerlukan paid plan,
tarif acuan$0.011/1.000neurons di atas alokasi; model tertentu berbayar khusus.
Ini bukan angka biaya aktual akun Zikra. Vercel/Redis/FCM/egress/dukungan terpisah.

Rumus proyeksi: DAU×pesan/DAU×(tokeninput×tarifinput + tokenoutput×tarifoutput)
+gambar+hosting+storage+egress+monitoring+dukungan. Asumsi contoh20/100/1000DAU,
10pesan/hari menghasilkan200/1000/10000request; bukan bukti mampu menanganinya.
Belum ada pengukuran token/neurons perrequest yang membenarkan estimasi rupiah.

Usulan etis: Free mempertahankan bacaan dan rutinitas inti; Plus kelak bisa
menambah kuotaAI/cadangan pilihan setelah biaya dan pemulihan jelas. Jangan
paywall informasi keselamatan, menjual data ibadah/chat, memasang iklan berbasis
profil remaja atau menjanjikan AI unlimited. Tidak membuat fitur berbayar sekarang.

## J. Checkpoint Git

Stage hanya kode/tes/dokumen tugas dan bukti tersanitasi. WIPPROJECT_HANDOFF,
Infaq/XMLline-ending, stashaudio, sumberMP3, env, kunci, APK dan data pengguna
tetap di luar commit. Hash commit dan remote sesudahpush dicatat pada jawaban
checkpoint; jangan memasukkan hash commit sendiri secara sirkular ke dokumen.

## K. Urutan eksekusi berikutnya

1. Tuntaskan bukti PA06 dan konsistensi status/handoff; pertahankan HOLD.
2. PA03: siapkan SDK36/toolchain kompatibel lalu migrasi/uji behavior berurutan.
3. PA04: rancang alur laporan nyata dan minimisasi/retensi, lanjut implementasi
   lokal server/client sebelum deployment terpisah. Tidak menerima laporan nyata
   tanpa penerimaan/akses/triase yang siap.
4. J11 usia/wali/lokasi dan akses akun retensi; J04–J06 sumber/hak paralel secara
   alur kerja, bukan mengasumsikan izin diperoleh. Tidak mengirim email baru.
5. J12 signer/migrasi, satu sesiHPJ13 saat siap, lalu J14/beta20–50penguji.
6. J15 website sesuai arahan pengguna, kemudian Play setelah seluruh gate.

Perkiraan durasi baru layak diberikan sesudah akses/hak dan hasil regresi
tersedia. Tidak menjanjikan tanggal rilis dari jumlah fitur atau build sukses.

### Checkpoint PA03 — 15 November

Migrasi sumber ke compile/target 36, min 26, AGP 8.10.1, Gradle 8.11.1 dengan checksum distribusi. SDK platform 36 revisi 2 berhasil dipasang. Build awal APK aplikasi/instrumentasi dan tes JVM berhasil. Home menangani cutout samping. Layar BaseActivity lain memakai safe area bilah sistem dan keyboard di Android 16+. Lima layar memakai opt-out predictive back sementara untuk mempertahankan perilaku Kembali khusus. Versi kerja 1.9.53/code80. Kandidat publik tetap HOLD. Bukti runtime dicatat terpisah sesudah uji.

Rujukan: [kompatibilitas AGP](https://developer.android.com/build/releases/agp-8-10-0-release-notes), [perubahan Android 16](https://developer.android.com/about/versions/16/behavior-changes-16).

## Checkpoint 1.9.53

PA03 PARTIAL: compile/target36, AGP8.10.1/Gradle8.11.1 berhasil dibangun; lima nativeAPI36 lulus. Matriks lintas versi/OEM/tablet/landscape belum tuntas.
PA04 PARTIAL: UI dan server pelaporan sudah diimplementasikan,135backendtes lulus. Produksi masih nonaktif; triase/retensi/E2E terbuka. Tabel temuan sebelumnya mencatat keadaan awal audit52.
[Bukti dan batas](../../android/validation/api36-reporting-2026-11-15/README.md). HOLD.
