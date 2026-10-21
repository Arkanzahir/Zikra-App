# Perapian proporsi Mushaf — 21 Oktober 2026

Menindaklanjuti umpan balik pengguna bahwa susunan belum rapi:

- Menghapus reservasi satu baris judul dalam badan halaman ketika judul itu
  sudah dipasang pada panel luar. Basmalah dan bacaan mulai lebih dekat panel.
- Mengurangi ukuran judul atas dan panel antar-surah, menambah ruang di sekitar
  huruf agar tidak terasa memenuhi bingkai.
- Membatasi jarak antarkata. Jika perataan akan membuat celah terlalu besar,
  kelompok kata pada baris itu dipusatkan, tanpa memindah kata ke baris lain.
- Jarak vertikal halaman pembuka dibatasi; medali lebih bulat dan seimbang
  terhadap huruf/angka. Nomor, huruf, urutan ayat dan baris sumber tetap sama.

## Bukti aktual

[Al-Fatihah](screens/AlFatihah.png), [tiga surah halaman 604](screens/Page604.png),
[nomor tiga digit halaman 49](screens/Page49.png),
[mode malam/zoom](screens/NightZoom200.png), [teks sistem 200%](screens/SystemFont200.png).

Screenshot berasal dari APK native AVD Zikra_Mushaf_Test API 37, 1080×2400 / 420 dpi.
Al-Fatihah dan halaman 604 ditinjau visual. Ruang sisa di bawah Al-Fatihah tidak
dipakai untuk memaksa jarak antarbaris menjadi terlalu besar. Ukuran 200% tetap
menggunakan pergeseran halaman. Ini tata letak aplikasi, belum sertifikasi cetak.

## Validasi

- [Build/lint/JVM](build.txt): assembleDebug, assembleDebugAndroidTest,
  testDebugUnitTest, lintDebug berhasil; 80 JVM, 0 gagal/error/skip dengan audit lokal.
- [Android](device-api37.txt): 4 metode unik lulus, 22.589 detik. Halaman/font,
  navigasi, zoom, malam, mode/ayat, menu, validasi halaman/juz, simpan posisi,
  serta mini player/audio asli dan jeda/seekbar.
- [Teks sistem 200%](device-font200-api37.txt): satu metode diulang dan lulus
  5.122 detik. Skala sistem dikembalikan ke 1.0. Tidak dihitung sebagai metode
  unik kelima. Suite offline lengkap tidak diulang pada perubahan jarak ini.

APK: `D:/Download/Zikra-App-main/Zikra-Mushaf-Tidy-20261021-debug.apk`.
SHA256: `357FA8051F7B735629FE0FF68260FBC3A01A7414554A9AF7D78DA55D9D7BA765`.

Tetap Android 12+, halaman online, belum sorotan kata emas/offline Mushaf penuh,
belum audit visual seluruh halaman dan belum pengujian HP fisik.
