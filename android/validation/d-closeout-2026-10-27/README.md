# Penutupan D — 27 Oktober2026

Prioritas pengguna: tuntaskan D sebelum E. Status berikut membedakan bukti beta
dan gerbang yang belum terpenuhi; bukan klaim rilis publik siap.

| ID | Status | Bukti/sisa konkret |
| --- | --- | --- |
| D01 | Terbuka, implementasi/uji terarah ada | [Referensi270 peristiwa dan emulator](../location-qibla-d01-2026-10-26/README.md). HP untuk GPS/approximate, magnet/kalibrasi/arah acuan dan empat rotasi, sensor stale vendor. |
| D02/D02a | Terbuka, implementasi/uji terarah ada | [Empat mode/pemutar/focus/stop](../prayer-adhan-d02-2026-10-27/README.md), [banner](../adhan-banner-2026-10-27/README.md), [reboot/forced Doze sungguhan](../prayer-system-d02-2026-10-27/README.md). HP untuk suara/getar/senyap/kanal, reboot/jam/zona dan layar mati lama/baterai vendor. [Asal dua MP3 cocok hash](../adhan-source-d02-2026-10-27/README.md); lafaz penuh dan ketentuan redistribusi belum terbukti. |
| D03 | Selesai teknis beta | [Quran/Mushaf/audio/gulir](../quran-d03-2026-10-27/README.md), batas font/timing/sampel tetap berlaku. |
| D04 | Selesai teknis beta | [Study/kelas/tenggat/timer](../study-d04-2026-10-26/README.md), pemeriksaan vendor/Doze lama tetap G02. |
| D05 | Selesai teknis beta | [Jurnal/Hifz/khatam](../faith-d05-2026-10-27/README.md). |
| D06 | Selesai teknis beta | [Upgrade/reinstall APK asli berisi data](../storage-d06-2026-10-27/README.md), bukan cloud backup/uninstall/downgrade. |

Runtime terbaru tetap **1.9.14/code41**, source5288e03. APK debug41.592.382byte
(<100.000.000), hash/source/signernya pada receipt D05/D06. Penambahan tes/bukti
D06 dan sistem D02 tidak mengubah aplikasi, tidak memerlukan APK baru.

HP ditunda oleh pengguna; tidak meminta sambungan berulang dan tidak memalsukan
hasil fisik. Putaran HP berikut dapat digabung: lokasi/Kiblat terlebih dahulu,
lalu suara/stop/empat mode/kanal, reboot dan uji layar mati lama. Hasil harus dicatat
per kasus/perangkat; satu hasil tidak otomatis menutup seluruh checklist.

Untuk distribusi audio: cari ketentuan eksplisit dari pemegang hak atau pilih
rekaman dengan izin yang dapat dibuktikan dan persetujuan pengguna. Gratis unduh
bukan bukti izin memasukkan rekaman ke aplikasi. Jangan mengganti pilihan audio
pengguna atau mengirim email baru tanpa instruksi. B01 Hadis tetap independen,
belum selesai; jangan menganggap sisa D juga menutup semua C/G/publik.

Data emulator dipulihkan setelah setiap putaran, helper dihapus dan emulator
ditutup. WIP PROJECT_HANDOFF.md, stash/arsip audio, file privat dan desain Home/AI
dipertahankan. Semua checkpoint terverifikasi dipush ke origin/main.
