# E03 — ketepatan slot dan transport cuaca, 11 November 2026

Subset teknis E03, **bukan seluruh E03 atau kesiapan rilis publik**. HP/USB dan
emulator tidak diakses. Layout, aset, font dan notifikasi yang disetujui tetap.

## Masalah dan hasil perubahan

Selector umum sebelumnya memilih slot terakhir ketika seluruh deret sudah lewat.
Parser Open-Meteo menggunakannya tanpa memeriksa cakupan waktu deret; waktu unduh
yang baru tidak menjamin prediksi masih mencakup waktu pengguna. Contoh: data
72 jam yang berakhir pukul 23.00 masih diterima saat pukul 00.00 berikutnya.

Parser kini memakai pemeriksaan khusus deret per jam: interval wajib tepat satu
jam, epoch Unix pecahan ditolak, slot terpilih tidak boleh lebih dari satu jam
di depan waktu permintaan dan cakupan berakhir satu jam sesudah slot terakhir.
Di luar cakupan, data ditolak melalui jalur gagal/cache yang sudah ada.
Protector dan detail cuaca juga mengosongkan slot yang melewati masa berlakunya
saat refresh atau kegagalan pembaruan, termasuk bila waktu unduh masih baru. Pemilihan
prakiraan mendatang terdekat dan label waktu sumber dipertahankan; bukan klaim
pengukuran cuaca langsung. Selector BMKG dengan aturan slot berbeda tidak diubah.

[Dokumentasi Open-Meteo](https://open-meteo.com/en/docs) menjadi acuan data hourly
dan opsi waktu Unix/zona lokasi. Aturan penolakan di atas adalah validasi aplikasi
untuk endpoint per jam yang diminta, bukan jaminan akurasi kondisi di lapangan.

HTTP cuaca kini menolak semua redirect, termasuk perubahan protokol, sehingga
koordinat pada permintaan tidak diteruskan ke endpoint redirect. Retry koneksi
otomatis dimatikan; connect10/read25/write10/call25 detik. Endpoint HTTPS,
parameter, batas respons 256 KiB, cache segar 10 menit, fallback maksimal enam
jam dan empat lokasi tetap. Tidak ada API key, layanan atau paket baru.

## Verifikasi

- [Tes/lint](tests.json): 189 tes lulus, 4 dilewati, 0 gagal/error; lint 0 error,
  878 warning. Delapan tes baru mencakup tiga zona lokasi, data lama/future/gap/
  duplikat/epoch pecahan, lokasi/zona/unit salah dan HTTP nyata lokal yang
  mengembalikan redirect. Endpoint tujuan tidak menerima koneksi.
- [Probe provider](live-probe.json): satu GET host pada koordinat contoh publik
  yang sudah ada di repo, HTTP200, 72 slot lolos parser baru, Asia/Jakarta.
  Bukan GPS HP. Probe memakai transport JDK tanpa redirect; factory Android
  diverifikasi terpisah oleh regresi HTTP lokal, bukan diklaim diuji di HP.
- Build APK/debug berhasil. Tidak menjalankan ulang native/emulator atau
  mengambil screenshot karena tidak mengubah layout. Sisa perilaku perangkat,
  lifecycle UI dan jaringan vendor bukan bukti yang dihasilkan sesi ini.
- [Audit APK/preservasi](apk.json): tidak ditemukan nilai credential server yang
  dikenal atau file konfigurasi privat; signer dan pasangan audio tetap, WIP,
  stash, arsip audio, env, logo dan master foreground cocok hash awal. Audit
  terbatas pada nilai rahasia yang dikenal dan pola yang diperiksa.

## APK dan sisa pekerjaan

1.9.40/code67, 49.680.570 byte, SHA-256:
`8263961c8db002ed1b537704aced709b8c8bf1ff7a5d7a4a1d70f71313d54fcf`.
Salinan penyerahan: `D:\Download\Zikra-App-main\Zikra-Weather-v1.9.40-beta-debug.apk`.
HP terakhir 1.9.39; APK baru belum dipasang atau diunggah ke Drive.

[Cakupan](scope.json): E03 tetap terbuka untuk kontrol alert cuaca yang jelas,
ambang/sumber yang dinyatakan, opt-in dan deduplikasi, doa perjalanan/pagi/petang,
serta briefing pagi dari data lokal pilihan pengguna. Tidak mengaktifkan
notifikasi baru atau mengirim data pribadi ke AI secara otomatis. D02/D02a,
B01/C02/E04/F/G/publik juga tidak ditutup oleh perubahan ini.
