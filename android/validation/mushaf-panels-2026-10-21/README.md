# Judul surah dan penanda ayat — 21 Oktober 2026

Panel atas sekarang berisi **سُورَةُ + nama surah** dari metadata aplikasi, dengan
huruf lebih besar dan ukuran otomatis dibatasi lebar/tinggi area kosong panel.
Judul dipusatkan menurut metrik font dan dibatasi ke area aman sebelum ornamen.
Awal surah berikutnya di halaman yang sama menggunakan panel teal senada,
menggantikan cartouche tipis sebelumnya. Contoh halaman 604 menampilkan Al-Ikhlas,
Al-Falaq dan An-Nas dengan panel terpisah.

Medali nomor ayat kini memakai garis teal yang lebih tegas, latar terang dan
angka lebih besar. Slot nomor diberi lebar minimum berdasarkan jumlah digit;
ukuran seluruh baris dihitung kembali agar tetap muat. Huruf, nomor, urutan ayat
dan pembagian baris sumber tetap sama; jarak horizontal dapat berubah karena
slot medali diperlebar. Ukuran bacaan juga dibatasi oleh tinggi baris agar harakat
tidak bertabrakan dengan baris di dekatnya. Tidak ada sorotan emas per kata baru.

## Bukti native

- [Al-Fatihah / judul atas](screens/AlFatihah.png)
- [Halaman 604 / tiga judul](screens/Page604.png)
- [Halaman 49 / angka tiga digit](screens/Page49.png)
- [Al-Mulk](screens/AlMulk.png)
- [Audio asli aktif](screens/AudioActive.png)
- [Mode malam + zoom 200%](screens/NightZoom200.png)
- [Teks sistem 200%](screens/SystemFont200.png)

Screenshot APK native dari AVD khusus Zikra_Mushaf_Test API 37, 1080×2400 / 420 dpi.
Judul atas, panel surah berikutnya, angka tiga digit, mode malam/zoom dan teks
besar ditinjau visual. Saat zoom 200%, bagian halaman di luar viewport dicapai
dengan geser; teks judul tetap dipasang berdasarkan kotak halaman penuh.

## Pengujian

- [Build/lint/JVM](build.txt): assembleDebug, assembleDebugAndroidTest,
  testDebugUnitTest dan lintDebug berhasil; 80 JVM, 0 gagal/error/skip menggunakan
  snapshot/font audit lokal yang sama dengan tahap sebelumnya.
- [Android](device-api37.txt): 4 metode unik lulus, 21.775 detik. Font/glyph,
  halaman 1/49/300/562/604, navigasi, zoom/malam, mode/ayat, menu, input halaman,
  juz, posisi terakhir, mini player audio asli dan jeda/seekbar.
- [Teks sistem 200%](device-font200-api37.txt): 1 metode diulang, lulus 3.573 detik;
  bukan metode unik kelima. Skala sistem dipulihkan ke 1.0.
- Suite offline lengkap tidak diulang pada perubahan tata letak ini.

APK: `D:/Download/Zikra-App-main/Zikra-Mushaf-Panels-20261021-debug.apk`.
SHA256: `5707158C9C77F8074501E04A492B70D3BC85158DB7EBB06030F6385431C89C6E`.

Batas tetap: Android 12+, halaman online, belum sinkron kata/offline Mushaf penuh,
belum audit visual seluruh 604 halaman atau pengujian HP fisik. Asal JPEG bingkai
tetap tercatat pada laporan mushaf-teal-2026-10-21.
