# Mulai di sini — pindah akun GPT

Checkpoint 13 November 2026. Ini ringkasan operasional terbaru; baca sebelum bagian
historis di dokumen besar. Akun GPT baru tidak otomatis menerima percakapan,
otorisasi akun layanan, berkas privat atau memori akun lama.

## Urutan baca

1. `AGENTS.md` dan dokumen ini.
2. `SESSION_HANDOFF.md`, `IMPLEMENTATION_PLAN.md`, `RELEASE_SCOPE.md`.
3. `docs/beta/JOBDESK.md`, `docs/beta/FINAL_BATCH.md`.
4. `android/validation/final-batch-2026-11-13/README.md` dan
   `android/validation/quran-sync-integration/README.md`.
5. `docs/design/APP_GUIDE.md`, `docs/beta/OPERATOR_RUNBOOK.md`,
   `docs/beta/PRIVACY_DRAFT.md`, `docs/beta/SERVICES.md`.

Catatan paling atas yang lebih baru mengoreksi status historis, bukan berarti
hasil lama berlaku otomatis untuk kandidat baru. Checklist hanya dicentang jika
semua kriterianya berbukti. Baseline 1.7.6/83312da sudah historis; jangan reset ke sana.

## Workspace dan aturan pengguna

- Root repo sebenarnya `D:/Download/Zikra-App-main/Zikra-App-main`;
  folder induknya hanya wrapper/output APK. Remote GitHub `Arkanzahir/Zikra-App`,
  branch `main`. Periksa HEAD/remote saat mulai; jangan hard-reset/pull menimpa WIP.
- Selalu gunakan caveman, komunikasi Indonesia ringkas. Skill lokal
  `C:/Users/Lenovo/.codex/skills/caveman/SKILL.md`; frontend-design untuk UI.
- Pengguna meminta selesaikan berdasarkan prioritas, hemat limit, perubahan
  diaudit/commit/push. Jangan minta ulang keputusan yang sudah jelas. Uji HP
  hanya ketika perangkat siap dan penggunaan terkoordinasi.
- Tidak memulai ulang proyek, menghapus data pengguna, mengunggah rahasia,
  mengganti desain disetujui, menerima ketentuan/biaya atau mengirim email diam-diam.
- Unduhan awal wajib <100.000.000byte. Paket debug tidak boleh jadi rilis publik.
- Login Google/sync/backup cloud/admin (F05–F08), mode orang tua dan sorotan Quran
  UI10 ditunda. Jangan menambahnya untuk menghindari pekerjaan rilis inti.

## Kandidat dan bukti

- APK internal 1.9.46 / code 73:
  `D:/Download/Zikra-App-main/Zikra-Polish-v1.9.46-beta-debug.apk`.
  49.511.880byte;SHA256
  `bc52f34baa7a64fdd5783cd0ecd8ac302f0a00ecbe19235325b2e8a7dd930fc0`.
- Audit `android/validation/ui-f-code-2026-11-13/apk-audit.json`: tanda tangan
  debug,private entries0/credential dikenal0,backup/cleartext nonaktif. Bukan
  jaminan absolut keamanan atau persetujuan distribusi.
- Sesi gabungan terbaru: backend125 lulus;JVM200 lulus/4 skip;native9 lulus;
  lint0 error/891 warning.12 screenshot enam layar ×terang/gelap. Scope/gap ada di receipt.
- HP terakhir yang terbukti pada checkpoint sebelum sesi ini1.9.39. Sesi ini
  tidak memakai HP. Jangan mengklaim1.9.46 telah diuji fisik atau dipasang pengguna.
- Tidak membuat versi baru hanya untuk dokumen,website atau test-only.

## Arsitektur dan implementasi yang sudah ada

- Android native Java/XML di `android`; data tugas/profil/hafalan/chat lokal,
  token akses dilindungi Keystore. AI online backend HTTPS Vercel,
  `https://zikra-ai.vercel.app`,provider Cloudflare Workers AI. Mode offline
  rule-based, bukan LLM lokal. Riwayat lokal bukan memori cloud/RAG permanen.
- Contoh konfigurasi model pada `backend/.env.vercel.example` memakai
  `@cf/qwen/qwen3.8-27b`; jangan menganggap contoh membuktikan model produksi
  aktif tanpa pemeriksaan metadata konfigurasi privat. Featherless legacy saja.
- Gateway akses instalasi/kuota via Upstash;credential provider server-only.
  Poll gempa Cloudflare cron 5 menit→backend→BMKG→FCM opt-in; bukan earlywarning.
- Content SyncQuran: 3 group kanonis,mushaf1/quran_core1/translations33,
  114 surah / 6236 ayat / 83665 kata/604 halaman. CacheRedis AES256GCM staging+pointer,
  TTL 8 hari / refresh 6 hari. `/v1/quran` reader/Mushaf/locate terautentikasi danquota
  terpisah. Hosted200/401/429/revoke pernah teruji; detail receipt.
- Android `QuranSyncGateway / Store`,Repository danDownloadService memakai staging
  chapter lengkap/atomic,mempertahankan generasi lama danguardcancel/storage.
  `MushafFontCache` validasi sebelumpublish,retensi file lama saat outage,
  AccessDenied tidak disamarkan. Font/audio tetap unduhan terpisah.
- Jadwal salat mengikuti lokasi,zona,tanggal,sumber regional/cache dan fallback
  hitung berlabel. Jangan paksa selisih2menit global agar sama pencarianGoogle.
  Lihat implementasi/receipt sumber saat menjelaskan; jangan ganti atribusi
  upstream menjadi “resmi Kemenag” tanpa bukti hubungan sumber.
- E03: empat pengingat default off,cuaca perlu persetujuan lokasi tersimpan,
  JobScheduler 15 menit best-effort,day dedupe/cooldown/revision,tidak menyalakan GPS.
- E04: nomor darurat membuka dialer,draf keluarga tanpaGPS/kirim otomatis;
  peta OSM / Overpass tetap,Google Maps hanya web / geo / fallback manual. Billing ditolak
  pengguna; jangan aktifkan Maps SDK berbayar.
- F01 panduan desain danF04 guard motion/foreground/viewport/power/screen/sensor
  dipaketkan. DockAI,splash,lampu malam dan aset diterima dipertahankan. Semua
  layar/power/200%font belum tuntas diverifikasi.
- `website/dist`:landing,fitur,FAQ,dukungan,draf privasi,aset lokal asli.
  DesktopChrome/FAQ/privasi diuji;mobile belum. Download disabled, HOLD, tanpa URL APK.
  Belum hosting/publikasi. `D:/node.exe website/check.cjs` untukcek lokal.

## Sisa, urut konkret

1. Lanjutkan gap J02/J03 terarah: gateway Android→hosted,update data terisi,
   font/Mushaf/audio danoffline/failure. Hindari mengulang seluruhtes yang sudah
   lulus tanpa perubahan. Testfresh-only Quran terakhir tidak aman direrun utuh
   pada store yang sudah diisi; buat emulator disposable baru, bukan hapus data pengguna.
2. J08/J09/F02–F04:cuaca live, ganti lokasi, stop job, permission danfallbackmaps /
   emergency draft;teks200%,keyboard,scroll/power. Jangan benar-benar menelepon,
   mengirimSMS/broadcast atau memakai berkas pribadi untuk bukti.
3. J04–J12: B01 Gading belum memberi klarifikasi dua kitab lengkap;HadeethEnc 2260
   opsional bukan pengganti diam-diam. Hak dua audio/penelaahanlafaz,penerbit/
   retensi / proses penghapusan dan signer release / migrasi masih terbuka. Pengguna menunda
   pembahasan izin, bukan berarti izin sudah diperoleh. Email dukungan publik
   yang diizinkan: `arkanzahir46@gmail.com`; identitas penerbit final belum diputuskan.
4. Satu sesi HP J13 pada hash kandidat sama: update tanpa data hilang,AI online
   independen laptop,sisa adzan idle / power,smoke lintas fitur. Koordinasikan restart.
   Jangan menyimpulkan ujiHP lama menutup kandidat baru.
5. J14 putusan beta,kemudian J15 rilis website / download HTTPS + install / update nyata.
   Jangan uploadAPKdebug atau promo sebelumgerbang. Promo visual disetujui;
   hak voice-over / komersial belum ditutup. Parent A–D/E/F/G belumselesai seluruhnya.

## Lindungi WIP dan konfigurasi

- Jangan stage `PROJECT_HANDOFF.md` (WIP pengguna), `InfaqActivity.java`,
  banyak res/valuesXML yang hanya berubah lineending,design lama tidak dilacak,
  crashlogs atau `__pycache__`. Selalu stage path eksplisit milik tugas.
- PROJECT_HANDOFF SHA256
  `e6df1b9dfc227883098e6db19ea7413cac6206a61fa6834eeb1bc2a7fae63650`.
  Stashaudio `35686be384ac549e5a5bf85be87cfe56e3b667df` janganapply/drop.
  Arsip `.local/pending-audio-a03-2026-10-24/source-wip.zip`,SHA256
  `f2b905b613a5083a6f9bd3e548d8bcb661a458bfc143302cb116c7a06984724f`.
- Rahasia hanya di `.local`:featherless.env,vercel-client.env,
  vercel-production.env,cloudflare.env,cloudflare-cron.env,earthquake-cron.env,
  firebase-server.json,quran-foundation.env,quran-sync.env. Jangan cetak isi,
  commit,APK,website,atau kirim ke chat. Baselinehashprivat
  `.local/beta-release-20261112/private-hashes.json`; bukan berkas untuk diunggah.
- Logo diterima `design/branding/zikra-logo-primary.jpg`,SHA256
  `7fa6744a626bbb27e1d944c18090730a64d37658a0e8f5aaea5212f14d52d963`;
  foreground Home SHA256 `1fedfc7a3eab18615822cb95a153293c325900ff7fd51984c3033ce234e57a90`.
- GitHub tidak membawa `.local`,APK,MP3,signerprivat atau akses layanan. Pada
  komputer yang sama filelokal tetap ada; komputerbaru membutuhkan pemindahan
  privat oleh pemilik. Jangan menganggap login GPT baru menghubungkanGitHub/
  Drive/Cloudflare/Vercel otomatis.

## Lingkungan dan perintah penting

PowerShell;Node `D:/node.exe`,Java `C:/Users/Lenovo/.jdks/jbr-17.0.14`,
AndroidSDK `E:/AndroidSDK`,Python
`C:/Users/Lenovo/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe`.
Git memakai `-c safe.directory=D:/Download/Zikra-App-main/Zikra-App-main`.

Build Gradle memerlukan properti audio lokal TERPILIH:
`-PzikraAdhanAudio=D:/Download/Zikra-App-main/Zikra-App-main/.local/d02-audio-20261026/selected/adhan_misyari_user.mp3`
dan `-PzikraFajrAudio=D:/Download/Zikra-App-main/Zikra-App-main/.local/d02-audio-20261026/selected/adhan_fajr_user.mp3`.
Tanpa properti ini paket bisa memakai audio berbeda. Cache Gradle E perlu izin
lingkungan. Tidak ada env signing release dikonfigurasi pada sesi ini.

Backend test fixture Python `.local/vercel-test-venv/Scripts/python.exe` via
`ZIKRA_REDIS_TEST_PYTHON`. JVM fixture `ZIKRA_HADEETHENC_FIXTURE` menunjuk
`.local/hadeethenc-audit-20261112/HadeethEnc.com_id-v1.22.0.csv`.
Selalu sebutskip; jangan menyembunyikannya.

Node fetch lokal pernah TCP timeout Vercel / Redis meskiWindows HTTPS berhasil.
Adapter uji privat `.local/quran-sync-integration/windows-fetch.mjs` dan
`hosted-proof.mjs` tersedia; jangan melemahkan TLS atau menganggap produksi mati.
Jangan hapus seluruh registry Redis. Uji hanya identitas sintetis diketahui,
simpan ID privat sebelum issue, TTL dan finally cleanup. Satu run awal gagal sebelum
ID persist mungkin menyisakan identitas tak teridentifikasi; jangan klaim seluruh
registry bersih tanpa bukti.

Audit staged `.local/quran-sync-integration/audit-staged.cjs` memeriksa nilai
credential dikenal / path privat; jalankan sebelum commit. `tools/audit_beta_release.py`
output eksklusif: pakai nama file baru; debug menghasilkan status HOLD / exit 1 yang diharapkan.
`tools/prepare_beta_manifest.py` hanya HOLD, bukan persetujuan rilis.

## Prompt akun baru (salin)

Lanjutkan Zikra di D:\Download\Zikra-App-main\Zikra-App-main. Selalu gunakan
skill caveman. Baca ACCOUNT_HANDOFF.md, AGENTS.md, SESSION_HANDOFF.md,
IMPLEMENTATION_PLAN.md, RELEASE_SCOPE.md dan docs/beta/JOBDESK.md terlebih dahulu.
Kandidat internal1.9.46 masih HOLD; hasil terbaru dan gap ada di
android/validation/final-batch-2026-11-13/README.md. Website sudah pratinjau lokal,
belum publik. Jangan ulang proyek atau menutup A–D/E/F/G tanpa bukti. Pertahankan
WIP PROJECT_HANDOFF,stash audio,data pengguna, desain disetujui dan konfigurasi
privat. Lanjutkan gap terdekat sesuai urutan handoff; ujiHP hanya ketika siap.
Audit,commit danpush perubahan milik tugas; jangan masukkan rahasia ke Git/APK.
