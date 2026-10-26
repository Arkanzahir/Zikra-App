# Operasional backend Zikra

26 Oktober2026. Jalur aktif Vercel Hobby + Upstash Free; tidak mengaktifkan paket
berbayar, logging payload, endpoint admin publik atau database baru.
[Bukti operasi dan batas C02](../../android/validation/backend-operations-c02-2026-10-26/README.md).

## Pemeriksaan kesehatan

```sh
node backend/operations-health.mjs https://zikra-ai.vercel.app
```

Probe memeriksa JSON liveness `/health` dan penolakan401 `/v1/capabilities` dengan
credential acak yang **tidak pernah diterbitkan**. Penolakan tersebut melalui
lookup Redis pada handler Vercel. Redis gagal menghasilkan503 sehingga health200
saja tidak menutupi gangguan registry. Tidak memerlukan key/token yang aktif,
tidak mengambil kuota chat/gambar dan tidak menjalankan inference. Output hanya
status, durasi, timestamp dan boolean; body/credential tidak dicetak. Redirect
ditolak, tiap probe/decode dibatasi10 detik dan4KiB.

Workflow `.github/workflows/backend-health.yml` menyiapkan pemeriksaan empat kali
sehari serta pemicu manual. Checkout dipin SHA, permission hanya contents:read,
credential Git tidak disimpan dan tidak ada secret GitHub yang dibutuhkan.
Checkout v7.0.1 dipin pada SHA resmi dan memakai Node24. Hanya berkas probe yang
di-checkout; push ke main yang mengubah workflow/probe ikut memicu pemeriksaan.
**Sudah aktif di default branch; pemicu manual berhasil26 Oktober2026.**
[Bukti job/probe](../../android/validation/backend-c02-cloud-2026-10-26/README.md);
invocation terjadwal belum diamati.
Jadwal bukan jaminan SLA/notifikasi; periksa hasil gagal melalui Actions sesuai
preferensi notifikasi akun. Probe tidak membuktikan kuota/model/provider siap.

Jika gagal: bedakan TLS/DNS/deployment, status503 Redis,401 yang berubah menjadi
200, dan body HTML/terlalu besar. Periksa status akun/platform, kuota dan env
production; jangan mematikan TLS, menambahkan shared token atau mengaktifkan
upgrade berbayar agar probe hijau. Setelah perbaikan, jalankan probe lagi.
Completion sintetis terpisah memakai `check-vercel.mjs --live` hanya bila perlu;
perintah itu benar-benar memakai satu percobaan provider.

## Cadangan registry terenkripsi

Cadangan ini untuk **UUID/hash token/status pencabutan akses**, bukan chat,
profil/tugas pengguna, paket Quran, provider key atau isi dokumen/gambar.
Snapshot memakai Lua atomik yang dibatasi100 instalasi/64 halaman SCAN per pola;
registry rusak/tidak berpasangan/terlalu besar ditolak, tidak dipotong diam-diam.
Jika skala melampaui batas, tinjau mekanisme snapshot dan batas transport64KiB
bersama-sama sebelum menaikkan kapasitas. Jangan menambahkan KEYS/scan tak terbatas.

Kunci acak256 bit dibuat oleh CLI. AES-256-GCM memakai nonce baru dan autentikasi
versi format. Berkas baru memakai exclusive create, tidak menimpa cadangan/kunci
lama. Path wajib absolut, di `.local/` yang di-ignore atau di luar repo; path
di source biasa ditolak. Kunci backup tidak dimasukkan ke environment Vercel/APK.
Simpan salinan kunci secara terpisah dari blob bila blob dipindahkan ke media lain.
Tanpa kunci yang benar, blob tidak bisa dipulihkan.

Contoh PowerShell dari root repo. Parent folder harus ada. `init-key` hanya sekali
untuk nama berkas yang belum ada; jangan mengganti kunci lama tanpa mempertahankan
kunci untuk semua cadangan yang masih diperlukan.

```powershell
New-Item -ItemType Directory -Force .local/operations-c02 | Out-Null
$taskBackupRoot=(Resolve-Path .local/operations-c02).Path
node backend/registry-backup-admin.mjs init-key "$taskBackupRoot/registry-key.env"
node --env-file=.local/vercel-production.env --env-file=.local/operations-c02/registry-key.env backend/registry-backup-admin.mjs backup "$taskBackupRoot/registry-NAMA-BARU.json"
node --env-file=.local/operations-c02/registry-key.env backend/registry-backup-admin.mjs inspect "$taskBackupRoot/registry-NAMA-BARU.json"
```

POSIX mode0600 bukan pengganti ACL Windows. Batasi folder/operator files kepada
akun operator, SYSTEM dan administrator; ACL direktori cadangan lokal pada bukti
26 Oktober sudah diperiksa. Jangan mengirim kunci melalui chat, argumen command,
Git, laporan atau bersama blob. Output inspect hanya ringkasan, tidak UUID/hash.

Kebijakan operator: ambil setelah penerbitan/pencabutan akses, sebelum perubahan
registry/deploy yang memengaruhinya, dan sekurangnya mingguan ketika layanan aktif.
Simpan cadangan mingguan selama30 hari; periksa decrypt/summary setiap pengambilan.
Ini prosedur manual, bukan janji scheduler backup aktif. Jangan menghapus file di
luar direktori cadangan milik operator, kunci yang masih diperlukan, atau arsip WIP.
Pemantauan, durabilitas penyedia dan cadangan operator adalah lapisan berbeda.

## Uji pemulihan dan insiden

```powershell
node --env-file=.local/vercel-production.env --env-file=.local/operations-c02/registry-key.env backend/registry-backup-admin.mjs restore "$taskBackupRoot/registry-NAMA-BARU.json" zikra:recovery:NAMESPACE-BARU:
```

Restore **menolak prefix live/source dan setiap namespace yang sudah berisi key**.
Backend tidak otomatis dipindahkan. Seluruh credential impor ditandai revoked,
termasuk yang masih aktif saat snapshot: backup lama tidak boleh menghidupkan
kembali token yang dicabut setelah snapshot. Kuota global gambar disegel sampai
hari UTC berganti agar pemulihan tidak membuka lagi jatah hari yang tidak pasti.
Counter/lease/nonce lama tidak direplay. Riwayat chat Android tetap lokal.

Saat insiden: validasi blob/kunci, pulihkan hanya ke namespace baru, periksa hasil,
tentukan akses baru melalui `redis-client-admin.mjs` setelah verifikasi operator,
dan tinjau sisa kuota provider sebelum mengaktifkan akses/inference. Perubahan
env/switchover deployment harus disengaja dan diuji; CLI ini tidak melakukannya.
Jika restore terputus, jangan retry ke target yang sama atau menghapus prefix live.
Target parsial tetap tidak memberi akses; pakai target baru dan tinjau sisa target
lama berdasarkan daftar key yang diketahui. Redis error di tengah script tidak
dianggap rollback semua write. Tidak memakai FLUSHDB/FLUSHALL.

Uji26 Oktober memakai Upstash nyata dengan namespace acak terpisah; credential
impor ditolak, credential produksi tetap valid, registry produksi sama dan key
uji dibersihkan. Ini membuktikan drill, bukan switchover bencana penuh.

## Retensi dan batas penyedia

- Kode gateway tidak menyimpan payload chat/lampiran/gambar atau menulisnya ke log.
  Registry/pencabutan Redis tidak memiliki TTL; usage TTL48 jam sejak pemakaian
  terakhir, nonce48 jam, lease55
  detik dengan key60 detik. Jangan menghapus registry untuk melewati kuota.
- [Vercel Hobby runtime logs](https://vercel.com/docs/logs/runtime) memiliki retensi
  satu jam. Konfigurasi deployment expiration yang dibaca26 Oktober adalah30 hari;
  deployment aktif/protected mengikuti aturan platform. Sampel log kosong tidak
  mengesahkan seluruh tracing/log platform. Jangan memasang drain/tracing payload
  atau logging Authorization/env. Tidak mengaktifkan Observability berbayar.
- [Featherless](https://featherless.ai/privacy) menyatakan tidak menyimpan prompt/
  completion, tetapi menyimpan metadata penggunaan. Ini pernyataan penyedia,
  bukan audit independen. [Workers AI](https://developers.cloudflare.com/workers-ai/platform/data-usage/)
  menyatakan retensi konten inferensi standar sebatas pemrosesan, dengan pengecualian
  fitur stateful/keamanan/legal dan metadata operasional; integrasi Zikra memakai
  REST langsung, tanpa AI Gateway logging/cache atau storage Cloudflare tambahan.
- [Backup platform Upstash](https://upstash.com/docs/redis/features/backup) berbeda
  dari logical backup di atas; restore platform dapat menghapus isi database target.
  Fitur/syarat paket harus diperiksa sebelum dipilih. Tidak menyalakan backup
  berbayar, memasukkan kartu atau menaikkan paket pada pekerjaan ini.

C02 tetap menunggu uji HP saat laptop mati. Monitor manual dan mekanisme timeout
35 detik dalam preview cloud sudah dibuktikan dengan batas fixture yang dijelaskan
pada laporan terbaru. Uji PC/backup/probe tidak menggantikan pemeriksaan itu.

Pembaruan26 Oktober setelah checkpoint ee2b751: [push dan workflow](../../android/validation/checkpoint-push-2026-10-26/README.md)
sudah diverifikasi di GitHub (state active); hasil job belum diverifikasi.
Status workflow lokal pada catatan sebelumnya bersifat historis. C02 tetap[ ].

Lanjutan26 Oktober: [monitor sukses dan timeout preview cloud](../../android/validation/backend-c02-cloud-2026-10-26/README.md).
Preview dan automation bypass baru CLI sudah dibersihkan; jangan menyuplai credential
production pada fixture atau menganggap fixture sebagai outage provider sebenarnya.

Konfigurasi Node24/sparse/pemicu push sudah lulus [run40277773046](../../android/validation/backend-c02-cloud-2026-10-26/monitor-push-run.json).
Uji HP/laptop mati tetap mengikuti [prosedur terpisah](../../android/validation/backend-c02-cloud-2026-10-26/PHONE_LAPTOP_OFF.md), belum dijalankan.
