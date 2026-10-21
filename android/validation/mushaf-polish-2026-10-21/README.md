# Poles tampilan Mushaf — 21 Oktober 2026

Mengikuti arah referensi pengguna: kertas ivory, bingkai sage ganda dengan sudut
geometris tipis, judul surah dalam ornamen sand gold, metadata terpusat, judul UI
serif, dan player khusus Mushaf dengan informasi disusun dalam dua baris.
Al-Fatihah dan halaman pembuka kedua lebih lapang. Ukuran halaman dipadatkan agar
bingkai dan semua baris terlihat pada viewport normal emulator uji. Urutan glyph,
kata, ayat dan baris sumber tetap sama; ornamen adalah dekorasi aplikasi.

## Bukti aktual

- [Al-Fatihah](screens/AlFatihah.png)
- [Al-Mulk](screens/AlMulk.png)
- [Malam + zoom 200%](screens/NightZoom200.png)
- [Teks sistem 200%](screens/SystemFont200.png)

Screenshot berasal dari APK native pada AVD khusus Zikra_Mushaf_Test API 37,
1080 × 2400 / 420 dpi. Semuanya diperiksa secara visual. Saat zoom 200% atau
teks sistem besar, halaman tetap dapat digeser; seluruh halaman tidak dipaksa
mengecil. Status panjang player memakai ellipsis; pengaturan audio tetap tersedia.

## Pengujian

- assembleDebug, assembleDebugAndroidTest, lintDebug berhasil; lihat [build](build.txt).
- 78 JVM, 0 gagal/error/skip; menggunakan snapshot/font audit lokal yang sama
  dengan tahap sebelumnya melalui ZIKRA_MUSHAF_AUDIT_DIR.
- [Tiga metode Mushaf](device-api37.txt) lulus: font/glyph asli, halaman, zoom,
  malam, chrome, perpindahan mode/ayat, input halaman, simpan posisi dan lompat juz.
- [Ulangan teks sistem 200%](device-font200-api37.txt): satu metode lulus.
  Ini pengulangan, bukan metode keempat yang unik. Font scale dikembalikan ke 1.0.
- Layout player Mushaf terpisah; layout reader terjemahan tidak diubah.
  Streaming/offline audio tidak diuji ulang pada perubahan kosmetik ini;
  bukti tahap sebelumnya tetap di direktori mushaf-reader-2026-10-21.

Percobaan pemasangan awal ditolak karena ANDROID_USER_HOME baru membuat kunci
debug berbeda. Tidak ada uninstall/clear data. Dibangun ulang dengan kunci debug
proyek yang sudah ada, lalu kedua APK diperbarui sukses sebelum tes yang dicatat.
[Log build pemulihan](signing-recovery.txt).

APK lokal: `D:/Download/Zikra-App-main/Zikra-Mushaf-Polished-20261021-debug.apk`.
SHA256: `1BF174A1ED0E5ED0241F8AA5379CC53EBB58BA5CCF85A370BD4251BD0F22C952`.

Tetap Android 12+, halaman online, belum sorotan kata emas/offline penuh.
Belum diuji pada HP fisik atau diaudit visual seluruh 604 halaman.
