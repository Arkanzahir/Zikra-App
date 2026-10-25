# UI04 — Penerapan referensi delapan pintasan

25 Oktober 2026. Pengguna memilih [referensi garis/circle](../../../design/approved-2026-10-25-home-shortcuts/README.md) dengan pesan “jadi kekgituaja”. Kandidat **1.9.3 beta/code30**, source `1cace78b05c1bc3521e8e97dd64ca827c93c1b92`. Usulan ikon Lucide atau duotone sebelumnya tidak diterapkan.

## Hasil Android

- [Home pada rasio acuan](native/Reference-sky-day.png), [malam](native/Reference-sky-crescent.png).
- [Home pada layar tinggi](native/Refresh-sky-day-100.png), [teks200%](native/Refresh-home-top-200.png), [layar sempit320dp/200%](native/FoundationHomeLargeText.png).
- [Gulir/agenda](native/Refresh-home-agenda-100.png), [kondisi kosong](native/Refresh-home-empty-bottom-100.png).

Delapan VectorDrawable baru mengikuti bentuk pilihan: Quran dengan rehal, tangan Doa, kompas, Hifz berpita, dua kilau AI, perisai centang, toga Study dan buku Hadis berlambang geometris. Lingkaran Home56dp, padding9dp dan baris minimal88dp; Quran/Kiblat/AI/Protector/Study mint, Doa/Hifz/Hadis krem. Garis hijau tua, bobot/ukuran dibuat sekeluarga. Font200% tetap reflow dua kolom sesuai dukungan sebelumnya. Resource dipisahkan untuk Home agar halaman lain tidak ikut berganti; ID/tujuan menu tetap. Master logo merek dan database tidak berubah.

Bentuk digambar ulang dari referensi raster; bukan lapisan sumber asli atau jaminan identik per piksel. Screenshot pada laporan ini berasal dari APK native sebenarnya, bukan preview browser. Waktu/fase/lokasi/bacaan/tugas adalah fixture sintetis: Surabaya, Al-Kahf12, Clock12:30/20:00, fase0,18/1; tidak mengklaim fase astronomi tanggal25 Oktober sesuai gambar.

## Pemeriksaan dan preservasi

[Checks/log](checks.json), [source perubahan](source-files.json), [receipt referensi](reference.json): build debug + helper + lint berhasil dengan satu worker/heap768MiB saat emulator mati. Lint **0 error/721 warning**. **6 eksekusi native, 3 metode unik, 0 gagal**: dua metode HomeRefresh pada font100%/200%, satu Foundation sempit200%, satu pengulangan sky pada rasio acuan. Gulir, data agenda/bacaan, tujuan Study/fokus, label, dock dan render langit diperiksa memakai harness yang sudah tersedia. Tidak menambah tes yang hanya mencerminkan warna/path ikon; unit logika tidak diulang untuk perubahan resource ini. Kalender dan AI tidak dihitung sebagai pengujian baru.

[Preservasi](preservation.json): sepuluh file internal asli emulator pulih cocok hash, font/ukuran layar dipulihkan, marker/helper dihapus, emulator ditutup. Tidak memasang kandidat pada HP. [Audit terarah](audit.json) memindai nilai rahasia yang diketahui pada file Git/source/bukti dan418 entri APK terdekompresi: 0 kecocokan/entri privat/helper. WIP PROJECT_HANDOFF, env, stash/arsip audio serta APK baseline tetap utuh; APK1.9.2 juga dipertahankan. Log publik hanya dinormalisasi CR/LF/spasi akhir; hash asli privat tercatat.

[APK/signature/source](release.json): `Zikra-Home-v1.9.3-beta-debug.apk`, **34.408.836 byte**, SHA-256 `19650c988be7302044125de2d53f07fa51d23d65b57b65f93ff1b201a53e1dc5`. Unduhan awal<100.000.000 byte, sertifikat debug sama baseline, SQLite4. Ini kandidat beta, belum signed release publik atau bukti performa HP.

UI04 keseluruhan tetap[ ] karena desain/fungsi AI baru masih tersisa. C02/C03/C04/C06/C07–C09, B01, adzanD02a, F/G dan rilis publik tidak ditutup oleh penerapan ikon ini.
