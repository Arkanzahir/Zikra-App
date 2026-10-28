# Lokasi dan langit Home — beta1.9.17/code44

Pengguna menyetujui pengerjaan pemilih lokasi pada referensi, kemudian penerapan ke Android. Checkpoint ini menerapkan langit yang diperbaiki pada Home dan mempertahankan pemilih GPS/kota manual yang sudah ada. [Receipt/source/hash](release.json), [audit APK/unit/lint](audit.json), [native akhir](native-results.json), [upgrade](upgrade.json), [isolasi](preservation.json).

## Hasil

- Nama lokasi dan panah bawah berukuran16dp berada dalam area sentuh Home minimal48dp yang sama. Keduanya membuka `LocationActivity`: lokasi HP atas permintaan atau pilihan kota manual. Label aksesibilitas menjelaskan pilihan tersebut. Tidak ada GPS otomatis baru, perubahan izin, atau penggantian titik lokasi tersimpan.
- Saat kembali dari pemilih, alur `LocationStore` yang sudah ada memperbarui nama daerah/kota, zona, jam, tanggal, jadwal, langit dan hitung mundur. Batal konfirmasi tidak mengubah pilihan. Rumus `PrayerSchedule`, tambahan kehati-hatian dan metode pengingat tidak diubah.
- Header lebih tenang: tanggal, jam setempat yang lebih besar, nama salat tujuan, hitung mundur24sp dan jam salat. Sapaan tambahan disembunyikan. Tema Home yang sudah tersimpan tetap berlaku; tombol tema tetap tersedia. Pada font besar, lokasi dan zona jam mengalir ke baris tersendiri tanpa mengecilkan teks pengguna.
- Matahari kecil dan bercahaya lembut bergerak melintasi ilustrasi sesuai terbit/transit/terbenam lokasi. Warna fajar/senja berubah bertahap, malam memuat bintang dan bulan19unit desain dengan detail halus. Arah dan posisi gambar tetap ilustratif, bukan kompas atau peta terbit bulan.
- Awan menggunakan [WebP transparan](../../../design/assets/home-sky-realistic-2026-10-28/cloud-bank-v1.webp) hasil built-in imagegen dari checkpoint referensi sebelumnya; [prompt](../../../design/assets/home-sky-realistic-2026-10-28/PROMPT.txt). Dua lapis memiliki pergeseran lambat dan respons parallax berbeda. Awan dekoratif, bukan cuaca langsung.
- Hanya bitmap masjid siang tanpa lampu yang digunakan pada seluruh waktu; tidak lagi menumpuk dua kubah berbeda. Kecerahan berubah, lampu fasad ditambahkan secara terpisah. Masjid dan lampu bergerak dalam satu transformasi parallax.
- Siklus gambar dibatasi jeda33ms, aset dan shader digunakan ulang, warna diperbarui saat detik lokal berubah. Sentuhan memiliki prioritas atas sensor gravitasi selama jari menempel; status gerak yang sama tidak membaca ulang pengaturan setiap callback gulir. Gerak/sensor berhenti ketika Home dijeda, tidak terlihat, kehilangan fokus, penghemat daya atau skala animasi sistem mematikannya. Waktu animasi disimpan selama jeda agar awan tidak melompat ke awal saat dilanjutkan. Teks tetap diam.

## Bukti Android

**147 unit lulus,4 dilewati; lint0 error/821 warning.** **9 eksekusi native akhir lulus** pada emulator khusus `ZikraApproved` API37.1, tanpa HP/data pengguna:

1. APK1.9.16 asli dipasang, fixture berisi dibuat, lalu `install -r` kandidat1.9.17. Lima database, nilai preferensi, file tahan lama dan kredensial dummy terenkripsi terjaga;12 pembukaan halaman dan process restart lulus. Ini data uji publik/sintetis, bukan data HP.
2. Sentuhan nyata pada nama lokasi dan panah membuka pemilih. Konfirmasi Makassar menghasilkan09.45WITA, Zuhur11.49 dan02:04:00 pada instant uji yang sebelumnya08.45WIB. Batal Jayapura menjaga Makassar; menerima Jayapura menghasilkan10.45WIT dan tetap tersimpan setelah Home dibuka ulang. [Bukti](native/location.json), [Makassar](native/location-makassar.png), [Jayapura](native/location-jayapura.png).
3. Hitung mundur08.45→02:30:00,11.14.59→00:00:01,11.15→Asar03:13:00,20.30→Subuh besok07:15:00. Geometri masjid identik pada05.00,11.39,16.50,17.23,20.30. Dua bitmap yang didekode berjumlah **1.923.072 byte**; bitmap masjid malam lama tidak dimuat. [Bukti](native/sky-geometry-countdown.json), [siang](native/sky-11-39.png), [senja](native/sky-16-50.png), [malam](native/sky-20-30.png).
4. Hitung mundur asli bergerak dengan jam nyata. Perbandingan tangkapan layar selama2,4detik menemukan24.951piksel berubah di wilayah awan; wilayah yang sama identik saat dijeda dan setelah animasi sistem dimatikan. Parallax merespons sentuhan nyata dan sensor tidak menimpa target jari. Gerak berhenti saat digulir keluar layar dan kembali aktif saat terlihat. [Bukti](native/motion.json).
5. Font200%/320dp: nama lokasi, panah, jam dan hitung mundur tidak terpotong; zona jam berpindah ke baris terpisah. Strip jadwal dapat digeser seperti sebelumnya. [Gambar](native/large-text.png).

Audio privat Subuh dan Alafasy biasa tetap cocok hash dengan file pilihan pengguna; tidak dimasukkan Git. APK40.528.718byte, di bawah100.000.000byte. Penguji dihapus, alias keystore fixture dipulihkan dan emulator ditutup.

## Percobaan penguji yang dikoreksi

[Riwayat gagal](earlier-native-attempts.json) tidak dihitung lulus. Penguji awal memakai koordinat relatif jendela untuk tombol dialog. Koreksi berikutnya memakai titik awal layar ikon yang diputar, bergeser ke tombol Tema Home di sebelah panah; [layar gagal](earlier/picker-missing-theme.png). Satu percobaan pencatatan juga mengasumsikan baris kota dinamis memiliki ID resource dan gagal; pencatatan diperbaiki. Penguji akhir memakai batas terlihat yang ditransformasikan ditambah posisi root jendela dan menunggu fokus. Sentuhan fisik terinjeksi akhirnya menekan nama, panah, baris kota dan tombol dialog yang benar. Tidak memakai `performClick` sebagai pengganti sentuhan akhir tersebut. Source produk tidak diubah untuk meloloskan kesalahan koordinat penguji.

## Referensi dan batas

[Referensi interaktif/aset](../../../design/assets/home-sky-realistic-2026-10-28/README.md) kini memiliki panah dan pilihan empat kota. Data contoh tanggal27–31Oktober2026 dihitung dengan kelas `PrayerSchedule`/`SkyDay` aplikasi pada koordinat publik; pemilihan kota mempertahankan instant sehingga zona/tanggal lokal ikut berubah. Browser pada320/390/736px memeriksa pemilih, tiga zona, pergantian tanggal tengah malam, jadwal, countdown dan gerak. Referensi tidak mengakses GPS; tombolnya menjelaskan bahwa GPS tersedia di APK.

Uji ini tidak membuktikan akuisisi GPS/geocoding vendor yang baru, suara speaker HP, seluruh bentuk perangkat, baterai/jank HP atau akurasi cuaca. API37.1 menaikkan RAM emulator dari permintaan1536 menjadi4096MB; ini bukan kebutuhan RAM aplikasi. Bukti alokasi bitmap bukan pengukuran seluruh heap/energi. D01 mengikuti bukti fisik terdahulu; D02/D02a tetap terbuka untuk sumber audio dan sisa HP. F01/F04 hanya subset Home, seluruh F/G dan rilis publik tidak ditutup. WIP `PROJECT_HANDOFF.md`, file privat dan APK lama tetap dipertahankan.
