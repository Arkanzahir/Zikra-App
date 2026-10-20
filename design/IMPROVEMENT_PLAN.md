# Rencana implementasi Zikra — 20 Oktober 2026 (WIB)

Rencana ini tersimpan di repository. Status berasal dari source dan bukti Android,
bukan preview web. Kerjakan per tahap, pertahankan data pengguna, dan cocokkan
hash GitHub setelah setiap push. Tetap Android Java/XML, JDK 17, SDK/target 35.

## Arah Quran/Mushaf yang dibahas setelah Pomodoro

[Acuan gabungan dan batas implementasi](QURAN_READER_DESIGN.md) tersimpan. Desain
reader/panel audio baru, Mushaf halaman dan sorotan kata masih rencana; jangan
menganggap gambar konsep sebagai source atau bukti APK.

## Pembaruan Fokus/Pomodoro berdasarkan rekaman pengguna

Permintaan setelah `b8f3a04`: Jeda fokus juga memakai pose duduk/cangkir, lalu kembali
membaca saat Lanjutkan. Sisa waktu dan fase timer tidak diganti.
[Bukti pose jeda](../android/validation/focus-pause-pose-2026-10-20/README.md).

Arah aktif mengikuti referensi kedua: satu karakter dengan pose membaca dan istirahat.
Revisi setelah umpan balik pengguna pada `7f9d60a`: cincin besar dihapus, angka di atas
karakter yang lebih besar, gerak badan/kepala/tangan/buku terlihat dan pose duduk
saat istirahat. Label dan karakter menempati view terpisah.
[Bukti revisi](../android/validation/focus-motion-placement-2026-10-20/README.md).
Paragraf berikut mencatat checkpoint pertama yang telah direvisi.
Canvas native menampilkan karakter daun orisinal Pine/emas, cincin progres, napas/kedip,
transisi fase dan tombol yang tetap terjangkau. Animasi mengikuti pengaturan sistem,
penghemat daya, lifecycle serta jeda timer. Font 200% memperoleh ruang ekstra.
Tidak mengubah logo master, schema atau mekanisme penghitungan timer.
Pengguna meminta seluruh checkpoint terbaru langsung masuk main setelah pemeriksaan.
[Bukti video dan hasil](../android/validation/focus-motion-2026-10-20/README.md).

## Titik awal yang telah diperiksa

- Repo utama `Arkanzahir/Zikra-App`; `prototype_zikra` hanya referensi lama.
- GitHub `main` sudah `1045fa7`, berisi recovery `d9d2ed4` serta dua commit desain/audio.
  Branch `recovery/study-hifz-refresh` masih `d9d2ed4`; jangan kembali ke sana dan
  kehilangan pembaruan yang sudah ada di main. Periksa ulang ref ketika melanjutkan.
- Checkout saat audit bersih. Tahap baru memakai `feature/location-polish-2026-10-20`,
  berdasarkan `1045fa7`. Jangan reset atau menimpa perubahan yang ditemukan.
- [Validasi audio 19 Oktober](../android/validation/location-quran-2026-10-19/README.md):
  46 tes JVM dan 20 metode Android 9/API 28 lulus. Audio uji sintetis, bukan murottal;
  rekaman qari live, full download jaringan, API 35 dan HP fisik belum terverifikasi.

## Tahap 1 — masjid dan pemilih lokasi (prioritas aktif)

Tujuan: lokasi pencarian mudah dibaca, hasil mudah dipilih, dan status jaringan jelas.

- Header, status/navigation bar dan latar halaman menyatu memakai netral `#F4F6F8`.
  Pisahkan dari warna kertas onboarding; jangan mengubah onboarding/Home yang diterima.
- Ringkas lokasi aktif; nama kota dominan, sumber/zona lebih kecil, koordinat/akurasi
  melalui rincian. Tetap ada acuan awal berlabel dan konfirmasi sebelum menyimpan.
- Pertahankan satu kontrol Daftar/Peta, hierarki nama → jarak → tombol arah minimal 48dp.
  Marker dan kartu pilihan membuka Google Maps eksternal; jarak tetap garis lurus.
- Tampilkan indikator pencarian kota; pilihan offline tetap dapat dipilih selama menunggu.
  Memilih kota manual membatalkan lookup/GPS yang tertunda; callback lama tidak mengganggu
  pilihan baru. Penolakan izin tetap menyediakan kota manual.
- Bedakan belum mencari, sedang memuat, hasil kosong dan gagal. Pertahankan hasil/waktu
  lama saat refresh gagal; jelaskan pembatasan layanan dan jedanya. Hasil kosong tetap
  dipulihkan saat Activity dibuat ulang. Jangan mengisi hasil contoh pada APK pengguna.
- Periksa UI Android aktual, pilihan kota/zona, perubahan kota, daftar/peta/arah,
  loading/kosong/gagal, recreation dan font besar. Screenshot fixture diberi label/konteks.

[Bukti tahap 1](../android/validation/location-polish-2026-10-20/README.md).
Data komunitas OSM dan server publik tidak menjamin semua masjid tersedia atau SLA.

## Tahap 2 — Quran, murottal dan Hifz (sudah terpasang; verifikasi live terbuka)

Prioritas pengguna sebelumnya: murottal → gulir Quran → hafalan. Source pada main sudah memuat:

- Pemutar per ayat, tiga qari, putar/jeda/seek/sebelumnya/berikutnya, rentang/pengulangan;
  foreground service `mediaPlayback`, MediaSession/notifikasi dan audio focus.
- Unduhan eksplisit per surah/qari, service `dataSync`, berkas atomik, penolakan berkas
  parsial/rusak dan hapus audio tanpa menghapus tugas/bookmark/target Hifz.
- Gulir baca berkecepatan terpisah dari mengikuti audio; sentuhan menjeda gerakan.
- Target Diary Hifz meneruskan surah/ayat awal/akhir ke reader, tanpa penilaian AI atau
  otomatis menandai latihan selesai. Source audio rekaman, bukan TTS doa.

Lanjutan pemeriksaan: rekaman qari asli dan kebenaran ayat, unduh melalui jaringan,
putus/sambung, layar mati, telepon/headset/Bluetooth, penghemat daya vendor dan API 35.
Cloud saat ini membatasi host CDN. Jangan menganggap WAV tes sebagai Quran atau
mengklaim full download telah diuji. Mushaf layout edisi tetap pekerjaan terpisah.

## Tahap 3 — konsistensi Study, Hifz, asisten dan reader

- **Study:** tonjolkan tugas aktif dan langkah berikutnya, ringkas kartu kosong. Tetap
  akademik; tugas/progres hanya dari tindakan pengguna, data lama tetap tersimpan.
- **Hifz:** pertahankan kartu Pine; utamakan nama surah dan rentang ayat, lalu progres/
  tindakan latihan. Jangan mencampurkan ibadah/akademik atau membuat progres contoh.
- **Asisten:** kurangi pembuka berulang, utamakan percakapan dan composer yang terjangkau.
  Label aturan offline tetap jujur. Gunakan mikrofon vektor untuk input suara.
- **Quran:** jarak ayat proporsional, kontras/ukuran teks nyaman dan kontrol tidak menutup
  bacaan. Periksa tema malam serta font besar. Pertahankan bookmark/resume.
- Gunakan komponen/resource bersama dan ikon vektor konsisten. Jangan mengubah master
  `branding/zikra-logo-primary.jpg`: bulan, buku, spark dan proporsinya tetap; Pine `#15332F`.

Ini arah tahap berikutnya, bukan klaim bahwa seluruh perapian tersebut sudah dikerjakan.

## Checkpoint lanjutan peta/audio/Study

Branch `feature/map-audio-study-2026-10-20`, berdasarkan `2b13539`:

- Status loading/belum termuat dan coba ulang tile peta; penanda tidak diklaim sebagai bukti jalan sudah termuat.
- Unduhan memeriksa decoder cache audio dan mengganti berkas rusak melalui transfer atomik.
- Study menonjolkan tugas berurutan tenggat; header/ruang kosong diringkas. Detail rentang Hifz diperbesar dan kartu Pine dipertahankan.
- Timer 25/5 dan 50/10, hitung monotonic, jeda/lanjut, simpan/recreation, background tanpa foreground service.
  Pergantian fase dimulai sendiri, tidak menandai tugas selesai. Reboot menjeda sisa tersimpan.
- Tenggat/pengingat Study opsional, draft editor tersimpan dan migrasi SQLite v3 mempertahankan data.
  Notifikasi/alarm lokal inexact dapat tertunda sistem; perubahan/selesai/hapus memblokir alarm lama.
- Akses live OSM/Overpass/CDN masih CONNECT 403; uji fixture Android tidak menggantikan verifikasi provider asli.

[Bukti checkpoint](../android/validation/map-audio-study-2026-10-20/README.md).

## Tahap 4 — rincian akademik terpasang dan backlog berikutnya

- **Timer fokus akademik — terpasang pada branch lanjutan:** siklus fokus/jeda, pulihkan waktu setelah background/recreation,
  tindakan mulai/jeda/selesai jelas; jangan menandai tugas selesai otomatis.
- **Pengingat akademik — terpasang pada branch lanjutan:** tenggat dan preferensi per tugas, izin notifikasi saat diperlukan,
  penjadwalan ulang setelah edit/reboot/perubahan waktu. Uji izin ditolak dan hemat daya.
  Penyimpanan baru harus memakai migrasi yang mempertahankan data; tanpa reset SQLite.
- **AI generatif:** backend HTTPS dengan kredensial provider di server, bukan APK/Git/chat.
  Konteks tugas/Hifz/jadwal hanya yang benar-benar tersedia dan diperlukan. Usulan agenda
  ditinjau sebelum disimpan. Tanpa backend/model/kredensial valid, jangan memakai balasan
  template sebagai bukti integrasi generatif atau penilaian hafalan.
- **Mushaf halaman/Aladhan:** verifikasi sumber/layout/font/lisensi atau metode/jadwal lebih
  dahulu; bukan sekadar menyembunyikan terjemahan atau mengganti label jadwal lokal.

## Penyimpanan dan bukti setiap tahap

1. Periksa status/branch/commit/remote/perubahan lokal; baca README, RECOVERY, API_PLAN,
   rencana ini dan laporan validasi terbaru. Jangan menyimpulkan source dari nama environment.
2. Selesaikan satu tahap, tampilkan screenshot/preview Android aktual, lalu satu putaran
   build/lint dan tes relevan. Ulang hanya bila ada perubahan, kegagalan atau masalah terbuka.
3. Simpan hasil dan batas pemeriksaan di `android/validation/`, perbarui status/rencana,
   commit dan push checkpoint. Cocokkan hash remote; laporkan bila push gagal.
4. Jangan menganggap autentikasi cloud berhasil hanya karena pernah berhasil. Gunakan
   koneksi resmi yang tersedia; jangan meminta token di chat. Main digabung sesuai
   permintaan pengguna; branch checkpoint tetap memungkinkan tinjauan per tahap.
