# Website dengan promo —16November2026

HOLD; preview lokal saja. Tidak memakai HP, mengubah backend, membagikan APK atau memublikasikan suara/video.

## Implementasi

Setelah menelaah Things, LocalSend dan Ente, pembuka memakai tipografi satu keluarga tanpa italic. Video promo52detik ditempatkan sebelum bagian fitur. Tiga bagian fitur menggantikan kartu generik, memakai screenshot aplikasi asli. Logo vektor tetap sama dengan Android. Pemutar native, tanpa autoplay, preload none, playsinline, suara tidak dibisukan. Naskah Bahasa Indonesia tersedia melalui details HTML.

## Bukti pemeriksaan

- Pemeriksaan statis `website/check.cjs`: PASS untuk referensi HTML/CSS/media, jangkar, kandidat/ukuran, HOLD, tanpa script/remote resources/APK URL; controls/no autoplay/no muted/playsinline/preload none dan naskah tersedia.
- `website/prepare-preview.cjs`: hash video/poster cocok sebelum salin. Video sama byte dengan versi final pilihan pengguna; bukan hasil render/narasi baru.
- Browser lokal: pemutaran dimulai melalui Space pada video; duration52, currentTime6,475detik, pausedfalse, readyState4, errornull, mutedfalse. Jeda berhasil pada17,509detik. Ini bukti playback dan keadaan volume browser, bukan penilaian kualitas suara dari pendengaran.
- Naskah dibuka dan ditutup; isi naskah tampil pada pohon aksesibilitas. Navigasi Jelajahi fitur berhasil.
- Setelah stylesheet final dirapikan:1440CSSpx video1120px sama dengan pembungkus; tiga aturan media valid di CSSOM dan tidak ada overflow.390CSSpx video335,2px;320CSSpx video264,8px; keduanya tanpa overflow horizontal. Gutter scrollbar termasuk perhitungan.
- Viewport sementara direset; tab website ditinggalkan sebagai preview.

Screenshot: `desktop.png`, `mobile.png`, `features.png`. Gambar/video menggunakan data contoh dari rekaman terdahulu. Bukan bukti seluruh fitur APK terbaru58. Video tidak diputar ulang sampai akhir pada sesi ini; bukti decode1560frame/audio final terdahulu ada pada `design/promos/2026-11-12/checks-no-cloud.json`.

## Batas

Belum pemeriksaan lintas browser/HP fisik/screen reader atau caption tersinkronisasi. Naskah statis bukan pengganti caption tersinkronisasi. Tidak menguji unduhan APK karena distribusi belum disetujui. Hak distribusi suara promo masih terbuka; video/poster tetap lokal dan tidak masuk Git. Kunci/konfigurasi privat, WIP akun, data pengguna, stash audio dan paket aplikasi tidak diubah.
