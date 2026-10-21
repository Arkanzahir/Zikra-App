# Bingkai teal dari gambar pengguna — 21 Oktober 2026

Pengguna meminta langsung memakai ornamen teal pada gambar yang diunggah,
setelah menunjukkan sumber Pinterest. File JPEG pengguna disalin utuh ke
`app/src/main/res/drawable-nodpi/mushaf_teal_reference.jpg` (650×877).
Referensi pin yang ditunjukkan: https://id.pinterest.com/pin/1089871178545353907/.
Ini catatan asal gambar dan instruksi pengguna, bukan hasil pemeriksaan lisensi.

Renderer mengambil bagian tepi/panel gambar saat menggambar: sulur empat sisi,
garis berlapis, panel atas dan bawah. Strip sisi disesuaikan ke tinggi halaman
tanpa sambungan tile berulang. Warna kertas diselaraskan ke biru muda, mode malam
menggunakan filter warna. JPEG sumber tetap utuh; resolusinya membatasi ketajaman
ketika diperbesar. Ini pemakaian gambar asli, bukan klaim hasil vektor.

Pusat panel atas yang mengandung kaligrafi gambar tidak digambar. Nama surah
ditulis oleh aplikasi dari metadata yang sudah ada. Isi Quran, glyph, nomor ayat,
basmalah dan urutan baris tetap memakai sumber aplikasi. Judul surah awal halaman
berada di panel atas; judul surah berikutnya pada halaman yang sama tetap di slot
baris sumber. Margin dan jarak panel ditambah agar tidak menutup huruf/harakat.

## Bukti native

- [Al-Fatihah](screens/AlFatihah.png)
- [Al-Mulk](screens/AlMulk.png)
- [Halaman 49](screens/Page49.png)
- [Halaman 604 / tiga surah](screens/Page604.png)
- [Audio asli aktif](screens/AudioActive.png)
- [Mode malam + zoom 200%](screens/NightZoom200.png)
- [Teks sistem 200%](screens/SystemFont200.png)

Screenshot diambil dari APK native pada AVD khusus Zikra_Mushaf_Test API 37,
1080×2400 / 420 dpi. Tampilan Al-Fatihah, halaman 49/604, malam/zoom dan teks
besar diperiksa secara visual. Pada viewport sempit/zoom, halaman tetap dapat
digeser. Tidak ada screenshot konsep yang dilabeli sebagai hasil APK.

## Validasi

- [Build](build.txt): assembleDebug, assembleDebugAndroidTest, testDebugUnitTest,
  lintDebug berhasil. 80 JVM, 0 gagal/error/skip dengan snapshot/font audit lokal.
- [Android](device-api37.txt): 4 metode unik lulus, 19.09 detik. Font/glyph,
  halaman 1/49/300/562/604, navigasi, zoom, malam, mode/ayat, menu sumber,
  validasi halaman/juz, simpan posisi, mini player audio asli dan jeda/seekbar.
- [Teks sistem 200%](device-font200-api37.txt): 1 metode diulang, lulus 3.721 detik,
  bukan metode unik kelima. Skala sistem dipulihkan ke 1.0.
- Suite offline penuh tidak diulang untuk perubahan tampilan ini.

APK: `D:/Download/Zikra-App-main/Zikra-Mushaf-Teal-20261021-debug.apk`.
SHA256: `7E3DCF9C28AAD9C5B06EA3A1634D31B86452FF9A59051B3692D099F0CBAC45C3`.
SHA256 JPEG: `55331951B9C14189F3E9E74537C2CD8F79F43353855D7A86D21CD95E822BD1EB`.

Batas tetap: Android 12+, halaman online, belum sorotan kata emas/offline penuh,
belum audit visual semua 604 halaman atau pengujian HP fisik.
