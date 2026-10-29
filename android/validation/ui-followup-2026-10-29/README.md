# UI06 — Dock AI, lampu malam, splash native

29 Oktober2026. Beta **1.9.24 / code51**, lanjutan yang diprioritaskan pengguna sebelum C04. Ketiga koreksi selesai teknis beta dalam batas berikut; bukan penutupan seluruh A–D/F/G atau rilis publik.

## Perubahan terverifikasi

- [x] **Dock AI:** Home, Quran, Zikra AI, Study, Profil memakai NavDock yang sama. Menu AI aktif, ikon asli dan font dock konsisten. Composer mendapat ruang sesuai tinggi dock/font; dock disembunyikan saat IME nyata tampil dan kembali sesudah ditutup. Baris kontrol diratakan ke tengah dengan ruang vertikal2dp supaya target Kirim48dp tetap penuh pada font200%. Draf/chat tidak berubah saat Home → AI, tanpa kirim otomatis.
- [x] **Lampu masjid:** memakai satu bitmap masjid siang yang sama. Cahaya hangat fasad dan refleksi lantai ditambahkan dalam transformasi parallax yang sama, tanpa kubah/gedung kedua. Padam sepanjang siang; memudar selama20menit di tepi matahari terbit/terbenam lokasi dan penuh pada malam. Independen dari tema terang/gelap. Shader disimpan, tanpa bitmap baru atau alokasi shader per frame.
- [x] **Splash native:** empat bagian vektor logo asli dan wordmark Plus Jakarta Sans. Ikon splash sistem Android12+ dipadatkan ke ukuran yang selaras, lalu dilepas sebelum satu logo aplikasi bergerak ke wordmark. Animasi nominal400ms, tidak loop/tidak menambahkan hold; versi referensi lokal1,12detik dipadatkan untuk cold launch. Warm process/recreation melewati intro; animasi sistem mati/penghemat daya langsung menuju Home. Pause/fokus hilang membatalkan animasi dan tidak membuka Home dari background; resume masuk sekali.

## Bukti

[Preview native](preview.jpg), [video cold launch Android](native/splash-cold-launch.mp4), [GIF tinjauan](native/splash-cold-launch.gif), [urutan frame](native/splash-frames.jpg), [hasil native](native-results.json), [unit/lint](checks.json), [APK](apk.json), [preservasi](preservation.json).

GIF mengulang untuk tinjauan; intro produksi berjalan sekali per proses. Video adalah rekaman emulator Android, bukan ekspor Figma atau simulasi HTML. Frame asli pertama/logo/transisi/Home diperiksa: tidak ada logo besar bayangan bersama logo kecil. Video dipotong dari rekaman privat untuk membuang launcher, tanpa mengubah isi frame aplikasi. Source logo utama tetap utuh dan empat path salinan cocok dengan referensi.

**155 unit lulus /4 skip /0 gagal**. Build APK dan helper lulus. **11 eksekusi native /8 metode unik** pada emulator khusus API37.1: seed APK1.9.23 aktual lalu update1.9.24 (profil/chat/draf tetap), dock/navigasi/IME nyata kedua tema, pencahayaan12.00/18.10/21.00/05.50 serta tema gelap saat siang, cold/warm launch menuju satu Home, background/resume, animator0, penghemat daya nyata emulator, ukuran sempit/font200%, dan regresi tema global/back stack/draf. Perubahan baris kontrol terakhir diperiksa ulang secara terarah pada kedua ukuran.

Ukuran normal1080×2400/420dpi/font100%; ukuran sempit640×1380/320dpi (320dp)/font200%. Lampu dibandingkan dari piksel render Canvas Android dengan/ tanpa komponen cahaya pada scene malam yang sama; siang tidak menambah cahaya, malam menambah area hangat terukur. Screenshot memakai clock/lokasi fixture, bukan perubahan jadwal salat atau klaim observasi astronomi. Font besar Kirim tetap48dp dan seluruh hitbox terlihat; navigasi tidak menindih composer. Draf/metode pesan menggunakan data uji tersimpan, bukan respons provider yang baru dibuat.

**Lint0 error/862 warning**; warning belum dinyatakan bersih. Emulator sempat tertahan karena commit memory kosong kurang dari kebutuhan4GB; pengguna mengosongkan aplikasi sendiri, tanpa agent menutup game/IDE. Kasus IME sebelum jendela fokus dan batas baris satu piksel diperbaiki dengan penantian fokus/layout nyata serta perataan/padding kontrol, bukan menurunkan ukuran target sentuh. OS/baterai/viewport/font emulator dipulihkan, helper dihapus, emulator ditutup. HP tidak diubah.

## Paket

APK lokal folder pembungkus: `Zikra-UI-v1.9.24-beta-debug.apk`. **43.867.733byte**, SHA256`8416d3756b92dd5af2e32635230c19b2656633865ac287acd2da7d2a4bc08fe1`. Sertifikat debug sama dengan beta sebelumnya; cocok untuk pembaruan tanpa uninstall. Seluruh entry APK didekompresi dan diperiksa terhadap nilai kredensial privat yang diketahui:0temuan. Audio biasa/Subuh tetap cocok byte/hash dengan pilihan terakhir pengguna dan hanya input debug privat; APK/MP3/DB/log/raw video pribadi tidak masuk Git.

## Batas yang tetap terbuka

Uji HP/vendor nyata untuk tampilan, launch/performa/baterai dan alarm masih mengikuti gerbang masing-masing. API26–30 memiliki fallback tanpa SplashScreen API31 dan lolos build/lint; branch lama belum dieksekusi di emulator karena hanya image37.1 tersedia. Figma canvas lama tetap belum selesai karena kuota; checkpoint ini adalah implementasi Android dari referensi lokal dan tidak mengklaim file Figma selesai. Referensi1,12detik tetap sebagai riwayat; native sengaja lebih singkat. Cahaya fasad adalah ilustrasi, bukan kondisi lampu bangunan sebenarnya.

A–D belum seluruhnya selesai. B01 masih menunggu sumber Hadis; C02/C04/C07, ketentuan/sisa HP D02/D02a dan gerbang E/F/G/publik tetap terbuka. C04 menjadi tindak lanjut setelah tiga koreksi ini, bukan menggantikannya. Untuk pemeliharaan: satu NavDock, satu shader/lampVisibility berdasarkan waktu lokasi, satu guard navigasi SplashActivity; jangan menambah timer/startActivity baru tanpa pengujian pause/recreation/animator0.
