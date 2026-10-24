# Tampilan Doa Harian 1.7.2

24 Oktober 2026, permintaan pengguna melihat desain yang sudah diterapkan. Screenshot langsung dari APK 1.7.2/code20, source `91c8739`, bukan mockup. [Metadata/hash](release.json).

- [Halaman awal, 14 pilihan luring](Doa-Overview-v1.7.2.png)
- [Daftar setelah digulir](Doa-List-v1.7.2.png)
- [Bacaan dibuka: Arab, Latin, arti, rujukan dan TTS](Doa-Expanded-v1.7.2.png)

Satu metode native `DailyDesignReleaseTest#testDuaFilterAndReadWithoutVoice` lulus: membuka halaman, membuka bacaan pertama, memeriksa rujukan, mencari hasil kosong lalu mengembalikan daftar. [Log](native.txt). Bukan pengujian seluruh kategori/14 bacaan, keluaran suara atau penelaahan seluruh konten. B01/B04 tetap terbuka. Tidak membangun ulang aplikasi; memakai APK yang sudah dibuktikan hash-nya. Emulator ditutup setelah gambar diambil.
