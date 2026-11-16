# Penyajian fitur terinspirasi Ente —16November2026

Perubahan mengikuti permintaan eksplisit pengguna: website menyerupai susunan Ente dan menjelaskan fitur utama secara lengkap. Rujukan visual diperiksa langsung pada https://ente.com/: judul dua warna, navigasi putih membulat, susunan visual produk, serta panel fitur besar. Identitas Zikra, logo, screenshot dan ikon aplikasi dipertahankan. Tidak menyalin aset/testimoni/angka pengguna atau klaim enkripsi Ente.

## Hasil

- Hero judul dua warna dan tiga screenshot aplikasi tersusun statis. Tidak ada gerakan HP berulang, autoplay video, atau simbol privasi dekoratif.
- Enam panel visual: Quran, AI, Study, Hifz, kalender, Protector. Dua belas topik pelengkap; [pemetaan sumber](../../FEATURE_MAP.md).
- Delapan ikon sama dengan vectorAndroid, termasuk transformasi grup ikonDoa. Semua ikon dimuat pada pemeriksaan browser.
- Pemeriksaan statis website/check.cjs PASS: file HTML/CSS/media, jangkar unik/valid, metadata/HOLD, tanpa script/remote resources, video controls/preload none/no autoplay dan naskah.
- Browser1440CSSpx: enam panel,12entri tambahan, ikon tersedia, tanpa overflow horizontal.390CSSpx: satu kolom panel335,2px dan tanpa overflow.320CSSpx: tanpa overflow, panel mengikuti lebar layar. Potongan sayap screenshot hero sengaja dibatasi oleh wadah, bukan halaman yang melebar.
- Tautan ikonHadis berhasil membuka fragment#hadis. Tidak ada gambar yang selesai dimuat tetapi gagal pada pengamatan akhir. Tombol unduhan tetap disabled.
- Video final sama byte dengan versi52detik terdahulu. Tidak merender ulang/menambah VO atau menguji ulang playback sampai akhir. Bukti playback sebelumnya di ../2026-11-16-product/README.md masih berlaku untuk berkas yang sama.
- Viewport sementara direset, tab preview tetap tersedia.

## Bukti dan batas

`desktop.png`: hero. `features.png`: sebagian panel Quran/AI/Study/Hifz/kalender pada desktop. `catalog.png`: katalog penjelasan fitur saat navigasiHadis. Bukan screenshot penuh seluruh halaman atau pengujian APK.

Tidak memakai HP, tidak deploy, tidak mengubah backend, dan tidak membuka distribusi APK. Video/poster/frameAI tetap di folder media yang diabaikanGit sampai hak publikasi ditutup. Login18+ tetap WIP; penjelasannya berlabel rencana. Uji lintas browser, screen reader dan caption video tersinkronisasi belum dilakukan. HOLD tetap, A–D tidak dicentang selesai.
