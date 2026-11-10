# HP 1.9.36 — C07, adzan, dan push gempa

10 November 2026, POCO F7 / Android API36. Aplikasi diperbarui **1.9.31 → 1.9.36**
dengan pemasangan pembaruan, tanpa uninstall aplikasi utama. APK tetap
50.062.820 byte; tidak ada APK produksi baru pada sesi ini. Cadangan 41 berkas
internal dan APK sebelumnya disimpan privat sebelum pengujian.

## C07: pemilih berkas fisik terpenuhi

[Satu metode native lulus](picker-instrumentation.txt), [hasil](picker-result.json):
foto PNG, PDF scan tiga halaman, dan DOCX tabel UTF-16 dipilih melalui DocumentsUI
Android yang sebenarnya. Harness hanya menentukan direktori awal berkas contoh;
pemilihan dan grant tetap dilakukan DocumentsUI. Membatalkan pemilih menjaga draf
sebelumnya, dan mencoba membaca dokumen yang belum dipilih ditolak provider.
Interceptor memblokir jaringan assistant: **0 request**, tanpa inference baru.
Ini tidak menyatakan Gallery menggunakan Android Photo Picker; jalurnya SAF.

[Berkas contoh](fixtures/) seluruhnya sintetis. Tidak memilih, membuka, atau
mengirim dokumen/foto pengguna. Bukti hosted vision 30 Oktober dan konfirmasi
kamera manual pengguna sebelumnya tetap dikaitkan pada versinya sendiri:
[hosted terarah](../non-usb-closeout-2026-10-30/README.md),
[parser/native](../attachments-c07-2026-10-30/README.md),
[konteks dan izin sekali](../assistant-consent-once-2026-10-30/README.md).
C07 selesai **beta terarah** berdasarkan gabungan bukti ini; bukan klaim OCR
sempurna pada semua tulisan, seluruh PDF, penyimpanan RAG permanen, atau semua HP.
Batas satu lampiran/8 MB, PDF tiga halaman, DOCX/TXT12.000 karakter tetap berlaku.

## D02/D02a: pasangan rekaman saat ini dan audio focus

[Dua metode native lulus](audio-instrumentation.txt), waktu total432,481 detik.
[Pemutaran utuh](phone-ad-playback-result.json) tanpa seek atau simulasi selesai:
rekaman biasa232.028 ms, Subuh187.272 ms. Keduanya berakhir melalui penyelesaian
alami. [Audio focus](phone-ad-focus-result.json): MAY_DUCK tetap memainkan dan
memajukan posisi; kehilangan fokus sementara yang lebih kuat menghentikan adzan,
dan kembalinya fokus tidak memulai ulang suara. Soft start biasa mencapai gain1.

Tidak mengubah rekaman, volume/DND sistem, waktu salat, atau sumber jadwal.
Rekaman tetap pilihan pengguna: Adzan-Misyari-Rasyid.mp3 dan fajr_128_44.mp3.
**D02/D02a tetap terbuka** untuk ketentuan redistribusi audio serta sisa uji
reboot/Doze/vendor/interaksi fisik. Uji alarm layar mati/Stop yang terdahulu tidak
diulang atau dilabeli baru. Lisensi publik belum disimpulkan dari file pengguna.

## E02: FCM nyata ke satu HP

[Registrasi](testRegisterNewPhoneOptIn.txt),
[penerimaan](testInspectRealCloudNotification.txt),
[aksi notifikasi](testSelectedReportOpenedFromNotification.txt), dan
[pembersihan akhir](testRemoveOnlyNewTestRegistration.txt) masing-masing lulus.
Notifikasi sintetis berlabel **UJI NOTIFIKASI ZIKRA — bukan gempa nyata** hanya
dikirim ke token HP yang diizinkan; **tidak ke topic atau subscriber lain**.
Token tetap privat. [Receipt pertama](cloud-first.json) dan
[duplikat](cloud-duplicate.json) menyatakan penerimaan oleh FCM, bukan jaminan
pengiriman semua perangkat.

Sebelum pengiriman, proses aplikasi tidak berjalan dan layar berstatus Dozing.
Notifikasi kemudian teramati melalui sistem **sebelum** instrumentation pembaca
dibuka. [Perbandingan waktu sistem](duplicate-comparison.json) tidak berubah
sesudah duplikat diterima FCM dan diamati kembali. ContentIntent notifikasi nyata
membuka snapshot dengan ID yang sama, bukan laporan terbaru. Ini pengujian aksi
PendingIntent asli, bukan ketukan jari/manual pada panel notifikasi. Tidak ada
uji jaringan mati baru pada HP ini; bukti offline emulator tetap historis.

Registrasi cloud sementara dihapus melalui unsubscribe/deleteToken/delete
installation, dan pilihan opt-in awal nonaktif dipulihkan. Notifikasi fitur lain
tidak dibatalkan. Percobaan unsubscribe awal timeout; ulang akhir lulus, bukan
menyembunyikan kegagalan atau membuktikan unsubscribe selalu seketika.

## Preservasi, kegagalan dan batas

[Preservasi](preservation.json): semua baris asli17 tabel tetap, preferensi asli,
berkas tahan lama, pengaturan sistem, izin serta kebijakan baterai terpelihara.
Satu audit awal menandai cache langit berubah. Ditinjau: koordinat/zona sama,
efemeris diperbarui untuk10 November; cache lebih baru dipertahankan. Tidak
menimpa database/cache pengguna dengan cadangan lama. Empat berkas contoh dan
helper dihapus; aplikasi dibuka kembali melalui launcher resmi.

[Ledger percobaan](attempts.json) membedakan kegagalan alat uji, gangguan UI
bersamaan, timeout pembersihan, dan hasil akhir. Pemeriksaan layar bersamaan
sempat berbenturan dengan UiAutomation; putaran akhir dilakukan setelah pengguna
mengizinkan HP tidak disentuh. Perapian format dan guard pembersihan draf sesudah
uji hanya dikompilasi kembali, bukan diklaim sebagai putaran fisik tambahan.

[Paket](release.json): signer, hash kedua audio, WIP PROJECT_HANDOFF, stash/audio
archive, konfigurasi privat, logo dan master artwork tetap; pemindaian APK
terhadap nilai credential server yang dikenal menemukan0. Identitas client
Firebase sengaja berada di APK; private key pengirim tidak ada di sana. Audit
ini tidak menyatakan tidak mungkin ada rahasia yang belum dikenal.

C02 laptop mati belum diuji; B01 menunggu klarifikasi sumber. A–D, E, F/G dan
rilis publik tidak disebut selesai seluruhnya. [Gerbang sesi](gates.json).

## Penjadwal

Dua run GitHub `schedule` sukses nyata berjarak9 jam4 menit54 detik, sehingga
tidak memenuhi observasi lima menit. Pengguna menyetujui pengganti Cloudflare
hanya Free dan penyimpanan secret pemicu di sana. Dashboard menunjukkan
**Free / $0 / Current plan**. Akses lama403 saat upload; token terpisah hanya
Workers Scripts Write pada akun ini, kedaluwarsa10 Desember2026, disimpan lokal
privat. Token AI tidak diganti. Jalur Profile ditolak review otomatis; pembuatan
token memakai menu Account API tokens yang sesuai, tanpa membuka Profil.

[Deploy](cron-deployment.json) dan [konfigurasi](cron-configuration-verified.json):
satu `*/5 * * * *`, workers.dev/preview mati, satu secret binding pemicu, log
invocation aktif. `usageModel: standard` adalah metadata runtime Worker, bukan
bukti upgrade billing; paket akun diperiksa pada dashboard Free.
Konfigurasi berhasil tidak disamakan dengan observasi pemicu otomatis.
[Paket/runbook](../../../backend/deploy/earthquake-cron/README.md).

[Lima tes Worker](worker-tests.txt) lulus; backend lengkap diulang dengan Redis/
Lua lokal:79 lulus/0skip. Putaran tanpa pengaturan Lua sebelumnya68 lulus/11skip
tetap disimpan. Tidak menjalankan Redis sungguhan milik produksi melalui fixture.

## Hasil penjadwal setelah perbaikan

[Observasi otomatis](cron-observation.json) mencatat kegagalan09.35 WIB dan dua
run sukses09.40/09.45 WIB, dengan selisih scheduledTime300.000ms. Tidak memakai
pemicu manual sebagai bukti jadwal. Kode terpasang cocok hash source lokal,
secret binding tetap privat, worker.dev/preview nonaktif. [Perbaikan deploy](cron-deployment-repair.json).
[Dua run GitHub lama](github-schedule.json) tetap dicatat, tidak dianggap lima
menit. GitHub dibiarkan aktif sebagai cadangan; lease/deduplikasi Redis tetap
melindungi pemeriksaan bersamaan. **E02 selesai teknis beta terarah** berdasarkan
gabungan bukti ini dan [validasi kontrak/izin/restart sebelumnya](../earthquake-e02-2026-11-09/README.md).
Pengamatan ini dua sampel setelah perbaikan, bukan SLA, observasi berhari-hari,
jaminan semua subscriber/vendor, atau peringatan sebelum guncangan. G02/operasi,
C02 laptop mati, D02/D02a, B01 serta E03/E04 tetap mengikuti gerbang sendiri.
