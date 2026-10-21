# Basmalah lebih besar — 21 Oktober 2026

Basmalah sebelumnya memakai serif sistem dengan ukuran 0,59× teks ayat, sehingga
tampak kecil dibanding referensi pengguna. Kini memakai empat glyph asli QCF V2
Al-Fatihah 1:1 tanpa medali, dengan ukuran berdasarkan batas tinta sebenarnya.
Skala seragam mempertahankan proporsi, dipusatkan dalam baris antara judul dan
ayat. Teks ayat, nomor dan pembagian baris sumber tetap.

## Sumber dan verifikasi

- [Data halaman 1 Quran.com](https://api.quran.com/api/v4/verses/by_page/1?mushaf=1&words=true&word_fields=code_v2,text_uthmani&per_page=50):
  empat kata pertama ayat 1:1, urutan code_v2 FC41–FC44 dengan spasi pemisah.
  Tes host membandingkan konstanta renderer dengan snapshot respons aktual.
- [Font p1 resmi](https://verses.quran.foundation/fonts/quran/hafs/v2/ttf/p1.ttf),
  diunduh utuh ke satu cache `qcf-basmalah/p1.ttf`, refresh tujuh hari dan penulisan
  atomik. Terpisah dari eviksi font halaman. Tidak ada font, JSON Quran atau
  kredensial baru dibundel ke APK/Git; atribusi tetap di Sumber Mushaf.
- SHA256 font host dan cache emulator identik:
  `1686695486474B88A24A05CD4578F5295961BC9516885309EC95BE574F213C27`.
- Al-Fatihah tetap memakai ayat pertama sendiri, tanpa basmalah tambahan.
  At-Taubah diperiksa pada halaman 187, tanpa basmalah tambahan.

## Bukti native dan pengujian

[Halaman 604](screens/Page604.png), [Al-Mulk](screens/AlMulk.png),
[Al-Fatihah](screens/AlFatihah.png), [At-Taubah](screens/AtTaubah.png),
[malam/zoom 200%](screens/NightZoom200.png), [teks sistem 200%](screens/SystemFont200.png).

- [Build](build.txt): assembleDebug, assembleDebugAndroidTest, lintDebug dan
  testDebugUnitTest berhasil. XML hasil tes: 80 JVM, 0 gagal/error/skip dengan audit lokal.
- [Android final](device-api37.txt): 5 metode unik lulus, 21.923 detik. Pemeriksaan
  native batas tinta basmalah ditambahkan ke metode font; navigasi mencakup
  halaman 187, selain 1/49/300/562/604. Suite juga menguji reader dan audio asli.
- [Teks sistem 200%](device-font200-api37.txt): satu metode diulang, lulus
  3.446 detik. Bukan metode unik keenam; skala sistem dikembalikan ke 1.0.
- [Kegagalan awal](device-initial-failure.txt): tes posisi halaman mendapati
  expected 604 tetapi tersimpan 562. Status `ready` diterbitkan sebelum bind dan
  penyimpanan posisi selesai. Kini posisi disimpan sebelum status siap dan
  notifikasi audio. Suite lengkap dijalankan ulang pada APK final, lulus.

Screenshot berasal dari APK native pada AVD khusus Zikra_Mushaf_Test API 37,
1080×2400 / 420 dpi. Halaman 604, Al-Mulk dan At-Taubah ditinjau visual; bukan
mockup dan tidak mengklaim identik dengan font foto. Belum diuji di HP fisik
atau seluruh 604 halaman. Pembesaran teks sistem membuat kontrol lebih tinggi;
halaman tetap dapat digulir. Mushaf Android 12+/online; sorotan emas per kata
dan backend terautentikasi belum terhubung. Suite offline lengkap tidak diulang.

APK: `D:/Download/Zikra-App-main/Zikra-Mushaf-Basmalah-20261021-debug.apk`.
SHA256: `69EC638510745259569551D78FF32048DF79D90433B746C98A7FDF63651F3539`.
