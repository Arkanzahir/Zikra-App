# C02 — operasi backend, cadangan dan monitor

26 Oktober2026. **C02 tetap[ ]**, dengan bagian operasional di bawah telah
dibuktikan. Deployment/runtime Vercel, provider key, token aktif, resource database,
paket/billing dan data HP tetap. Drill memakai namespace uji yang dibersihkan;
uji cancel/retry memakai dua percobaan aplikasi. APK tetap1.9.7/code34,37.218.400 byte.

## Hasil

- `operations-health.mjs` memeriksa HTTPS liveness dan lookup Redis yang menolak
  credential acak/tidak terdaftar. Tidak memakai token aktif, kuota aplikasi atau
  inference. JSON/status/durasi saja, tanpa payload/header/credential di output;
  response4KiB/deadline10 detik/redirect ditolak. [Probe deployment nyata](health-live.json)
  memberi health200 dan penolakan401 melalui handler Vercel/Redis produksi.
- Workflow GitHub memakai checkout dipin SHA, permission contents:read dan tanpa
  secret. Empat kali sehari/pemicu manual **baru aktif setelah perubahan masuk
  default branch dan Actions diaktifkan**. Saat laporan ini dibuat masih lokal;
  bukan monitor yang telah berjalan. Tidak membuka endpoint admin baru.
- `registry-backup.mjs`/CLI memisahkan snapshot atomik100 instalasi, validasi schema,
  AES-256-GCM dengan nonce baru, dan restore konservatif. Output privat wajib
  absolut di `.local/` atau di luar repo, exclusive create/tanpa overwrite.
  Key256 bit baru khusus backup, tidak mengganti key provider/token pengguna.
  Snapshot hanya UUID/hash token/revoked; tanpa chat/raw token/kuota/lease/berkas.
- [Drill Upstash nyata](registry-restore-live.json): blob408 byte terenkripsi
  berhasil diautentikasi dan dipulihkan ke namespace acak terpisah. Credential
  impor ditolak, credential produksi tetap valid, registry asli sama. Kuota gambar
  target disegel untuk hari UTC berjalan. Key uji target dibersihkan dan backend
  tidak dialihkan. Blob/key/namespace/UUID/hash tetap privat, tidak di Git.
- Restore menolak source/live prefix serta target tidak kosong, mengimpor seluruh
  credential sebagai revoked agar backup lama tidak menghidupkan akses yang sudah
  dicabut. Batas global gambar berasal dari konstanta yang sama dengan layanan
  gambar. Tidak memakai FLUSHDB/FLUSHALL atau replay counter harian lama.
- [Cancel cloud nyata](cancel-live.json): lease terlihat, client fetch benar-benar
  di-abort dan slot teramati lepas dalam325ms, jauh sebelum expiry55 detik. Retry
  eksplisit dengan body malformed mendapat400 dan tidak mengulang inference.
  Permintaan pertama mungkin sudah memakai provider; tidak menjamin tagihan/
  komputasi provider ikut dibatalkan. Prompt teknis sintetis, tanpa konteks pribadi.
- [API metadata](platform-audit.json) mengonfirmasi Vercel Hobby/Node24.x,
  Upstash Free, autoUpgrade/eviction/prodPack false dan deployment expiration30 hari.
  [Target credential](environment-targets.json) production-only; tidak menyalin
  nilai credential. Proyek Vercel tidak terhubung ke Git; deploy dilakukan CLI.
- Runtime log query10 menit berhasil dengan **0 record runtime**. Satu baris
  stdout adalah summary CLI, bukan satu log aplikasi. Nol kecocokan secret pada
  sampel kosong bukan sertifikasi seluruh tracing/retensi/log penyedia.
  Kebijakan [Vercel Hobby](https://vercel.com/docs/logs/runtime) menyatakan satu jam
  runtime logs; metadata deployment expiration30 hari dibaca dari API aktual.

## Verifikasi dan perlindungan

[58 tes backend](backend-tests.txt) lulus,0 gagal/skip pada Node22.17.0 lokal;
Vercel tetap24.x. Tambahan5 metode monitor dan6 metode backup mencakup deadline
body, origin/redirect/ukuran, no inference, AES tamper/wrong key, schema/batas,
registry rusak/tidak berpasangan, penolakan live/occupied target, stale revocation,
kuota gambar terkunci, CLI output privat dan no overwrite. Delapan metode Vercel
lama memakai helper fixture Redis bersama yang diekstrak agar tidak diduplikasi.
Lua asli dieksekusi fakeredis/Lupa lokal, terpisah dari drill Redis nyata di atas.
Tes sementara hanya menghapus direktori temp yang dibuat sendiri setelah target
absolut/parent/nama diperiksa.

[Audit source/history/APK](audit.json) memakai nilai rahasia yang diketahui;
bukan klaim seluruh jenis secret historis sudah terdeteksi. Backup terenkripsi
dan key tidak termasuk bukti publik. ACL folder operator baru dibatasi ke akun
operator/SYSTEM/admin, tanpa mengubah ACL konfigurasi privat lama. WIP
PROJECT_HANDOFF.md, env lama, stash/arsip audio, logo/aset disetujui dan APK lama
dipertahankan. Admin/monitor/tes tidak diizinkan oleh allowlist deployment.

CLI backup/inspect serta snapshot/restore produksi sudah diuji; tidak menyalakan
backup terkelola berbayar atau otomatis menghapus cadangan/deployment. Kebijakan
cadangan operator mingguan/30 hari bersifat manual. Ini backup registry akses di
luar layanan Redis, bukan backup/sinkronisasi chat HP. Lihat
[runbook](../../../backend/deploy/OPERATIONS.md) untuk pemulihan dan batas.

## Gerbang yang tersisa

- Aktifkan dan buktikan hasil workflow monitor di GitHub; kode lokal saja belum.
- Timeout provider/cloud panjang belum dipaksakan. Client deadline, local handler
  timeout dan abort cloud adalah bukti yang berbeda; jangan disamakan.
- HP saat laptop mati, serta C06 mikrofon/C07 kamera/picker fisik belum tersedia.
  Pengguna memilih lanjut pekerjaan tanpa HP; tidak menandai kasus itu lulus.
- C04 mengikuti jadwal D04. B01, D02a, UI04/F/G dan rilis publik tetap terbuka.

[Hash source operator](source-files.json) mengikat kode/tool/konfigurasi workflow;
runtime cloud tetap deployment C09 sebelumnya, tidak ada redeploy pada pekerjaan ini.

Pembaruan26 Oktober setelah checkpoint ee2b751: [push dan workflow](../checkpoint-push-2026-10-26/README.md)
sudah diverifikasi di GitHub (state active); hasil job belum diverifikasi.
Status workflow lokal pada catatan sebelumnya bersifat historis. C02 tetap[ ].
