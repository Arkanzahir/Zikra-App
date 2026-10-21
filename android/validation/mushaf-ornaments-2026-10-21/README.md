# Ornamen mengikuti detail referensi — 21 Oktober 2026

Fokus permintaan pengguna pada bentuk hiasan, bukan tata letak ayat:

- Sudut bingkai: sulur melengkung dengan daun dan bunga kecil.
- Ujung bingkai judul: finial floral simetris menggantikan garis/diamond geometris.
- Medali nomor: bintang delapan bersudut lembut dengan garis emas pucat berlapis,
  menggantikan medali bulat bawaan font. Nomor berasal dari metadata ayat API.

Ornamen digambar native sebagai path; font resmi tidak dimodifikasi. Penanda akhir
ayat hanya diganti ketika metadata `char_type_name=end`. Lebar slot glyph sumber
dipertahankan, urutan kata/baris dan posisi nomor tidak dipindah. Judul dan teks
bacaan tidak diubah. Ini pendekatan vektor terhadap gambar referensi resolusi kecil,
bukan klaim salinan identik piksel.

## Pemeriksaan tambahan dan perbaikan

Uji halaman 49 awal gagal dimuat. Pemeriksaan respons dan font resmi di komputer
menemukan kode glyph pertama 2:283 berisi U+0020 di antara dua glyph, sedangkan
font tidak menyediakan glyph untuk spasi. Parser metadata tetap valid, tetapi
pengukuran/penggambaran sebelumnya menolak karakter itu.

Renderer kini mempertahankan spasi pemisah sebagai advance 0.08 em (sesuai celah
dasar renderer), tanpa mencoba menggambarnya sebagai huruf font. Tidak ada huruf
yang dihapus/diubah. Uji regresi mencakup seluruh glyph halaman 49 dan empat medali
283–286. Respons/font audit hanya di `.local`, tidak dibundel ke aplikasi/Git.

## Bukti

[Al-Fatihah](screens/AlFatihah.png), [Al-Mulk](screens/AlMulk.png),
[halaman 49 / tiga digit](screens/Page49.png), [halaman 604](screens/Page604.png),
[audio asli aktif](screens/AudioActive.png), [malam + zoom](screens/NightZoom200.png).

Screenshot APK final diambil dari AVD Zikra_Mushaf_Test API 37, 1080×2400 / 420 dpi.
Al-Fatihah dan halaman 49 ditinjau secara visual, termasuk medali 283–286.

## Hasil final

- [Build/lint/JVM](build.txt): assembleDebug, assembleDebugAndroidTest,
  testDebugUnitTest dan lintDebug berhasil. 80 JVM, 0 gagal/error/skip, dengan
  snapshot/font audit lokal. Termasuk regresi tipe end dan glyph/spasi halaman 49.
- [Android final](device-verified-api37.txt): 4 metode unik lulus, 23.177 detik.
  Halaman 1/49/300/562/604, navigasi, font, menu, zoom/malam, perpindahan mode,
  input halaman/juz, mini player audio asli serta jeda/seekbar.
- [Ulangan terisolasi](device-retry.txt): tes halaman/font juga lulus.
- [Kegagalan awal halaman 49](device-before-space-fix.txt) dan
  [isolasi host](host-before-space-fix.txt) disimpan untuk menunjukkan temuan glyph.
- [Run terinterupsi](device-emulator-interrupted.txt) tidak dihitung lulus:
  setelah restart, System UI emulator macet dan pemuatan halaman 1 gagal; proses
  dihentikan. Dialog ditutup dan koneksi emulator dipulihkan sebelum run final.
- Tidak mengulang suite offline penuh/teks sistem 200% pada iterasi ornamen ini.

APK: `D:/Download/Zikra-App-main/Zikra-Mushaf-Ornaments-20261021-debug.apk`.
SHA256: `065721BC09040CA17FE7F48627AEE09CCEA9BB04C2EB32729679C7D87C750A49`.

Batas sebelumnya tetap: halaman online, Android 12+, belum sorotan kata emas,
belum audit visual seluruh 604 halaman atau uji HP fisik.
