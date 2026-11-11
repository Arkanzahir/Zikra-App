# D02 — restart nyata teramati, idle belum konklusif

**Pemulihan daya uji masih menunggu HP tersambung kembali.** Jangan menganggap
HP sudah dipulihkan atau D02 selesai. Pengguna sudah diminta menyambungkan USB.

## Restart

Pengguna menyetujui satu restart USB baru. Cadangan terbaru 48 berkas dibaca
dan arsip diverifikasi sebelum restart; tidak force-stop, memasang helper atau
memasang ulang APK utama 1.9.39. Setelah boot baru dan data terbuka, observer
laptop mendapatkan dua set alarm masa depan identik dengan baseline.

Pemeriksaan berikutnya: 31 alarm exact, tidak ada UI/instrumentation Zikra aktif,
log terbatas tidak menunjukkan pembukaan UI sejak boot; 16 berkas preferensi
byte-sama, seluruh baris 20 tabel sama dengan cadangan sebelum restart.
Keterbatasan log dan snapshot berurutan tetap seperti pada
[prosedur observer](../../../docs/prayer-recovery-observer.md).
Ini bukti pemulihan pada satu perangkat/boot, bukan jaminan seluruh vendor.

Inspeksi tambahan pertama ditolak allowlist adapter karena pembacaan setting
tambahan belum didukung; diperbaiki dengan pembacaan host eksplisit. Percobaan
tersebut tidak mengubah HP atau menggagalkan hasil observer boot.

## Idle

Master pengingat aktif dan alarm asli berikutnya bermode ADHAN. Epoch dan
jadwal tidak diubah. Alarm tersebut terlebih dahulu terlihat exact sebelum
waktunya dan tanpa layanan/marker adzan aktif.

Untuk uji terkontrol, simulasi baterai unplug, hemat baterai dan layar mati/
forced idle diterapkan. Dari 90 sampel, 20 lengkap; kondisi idle/layar mati/
setting hemat baterai terlihat. Sampel lengkap terakhir pada detik 66,24;
USB lalu tidak terdeteksi. Marker pengiriman/layanan/notifikasi untuk alarm
pilihan belum teramati. Timeout menghasilkan belum konklusif. Tidak ada klaim
suara, rekaman selesai atau alarm tidak bekerja dari terputusnya sambungan.

Kelima langkah pemulihan di `finally` gagal karena perangkat tidak tersambung.
Percobaan menunggu sambungan berikutnya juga berakhir tanpa menemukan HP.
**Simulasi daya mungkin masih aktif; pemulihan belum terverifikasi.**

## Tindak lanjut wajib saat USB kembali

1. Lepas forced idle dan simulasi baterai; pulihkan nilai awal low_power serta
   stay_on_while_plugged_in dari baseline privat, tanpa menimpa perubahan lain
   pengguna yang tidak dikenal. Bangunkan layar.
2. Verifikasi setting, forced-idle, simulasi baterai dan alarm; simpan bukti
   pemulihan terpisah. Jangan melakukan restart atau uji baru dahulu.
3. Revisi status pending ini setelah pemulihan terbukti. Audio/lafaz/hak,
   idle terarah dan jangka panjang masih mengikuti gerbang D02/D02a/G02.

[checks.json](checks.json) memuat ringkasan tanpa serial, koordinat, isi
preferensi, data tabel atau berkas cadangan. Seluruh data mentah tetap `.local`.
Tidak ada source runtime, desain, audio, provider, backend atau email berubah.
