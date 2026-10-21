# Home/Quran: mengikuti komposisi referensi — 1.3.2

Pengguna menunjukkan perbedaan besar antara Study native dan konsep, lalu menanyakan
Home serta Quran. Versi sebelumnya baru mendekati palet dan kelompok konten;
ilustrasi Canvas serta proporsi belum setara. Tahap ini memperbaiki **Home dan
katalog Quran**. Ilustrasi siswa Study/orb AI belum diganti atau diklaim setara.

## Perubahan yang terlihat

- Home memakai ilustrasi masjid bertekstur dan bukit/palma pada PNG transparan,
  dipadukan dengan gradien serta awan/matahari/bulan native yang tetap terpisah.
  Latar dekoratif tidak lagi menentukan tinggi hero. Logo master dipertahankan.
  Sapaan, waktu, jadwal dan progres tetap data aplikasi; tidak menyalin data contoh.
- Hero lebih pendek, panel jadwal gelap berbingkai, lima slot salat wajib;
  Terbit tetap ada di jadwal lengkap. Delapan pintasan, lanjut membaca dan kartu
  ibadah kini terlihat pada layar awal 360x800dp/font100%. Ikon sumber membuka
  sumber langit dan pengingat. Lokasi/tanggal tetap dapat diketuk.
- Quran: ilustrasi buku/rehal dalam lengkung emas, hero136dp, resume ringkas,
  aksi berbentuk persegi membulat, daftar bergaris sekitar56dp tanpa kartu tebal.
  Lima surah terlihat pada ukuran uji tanpa mini player aktif. Huruf Arab ayat,
  font/pemetaan Mushaf dan sumber teks tidak diubah.
- Juz membuka pemilih juz Mushaf yang sudah ada (Android12+); Penanda membuka
  terakhir dibaca. Itu aksi navigasi, bukan dua daftar katalog baru. Mini player
  hanya ditampilkan bila pemutar memang aktif; tidak memasukkan player contoh.

## Bukti native dan batas

[Home](home.png) · [Quran](quran.png) · [Pemilih juz](juz-picker.png) ·
[Penanda terakhir](bookmark.png). Ini screenshot APK asli, bukan mockup.
[Font200% Home](home-large.png) dan [Quran](quran-large.png) memperlihatkan reflow;
masih ada pembungkusan kata pada tombol Unduhan/dock besar. Tidak dinyatakan
lulus audit aksesibilitas menyeluruh. Pada font besar, isi perlu digulir.

Satu emulator Medium_Phone Android37.1 x86_64, 720x1600/density320, GPU host dan
Vulkan off, tanpa suara/jendela, guestRAM1536MiB/2core. Ditutup setelah capture;
adb kosong dan tidak ada proses emulator/qemu. RAM bebas6,51GiB. Tidak memakai HP.

[Build/lint](build.txt) lulus35detik, 1worker/heap512MiB. Lint: {'Error': 0, 'Fatal': 0, 'Warning': 663}.
Tidak menjalankan ulang suite96JVM, empat tes motion1.3.1 atau tes backend karena
bagian tersebut tidak berubah. Tidak menjalankan metode instrumentasi otomatis
pada tahap ini; pemeriksaan native dilakukan lewat navigasi/screenshot di atas.
[Buffer crash](crash-log.txt) kosong. Tidak menguji ulang audio/seek/sorotan atau
adzan. Gerak langit dipertahankan di kode, bukan klaim ukur performa/baterai HP.

APK: `Zikra-Home-Quran-v1.3.2-debug.apk` di luar repo, versionCode7.
SHA256: `54a7961e51f347d42ea0049287d2130a7469a4aa0b12d5813bbe6baf19905f42`.
Debug signer sama: `c7888fe2ca40e14698726ed20b2f93947033cef0bdee9cfd127c429d0ea0ca1b`.
Sumber build terisolasi tetap mengecualikan eksperimen prefetch audio lama.
[Catatan ilustrasi dan prompt](../../../design/reference-fidelity-2026-10-22.md).
