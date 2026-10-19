# Rencana pengembangan yang dibahas — 19 Oktober 2026

Kerjakan dan simpan checkpoint per tahap pada `recovery/study-hifz-refresh`.
Daftar ini membedakan rencana dari kemampuan yang sudah tersedia; bukan pernyataan
bahwa seluruh integrasi telah selesai. Tetap Java/XML native, tanpa login dahulu.

1. **Onboarding — selesai pada checkpoint sebelumnya.** Tiga ilustrasi 2D tetap statis. Perbaiki keseimbangan
   ruang, pengantar kecil, judul, paragraf manfaat dan keterbacaan. Pertahankan simbol
   master dan alur belajar → ibadah → Al-Quran. Jelaskan kemampuan yang sudah tersedia,
   tanpa menjanjikan lokasi otomatis, murottal atau AI generatif yang belum ada.
   Setelah komposisi disetujui, tambahkan parallax sentuh/gulir terbatas pada gambar
   utuh dan teks; karakter tetap diam. Hormati animasi sistem/penghemat daya, tanpa loop idle.
2. **Lokasi bersama — terpasang dan diperiksa.** Pilih posisi pengguna dengan izin saat diperlukan, dengan pilihan
   manual ketika izin tidak tersedia. Hubungkan lokasi yang sama ke jadwal sholat,
   cuaca, Kiblat dan pencarian masjid. Sukolilo menjadi acuan awal berlabel; pilihan pengguna dibagikan ke fitur terkait.
3. **Masjid terdekat — versi awal OpenStreetMap, sesuai pilihan pengguna.** Daftar
   dan peta native di Zikra, alamat yang tersedia, jarak garis lurus, cache dan tombol
   arah yang membuka Google Maps. Tidak memerlukan akun/billing Google untuk versi ini.
   Data komunitas serta server publik mempunyai keterbatasan; tidak mengarang fasilitas/
   jam jamaah. Google Places/Maps di dalam Zikra tetap opsi integrasi berikutnya setelah
   project, billing, pembatasan kunci dan kebutuhan backend disiapkan.
4. **Murottal dan hafalan.** Pilihan qari, play/pause, pindah ayat dan ulang satu ayat/
   rentang. Bedakan gulir baca mandiri yang dapat diatur dengan mengikuti ayat audio;
   mode mengikuti audio membutuhkan data waktu per ayat yang sesuai rekaman.
   Hubungkan target Diary Hifz ke rentang bacaan. Pemutar kecil, kontrol notifikasi
   dan unduhan per surah menyusul sesuai dukungan sumber audio/lisensi/akses.
5. **Asisten.** Mulai dari konteks tugas, target hafalan, bacaan terakhir dan jadwal
   sholat yang benar-benar tersedia. Usulan agenda ditinjau pengguna sebelum disimpan.
   Asisten sekarang masih berbasis aturan offline; layanan generatif perlu integrasi
   tersendiri dan kunci provider tidak disimpan di APK.

Study Hub tetap khusus akademik; ibadah dan Diary Hifz terpisah. Pengingat tenggat,
timer fokus dan statistik adalah lanjutan akademik, belum tersedia hanya karena
tercantum pada rencana ini. Pada setiap tahap, rapikan konsistensi visual dan periksa
kondisi data kosong, jaringan gagal dan izin ditolak yang relevan. Uji HP fisik tetap
diperlukan untuk performa, sensor, pengingat dan audio; hasil emulator tidak menggantikannya.
