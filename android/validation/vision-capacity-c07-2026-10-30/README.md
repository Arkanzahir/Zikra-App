# C07 — uji hosted dan penanganan kapasitas penyedia, 30 Oktober 2026

**Status: parsial; C07 belum selesai.** Source `200b42972420f94da6912c8cd8c04ae6f0bf1425`, APK debug privat1.9.33/code60. Semua permintaan baru memakai data sintetis; HP tidak terhubung atau diubah. Bukan pengujian picker/izin Android atau bukti C02 saat laptop mati.

## Hasil nyata, termasuk kegagalan

PC → HTTPS Vercel → Featherless, tanpa interceptor. [Permintaan](requests-synthetic.json), [fixture/hash](fixtures.json), [hasil awal](hosted-initial.json), [retry](hosted-retry.json), dan [hasil sesudah deploy](hosted-postdeploy.json) dipertahankan. Base64 dihilangkan dari daftar permintaan; JPEG sintetis tersedia di folder ini untuk mereproduksi input.

| Kasus | Hasil awal | Penilaian |
| --- | --- | --- |
| Foto jelas, kode ZK-7348 dan3×12000 |502 invalid_provider_response; satu percobaan ulang juga502|Gagal, bukan OCR lulus|
| Foto kontras rendah/kemiringan3° |200, kode dan36000 benar|Lulus sampel terarah|
| Tiga halaman scan sintetis |200, hasil23/48/57 benar|Pembacaan hitungan benar pada telaah manual; **gagal format JSON** yang diminta, sehingga penilaian strict tetap gagal|
| Teks DOCX bertab |200, kode/hari/jam benar|Lulus kontrak teks, bukan impor DOCX Android baru|

Tiga halaman dibuat langsung sebagai JPEG dengan Pillow dan diberikan ke kontrak `pdf`; tidak ada PDF nyata yang diproses Android dalam probe ini. Parser Android/PDF sebelumnya mempunyai [bukti terpisah](../attachments-c07-2026-10-30/README.md). Hasil ini tidak membuktikan semua scan, tabel, tulisan tangan atau dokumen pengguna akurat.

Diagnosis upstream langsung memakai model vision terakhir yang tercatat `Qwen/Qwen2.5-VL-7B-Instruct`, prompt sistem/lampiran/budget yang sama. [Respons](provider-capacity.json) menunjukkan HTTP200 berisi `error.code=capacity_exhausted`, tanpa jawaban. [Diagnosis sebelumnya](provider-empty.json) juga200 tanpa `choices`; detail kode belum direkam pada percobaan itu. Konfigurasi model hosted terkini tidak dibaca ulang secara independen, sehingga diagnosis langsung ini tidak membuktikan isi persis setiap502 awal. Percobaan diagnosis lain memakai placeholder key lokal dan ditolak401; tidak dihitung sebagai kegagalan produksi atau inference berhasil.

Sesudah deploy, satu foto jelas kembali diuji dan mendapat504 setelah36.470ms di sisi PC. Batas upstream handler35detik tetap berlaku. **Belum membuktikan foto jelas berhasil atau branch503 pada penyedia nyata.** Tidak dilakukan retry otomatis atau pergantian model/paket. Keseluruhan kesiapan vision tetap gagal.

## Perbaikan yang diterapkan

- Respons200 dengan kode tepat `capacity_exhausted` menjadi503 `provider_busy` dan `Retry-After:60`; HTTP429 penyedia juga mendapat header60. Error upstream lain tidak boleh menyamarkan dirinya sebagai jawaban atau membocorkan pesan internal. Batas respons128KiB/deadline/abort tetap.
- Android membedakan BUSY dari kuota instalasi. Pesan: “Layanan AI sedang penuh. Tunggu sekitar satu menit, lalu tekan Coba lagi.” Ini saran jeda, bukan janji layanan kembali setelah60detik. Tombol retry yang ada tetap membutuhkan tindakan pengguna; tidak auto-upload/auto-retry atau mengganti jawaban menjadi offline.
- Dua tes backend tambahan menguji error200/kapasitas, sanitasi, jawaban palsu yang menyertai error ditolak, satu panggilan upstream dan pelepasan lease. Kuota tetap didebit oleh aturan akses yang sudah ada; tidak menjanjikan refund pada inference gagal.

## Verifikasi, deployment dan APK

[Checks](checks.json):71backend lulus/0skip;163Android unit lulus/4skip; assemble berhasil, lint0error/861warning. Native/emulator/HP tidak dijalankan pada checkpoint ini. Uji Android memeriksa pemetaan kode BUSY/kuota/auth/server; resource pesan dikompilasi, bukan bukti visual/sentuhan HP baru.

[Deployment](deployment.json) READY `dpl_9RY5QCpBLoWL2vJpkJ9vMv39WVxA`, immutable [Vercel](https://zikra-hppowohw1-arkanzahir86-3257s-projects.vercel.app), alias [layanan](https://zikra-ai.vercel.app).17file allowlist diperiksa terhadap nilai privat dikenal sebelum upload. Tidak mengubah env/provider key/model/paket berbayar; [health](health.json)200 dan tanpa token401. Health hanya liveness, bukan sertifikasi ketersediaan model atau laptop-mati.

APK lokal: `D:\Download\Zikra-App-main\Zikra-AI-v1.9.33-beta-debug.apk`;45.420.472byte (<100.000.000byte), SHA256 `5e3e820688c2ea9c3d1c03520456a4f2be8da48d24e8fb6f818831f2ff38256d`. [Audit APK](apk.json)0nilai credential dikenal, dua audio sama hash; [signer](signature.txt) sama. [Aturan backup](backup-policy-apk.json) legacy/cloud/D2D9domain tetap terpasang; bukan tes transfer OEM. HP terakhir dibuktikan1.9.31, bukan33. APK privat/debug, belum rilis publik.

[Preservasi](protected-state.json): WIP PROJECT_HANDOFF/arsip audio/stash/env sumber/logo/artwork cocok hash. Setiap probe hosted memakai identitas/token acak tersendiri;3cleanup registry/usage/lease terverifikasi. Tidak menghapus global key bersama atau mengubah instalasi asli. Token/backup/GPS/dokumen pribadi tidak dipublikasikan.

## Sisa yang belum dicentang

C07: picker/grant/penolakan izin HP dan keandalan/variasi vision. C02: permintaan dari HP saat laptop benar-benar mati. D02/D02a: sisa pengujian fisik vendor/reboot/Doze dan izin redistribusi audio; bukti layar mati/Stop terdahulu tetap berlaku dalam batasnya. B01 menunggu sumber Hadis. E/F/G dan seluruh A–D/publik tidak ditutup oleh checkpoint ini; UI10 sorotan dan mode orang tua tetap ditunda.

Untuk C02 nanti: setelah laptop dimatikan sendiri, gunakan internet HP lalu kirim “Hitung19×7 dan sertakan kode ZK-C02-FAA755. Jawab satu baris.” Simpan screenshot jawaban/status Online dan waktunya; hasil133/kode harus sesuai. Challenge ini belum dikirim ke provider pada sesi ini. Laptop tidak dimatikan otomatis oleh agen. Sisa uji alarm harus dikoordinasikan ketika HP siap, tanpa game/video/musik dan tanpa mengubah jam salat asli.
