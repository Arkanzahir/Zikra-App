# Referensi visual Zikra — 27 Oktober2026

Status: **logo dipoles disetujui pengguna28 Oktober2026**. Layar lainnya masih
pratinjau untuk ditinjau; belum implementasi native.
Arahan terbaru pengguna: delapan ikon fitur dipertahankan; logo utama dapat
**dipoles** sambil mempertahankan identitas; Hifz, kalender Hijriah dan Profil
perlu perbaikan lebih matang. Home memerlukan jam lokal dan gerak awan terlihat.

## Cakupan referensi

- [Sumber interaktif](reference.html): lima tab Home/Hifz/Kalender/Profil/Logo.
  Semua nama, tugas, catatan, angka, tanggal dan jadwal berlabel **contoh**.
- Hifz: ringkasan target yang jelas, daftar surah/rentang/status, catatan latihan,
  serta keadaan kosong dengan tindakan dekat isi. Tidak memakai0/0 atau centang
  selesai saat target kosong. Footer detail tidak menyorot tab Study yang ambigu.
- Kalender: tujuh kolom utuh, tanggal Hijriah utama/Masehi pendamping, hari ini
  dibedakan dari tanggal dipilih, label perkiraan dekat judul. Model contoh memakai
  Intl islamic-umalqura; bukan penetapan hari besar resmi atau perubahan model native.
- Profil: header identitas ringkas, ringkasan aktivitas, simpanan, kelompok
  pengaturan; susunan ikon dan teks mengikuti grid/jarak yang sama.
- Home: jam21.30WIB dibedakan dari durasi menuju salat. Awan dua lapisan bergerak;
  suasana siang memakai aset lampu mati yang sudah tersedia. Contoh lima salat pada
  kartu Ibadah tidak mengunci jumlah target produksi; implementasi harus membaca
  target aktual dan tidak menyimpulkan kegiatan selesai dari jam/pergerakan animasi.
- Logo: [dipoles](logos/logo-polished.svg) dan
  [pembanding vektor bentuk sebelumnya](logos/logo-original-trace.svg). Pembanding
  adalah penelusuran bentuk untuk review, bukan file asli yang diganti. Identitas
  sabit/bintang/Quran tetap. Eksplorasi Quran–Z terdahulu tidak dipakai/dipilih.

## Warna dan aset

Latar#F1F6F2, kartu putih, teks#153C33, primer#185447, mint#E5F0E8.
Aksen emas kecil pada jadwal; tidak ada blok cokelat susu. Tipografi memakai font
Plus Jakarta Sans yang telah ada. Ikon fitur menyalin **path dan grup mirror asli**
dari delapan VectorDrawable `home_shortcut_ref_*`, tidak menggambar simbol baru.
[Hash asal/cek path](shortcut-preservation.json). Warna/latar mint dan ukuran adalah
penyajian konsep; source/aset Android tidak diubah.

Ilustrasi masjid siang/malam/awan dipakai ulang dari aset yang disetujui. Salinan
WebP diperkecil untuk referensi; sumber PNG tetap utuh. File referensi berada
di bawah batas1MB untuk pratinjau percakapan. Ini bukan ukuran paket APK.

## Pemeriksaan dan batas

[Hasil browser](checks.json): seluruh tab, tanggal/bulan/hari ini, centang ibadah,
keadaan Hifz kosong/terisi, catatan, editor contoh, pilihan logo, suasana siang,
gerak awan dan reduced-motion lulus tanpa error JavaScript. Lebar viewport320px
(permukaan ponsel288px) dan900px (ponsel390px) diperiksa; tidak ada overflow
horizontal pada lima halaman. [Tangkapan layar](screens/) adalah render konsep,
bukan screenshot APK/native atau data pengguna.

Tindakan di luar fokus referensi hanya memberi feedback contoh. Simpan contoh
hanya mengubah pratinjau, tidak mengubah SQLite/nama/jadwal asli. Tidak ada fetch,
API AI atau unggahan. Host widgetState hanya menyimpan pilihan tampilan/contoh.

Runtime/APK1.9.14, seluruh source Android, logo disetujui, data pengguna, WIP,
stash audio dan konfigurasi privat dipertahankan. Tidak ada build APK baru.
UI04/F01–F04/D01/D02/D02a/B01 dan gerbang rilis publik tidak dicentang dari hasil
referensi ini. Perubahan native menunggu tanggapan pengguna atas tampilannya.

## Keputusan28 Oktober2026 — satu identitas ikon per fitur

Pengguna menyetujui logo dipoles, dan meminta ikon fitur konsisten pada semua
halaman. Bentuk asli Quran/Doa/Kiblat/Hifz/AI/Protector/Study/Hadis tetap acuan;
[aturan ikon lintas halaman](FEATURE_ICON_RULES.md) mencatat sumber dan batasnya.
Referensi diperbaiki: kartu simpanan Profil, navigasi bawah, lanjut membaca,
keadaan Hifz kosong serta kartu target memakai simbol yang sama dengan Home.
Ukuran mengikuti konteks; geometri sumber tetap sama. Panah, tambah, edit,
pencarian, centang selesai dan bookmark sebagai tindakan tetap memakai ikon
fungsinya. Bookmark tidak dipakai sebagai pengganti identitas fitur Hadis.

Persetujuan logo tidak otomatis berarti seluruh layar sudah diterima atau
perubahan native sudah diizinkan. F01/F02/F03/F04 tetap belum dicentang.
