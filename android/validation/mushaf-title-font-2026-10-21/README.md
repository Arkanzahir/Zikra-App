# Font kaligrafi khusus judul surah — 21 Oktober 2026

Pengguna menekankan bahwa perbedaan dengan foto Mushaf nyata ada pada bentuk
huruf nama surah. Judul sebelumnya memakai serif Arab sistem. Kini judul atas
dan pemisah surah menggunakan font **Surah Names v1** yang digunakan Quran.com,
diambil utuh dari CDN Quran Foundation. Teks ayat QCF V2 tidak diganti.

## Sumber dan pemetaan

- [Deklarasi font Quran.com](https://github.com/quran/quran.com-frontend-next/blob/production/src/styles/fonts.scss)
- [Pemetaan nama 001–114](https://github.com/quran/quran.com-frontend-next/blob/production/src/components/chapters/ChapterIcon/index.tsx)
- [Prefiks surah terpisah](https://github.com/quran/quran.com-frontend-next/blob/production/src/components/chapters/ChapterIcon/ChapterIconContainer.tsx)
- Font: `https://verses.quran.foundation/fonts/quran/surah-names/v1/sura_names.ttf`

Renderer membentuk outline ligatur nama dan prefiks, mempertahankan urutan
visual Arab (prefiks di kanan). Ukuran berdasarkan batas tinta yang terlihat,
dipusatkan dalam kotak aman dengan skala seragam; tidak direnggangkan horizontal
atau dinilai dari tinggi font sistem yang membuat bentuk tampak kecil.

Font 154900 byte disimpan sebagai satu cache terpisah `qcf-title-font`, refresh
tujuh hari, penulisan atomik. Cache judul tidak ikut eviksi delapan font halaman.
Tidak ada font/token baru dibundel ke APK/Git. Atribusi Quran.com/Foundation
tetap tersedia melalui Sumber Mushaf. Pemuatan judul menjadi bagian pemuatan
halaman; kegagalan unduh/font memberi jalur error/retry yang sama.

SHA256 berkas host dan cache emulator identik:
`6CAA808E10ABC7E5D93EF45815E50ECF53287CEB010D3216266E49E44871E673`.

## Bukti native dan pengujian

[Al-Fatihah](screens/AlFatihah.png), [tiga surah halaman 604](screens/Page604.png),
[halaman 49](screens/Page49.png), [Al-Mulk](screens/AlMulk.png),
[mode malam/zoom](screens/NightZoom200.png), [teks sistem 200%](screens/SystemFont200.png).

- [Build](build.txt): assembleDebug, assembleDebugAndroidTest, lintDebug dan
  testDebugUnitTest berhasil; 80 JVM, 0 gagal/error/skip dengan audit lokal.
- [Native Android](device-api37.txt): 5 metode unik lulus, 24.948 detik.
  Tes baru memeriksa 114 ligatur nama (`hasGlyph`) dan outline tinta nonkosong,
  serta prefiks kaligrafi. Empat tes reader/audio sebelumnya juga lulus.
- [Teks sistem 200%](device-font200-api37.txt): satu metode diulang, lulus
  3.711 detik; bukan metode unik keenam. Skala dipulihkan ke 1.0.

Screenshot APK native dari AVD khusus Zikra_Mushaf_Test API 37, 1080×2400 / 420 dpi.
Al-Fatihah dan halaman 604 ditinjau visual untuk bentuk judul. **Pemeriksaan 114
ligatur tidak berarti audit visual semua nama atau semua 604 halaman.** Tidak
mengklaim identik dengan font pada foto pengguna. Suite offline lengkap tidak
diulang. Belum pengujian HP fisik; halaman tetap online dan Mushaf Android 12+.

APK: `D:/Download/Zikra-App-main/Zikra-Mushaf-Calligraphy-20261021-debug.apk`.
SHA256: `4E9A0C1D1897F0365CE968FAE868601907071ADAD7E14F11828AAEF8ADADB392`.
