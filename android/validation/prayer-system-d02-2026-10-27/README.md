# D02/D02a — reboot nyata dan forced Doze pada emulator

27 Oktober2026, API37.1. **Bukti bertambah; D02/D02a tetap terbuka.** Runtime
1.9.14/code41/source5288e03 dan APK41.592.382byte tidak diubah. Hanya helper
pengujian baru. [Receipt](release.json), [hasil](system-results.json),
[native](native-results.json), [build/lint](build.txt), [preservasi](preservation.json).

## Reboot sungguhan, tanpa broadcast BOOT buatan

Fixture menyimpan master aktif dan empat mode: Subuh adzan, Dzuhur notifikasi,
Ashar getar, Maghrib mati, Isya notifikasi. Lokasi sintetis awal Sukolilo/Surabaya,
Asia/Jakarta;24 slot masa depan dalam tujuh tanggal dijadwalkan.

Host menjalankan reboot perangkat emulator; ID boot kernel benar-benar berubah.
**Sebelum** menjalankan instrumentation/halaman/sync apa pun sesudah boot, host
membaca AlarmManager dan menemukan24 alarm RTC_WAKEUP aplikasi. Artinya bukan
sekadar XML plan bertahan atau identitas PendingIntent yang dibuat oleh test.
[Potongan alarm milik Zikra](alarms-after-reboot-owned.txt).

Native berikutnya hanya membaca pilihan/plan, memastikan24 slot, lokasi/revisi,
mode dan identitas PendingIntent. Tidak memanggil sync atau receiver secara manual.
Field `audioNotResumedAfterBoot` hanya mengamati state player di proses verifikasi,
bukan bukti output suara fisik; pengujian tidak mengklaim suara HP setelah reboot.

## Alarm sungguhan ketika deep idle dan layar mati

Native menjadwalkan alarm adzan Dzuhur **waktu fixture +25 detik** dengan
AlarmManager exact-and-allow-while-idle, bukan broadcast langsung. Host melepas
status pengisian daya emulator, mematikan layar, lalu memaksa deep idle melalui
fasilitas diagnostik Android. Wi-Fi/data mati.

Epoch `prayer_delivered_2` sesuai alarm yang dijadwalkan. Pemeriksaan sistem
menemukan AdhanService foreground, `createdFromFg=false`, serta notifikasi7400
pada kanal adzan dengan satu tindakan stop. Layar tetap Asleep dan deviceidle
masih IDLE sesudah pengiriman. [Potongan bukti milik aplikasi](idle-delivery-owned.txt).
Decoder dua MP3/posisi maju/stop/audio focus sudah teruji pada
[bukti D02 sebelumnya](../prayer-adhan-d02-2026-10-27/README.md); hasil ini membuktikan
pengiriman sistem saat forced idle, bukan uji mendengarkan ulang.

## Preservasi dan batas

4 metode native lulus. Internal asli diisolasi/cadangan privat sebelum fixture;
10 berkas asli dipulihkan cocok hash. Pengaturan jaringan/stay-awake/DND/ringer
pulih terverifikasi; izin notifikasi/exact dikembalikan melalui perintah yang
berhasil. Simulasi baterai/forced idle dilepas, alarm fixture dibatalkan sebelum
restore, helper dihapus dan emulator ditutup. HP fisik tidak diubah. Tidak ada
unggahan data pengguna atau panggilan provider AI.

Log sistem lengkap tetap privat; ekspor publik dibatasi ke blok aplikasi/data uji.
Semua metode mensyaratkan marker `files/d02-system-authorized` setelah host
mengisolasi data; jangan menjalankan pada HP/data pengguna aktif. Urutan host:
arm → reboot sungguhan → baca dumpsys alarm → read-without-resync → arm alarm
+25 detik → forced idle → periksa sistem → cancel → pulihkan data/pengaturan.

Forced deep idle singkat tidak membuktikan Doze semalam, pembatasan baterai vendor,
suara/getar/DND/kanal pada HP atau seluruh versi Android. Empat mode bertahan
reboot dan [pembaruan APK terisi D06](../storage-d06-2026-10-27/README.md), namun
penutupan D02/D02a tetap memerlukan pemeriksaan HP serta lafaz/kualitas dan
ketentuan distribusi audio. [Audit sumber exact-hash](../adhan-source-d02-2026-10-27/README.md).
