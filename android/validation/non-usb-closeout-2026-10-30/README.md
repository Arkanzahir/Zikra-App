# C07 hosted, C02 operasional dan persiapan audio — 30 Oktober2026

**Bagian vision hosted selesai untuk sampel terarah; C07 keseluruhan belum selesai.**
Source konfigurasi/runbook `43f821a554ab15d35eeb5f0f97ca9912c5134a83`.
Pengguna sedang memakai HP dan meminta pekerjaan tanpa USB: tidak ada akses ADB,
pengujian native, pembaruan APK atau perubahan data/pengaturan HP pada sesi ini.

## Model vision yang dipakai

Katalog [vision Featherless](https://featherless.ai/models?modalities=vision)
mencantumkan `Qwen/Qwen3-VL-8B-Instruct`. [Model card resmi Qwen](https://huggingface.co/Qwen/Qwen3-VL-8B-Instruct)
menyatakan image-text-to-text dan Apache2.0. Ini memakai API Featherless yang sama,
bukan mengunduh bobot model ke APK atau memakai provider/OAuth baru.

[Probe kandidat](candidate.json) berhasil2.448ms. [Enam sampel langsung](provider-evaluation.json)
semuanya200/strictpass sebelum rollout: foto jelas, kontras rendah/miring3°, scan
tiga halaman, gambar kosong, rotasi90°, serta pengulangan foto jelas. Ekspektasi
tetap ditetapkan sebelum panggilan; JSON hasil scan kini benar `[23,48,57]`, dan
gambar kosong menghasilkan fieldnull, bukan angka rekaan. Durasi1.486–3.935ms.
Ini bukan jaminan untuk semua dokumen/tulisan tangan atau uptime penyedia.

[Rollout](rollout.json) memverifikasi variabel vision hanya bertarget production
dan model lama masihQwen2.5-VL-7B. Hanya `FEATHERLESS_VISION_MODEL` diganti ke
Qwen3-VL-8B.19entri aplikasi lain dibandingkan sebagai snapshot env pull; secret
yang dimasking tidak didekripsi independen. Tidak ada perintah update key/model
chat teks/kuota/paket/Cloudflare atau penerimaan ketentuan baru. Capture lengkap
dan log CLI tetap privat. Prosedur pemulihan nilai lama+redeploy disiapkan dalam
[runbook](../../../backend/deploy/VERCEL.md).

[Deployment](deployment.json) READY `dpl_FaEoEnS1A1rBa7Rz6Jaip4GVUTcc`, immutable
[Vercel](https://zikra-dmprerhni-arkanzahir86-3257s-projects.vercel.app), alias
[Zikra](https://zikra-ai.vercel.app).17file allowlist dipindai terhadap nilai privat
dikenal sebelum upload. Runtime handler sama dengan checkpoint503 terdahulu;
perubahan efektifnya konfigurasi vision.

## Verifikasi melalui server Zikra

[Tujuh request hosted](hosted.json) PC → Vercel → Featherless semuanya200/strictpass:
enam kasus vision tadi dan satu kontrak teks DOCX bertab. Durasi2.136–4.284ms;
tidak ada interceptor, auto-retry, konteks pribadi atau berkas pengguna.
Model teks tetap dipakai untuk DOCX; model vision untuk foto/scan.
[Permintaan sintetis](requests-synthetic.json) tersedia tanpa Base64; input asli
foto/halaman ada pada [fixture sebelumnya](../vision-capacity-c07-2026-10-30/fixtures.json),
gambar kosong/rotasi ada di folder ini. Scan dibuat sebagai JPEG langsung ke
kontrakPDF, **bukan impor PDF/picker Android baru**. Parser Android dan preservasi
draf terdahulu tetap mempunyai [bukti terpisah](../attachments-c07-2026-10-30/README.md).

[72tes backend lulus/0skip](backend-tests.txt), termasuk validasi model yang
diganti dalam fixture konfigurasi. [Health](health.json)200/tanpa token401.
Tidak perlu build Android ulang karena kode/resource Android tidak berubah.
APK terakhir tetap1.9.33/code60,45.420.472byte (<100.000.000byte), SHA256
`5e3e820688c2ea9c3d1c03520456a4f2be8da48d24e8fb6f818831f2ff38256d`.
HP terakhir dibuktikan1.9.31. Pergantian model server berlaku pada jalur online
yang sudah ada; tidak memaksa mode HP atau membutuhkan APK baru untuk model ini.

[Checks](checks.json) dan [preservasi](protected-state.json): identitas/registry/
usage/lease uji dihapus terarah, global key bersama tidak dihapus. Tujuh hash WIP,
arsip, env sumber, logo, artwork dan dua audio cocok; stash audio tetap. Capture
env/credential/backup/GPS/MP3/APK tidak dipublikasikan. Hasil gagal Qwen2.5 pada
checkpoint sebelumnya tetap tersimpan, tidak diubah menjadi lulus.

## Gerbang lain tanpa USB

- **C02:** [tiga run schedule terbaru](monitor.json) semuanya success; yang terbaru
  [41010084288](https://github.com/Arkanzahir/Zikra-App/actions/runs/41010084288)
  selesai30Oktober12:44:28WIB pada sourcefc6bc04, sebelum rollout model ini.
  Tidak memicu job baru atau mengubah jadwal. Ini bukti monitor berlanjut, bukan
  inference saat laptop mati. [Panduan manual](c02-manual-check.md) siap, belum dijalankan.
- **B01:** pencarian baca-saja dan thread klarifikasi diperiksa30Oktober;
  [ringkasan](b01-mail-check.json) masih satu pesan SENT, tanpa balasan masuk yang
  ditemukan. Tidak mengirim follow-up atau memublikasikan identitas mailbox/ID
  privat/isi email. Asal edisi/terjemahan/ketentuan Hadis tetap belum terkonfirmasi.
- **D02/D02a:** dua hash audio pilihan cocok. Halaman unduhan masih belum memberi
  ketentuan bundling aplikasi eksplisit pada pemeriksaan ini. [Draf permintaan](audio-permission-request.md)
  untuk pasangan audio yang benar sudah disiapkan, **belum dikirim**; pemegang hak/
  penerima belum diverifikasi. Persiapan bukan izin penggunaan atau penutupan D02.

Sisa C07 adalah picker/grant/penolakan izin HP serta penilaian beta/HP dalam batas
dokumen nyata. C02 laptop-mati, D02/D02a fisik/izin audio, B01 dan E/F/G/publik
tetap terbuka. UI10 dan mode orang tua tetap ditunda. Seluruh A–D belum selesai.
