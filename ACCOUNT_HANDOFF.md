## Checkpoint aktif1.9.53 —15 November2026

HOLD. API36/toolchain dan pelaporanAI lokal diimplementasikan. [Bukti/batas](android/validation/api36-reporting-2026-11-15/README.md). PA03/PA04 PARTIAL. Produksi laporan belum aktif; HP tetap1.9.48. Berikutnya kompatibilitas lintas versi, triase/retensi dan E2E laporan. Bagian sebelumnya merupakan riwayat.

# Mulai di sini — handoff akun GPT

Checkpoint terbaru: **15 November 2026, kandidat internal 1.9.52/code79, HOLD**.
Dokumen ini menggantikan ringkasan versi lama di percakapan. Akun baru tidak
mewarisi chat, memori, koneksi layanan atau file privat akun sebelumnya.

## 1. Baca sebelum bekerja

1. AGENTS.md dan dokumen ini.
2. SESSION_HANDOFF.md, IMPLEMENTATION_PLAN.md, RELEASE_SCOPE.md.
3. docs/beta/JOBDESK.md dan docs/beta/STATUS.json.
4. android/validation/audio-map-2026-11-14/README.md; receipt1.9.47 tetap historis.
5. docs/design/APP_GUIDE.md, docs/beta/OPERATOR_RUNBOOK.md,
   docs/beta/PRIVACY_DRAFT.md dan docs/beta/SERVICES.md.

Bagian paling baru mengoreksi catatan historis, bukan menghapus batas bukti.
Baseline1.7.6/commit83312da sudah historis; jangan reset ke sana. Jangan menganggap
A–D/E/F/G selesai seluruhnya atau membuka unduhan hanya karena APK berhasil build.

## 2. Workspace dan keputusan pengguna

Repo: D:/Download/Zikra-App-main/Zikra-App-main. Folder induk hanya wrapper/output.
Remote: https://github.com/Arkanzahir/Zikra-App, branch main. Baca HEAD/status
sebelum mulai. Jangan hard-reset, clean massal atau pull menimpa WIP.

Selalu gunakan caveman, bahasa Indonesia ringkas. Skill lokal berada di
C:/Users/Lenovo/.codex/skills/caveman/SKILL.md; frontend-design untuk pekerjaan UI.
Pengguna meminta pekerjaan tuntas berdasarkan prioritas dan dokumentasi jelas,
audit/commit/push setiap checkpoint, hemat limit. Jangan hanya membuat rencana
atau mengulang tes yang sudah lulus tanpa perubahan/alasan baru.

Pertahankan desain/logo/ikon diterima, data pengguna dan konfigurasi privat.
Unduhan awal <100.000.000 byte. Tidak mengaktifkan biaya/billing, menerima
ketentuan, mengirim email/pesan, atau menghapus aplikasi pengguna diam-diam.
HP dipakai14November untuk update/smoke1.9.48; jangan restart tanpa koordinasi.
Pengguna meminta tidak mengulang adzan yang sudah dianggap aman; tidak ada uji
adzan baru pada sesi14November dan gapnya tidak diberi status lulus palsu.

Login Google, backup/sinkronisasi cloud, dashboard admin (F05–F08), mode orang
tua dan sorotan Quran UI10 ditunda. Jangan menambah fitur besar ini sekarang.

Nama penerbit publik **Zikra**, dikonfirmasi14November; email dukungan tetap
arkanzahir46@gmail.com. Ini tidak menutup retensi/usia atau mengizinkan publikasi.

## 3. Kandidat dan bukti terkini

Kandidat internal **1.9.52/code79**, tetap debug/HOLD. Bukti aktif:
[receipt dan batas](android/validation/product-audit-2026-11-15/README.md),
[audit produk](docs/beta/PRODUCT_AUDIT.md), [status](docs/beta/STATUS.json).
APK: D:/Download/Zikra-App-main/Zikra-Privacy-v1.9.52-beta-debug.apk.
49.820.578byte; SHA256 4c35b8617af581609ee70e439d5c1ec94624466056561762d7a756988d624035.
235JVM/0skip,lint0error/894warning. Native kandidat mengikuti receipt aktif;
jangan menjumlahkan tes versi lama. Backend tidak berubah,134tes14November
adalah runterdahulu. HP tetap1.9.48, bukan kandidat52.

Perubahan52: label dock pada320dp/teks200%, cleanup salinan PDF renderer dan
konsolidasi handoff; tidak menghapus PDF asli/chat/lampiran tersimpan.
Riwayat penting: [51 consent](android/validation/privacy-consent-2026-11-14/README.md),
[50 backup](android/validation/local-migration-2026-11-14/README.md),
[49 Quran](android/validation/quran-manifest-2026-11-14/README.md),
[48 HP](android/validation/phone-ops-2026-11-14/README.md).
Backup lokal core/lintas signer fixture sudah tersedia; jangan mengerjakan ulang.
Restore Android9+, SAF/HP dan signer final masih belum tuntas. Data HP42baris/
17tabel terjaga pada update48; bukti itu tidak mengesahkan kandidat52.

## 4. Sistem yang sudah tersedia

- Android native Java/XML di android; backend Node di backend.
- AI online melalui https://zikra-ai.vercel.app ke Cloudflare Workers AI.
  Mode offline adalah aturan lokal; riwayat lokal bukan RAG/ingatan cloud permanen.
  Foto/PDF/DOCX/TXT terbatas sesuai UI/draf privasi. Registry/kuota instalasi Upstash;
  provider key server-only. Featherless legacy tidak aktif untuk chat produksi.
  Contoh model pada backend/.env.vercel.example: @cf/qwen/qwen3.8-27b. Contoh bukan
  bukti model produksi saat ini; periksa metadata konfigurasi privat bila perlu.
- Quran Content Sync: mushafs1/quran_core1/translations33,114surah,6236ayat,
  83665kata,604halaman. Cache server AES256GCM, TTL8hari/refresh6hari.
  Endpoint /v1/quran terautentikasi, kuota terpisah. QF credential hanya server.
  Native QuranSyncGateway/Store/Repository/DownloadService memakai staging dan
  publikasi chapter atomik. Font CDN, timing metadata dan audio adalah jalur berbeda.
- Hosted native reader/Mushaf/locate/token salah/pemulihan berhasil; font CDN asli
  p1/nama surah/basmalah berhasil lewat MushafRepository. Fixture lengkap menguji
  mapping/publish/failure/recovery. Belum semua604font/qari/audio/timing/proses mati.
- Jadwal salat mengikuti tanggal/lokasi/zona, sumber regional/cache dan fallback
  hitung berlabel. Jangan paksa offset2menit global agar sama Google atau mengganti
  atribusi upstream menjadi resmi Kemenag tanpa bukti. Lihat source/receipt jadwal.
- Cuaca Open-Meteo; gempa laporan BMKG. Poll Cloudflare cron5menit→Vercel→FCM
  bagi pengguna opt-in, bukan early warning. Jangan broadcast ulang sebagai tes.
- E03:pengingat defaultoff, persetujuan cuaca memakai lokasi tersimpan tanpa GPS,
  JobScheduler15menit best-effort, journal/dedupe/cooldown/revision dan kontrol izin.
  Cuaca live kota default/cache serta penolakan stale work/hari/lokasi/izin teruji;
  pengiriman cuaca latar belakang dalam Doze/HP belum selesai.
- E04:dialer/draf tanpa submit, Maps web→geo→dialog salin. OSM/Overpass tetap;
  pengguna menolak billing Google Maps SDK. Intent/nomor/URL/fallback teruji dengan
  monitor yang mencegat; aplikasi penerima eksternal belum diuji menyeluruh.
- F:tema global, dock AI/keyboard/draf, lampu siang-malam, splash cold/warm,
  reduced motion, foreground/viewport/power/sensor guard tersedia.32kasus native mencakup
  smoke16tampilan, bukan persetujuan visual semua piksel atau baterai HP.
- Website statis di website/dist: fitur,FAQ,dukungan,drafprivasi,aset lokal.
  Desktop dan390CSSpx tanpa overflow diuji; tombol download disabled dan tidak
  ada URL APK. Belum hosting/publikasi. Kontak publik: arkanzahir46@gmail.com.

## 5. Pekerjaan berikutnya, bukan mulai dari nol

Baca urutan dan matriks [PRODUCT_AUDIT](docs/beta/PRODUCT_AUDIT.md).
1. J11 usia/wali/lokasi remaja dan retensi akun belum final. Pemeriksaan GET
   Vercel15November mendapat403, browser meminta login; jangan mengklaim paket/
   retensi akun terverifikasi. Permenkomdigi9/2026 perlu penelaahan teks final.
2. PA03: migrasi compile/target35 ke36 dan toolchain yang cocok + behavior
   Android16 sebelum Play; tidak mengganti angka lalu mengklaim kompatibel.
3. PA04: pelaporan konten AI in-app belum tersedia. Siapkan alur minimal,
   persetujuan/retensi/triase sebelum implementasi/deploy; jangan mengirim
   percakapan otomatis atau menganggap email eksternal sudah memenuhi syarat.
4. J03 font/audio/timing/hak dan matriks kegagalan yang belum tercakup; J02
   teknis sudah ditutup. ENOSPC/power-loss nyata belum dibuktikan.
5. J04–J06 sumber/hak, B01 dua kitab lengkap menunggu Gading; HadeethEnc bukan
   pengganti. Jangan mengirim email baru atau mengklaim lisensi tanpa bukti.
6. J12 signer tetap dan uji cadangan pada paket final; core backup50 sudah diuji.
   Jangan uninstall HP. J13 satu sesi HP terkoordinasi sesuai gap penting,
   tidak mengulang adzan/reboot identik tanpa perubahan atau alasan baru.
7. J14 keputusan beta20–50penguji sesudah gate; baru J15 website/unduhanHTTPS.
   Website tetap ditunda; preview48 dibekukan dan bukan manifest kandidat52.

Login Google/cloud/adminF05–F08,mode orang tua,UI10 tetap ditunda.

## 6. WIP, file lokal dan rahasia

Jangan stage PROJECT_HANDOFF.md, InfaqActivity.java, XML values yang berubah
line ending, desain lama untracked, crashlog atau __pycache__. Diff Infaq saat
checkpoint ini tidak menunjukkan perubahan logika setelah whitespace diabaikan.
PROJECT_HANDOFF adalah WIP pengguna dan tetap terpisah. Stage path eksplisit saja.

PROJECT_HANDOFF SHA256:
e6df1b9dfc227883098e6db19ea7413cac6206a61fa6834eeb1bc2a7fae63650.
Stash audio35686be384ac549e5a5bf85be87cfe56e3b667df: jangan apply/drop.
Arsip .local/pending-audio-a03-2026-10-24/source-wip.zip SHA256:
f2b905b613a5083a6f9bd3e548d8bcb661a458bfc143302cb116c7a06984724f.
Logo design/branding/zikra-logo-primary.jpg dan foreground Home diterima tetap.

Rahasia hanya di .local: featherless.env,vercel-client.env,vercel-production.env,
cloudflare.env,cloudflare-cron.env,earthquake-cron.env,firebase-server.json,
quran-foundation.env,quran-sync.env. Jangan cetak nilainya, upload atau masuk Git/APK.
Baseline tujuh konfigurasi: .local/beta-release-20261112/private-hashes.json.
GitHub tidak memuat file privat, APK, MP3, signer atau akses akun layanan.

Pada laptop yang sama, file lokal tetap tersedia saat ganti akun GPT. Pada mesin
lain, pemilik perlu memindahkan file privat secara aman. Login GPT baru tidak
otomatis menghubungkan GitHub/Drive/Cloudflare/Vercel atau membawa chat lama.

## 7. Lingkungan dan verifikasi

PowerShell;Node D:/node.exe;Java C:/Users/Lenovo/.jdks/jbr-17.0.14;
SDK E:/AndroidSDK;Python:
C:/Users/Lenovo/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe.
Git memakai -c safe.directory=D:/Download/Zikra-App-main/Zikra-App-main.

Gradle selalu memakai kedua properti audio berikut:
-PzikraAdhanAudio=D:/Download/Zikra-App-main/Zikra-App-main/.local/d02-audio-20261026/selected/adhan_misyari_user.mp3
-PzikraFajrAudio=D:/Download/Zikra-App-main/Zikra-App-main/.local/d02-audio-20261026/selected/adhan_fajr_user.mp3

Tanpa ini paket bisa memakai audio berbeda. JAVA_HOME/ANDROID_HOME seperti di atas.
Fixture JVM ZIKRA_HADEETHENC_FIXTURE menunjuk
.local/hadeethenc-audit-20261112/HadeethEnc.com_id-v1.22.0.csv.
Fixture Redis: ZIKRA_REDIS_TEST_PYTHON menunjuk Python runtime di atas;
PYTHONPATH=.local/vercel-test-venv/Lib/site-packages. Executable Scripts/python.exe
venv lama rusak; jangan dipakai. GRADLE_USER_HOME=E:/.gradle.
Perintah build: ./android/gradlew.bat -p android --no-daemon --console=plain
[dua properti audio] testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug.

Audit staged: D:/node.exe .local/quran-sync-integration/audit-staged.cjs.
Auditor APK tools/audit_beta_release.py memakai output eksklusif: nama baru setiap
run; debug menghasilkan HOLD/exit1, jangan menganggap exit0 otomatis siap rilis.
Manifest tools/prepare_beta_manifest.py juga hanya HOLD. Website: D:/node.exe website/check.cjs.

Node HTTPS lokal pernah timeout meski Windows HTTPS berhasil. Adapter privat
.local/quran-sync-integration/windows-fetch.mjs tersedia untuk uji; jangan
melemahkan TLS. Android37 dapat memblokir UID instrumentasi tanpa Activity
foreground: tunggu fokus/validasi jaringan, jangan mengubah firewall untuk meluluskan.

Native test hanya pada emulator disposable, serial eksplisit. Fixture/data pribadi
jangan dipaketkan ke testAPK. QuranSyncIntegrationTest bagian Z membutuhkan store
chapter2 belum ada; gunakan emulator baru, bukan menghapus data pengguna.
Host test perlu identitas sintetis privat, TTL, cleanup key sendiri dan finally.
.local/native-hosted-20261113/access.mjs menyediakan lifecycle untuk sesi terakhir.
Dua identitas sesi ini sudah dihapus dan EXISTS0 diverifikasi. Satu run historis
sebelum IDpersist mungkin menyisakan identitas takteridentifikasi; jangan scan/delete
seluruh registry atau mengklaim semua registry bersih. Emulator/server task sudah
dihentikan dan data task dibersihkan; jangan mencoba path AVD lama tanpa membuat
emulator disposable baru. Screenshot/log/ringkasan final tersedia di Git.

## Prompt siap salin untuk akun baru

Lanjutkan Zikra di D:/Download/Zikra-App-main/Zikra-App-main. Selalu gunakan caveman.
Baca ACCOUNT_HANDOFF.md, AGENTS.md, SESSION_HANDOFF.md, IMPLEMENTATION_PLAN.md,
RELEASE_SCOPE.md dan docs/beta/JOBDESK.md. Kandidat internal1.9.52 masih HOLD;
bukti terbaru ada di android/validation/product-audit-2026-11-15/README.md.
Baca docs/beta/PRODUCT_AUDIT.md untuk audit dan urutan prioritas.
Jangan ulang proyek atau menganggap A–D/E/F/G selesai. Pertahankan WIP, stash audio,
data pengguna, desain diterima dan konfigurasi privat. Lanjutkan gap terdekat
sesuai handoff; HP hanya saat siap. Audit, commit dan push perubahan milik tugas.

Uji ketahanan terbaru memakai tools/verify_quran_process_recovery.ps1, hanya emulator disposable. Jangan jalankan prepare/recovery sebagai suite biasa; baca android/validation/quran-failure-2026-11-14/README.md. APK utama tetap sama; tidak ada versi baru hanya untuk test.
