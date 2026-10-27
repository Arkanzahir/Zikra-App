# Penyelarasan halaman harian — 28 Oktober 2026

Beta **1.9.15/code42**. Pengguna meminta penerapan setelah membahas referensi,
mempertahankan delapan simbol fitur, menyetujui polesan logo bulan/bintang/Quran,
dan memilih Profil sebagai ruang pribadi. Ini menggantikan batas “referensi dahulu”
untuk perubahan yang dijelaskan di bawah; bukan persetujuan seluruh aplikasi.
[Receipt APK/source](release.json), [hasil native](native-results.json),
[upgrade](upgrade.json), [preservasi](preservation.json), [audit terarah](audit.json).

## Perubahan

- Profil: nama lokal, bacaan terakhir, tujuh hari aktivitas dari catatan nyata,
  rencana belajar/hafalan, simpanan dan pengaturan. Membuka Profil tidak menambah
  progres. Rencana belajar adalah seluruh rencana aktif, bukan statistik harian
  yang direka. Hari tanpa catatan tidak otomatis berarti pengguna tidak beribadah.
- Hifz: header ringkas, halaman kosong dengan tindakan tambah yang terlihat,
  kartu target/rentang, bacaan dan penandaan manual. Ringkasan 0/0 disembunyikan.
  Membuka bacaan tidak menandai hafalan selesai. Penyimpanan/editor tetap memakai
  jalur Study yang ada; adapter presentasi terpisah dari aturan target.
- Kalender: tujuh kolom mengikuti lebar layar, tanpa gulir horizontal; tanggal
  Hijriah disertai angka tanggal Masehi. Hari ini dibingkai, pilihan diisi hijau,
  pilihan bertahan saat recreation. Umm al-Qura tetap disebut perkiraan dan
  penetapan hari besar mengikuti otoritas setempat.
- Home: jam lokal HH.mm beserta zona terpisah dari hitung mundur salat; tanda
  ibadah memakai ID/status target sebenarnya, bukan mewarnai N salat pertama.
  Lima target pertama ditampilkan; bila lebih banyak ada tautan ke semua target.
  Awan dipercepat agar gerak terlihat; kebijakan gerak/lifecycle/penghemat daya
  yang sudah ada dipertahankan. Posisi dock memperhitungkan area gestur sistem.
- Alias ikon bersama menunjuk delapan vector Home asli tanpa mengubah path-nya.
  Dipakai pada Home, dock, Profil, kartu Hifz, orbit Study, pintasan pustaka Doa
  dan item Hadis. Ikon tindakan seperti cari/kembali/bookmark tetap berbeda.
  Polesan logo ditambahkan sebagai vector untuk branding/launcher; master asli
  tetap utuh. Warna mint/pine dan tema malam dibatasi pada halaman yang diubah.

## Bukti pada kandidat akhir

Build aplikasi/helper dan lint berhasil, **143 unit lulus/4 dilewati**, lint
**0 error/815 warning**. Empat skip opsional adalah katalog Doa, dokumen Mushaf
host, dan dua pengukuran font host; bukan keberhasilan empat pemeriksaan itu.
[Ringkasan build](build-summary.json), [log](build.txt).

**18 eksekusi/12 metode native unik**, emulator Medium_Phone API37.1:

| Kelompok | Jumlah | Yang diperiksa |
| --- | ---: | --- |
| Upgrade/lifecycle | 5 | Rekam/pulihkan KeyStore, seed APK lama, upgrade, restart |
| Tampilan normal | 5 | Profil nyata, kalender, Hifz kosong/reader, ikon bersama, jam/awan/ibadah |
| Navigasi regresi | 2 | Study utama tidak memakai kembali Hifz; checklist menuju ibadah |
| 320dp, font200% | 3 | Profil, kalender, Hifz; tanggal/kolom/judul terlihat dan alur tetap berjalan |
| Tema malam | 3 | Tiga alur yang sama dengan tema malam, font100% |

Pembaruan memakai **APK1.9.14 sebenarnya**, fixture terisi dibuat melalui kelas
versi lama, lalu kandidat1.9.15 dipasang dengan `install -r`. Lima database,
nilai preferensi, file tahan lama, riwayat AI dan gateway dummy terenkripsi
dibandingkan;12 pembukaan halaman dan restart tidak menghilangkan isi.
Ini pemeriksaan upgrade fixture terisi, terpisah dari preservasi baseline.

Profil diuji dengan dua hari aktivitas eksplisit, bacaan Al-Kahf12 dan rencana
sintetis. Hifz menguji tambah melalui CTA nyata dan pembukaan Al-Fatihah2–4 tanpa
auto-complete. Kalender memeriksa bounds semua sel, lebar label hari, keseluruhan
judul/tahun, perubahan pilihan, recreation dan perpindahan bulan. Bitmap ikon
Quran/AI/Study pada Home/Profil dibandingkan; ini tidak mengklaim screenshot
setiap ikon di setiap halaman sudah direview. Home menguji hanya Asar selesai
untuk membuktikan penandaan bukan jumlah berurutan, format jam/zona, frame awan
berbeda setelah3,5detik serta berhenti saat skala animasi sistem0.

Log metode ada di[logs](logs/), kode di
`android/app/src/androidTest/java/com/zikra/app/RefinedExperienceTest.java`.
Kelas membutuhkan marker dan emulator; **jangan menjalankan langsung pada data
pengguna**. Host terlebih dahulu memindahkan penyimpanan asli dan membuat fixture
terisolasi. Snapshot lengkap, cadangan dan log host hanya di`.local`.

## Pemeriksaan visual dan koreksi

Build awal ditolak lint karena accessibility heading memerlukan API28; guard
versi memperbaiki kompatibilitas min26. Putaran native1 lulus fungsi, tetapi
review gambar menemukan judul kalender terpisah kurang baik dan dock Home dekat
area gestur. Putaran2 lulus fungsi, tetapi review200% menemukan tahun kalender
tersembunyi akibat batas dua baris. Batas dihapus, assertion tinggi seluruh judul
ditambahkan, lalu putaran3 pada kandidat akhir lulus. Hasil awal tidak dihitung
sebagai kelulusan visual akhir. XML ditata agar struktur mudah ditelusuri.

- [Home/jam/ikon](native/native-normal/REFINED-home-clock-sky.png)
- [Profil](native/native-normal/REFINED-profile-personal.png)
- [Aktivitas pekan](native/native-normal/REFINED-profile-week.png)
- [Hifz kosong](native/native-normal/REFINED-hifz-empty.png)
- [Hifz target](native/native-normal/REFINED-hifz-target.png)
- [Kalender](native/native-normal/REFINED-calendar-selected.png)
- [Kalender200%](native/native-large-text/REFINED-calendar-selected.png)
- [Profil malam](native/native-dark/REFINED-profile-personal.png)

## Preservasi dan batas

10 berkas internal asli emulator (baseline lama kosong) dan126 berkas eksternal
pulih cocok hash. KeyStore asli dipertahankan; alias fixture dipulihkan/dibersihkan.
Jaringan, ukuran/density/font, tema, animasi, stay-awake dan izin notifikasi
dipulihkan; helper dihapus dan emulator ditutup. HP tidak diubah, pengujian luring
tanpa inference. Emulator tanpa audio; bukan bukti suara adzan terdengar.

Audit membandingkan WIP PROJECT_HANDOFF, stash/arsip audio, env privat, master logo,
delapan vector, artwork malam, source AI dan APK lama. Semuanya utuh. Nilai privat
yang kini dikenal tidak ditemukan pada source/evidence dan isi APK terurai;
bukan sertifikasi seluruh sejarah Git atau penutupan G03. APK <100.000.000byte,
signer debug sama1.9.14; audio privat pengguna tetap hanya dalam kandidat debug,
tidak masuk Git. Ketentuan distribusi/lafaz audio belum ditutup.

Google login, sinkronisasi antar-HP dan dashboard admin khusus **belum dibuat**;
baru dibahas. Login tidak otomatis menyinkronkan data lokal. Cakupan akun tetap
ditunda sampai keputusan implementasi tersendiri. Kode AI/provider/hosting tidak
diubah. D01/D02/D02a, B01, C02/C04/C07, F01–F04 seluruh layar dan gerbang publik
tetap mengikuti checklist. Teks200%/mode malam di sini meliputi tiga halaman,
bukan klaim seluruh aplikasi, Home font200%, HP/vendor/performa/baterai atau
kesamaan per piksel dengan konsep. Pengguna belum menilai hasil APK baru.
