# UI02 — perapian Doa Harian 1.7.3

24 Oktober 2026, permintaan pengguna memperbaiki desain setelah melihat screenshot 1.7.2. Implementasi Android Java/XML dengan skill frontend-design, memakai ilustrasi yang sama. [Versi, source, ukuran dan hash APK](release.json).

## Perubahan

- Tombol abu-abu bawaan Android diganti baris koleksi berwarna mint, dengan ikon, penjelasan unduhan luring dan panah. Navigasi tetap menggunakan ID/alur yang sama.
- Hero lebih ringkas, judul dua baris pada ukuran normal. Kategori memakai garis tipis dan status aktif hijau; daftar tetap satu kelompok dengan divider, bukan kartu terpisah untuk setiap bacaan.
- Ikon kategori dibedakan. Judul bagian dan keterangan luring dipisah dari badge jumlah.
- Arab mendapat bidang krem bertepi tipis. Latin bantu dan arti memiliki label/ukuran tersendiri; arti berwarna gelap untuk keterbacaan.
- Rujukan ringkas tetap terlihat. URL, asal terjemahan dan catatan kurasi dipertahankan di bawah tombol **Sumber & catatan**, yang dapat dibuka/ditutup. Kedua adaptor—pilihan bawaan dan katalog tambahan—memakai susunan bacaan yang sama.
- Tombol TTS diberi ikon suara; tidak diubah menjadi rekaman qari. Dataset tetap **14 pilihan bawaan + 225 bacaan tambahan setelah unduh**, dengan bacaan yang beririsan. Hash resource 14 pilihan sama dengan 1.7.2; perubahan ini tidak mengubah teks sumber atau mengesahkan konten B01/B04.

## Screenshot APK asli

- [Halaman awal](Doa-Overview.png)
- [Bacaan dibuka](Doa-Reading.png)
- [Koleksi tambahan / sumber](Doa-Library-Source.png)
- [Ukuran teks 200% — halaman awal](Doa-Large-Overview.png)
- [Ukuran teks 200% — bacaan](Doa-Large-Reading.png)

Gambar ditangkap dari APK yang dibangun, tanpa mengubah isi gambar atau memakai mockup sebagai bukti.

## Pemeriksaan

Build APK/debug driver dan lint lulus, tanpa error; peringatan lint tercatat di release.json. [Log build](build.txt).

Dua metode native lulus pada ukuran normal ([log](native.txt)):

1. `DailyDesignReleaseTest#testDuaFilterAndReadWithoutVoice`: daftar/bacaan pertama, Latin, rujukan, buka-tutup sumber, hasil pencarian kosong dan kembali ke daftar.
2. `OptionalContentTest#testRealDuaDownloadOfflinePagingSearchAndInvalidUpdatePreservesCache`: respons EQuran asli 227 entri/225 tampil, unduh/cache luring, paging, pencarian/kelompok dan isi bacaan; catatan sumber di susunan baru dapat dibuka. Pembaruan tidak valid tidak menimpa cache dan fixture dikembalikan setelah tes.

Metode pertama diulang pada **font_scale 2.0** ([log](large-font.txt)); total **2 metode / 3 eksekusi**. Ukuran font emulator dikembalikan ke 1.0 dan emulator ditutup. Tidak menjalankan suite penuh, tidak mengulang unduhan kitab Hadis, tidak memakai HP pengguna.

## Batas

Belum menguji keluaran suara TTS aktual, seluruh bacaan/ukuran layar atau HP fisik. Pada ukuran teks 200%, hero memanjang dan label navigasi membungkus; pemeriksaan/perapian seluruh aplikasi tetap F03. Tidak mengklaim seluruh aksesibilitas selesai. UI02 ini adalah perapian tampilan Doa dan alur yang diperiksa; B01/B04 serta F01–F04 keseluruhan tetap sesuai checklist aktif.
