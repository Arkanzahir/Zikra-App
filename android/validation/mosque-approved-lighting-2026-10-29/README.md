# UI08 — Referensi diterima dan diterapkan pada beta 1.9.27

29 Oktober 2026. Pengguna menyetujui arah pencahayaan dengan “gas sudah bagus”.
Tekstur pintu dari [referensi yang diterima](../../../design/concepts/mosque-lighting-review-2026-10-29/README.md)
diterapkan dan diuji pada Android. Ini menggantikan visual interior 1.9.26
yang ditolak; bukan mengganti seluruh gambar Home dengan hasil generatif.

## Implementasi

Delapan bukaan memakai potongan tekstur yang didaftarkan satu per satu ke
arsitektur asli, termasuk pintu pusat dengan kedalaman interior. Area terakhir
dipotong agar dedaunan tetap di depan. Glow pilar dan pantulan lantai diperkecil.
Langit, bulan, bintang, awan dan parallax tetap sistem native yang sama.
Lampu mengikuti waktu/matahari lokasi dengan fade yang ada, bukan pilihan
tema terang/gelap. Siang mati, senja berangsur, malam menyala.

PNG referensi dibundel utuh agar asalnya dapat diverifikasi, tetapi decoder
hanya mengambil band 703×180 piksel: sekitar 506 KB ARGB. Seluruh scene
1243×1266 tidak didecode menjadi bitmap Home. Path, Paint, shader dan rect
disimpan; tidak membuat bitmap/shader setiap frame. Decoder/stream ditutup;
kegagalan decode mempertahankan scene dasar. [Registrasi dan hash](texture-registration.json).
Ini ilustrasi cahaya, bukan pengamatan interior bangunan sebenarnya.

## Bukti Android

[Perbandingan detail siang/malam](preview.jpg), [Canvas siang](home-12.jpg),
[Canvas malam](home-21.jpg), [Activity siang](activity-day.jpg),
[Activity senja](activity-twilight.jpg), [Activity malam](activity-night.jpg),
[tema gelap saat siang](activity-dark-theme-day.jpg).
Gambar bukti berasal dari render/screenshot Android. JPEG hanya diperkecil;
preview menggabungkan crop kedua render tanpa retouch. Waktu/lokasi/jadwal
layar adalah fixture/cache pengujian, bukan validasi baru jadwal salat.

[Empat native lulus](native-results.json), jaringan emulator dimatikan:

- Tekstur berhasil didecode, memori band <600 KB, siang identik scene dasar,
  strength 0,5 di antara mati/menyala; pilar, langit dan dedaunan terlindungi.
- Home Activity pada 12.00/18.10/21.00/05.50: lampu 0/0,5/1/0,5 dengan fixture
  sunrise/sunset 06.00/18.00. Tema gelap saat siang tidak menyalakan lampu.
- Awan berubah 24.824 piksel, parallax sentuh merespons, countdown berdetak;
  pause/offscreen/reduced motion berhenti sesuai pengaturan. [Hasil](motion.json).
- Refresh gagal mempertahankan hasil dan tanggal cache masjid. Kasus ini
  memakai cache sintetis, bukan pengujian jaringan nyata baru.

[Unit/lint](checks.json): 160 unit lulus, 4 skip, 0 gagal; lint 0 error,
862 warning. Build debug/helper/unit/lint berhasil; helper akhir dan lint
diperiksa lagi sesudah tambahan assertion. Pencarian/TLS/provider tidak diubah;
[bukti jaringan 1.9.26 dan batas kegagalan provider](../mosque-repair-2026-10-29/README.md)
tetap berlaku dalam lingkupnya. Tidak ada lookup lokasi baru sesi ini.

## Paket dan preservasi

APK lokal `D:\Download\Zikra-App-main\Zikra-Lighting-v1.9.27-beta-debug.apk`,
code54, **45.336.132 byte / 45,3 MB**, di bawah 100.000.000 byte.
SHA256 `0ab5cb0955b80fe28ac5edea488e639a92bc57947f54b9f967f412692132d1a5`.
Signer debug tetap `c7888fe2ca40e14698726ed20b2f93947033cef0bdee9cfd127c429d0ea0ca1b`.
[Audit APK](apk.json): seluruh entry diperiksa terhadap nilai privat yang
diketahui, 0 temuan; kedua audio cocok hash pilihan terakhir. APK/MP3/env/
backup/log mentah tidak masuk Git.

[Preservasi](preservation.json): 32 berkas asli emulator dipulihkan cocok hash;
pengaturan dan viewport kembali, helper/marker/gambar uji dibersihkan, emulator
ditutup. HP tidak disentuh; versi terakhir yang diuji oleh agen adalah 1.9.25.
Jangan menyatakan 1.9.27 sudah dipasang atau diterima visualnya di HP.
PROJECT_HANDOFF WIP, stash/arsip audio, env, artwork dasar dan ikon tetap.

UI08 selesai teknis beta terarah dengan arah referensi disetujui. Font Arab
reader belum berubah. B01, C02/C04/C07, D02/D02a, E04/F04/G dan seluruh A–D/
publik tetap mengikuti gerbang terbuka pada plan; hasil ini tidak menutupnya.

## Source

[Receipt](receipt.json) mengikat commit source dengan APK dan pemeriksaan.
Commit receipt hanya dokumentasi. Remote diverifikasi setelah push;
installer dan semua data privat tetap lokal.
