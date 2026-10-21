# Mushaf mengikuti referensi — 21 Oktober 2026

Penyempurnaan setelah commit 19b16f7, mengikuti gambar referensi yang dikirim ulang
pengguna. Header kecil dengan Aa/menu, surah di kiri dan juz di kanan, bingkai
sage dengan detail sudut melengkung dan ornamen judul sand gold. Mini player
menampilkan ayat/qari dan kontrol ringkas. Bidang sage ayat aktif menyatu per baris.
Tidak ada warna emas yang mengklaim sinkronisasi kata.

Tinggi bingkai menyesuaikan ruang baca yang tersedia; glyph dan urutan baris asli
tetap sama. Halaman pembuka menggunakan jarak vertikal yang lebih lapang.
Referensi ilustratif tidak dijadikan sumber teks atau alasan mengubah susunan ayat.

## Penggunaan

- Aa: pembesaran, malam, pilihan ayat.
- Menu ⋮: pilihan ayat, kembali ke terjemahan, posisi audio, sumber Mushaf.
- Posisi audio membuka/menutup seekbar dan durasi; kontrol putar/jeda tetap di mini player.
- Atribusi QCF V2 / Quran Foundation dan penjelasan font tersedia melalui Sumber Mushaf.
  Status pemuatan/error tetap tampil ketika diperlukan.

## Bukti APK native

- [Al-Fatihah](screens/AlFatihah.png)
- [Al-Mulk](screens/AlMulk.png)
- [Audio asli sedang berjalan](screens/AudioActive.png)
- [Posisi audio dibuka](screens/AudioExpanded.png)
- [Mode malam + zoom 200%](screens/NightZoom200.png)
- [Teks sistem 200%](screens/SystemFont200.png)

Semua screenshot berasal dari AVD khusus Zikra_Mushaf_Test API 37, 1080×2400 / 420 dpi.
Screenshot ditinjau secara visual. Pada zoom/teks besar, halaman dapat digeser.
Gambar AudioActive menunjukkan satu ayat yang ditandai sage, bukan karaoke per kata.

## Hasil uji

- [Build](build.txt): assembleDebug, assembleDebugAndroidTest, testDebugUnitTest,
  lintDebug berhasil. 78 JVM, 0 gagal/error/skip dengan snapshot/font audit lokal.
- [Android](device-api37.txt): 4 metode unik lulus (15.955 detik). Tiga metode
  reader sebelumnya ditambah playback mini player dengan decoder audio asli.
  Mencakup glyph/font, navigasi, zoom, malam, chrome, mode/ayat, validasi halaman,
  posisi terakhir, juz, atribusi/menu dan buka/tutup seekbar.
- Playback Al-Mulk ayat 5 benar-benar berjalan (durasi/posisi native > 0), tombol
  jeda bekerja dan seekbar aktif. Audio dihentikan setelah tes. Ini bukan penilaian
  pelafalan oleh manusia atau pengujian headset.
- [Teks sistem 200%](device-font200-api37.txt): 1 metode diulang, lulus 3.259 detik;
  bukan metode unik kelima. Ukuran sistem dipulihkan ke 1.0.
- Suite streaming tiga qari/offline lengkap dari tahap sebelumnya tidak diulang.

APK: `D:/Download/Zikra-App-main/Zikra-Mushaf-Reference-20261021-debug.apk`.
SHA256: `9A6DD66A1B3236AFEEADAB604C19DEFBB62EE0EEF4A68F04FEF739092C8C4BEE`.

Batas tetap: Android 12+, halaman online, belum sorotan kata emas/Mushaf offline
penuh, belum audit visual seluruh 604 halaman atau uji HP fisik.
