# D02 — restart nyata teramati, idle belum konklusif

**Daya uji sudah dipulihkan dan diverifikasi setelah pengguna menyambungkan
USB kembali.** Hasil idle tetap belum konklusif; D02 belum selesai.

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
Pemulihan awal belum terverifikasi pada saat itu. Setelah pengguna
menyambungkan kembali, forced idle dan simulasi baterai berhasil dilepas, kedua
setting kembali ke baseline. Layar dibangunkan tanpa membuka Zikra/restart.
Tersisa 30 alarm masa depan, sama dengan baseline setelah satu waktu terlewati;
bukan kehilangan satu jadwal masa depan. Snapshot lengkap setelah sambung
kembali tidak menemukan marker epoch pengiriman untuk alarm pilihan maupun
layanan adzan aktif. Ini tidak membuktikan kondisi daya pada waktu pengiriman
dan tidak mengubah timeout menjadi lulus.

## Pemulihan selesai; tindak lanjut

1. Pemulihan daya terverifikasi pada `checks.json`; percobaan awal yang gagal
   tetap dicatat. Tidak ada pemasangan helper atau perubahan jadwal/mode audio.
2. Uji idle perlu sesi tersendiri dengan koneksi stabil dan kesiapan pengguna.
   Jangan menyimpulkan kegagalan aplikasi dari pengamatan yang terputus.
3. Audio/lafaz/hak dan pengamatan jangka panjang tetap mengikuti D02/D02a/G02.

[checks.json](checks.json) memuat ringkasan tanpa serial, koordinat, isi
preferensi, data tabel atau berkas cadangan. Seluruh data mentah tetap `.local`.
Tidak ada source runtime, desain, audio, provider, backend atau email berubah.

## Koreksi pemeriksaan instrumentation

Pemeriksaan lanjutan menemukan subperintah `activity instrumentation` tidak
didukung pada HP tetapi mengembalikan exit nol. Field lama instrumentation=false
tidak boleh dianggap bukti independen bebas instrumentation. Dua set alarm
boot dan preservasi tetap tercatat, bukan penutupan gerbang D02. Lihat
[perbaikan observer dan percobaan gagal](../prayer-observer-hardening-2026-11-11/README.md).
