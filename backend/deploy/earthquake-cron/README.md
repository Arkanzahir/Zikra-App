# Penjadwal Cloudflare E02 — observasi beta

GitHub Actions mencatat dua pemicu otomatis sukses pada 9–10 November 2026,
tetapi jedanya 9 jam 4 menit 54 detik. Itu tidak memenuhi observasi lima menit.
Worker ini hanya memanggil endpoint poll Vercel yang sama; BMKG, Redis, deduplikasi
dan pengiriman FCM tetap di backend. Tidak memuat key AI atau akun pengirim FCM.

Konfigurasi: satu Cron Trigger `*/5 * * * *`, tanpa akses HTTP publik, redirect,
isi respons pada log, atau percobaan ulang otomatis. Secret hanya
`ZIKRA_QUAKE_CRON_SECRET`, melalui secret binding Cloudflare, bukan kode/Git/APK.
Pemicu lima menit berarti 288 panggilan/hari. Workers Free menyediakan 100.000
panggilan/hari dan 10 ms CPU/panggilan; verifikasi paket akun sebelum deploy,
tanpa mengaktifkan paket berbayar. Layanan Vercel/Redis/FCM tetap memiliki batasnya.

10 November2026: pengguna menyetujui aktivasi hanya Free serta secret pemicu di
Cloudflare. Dashboard **Free / $0 / Current plan** terverifikasi. Token deploy
terpisah hanya Workers Scripts Write pada akun ini, berlaku30hari, disimpan
lokal privat `.local/cloudflare-cron.env`; tidak mengubah token Workers AI.
Worker dan secret binding sudah terpasang, workers.dev/preview nonaktif.
Konfigurasi bukan bukti pemicu otomatis sukses. [Bukti/deploy/riwayat](../../../android/validation/phone-closeout-2026-11-10/README.md).

Eksekusi pertama teramati gagal: runtime Cloudflare menolak `redirect: 'error'`.
Diperbaiki menjadi `manual`, yang tidak mengikuti pengalihan; setiap respons
selain200 tetap gagal dan body dibatalkan. Perbaikan tidak mengatur ulang jadwal.
Lima tes lokal lulus, termasuk penolakan302 dan satu pemanggilan tanpa retry.

Sesudah deploy, observasi beberapa event **scheduled** nyata dan langkah
poll berhasil. Perubahan cron dapat memerlukan hingga 15 menit. Jangan menutup
E02 atau menjanjikan pengiriman tepat lima menit hanya dari konfigurasi/manual.
Jangan mematikan penjadwal lama sebelum alternatif teramati; deduplikasi dan
minimum interval backend melindungi saat keduanya berjalan. Rollback: hapus
jadwal Worker ini, kemudian pastikan jadwal lama tetap aktif. Jangan hapus secret
bersama pada Vercel/GitHub.

Token deploy boleh kedaluwarsa tanpa menghentikan cron yang sudah terpasang.
Pembaruan berikutnya perlu token deploy sah dengan cakupan sama. Secret runtime
tetap dikelola terpisah; jangan memasukkan nilai secret pada konfigurasi Wrangler.

Validasi lokal: `node --test backend/deploy/earthquake-cron/worker.test.mjs`.

Referensi primer diperiksa 10 November 2026:
- [Cron Triggers](https://developers.cloudflare.com/workers/configuration/cron-triggers/)
- [Workers pricing](https://developers.cloudflare.com/workers/platform/pricing/)
- [Account settings API](https://developers.cloudflare.com/api/resources/workers/subresources/account_settings/methods/get/)


Observasi10November: run09.40 dan09.45WIB sukses, jarak scheduledTime300detik.
Kegagalan awal09.35 tetap dicatat. E02 ditutup hanya teknis beta terarah; dua
sampel bukan SLA/observasi panjang. GitHub cadangan tetap aktif. Sesi tail
sementara dibersihkan; tidak diperlukan untuk cron berjalan.
