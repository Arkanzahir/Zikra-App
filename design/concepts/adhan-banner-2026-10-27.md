# Notifikasi adzan — konsep disetujui 27 Oktober2026

Pengguna menyetujui penerapan banner atas layar dengan tindakan **Hentikan adzan**
dan kontrol yang tetap tersedia di panel notifikasi. [Preview konsep](adhan-banner-2026-10-27.png)
adalah gambar generatif untuk arah desain, bukan screenshot Android atau jaminan
susunan yang identik pada setiap merek HP.

- Identitas Zikra/ikon yang sudah ada, judul **Waktu Maghrib** (mengikuti salat),
  keterangan **Adzan sedang diputar**, satu tindakan berhenti yang jelas.
- Kartu ringkas: judul/status di kiri dan tombol mint/hijau di kanan. Banner dan
  panel diperluas: judul/status serta satu aksi berhenti bawaan Android. Header/app-name,
  perluasan, bentuk kartu dan pemunculan tetap mengikuti sistem.
- Palet pine/mint; teks terang/gelap menyesuaikan night resources. Tombol ringkas
  minimal48dp, font mengikuti sistem; judul/status ringkas boleh dipotong dengan
  ellipsis, isi penuh tetap tersedia di kartu yang diperluas. Pada font>=150%,
  kartu ringkas memakai labelStop dan contentDescription lengkap, tanpa subjudul
  ringkas agar tombol tidak terpotong.
- Pemutaran otomatis memakai kategori adzan berprioritas tinggi; preview tetap
  kategori playback tenang. Tidak ada nada sistem tambahan di atas rekaman.
- Pengaturan banner membuka kategori sistem yang tepat. Pengaturan lama tidak
  dihapus/dinaikkan; blok playback lama tetap dihormati bersama kategori baru.
- Menutup banner tidak mengirim perintah stop; tombol berhenti mengirim perintah
  ke service dengan token sesi, tanpa membuka Activity. Mengetuk kartu membuka
  jadwal. Izin/DND/senyap tetap berlaku dan tidak memakai overlay/full-screen.

Implementasi XML ringkas/diperluas dan `DecoratedCustomViewStyle` mengikuti
[batas layout notifikasi Android](https://developer.android.com/develop/ui/views/notifications/custom-notification).
Kategori baru dibutuhkan karena tingkat gangguan kategori lama tidak dapat
dinaikkan setelah dibuat; pengguna tetap mengontrol perilakunya melalui
[pengaturan kanal](https://developer.android.com/develop/ui/compose/notifications/channels).

[Bukti runtime dan batas beta](../../android/validation/adhan-banner-2026-10-27/README.md).
D02/D02a tetap terbuka untuk gerbang HP/sumber audio; konsep tidak menutup tugas.
