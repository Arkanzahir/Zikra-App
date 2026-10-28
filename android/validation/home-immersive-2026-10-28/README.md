# Home Imersif — beta 1.9.16, 28 Oktober 2026

Pengguna menyetujui referensi **Imersif** lalu meminta penerapan. Kandidat ini
memperbaiki kontras Home, nama lokasi, bingkai ikon dan pilihan audio.
[Source, APK dan hash](release.json), [audit paket/build](audit.json),
[hasil native akhir](native-results.json), [upgrade](upgrade.json),
[isolasi emulator](preservation.json).

Pengguna mengizinkan publikasi secara eksplisit. Source`38cb23a` dan
receipt`388bc66` sudah dipush ke origin/main pada28 Oktober2026; hash remote
`388bc662c841994493b17ac2a2981231f15508e9` cocok saat verifikasi. Penolakan
persetujuan otomatis sebelumnya sudah teratasi dengan izin pengguna.

## Hasil penerapan

- Home memakai Plus Jakarta Sans, latar terang lembut, kartu putih dan simbol
  fitur asli. Tombol setengah lingkaran di kanan atas membuka **Tema Home**:
  Terang, Gelap, Ikuti perangkat. Bawaan Terang; pilihan tersimpan. Suasana
  masjid/awan tetap mengikuti waktu lokasi, terpisah dari tema permukaan Home.
- Ikon Lanjut membaca memakai latar dan tinta yang sama-sama mengikuti tema;
  keterangan sekunder dan panah juga mengikuti palet. Identitas delapan fitur
  dan logo master tidak digambar ulang. Penyesuaian token palet harian juga
  berlaku pada halaman lain yang memakai resource refined yang sama.
- Header memuat nama daerah, kota dan zona. Pada teks besar, lokasi pindah ke
  baris penuh agar tidak terpotong. Nama daerah dicari dengan Android Geocoder
  hanya sesudah pengguna meminta lokasi; hasil dibatasi empat detik dan gagal
  tidak mengganti koordinat/zona dengan tebakan kota terdekat. Label generik
  lama tetap sampai lokasi diperbarui. Koordinat tetap tersedia di rincian.
- Logo launcher diberi bidang 108dp dengan geometri utuh dalam area aman;
  Android 13+ juga mendapat lapisan monochrome. Ikon kecil notifikasi memakai
  identitas bulan/bintang/Quran yang sama. Bingkai dan warna notifikasi akhir
  tetap mengikuti SystemUI perangkat.
- Azan biasa pada kandidat debug kini memakai rekaman Alafasy pilihan terbaru
  pengguna, Subuh tetap rekaman sebelumnya. Label Makkah diperbarui. Dependensi
  build memperbarui aset audio sebelum penggabungan resource sehingga cache
  tidak diam-diam mempertahankan MP3 lama. Kedua entri APK cocok hash berkas
  yang dipilih; MP3 tetap privat dan tidak dimasukkan Git/release source.
- Tautan **Jadwal · perhitungan lokal** menjelaskan asal dan parameter hitungan.
  Rumus dan tambahan kehati-hatian dua menit tidak diubah; belum menyamakan
  jadwal dengan Google atau feed resmi Kemenag.

## Pemeriksaan

**146 unit lulus / 4 dilewati**, lint **0 error / 819 warning**.
**12 eksekusi/metode native akhir lulus** di emulator sekali pakai
`ZikraApproved` API37.1, terpisah dari AVD/data pengguna:

- APK 1.9.15 asli dipasang, fixture berisi dibuat melalui kelas versi lama,
  lalu diperbarui dengan `install -r`. Lima database, nilai preferensi, file
  tahan lama dan pembacaan kredensial dummy terenkripsi tetap utuh setelah
  12 pembukaan halaman dan process restart.
- Home bawaan terang saat sistem gelap; pemilihan Gelap melalui dialog nyata,
  recreation dan pembukaan ulang mempertahankan tema. Rasio kontras teks dan
  ikon Lanjut membaca serta keterangan sekunder minimal 4,5:1 pada dua tema.
- Nama lokasi tampil dan membuka layar lokasi tidak mengubah titik pilihan.
  Font200%/320dp mengalirkan header dan dua kolom fitur; seluruh nama lokasi
  terjaga tanpa ellipsis. Strip jadwal tetap dapat digeser horizontal.
- Geometri foreground launcher berada dalam area lingkaran aman; bagian Quran
  pada ikon notifikasi tetap ada. Kedua MP3 terbaca metadata Android, benar-benar
  didekode/diputar dengan posisi maju, dan aksi Stop terikat sesi yang tepat.
- Panel notifikasi asli memperlihatkan logo utuh; sentuhan pada **Hentikan
  adzan** menghentikan player dan menghapus notifikasi. Tidak membuka aplikasi
  sebagai pengganti tindakan Stop.

Review awal menemukan nama lokasi terpotong pada font200% dan dikoreksi.
Audit APK awal menemukan cache MP3 lama; dependensi build diperbaiki dan paket
akhir dicocokkan hash. Putaran pertama uji panel gagal karena runner belum
mengirim sentuhan yang ditunggu penguji, bukan bukti tombol ditekan tetapi
gagal; [catatan percobaan](earlier-attempt.json). Putaran akhir menggunakan
tangkapan layar aktual lalu input sentuh dan lulus. Percobaan gagal tidak
dihitung sebagai hasil akhir lulus.

## Gambar APK asli

- [Home terang](native/approved-home/home-light.png)
- [Home gelap](native/approved-home/home-dark.png)
- [Home font200%](native/approved-home/home-large-text.png)
- [Lokasi](native/approved-home/location.png)
- [Notifikasi SystemUI](native/validation/D02-notification-shade-100.png)
- [Pemutaran Alafasy](native/validation/D02-playing-alafasy-100.png)

## Batas dan kelanjutan

APK debug **41.430.239 byte**, di bawah100.000.000byte. Bukan signed release
publik. Tidak menyentuh HP/data pribadi atau AVD pengguna; helper dihapus dan
emulator uji ditutup. Emulator memakai audio output nonaktif: pengujian decoder
dan kontrol bukan penilaian suara melalui speaker HP atau telaah lafaz.

Nama daerah pada bukti memakai fixture dari lokasi awal publik aplikasi;
bukan pembuktian hasil geocoding langsung di HP. Ketersediaan/ketepatan layanan
Geocoder vendor, nama wilayah setempat, suara/baterai vendor dan semua bentuk
launcher masih memerlukan pemeriksaan perangkat. Android memperingatkan bahwa
[Geocoder dapat tidak tersedia/gagal](https://developer.android.com/reference/android/location/Geocoder).
[Aturan area ikon adaptif](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)
menjadi acuan bingkai.

D01 tetap sesuai bukti fisik sebelumnya. D02/D02a tetap terbuka untuk sumber/
ketentuan distribusi audio dan pengujian HP yang tersisa. F01/F03 di sini hanya
subset Home/lokasi/branding; seluruh F, B01, C/E/G dan kesiapan publik tidak
dicentang dari hasil ini. WIP PROJECT_HANDOFF, stash audio, konfigurasi privat,
logo/ikon asli, desain AI dan APK lama dipertahankan.
