# Penutupan prioritas C02/D02 — 12 November 2026

Pengguna meminta urutan prioritas dan kemudian menunda HP. Audit ini tidak
menutup parent C02/D02, mengubah runtime, mengakses USB, inference, data pribadi,
konfigurasi cloud, audio atau APK. Kandidat tetap 1.9.41; HP terakhir 1.9.39.

## Hasil pemeriksaan tanpa HP

- Backend lokal: 78 lulus, 13 skip, 0 gagal dari 91 kasus. Ini bukan pengulangan
  hasil 91/91 lingkungan terdahulu. Lihat [log](backend-tests.txt).
- Observer alarm: 19 lulus. Replay boot sintetis menghasilkan observed setelah
  dua sampel stabil, tetapi physicalGateAutomaticallyClosed tetap false.
  Lihat [log](observer-tests.txt) dan [ringkasan](checks.json).
- Putaran backend awal: 77 lulus/13 skip/1 gagal karena akses direktori sementara
  AppData ditolak. Putaran akhir memakai direktori sementara khusus di .local;
  tidak mengubah kode untuk menyembunyikan kegagalan. Log awal tetap privat.
- Cek kesehatan HTTPS/auth Redis tidak dijalankan: automatic approval review
  gagal diproses (Bad Request), bukan keputusan bahwa aksi tidak aman. Tidak
  mencoba jalur lain untuk melewati pemeriksaan. Server hari ini belum terbukti.

## Bukti yang sudah cukup dan tidak perlu diulang tanpa alasan

Pemulihan alarm setelah boot teramati pada bukti 11 November. Suara dua rekaman
serta Stop pernah dikonfirmasi pengguna. Transport AI yang menunggu respons
12 detik dan pemisahan antrean sudah memiliki regresi terarah. Jangan meminta
restart lagi hanya untuk mengulang bukti ini.

## Sesi penutupan berikutnya

| Prioritas | Kekurangan bukti | Langkah dan syarat lulus |
| --- | --- | --- |
| C02 operasional | Laporan sukses pengguna pada 1.9.39 belum menutup semua kondisi yang dirinci gerbang | Setelah pemeriksaan layanan diizinkan, cek HTTPS/auth tanpa inference; uji dua pesan berurutan dengan internet HP mandiri saat laptop tidak aktif. Catat versi, jam, status Online, kode error jika ada. Jangan simpan isi chat pribadi/token atau mengubah provider/kuota untuk meluluskan tes |
| D02 idle | Pengiriman adzan saat idle/hemat baterai di HP belum konklusif | Ketika HP tersedia, cadangkan data, ambil baseline, pilih alarm asli ADHAN berikutnya yang dekat. Observer laptop read-only melihat exact alarm sebelum epoch, marker epoch yang sama, FGS/notifikasi serta kondisi daya. Jika USB hilang/timeout, hasil inconclusive dan prioritaskan pemulihan daya; jangan klaim lulus atau bug |
| D02 suara | Suara terdengar dan selesai alami perlu dibedakan dari proses yang sekadar aktif | Konfirmasi pengguna suara terdengar, berakhir alami atau Stop bekerja; jangan menyimpulkan suara terdengar dari log. Tes lengkap dua rekaman bila bukti natural completion masih kurang |
| D02 hak/lafaz | Identitas sumber/hash belum berarti izin distribusi | Verifikasi pemilik/kewenangan, izin APK/AAB, cakupan/atribusi dan lafaz. Draf docs/audio-permission belum dikirim. Tidak mengirim email tanpa instruksi dan penerima terverifikasi |

Untuk HP gunakan [runbook observer](../../../docs/prayer-recovery-observer.md).
Jangan force-stop atau menjalankan instrumentation panjang selama menunggu idle.
Daya tidak diubah sebelum koordinasi; tidak perlu restart baru untuk sesi idle.
Jika alarm asli terlalu jauh, tunda pengamatan, bukan mengubah jadwal salat.

Sesudah bukti C02/D02 terpenuhi, lanjut E/F lalu G (privasi/kontak, signing,
migrasi dan smoke paket final). B01 kitab lengkap dan distribusi tetap terbuka;
HadeethEnc opsional tidak menggantikannya. Website unduhan sesudah gerbang beta.
