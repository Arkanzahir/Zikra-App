# UI04 — Komposisi, gradasi panel dan lampu siang/malam

25 Oktober2026. Source `b282fd86924357e2fe8700dd073f7c7a9cd68133`, **1.9.4 beta/code31**, mengikuti [klarifikasi pengguna dan referensi](../../../design/approved-2026-10-25-home-alignment/README.md). Pengguna menilai panel1.9.3 terlalu pekat dan meminta posisi mendekati board, dengan lampu mati siang/menyala malam.

## Hasil aplikasi

- [Siang, lampu padam](native/Reference-sky-day.png), [malam, lampu menyala](native/Reference-sky-crescent.png), [bulan penuh](native/Reference-sky-full-moon.png).
- [Home100%](native/Refresh-sky-day-100.png), [Home200%](native/Refresh-home-top-200.png), [layar320dp/200%](native/FoundationHomeLargeText.png), [bagian agenda](native/Refresh-home-agenda-100.png).

Panel menjadi56dp dengan tiga tahap gradasi: bagian atas transparan lembut, tengah/bawah opak sebelum melintasi latar cream. Ini mempertahankan detail ilustrasi di atas tanpa mengulang sambungan abu-abu pada koreksi1.9.1. Font jam memakai medium, bukan bold700. Hero310dp, posisi masjid diturunkan, jarak greeting/countdown/jam, logo, pintasan dan Ruang harimu/bacaan dirapikan terhadap proporsi acuan. Bentuk/warna ikon terbaru dan tujuan menu tetap; master logo tidak diubah. Native font besar tetap reflow dan dapat digulir. Hasil diselaraskan, bukan klaim identik per piksel.

Lampu sebelumnya menyatu dengan satu gambar sehingga ikut menyala siang. Kini renderer memakai aset siang tanpa glow dan master malam bercahaya. Pemilihan/blend mengikuti waktu terbit/terbenam matahari dan zona lokasi, dengan transisi sekitar fajar/senja. Bitmap/paint disiapkan sekali, tidak membuat bitmap baru setiap frame. [Aset hasil built-in image_gen, path, prompt persis dan batas](asset-provenance.md). Gambar hasil edit dapat berbeda detail dari master; ada koreksi vertikal kecil untuk menyelaraskan kubah.

## Pemeriksaan

[Checks/log](checks.json), [source](source-files.json), [versi/hash/signature](release.json):

- Build debug/helper/lint dan **5 unit Sky** lulus (0 gagal/skip). Satu worker/heap768MiB, emulator mati saat build. Lint **0 error/721 warning**.
- **6 eksekusi native/3 metode unik** lulus: dua metode HomeRefresh pada font100%/200%, Foundation pada320dp/200%, serta sky pada rasio board720×1392. Gulir, bacaan/agenda kosong-berisi, tujuan Study/fokus, label dan dock tetap diperiksa.
- Harness menunggu frame aktual, lalu menghitung piksel jingga terang pada area lampu/fasad layar: [100%](motion-100.json) **1 siang vs2.469 malam**; [200%](motion-200.json) **1 vs2.376**. Ini proksi visual, bukan pengukuran fotometrik; satu piksel hangat siang dapat berasal dari warna alami. Screenshot menunjukkan titik lampu/pantulan padam saat siang. Pembuktian otomatis memakai12:30/20:00 fixture, bukan seluruh waktu/lokasi/perangkat.
- Area awan berubah pada layar nyata; saat animasi dimatikan area tetap identik. Bukan pengukuran baterai/HP. Transisi twilight diimplementasikan menurut fungsi daylight; native endpoint siang/malam diuji, bukan seluruh interval menit.

Semua data screenshot terisolasi/sintetis: Surabaya, Al-Kahf12, tugas, Clock12:30/20:00 dan SkyDay fase0,18/1. Screenshot sabit tidak mengklaim fase astronomi nyata tanggal25 Oktober. Produksi memakai jam/zona dan data aplikasi; tidak mengunci waktu ke contoh board.

## Preservasi dan kandidat

[Sepuluh file asli emulator](preservation.json) pulih cocok hash, font/ukuran dipulihkan, marker/helper dihapus, emulator ditutup. HP tidak dipasang kandidat ini. [Audit terarah](audit.json) atas nilai rahasia yang diketahui:0 kecocokan pada file Git/source/bukti dan419 entri APK terdekompresi,0 entri privat/helper. WIP PROJECT_HANDOFF, env privat, stash/arsip audio dan baseline dipertahankan; kandidat1.9.3 tidak ditimpa.

`Zikra-Home-v1.9.4-beta-debug.apk`: **36.594.943 byte**, SHA-256 `76a1367d52354fc24757b71221347766c7d2912160a1527015ffe1417ffc26b3`, <100.000.000 byte. Debug signer sama dengan baseline, SQLite4. Ini beta untuk tinjauan/pembaruan, bukan rilis publik atau bukti performa HP.

UI04 keseluruhan tetap[ ] karena AI baru belum diimplementasikan. C/B01/D02a/F/G dan rilis publik tidak ditutup oleh koreksi Home ini.
