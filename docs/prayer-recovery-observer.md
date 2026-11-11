# Pengamatan pemulihan alarm dari laptop

Alat `tools/observe_prayer_recovery.py` hanya membaca HP yang dipilih secara
eksplisit. Tidak memasang paket, membuka aplikasi, menjalankan instrumentation,
restart, menghentikan proses, mengubah izin, daya, jadwal atau preferensi.
Pengujian fisik tetap memerlukan kesiapan pengguna; jangan menjalankannya karena
HP pernah tersambung pada sesi sebelumnya.

## Mengapa metode diganti

Snapshot nol alarm terlalu awal tidak membuktikan kegagalan boot. Pada sesi
11 November, proses receiver baru mulai 41,718 detik setelah snapshot awal;
kemudian aplikasi terbuka sebelum tindak lanjut independen. Instrumentation yang
menunggu di HP juga dapat tertunda saat idle. Hasil fisik lama tetap belum
konklusif, tidak diganti menjadi lulus oleh perbaikan alat ini.

Android membatasi pekerjaan saat Doze; alarm yang diizinkan saat idle mempunyai
aturan tersendiri. Lihat [dokumentasi Android](https://developer.android.com/training/monitoring-device-state/doze-standby).

## Sebelum pengamatan

1. Cadangkan dan verifikasi data sesuai prosedur proyek. Catat izin dan pengaturan
   awal. Gunakan APK beta debug yang dapat dibaca melalui `run-as`.
2. Pastikan instrumentation sudah selesai. Buka Zikra normal untuk memasang
   jadwal asli. Jangan membuat alarm dari instrumentation yang masih berjalan.
3. Simpan baseline dengan serial perangkat yang sudah diverifikasi. Contoh
   berikut memakai placeholder, bukan perintah siap dijalankan:

```powershell
python tools/observe_prayer_recovery.py snapshot --serial SERIAL --output .local/d02-baseline.json
```

Semua keluaran perangkat wajib di `.local`, tidak ditimpa jika sudah ada.
Alat tidak menyimpan isi chat, nama profil, koordinat, token, isi notifikasi atau
log mentah. Pembacaan sistem sementara dapat memuat metadata aplikasi lain;
hanya boolean dan metadata alarm Zikra dipilih, sisanya dibuang. Identitas boot
disimpan sebagai hash. Jangan mempublikasikan baseline pribadi.

## Restart

Jalankan observer, lalu restart hanya setelah dikoordinasikan dengan pengguna:

```powershell
python tools/observe_prayer_recovery.py boot --serial SERIAL --baseline .local/d02-baseline.json --timeout 180 --output .local/d02-boot-result.json
```

Pengguna membuka kunci pertama kali memakai PIN/pola, tetap di layar utama dan
tidak membuka Zikra. Observer menunggu boot baru, boot selesai dan data aplikasi
terbuka. Dua snapshot lengkap berturut-turut harus mempunyai himpunan alarm
masa depan yang sama dengan baseline. Alarm yang waktunya sudah lewat diabaikan.
Putus USB, data terkunci, snapshot tidak lengkap dan nol alarm awal bukan gagal
aplikasi. Jika Zikra/instrumentation terbuka, hasil menjadi belum konklusif.
Jendela tidak cukup sesudah unlock juga belum konklusif.

Deteksi pembukaan aplikasi memakai aktivitas aktif dan log sistem terbatas;
log yang hilang/terpotong tidak membuktikan aplikasi tidak pernah dibuka.
Operator tetap perlu mengawasi agar tidak ada pembukaan aplikasi.

## Idle dan hemat baterai

Pilih satu alarm **asli**, exact, masih di masa depan pada baseline. Pastikan
pengguna memang mengaktifkan mode ADHAN untuk salat itu dan master pengingat.
Mulai pengamatan menjelang waktu tersebut, maksimal 900 detik:

```powershell
python tools/observe_prayer_recovery.py doze --serial SERIAL --baseline .local/d02-baseline.json --index 2 --epoch EPOCH_MILIDETIK --timeout 180 --output .local/d02-idle-result.json
```

Observer harus terlebih dahulu melihat alarm exact yang dipilih sebelum waktunya,
tanpa layanan/notifikasi adzan yang sedang berjalan dan tanpa marker pengiriman
untuk epoch tersebut. Lalu diperiksa marker epoch yang sama, layanan foreground
AdhanService dan notifikasi adzan, layar mati, status IDLE dan setting hemat
baterai aktif. Ini mengurangi kemungkinan preview atau pengiriman lama dihitung.

Observer **tidak** membuat kondisi idle/hemat baterai. Idle alami atau langkah
daya terkontrol perlu rencana dan persetujuan tersendiri; jangan mengubahnya
diam-diam. `power_save_setting` adalah setting global, bukan pemeriksaan langsung
`PowerManager.isPowerSaveMode()`. Pembacaan berlangsung berurutan, tidak atomik.
Setiap pembacaan dibatasi lima detik; batas waktu keseluruhan dapat terlewati
hingga satu putaran pembacaan. Sound audibel, rekaman selesai alami, ketepatan
jadwal, izin, serta perilaku vendor semalaman tetap harus diverifikasi terpisah.

## Keluaran dan batas

`observed` berarti kondisi teknis yang diperiksa terlihat, **bukan D02 selesai**.
Alat selalu mengeluarkan `physicalGateAutomaticallyClosed: false`. Timeout
menghasilkan `inconclusive`, bukan vonis bug aplikasi. Hentikan dengan Ctrl+C
jika pengguna perlu HP. Tidak ada pembersihan perangkat karena alat tidak
menulis ke perangkat.

Tes tanpa HP:

```powershell
python -m unittest discover -s tools -p test_observe_prayer_recovery.py -v
python tools/observe_prayer_recovery.py replay --fixture android/validation/prayer-observer-2026-11-11/delayed-boot-fixture.json
```

Replay menggunakan data sintetis; tidak boleh dijadikan bukti fisik. Instrumentation
`PhonePrayerRecoveryTest` masih berisi helper arm/restore lama yang mengubah data;
jangan menjalankan seluruh kelas untuk menggantikan observer ini.
