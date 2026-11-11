# D02/D02a — checkpoint HP dan batas uji, 11 November 2026

**D02/D02a tetap terbuka.** Tidak ada APK runtime baru atau perubahan mode
pengingat permanen. HP sudah dikembalikan dan dapat dipakai. Ini menyimpan hasil
yang berhasil, gagal dan belum konklusif; bukan klaim alarm pasti gagal maupun
seluruh pengingat sudah terjamin.

## Preservasi

HP POCO F7/API36 terdeteksi1.9.39, sesuai laporan pemasangan mandiri pengguna.
Cadangan awal48berkas dan APK asli disimpan privat; SHA256 APK sama dengan
[receipt C02](../assistant-reliability-c02-2026-11-11/apk.json).
Sebelum fase terakhir, dibuat cadangan baru sesudah pengguna menyetujui HP tidak
disentuh. [Verifikasi](preservation.json) membandingkan fase terakhir ini:
16file preferensi tanpa nilai asli berubah,20tabel dengan semua baris sama,
pengaturan sistem dan zona pulih. Cadangan awal sesi tetap terpisah; ini tidak
mengasumsikan aktivitas pengguna di antara kedua fase harus ditimpa.

Paket/berkas uji dan dua XML antarmuka sementara dibersihkan. Simulasi baterai
serta forced idle dilepas; tidak mengubah mulai otomatis/perizinan aplikasi.
Zikra dibuka normal setelah instrumentation berakhir supaya pengingat tidak
ditinggalkan dalam keadaan force-stopped. [Keadaan akhir](final-state.json):
31alarm salat aktif; APK utama tidak dihapus atau dipasang ulang.

## Uji reboot: belum konklusif

Fixture awal26alarm tanpa mode adzan berhasil disiapkan. Pilihan dan lokasi
bertahan pada restart pertama yang dilakukan pengguna. Namun instrumentation
Android36 mencatat force-stop saat selesai, sehingga urutan awal tidak cukup
untuk menyatakan alarm otomatis pulih. Metode native hanya memeriksa preferensi;
hasilnya bukan pengganti pembuktian AlarmManager oleh host.

Sesudah pengaturan asli dipulihkan dan aplikasi dibuka normal,31alarm tersimpan.
Satu restart lewat USB dilakukan sesudah persetujuan eksplisit pengguna; PIN
dibuka sendiri. [Snapshot awal](normal-reboot-result.json) menunjukkan0alarm
sebelum membuka Zikra/instrumentation, tetapi tidak boleh dianggap kegagalan
pemulihan: [catatan waktu](reboot-interpretation.json) menunjukkan proses untuk
`PrayerRescheduleReceiver` baru mulai41,718detik setelah snapshot tersebut.
Selisih jam HP/host yang diukur berada antara-0,789 dan+0,290detik.
Aplikasi kemudian terbuka sebelum pengambilan ulang yang sudah stabil.
Karena itu tidak ada bukti final untuk kelulusan atau kegagalan pemulihan boot.

Pemeriksaan berikutnya harus menunggu boot selesai, penyimpanan terbuka dan
receiver mulai, lalu mengamati alarm selama jendela terbatas tanpa UI ataupun
instrumentation. Jangan menyalahkan autostart hanya dari snapshot awal kosong;
tidak ada sakelar autostart/baterai aplikasi yang diubah pada sesi ini.

## Uji forced idle: belum lulus

Alarm exact asli dipasang+45detik dengan fixture terpisah, layar dimatikan,
forced deep idle tercapai dan hemat baterai aktif. Hasil playing tidak teramati;
host instrumentation mencapai batas150detik tanpa hasil JUnit selesai. Ini
bukan bukti suara berjalan maupun bukti pasti akar masalah pada aplikasi.
Pengaturan daya dipulihkan, metode restore berhasil, lalu data diperiksa.
Metode menunggu di dalam proses instrumentation perlu diperbaiki/ditinjau
sebelum dipakai sebagai bukti pengiriman saat idle. Doze semalam/vendor tetap
belum dibuktikan.

## Hasil dan kegagalan yang dipertahankan

[Ringkasan](checks.json):5eksekusi native selesai/3metode unik untuk arm,
inspeksi preferensi dan restore; bukan5uji pengiriman berhasil. Paket uji
dikompilasi dan [10unit jadwal/zona](targeted-unit.json) lulus tanpa skip.
Lint dan seluruh unit tidak diulang karena tidak ada perubahan runtime.

Pemasangan awal dua kali ditolak HP; pengguna menyetujui ulang lalu berhasil.
Query awal paket yang belum ada mengembalikan exit1 dan penanganannya diperbaiki.
Parser alarm awal mengharapkan URI intent, lalu disesuaikan dengan blok alarm
nyata Android36. Satu restore tertahan guard saat credential storage masih
RUNNING_LOCKED; setelah PIN dibuka, restore lulus. Log lengkap kegagalan guard
pertama terlanjur ditimpa retry sebelum penyimpanan per-attempt ditambahkan;
lokasi stack tercatat pada transkrip, keterbatasan ini tidak disembunyikan.
Raw log, snapshot/XML, preferensi, DB dan MP3 tidak diekspor ke Git.

## Audio dan keputusan lain

[Audit pasangan aktif](../adhan-source-current-2026-11-11/README.md) membuktikan
hash Misyari/Subuh cocok unduhan penerbit; bukan izin redistribusi atau telaah
seluruh lafaz. Tidak mengirim email atau mengganti rekaman. Audio publik tetap
memerlukan bukti izin, sehingga D02/D02a tidak dicentang.

[Laporan pengguna/keputusan Maps](user-report-c02-maps.json):AI membalas ketika
laptop ditutup, tetapi status daya tidak diukur independen dan bukan uji shutdown.
Google Maps dibatalkan pengguna karena uang muka billing; persiapan disimpan
privat dan source/aplikasi tetap OSM. Tidak ada pembayaran/key Google baru oleh
agen. B01,C02,E03/E04,F/G,publik dan penundaan UI10/mode orang tua tetap sesuai
gerbang masing-masing; bukan klaim seluruh A–D/E selesai.
