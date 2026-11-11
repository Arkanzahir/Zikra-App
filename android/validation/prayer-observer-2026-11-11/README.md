# D02 — metode pengamatan diperbaiki, 11 November 2026

**Bukan kelulusan uji fisik D02/D02a.** HP dan emulator tidak diakses pada
checkpoint ini. APK utama tetap 1.9.39; tidak ada pemasangan, restart, perubahan
daya/izin/data, audio, backend atau konfigurasi privat.

## Perubahan

- Observer laptop hanya memakai daftar perintah baca yang diizinkan, serial
  eksplisit, keluaran privat tanpa menimpa berkas, dan hasil yang tidak otomatis
  mencentang gerbang. [Prosedur dan batas](../../../docs/prayer-recovery-observer.md).
- Boot menunggu boot baru/unlock, lalu dua himpunan alarm masa depan yang stabil.
  Snapshot nol awal, snapshot tidak lengkap dan timeout bukan vonis bug.
  Pembukaan aplikasi/instrumentation menjadikan hasil belum konklusif.
- Idle diamati dari laptop terhadap alarm asli yang lebih dahulu terlihat,
  marker epoch, layanan dan notifikasi adzan, layar, idle dan setting hemat
  baterai. Tidak menunggu di proses instrumentation yang dapat tertunda sendiri.
  Metode `testAlarmDuringForcedIdle` lama dihapus dari helper; hasil timeout
  sebelumnya tetap pada checkpoint terdahulu.
- [Draf izin dua rekaman](../../../docs/audio-permission/request-template.md)
  dibuat, tidak dikirim. Penerima dan kewenangan belum diverifikasi.

## Verifikasi

[checks.json](checks.json) menyimpan hasil ringkas:

- 15 tes Python lulus. Percobaan awal 12 lulus/1 gagal menemukan bahwa nama
  paket notifikasi `.test` ikut diterima. Pencocokan paket diperbaiki dan
  regresinya kini lulus. Tidak menyembunyikan percobaan awal.
- Replay sintetis: snapshot nol pada detik 0, pulih pada detik 42 dan stabil
  pada detik 44, menghasilkan `observed` dengan gerbang tetap false.
- Parser pada dump privat sesi terdahulu mendapatkan 26/31/0/31 alarm aktif,
  semuanya exact pada dump yang memiliki alarm. Pada dump akhir, sepuluh tag
  riwayat pembatalan tidak ikut dihitung. Data perangkat mentah tetap privat.
- `assembleDebugAndroidTest -PzikraPhoneTests=true` berhasil sesudah metode
  lama dihapus. Tidak memasang helper atau mengulang unit/lint runtime Android,
  karena source aplikasi produksi tidak berubah.

## Batas yang tetap

Belum ada pengamatan perangkat nyata dengan observer baru. Pembacaan sistem
berurutan bukan snapshot atomik; log boot terbatas dapat kehilangan riwayat.
Setting hemat baterai bukan pengukuran langsung PowerManager. Layanan/notifikasi
tidak membuktikan suara audibel atau pemutaran selesai. Uji restart independen,
Doze fisik, perilaku vendor jangka panjang, izin distribusi dan pemeriksaan lafaz
masih perlu bukti. C02/B01/E03/E04/F/G/publik juga tidak berubah status.
