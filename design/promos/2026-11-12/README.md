# Cuplikan promo 15 detik — 12 November 2026

## Revisi kartun 2D sesuai klarifikasi pengguna

Kandidat semirealistis di bawah ditolak pengguna sebelum disampaikan sebagai
hasil akhir. Pengguna menginginkan karakter kartun. Ketiga PNG pada folder
characters sekarang adalah kartun2D hijau-krem dengan identitas konsisten,
menggantikan rupa lama. Prompt terpakai tercatat di characters/prompts.json.

Hasil terbaru D:/Download/Zikra-App-main/Zikra-Promo-52s-Kartun-2D-Putri.mp4.
Susunan characters-edit.json dipertahankan dan render dilakukan melalui
proses baru agar cache aset lama tidak terpakai. Suara/naskah dan durasi52detik
cocok dengan versi Putri yang dipilih pengguna.1560frame+audio decode lulus;
alpha, ukuran serta posisi lima vignette diperiksa; pembuka/paham/Study
ditinjau. [Bukti terbaru](checks-cartoon.json).
Gerak ilustrasi berupa masuk/fade/pergantian pose. Penerimaan visual oleh
pengguna dan hak komersial suara tetap belum dipenuhi. Video tetap lokal;
runtime dan gerbang aplikasi tidak diubah.


## Karakter pendukung cerita

Hasil lokal D:/Download/Zikra-App-main/Zikra-Promo-52s-Karakter-Putri.mp4.
Tiga karakter cutout berlatar alpha dibuat dengan built-in ImageGen, dengan
pose bingung, memahami melalui HP, dan menulis saat belajar. Tokoh mahasiswa
laki-laki semirealistis hijau-krem adalah pilihan visual agen untuk tinjauan.
[Asset/prompt lengkap](characters/README.md), [susunan](characters-edit.json),
[bukti pemeriksaan](checks-characters.json).

Karakter ditempatkan pada pembuka0-3,35, pengantarAI8,95-11,6, jawabanAI17,4-20,4,
Study20,4-23,3 dan fokus23,3-27,2. Pembuka berubah menjadi karakter di kiri dan
teks di kanan. Pada tutorial, karakter menjaga ruang layar dan penunjuk tombol.
Animasi berupa fade/masuk12px selama0,4detik, lalu hold; pose Study/fokus
berlanjut tanpa menghilang. Tidak ada gerak anggota tubuh. Durasi52detik dan
naskah/VO Putri dari pilihan pengguna; ADTS stream audio cocok byte-sama.
1560frame+audio decode penuh lulus, posisi karakter di viewport/tidak menutupi
HP saat hold lulus; pembuka/jawabanAI/Study dan contact sheet ditinjau.
Aset hanya digunakan pada promo, tidak ditambahkan ke APK. Tidak ada foto/data
pengguna atau akses HP/emulator baru. Video lokal untuk tinjauan; penerimaan
visual dan lisensi komersial suara tetap belum selesai, aplikasi tidak berubah.


## Suara dan naskah pilihan pengguna, 52 detik

Hasil lokal D:/Download/Zikra-App-main/Zikra-Promo-52s-Putri-Naskah-Baru-Final.mp4.
1280x720/30fps, H.264/AAC48kHz. Pengguna mengonfirmasi gaya HP/latar sama dengan
versi sebelumnya, dengan suara/naskah baru dan urutan adegan diselaraskan.
Naskah lengkap sesuai kiriman tersimpan di script-approved.txt,104kata.
Suara Putri Maharani pada tautan I7sakys8pBZ1Z5f0UhT9 cocok selected-radio value
demo resmi. Halaman profil menghasilkan audio blob yang tidak bisa diekspor
melalui media yang terlihat; hasil unduhan diperoleh melalui demo Indonesian
resmi dengan ID yang sama. Tidak ada API key/login/paket berbayar baru.

Bacaan51,069detik memakai tempo asli, diberi lead250ms dan ekor diam hingga
52detik. Urutan baru mulai dengan pertanyaan, pengenalan, tutorial AI, Study/
fokus, Quran/hafalan/Kiblat/salat, Protector, lalu ajakan beta. Fotografi/
bingkai/bahan rekaman berasal dari versi sebelumnya. Jeda jaringan pada rekaman
AI dipersingkat dan aksi UI dipotong sesuai selected-voice-edit.json.

Renderer menerima prepared-audio eksplisit dan durasi dari edit list; jalur
legacy47 tetap tersedia. Penjagaan menolak penggunaan cache lama untuk timeline
baru, termasuk ketika WAV legacy sudah ada. Teks shot pendek muncul lebih
cepat; penunjuk menu ditahan pada menu dan penunjuk jawaban baru muncul setelah
respons terlihat. Render percobaan awal tetap lokal, hasil Final sudah dikoreksi.
Bukti di checks-selected-voice.json:1560frame+audio decode penuh lulus, geometri/
hold lulus, rata-rata-16,5dB/puncak-1,4dB, visual menu/jawaban/contact ditinjau.
Penilaian pendengaran dan penerimaan estetika tetap memerlukan pengguna.
Lisensi komersial suara belum dipastikan; output tetap lokal untuk tinjauan.
HP/emulator dan runtime aplikasi tidak diakses; seluruh gerbang aplikasi tetap.


## Versi lengkap 47 detik

Hasil lokal D:/Download/Zikra-App-main/Zikra-Promo-47s-Landscape-Putri-Final.mp4.
1280x720/30fps, H.264/AAC48kHz, 47detik. Receipt final di checks-full.json.
File video/audio tetap lokal untuk tinjauan. Source dan edit decision list
di tools/render_zikra_promo_full.py, tools/promo_device.py dan full-edit.json.

### Susunan dan gerak

0-2,2 logo; 2,2-6,4 Home; 6,4-9,9 Quran; 9,9-14,55 hafalan/Kiblat/salat;
14,55-18,1 pengantar AI; 18,1-27,47 tutorial AI; 27,47-31,78 tambah tugas;
31,78-35,15 fokus; 35,15-39,7 Protector; 39,7-47 logo dan ajakan beta.
Bingkai perangkat diam setelah masuk. Putaran pendek menampilkan sisi belakang
dan rim saat perpindahan tiga bagian, tanpa ayunan terus-menerus. Bagian tutorial
tetap besar di tengah dengan penunjuk kecil pada input/menu/tombol.

### Bahan nyata dan penyuntingan

FreshAVD ZikraPromoFull memakai APK1.9.40 dan data contoh baru. Quran dipilih,
audio ditekan, AI mengetik pertanyaan fotosintesis publik, membuka menu lampiran,
mengirim dan menerima jawaban Online asli. Study merekam ketikan dan simpan
tugas contoh; fokus berjalan. Hifz/Kiblat/salat adalah hold tampilan aplikasi.
Home/Protector menggunakan klip asli sesi11November dengan data contoh.
Isi jawaban AI tidak diganti. Pemilihan foto/dokumen ditampilkan sebagai menu;
adegan ini tidak memperagakan unggah berkas baru. Jeda jaringan dipersingkat,
dan sebagian navigasi/ketikan dipercepat sesuai full-edit.json. Tidak ada suara
tilawah/adzan yang dicampur dengan narasi.

Narasi lengkap595karakter dihasilkan lewat demo publik ElevenLabs dengan suara
Putri Maharani, file41,8detik. Dipotong hanya di jeda terukur dan diberi jeda
tambahan menjadi47detik; kecepatan suara tetap. Normalisasi rata-rata-16,9dB
dengan puncak-1,5dB. Model tidak menilai audio lewat pendengaran langsung.
Lisensi komersial keluaran demo belum diverifikasi. Publikasi komersial menunggu
pemastian hak suara. Tidak ada paket berbayar yang diaktifkan.

### Pemeriksaan dan pemulihan

1410frame+audio didekode penuh; 360sampel siluet perangkat di dalam1280x720
dan pose hold diam. Contact sheet serta frame tutorial/akhir ditinjau visual.
Percobaan awal kekurangan kapasitas D, kemudian data emulator baru di E dipakai
setelah persetujuan. Dua take AI ditolak karena karakter awal/urutan navigasi,
take terakhir benar. Screenrecord tampilan diam berisi satu frame tanpa durasi;
decoder disesuaikan agar hold frame asli tetap bisa dipakai. Tanda baca rusak
di catatan kecil pada render awal dikoreksi sebelum ekspor Final.
Data aplikasi freshAVD dibersihkan, kedua proses milik sesi ditutup dan folder
E:/Zikra-Promo-Temp-20261112-0937 dihapus setelah validasi marker dan path.
WIP/stash/arsip/audio/env/logo aplikasi terpelihara; AVD lama dan HP tidak dipakai.
Perangkat video adalah proyeksi permukaan/rim dengan belakang prosedural.
Penerimaan estetika oleh pengguna serta gerbang rilis aplikasi tetap terbuka.


## Revisi v3 — ayunan dan lintasan melengkung, 60 fps

Pengguna masih menilai HP v2 kaku. Mode `--motion fluid --fps 60` menambah
keyframe spline dengan posisi/kecepatan kontinu, antisipasi, banking, perubahan
skala dan gerak lanjutan. Sisi lengkung/depth18, highlight kaca ringan serta
shutter empat sampel saat perpindahan cepat memperjelas bentuk dan arah gerak.
Percobaan shutter dua sampel sebelumnya menghasilkan bayangan ganda yang terlihat;
output percobaan tetap lokal dan bukan hasil yang disampaikan sebagai revisi akhir.

Hasil lokal: `D:/Download/Zikra-App-main/Zikra-Promo-Preview-Landscape-v3-Fluid-Final.mp4`.
Format1280×720/15detik/60fps; narasi Putri dan footage lama tetap. Ini masih
geometri/proyeksi layar dan rim, bukan render mesh perangkat 3D penuh.
Receipt/hasil decode dan kontinuitas berada di `checks-fluid.json`. Penerimaan
visual pengguna belum diasumsikan; lisensi komersial/video lengkap/publikasi
serta gerbang aplikasi tetap. Tidak memakai HP/emulator/provider atau biaya baru.

## Revisi mendatar v2 — format dan gerakan dikoreksi

Pengguna menilai v1 kaku dan mengoreksi format referensi yang mendatar. Versi
baru `D:/Download/Zikra-App-main/Zikra-Promo-Preview-Landscape-v2.mp4` adalah
1280×720/30fps/15detik, 2.708.357 byte. Teks ditempatkan di sisi berlawanan
dengan HP. Posisi, skala, yaw, pitch dan roll berubah berkelanjutan dengan kurva
quintic saat berganti sisi; perspektif front/rear memberi kesan ketebalan.
Ini proyeksi bidang layar dengan kedalaman, bukan mesh perangkat 3D penuh.

Narasi Putri dan rekaman sumber sama tanpa pembuatan suara/biaya baru.450frame
dan audio didekode penuh; visual3,5/5,5/7,5/13detik ditinjau. Posisi pada lima
batas perpindahan kontinu,405frame sesudah masuk layar terukur tidak terpotong.
Cek awal margin65px terlalu ketat: min54,94px tetap dalam viewport dan logo
terletak terpisah di kiri atas. Temuan awal tetap dicatat dalam receipt.
[Bukti revisi](checks-landscape.json), source `tools/render_zikra_promo_landscape.py`.
V1 di bawah disimpan sebagai riwayat; penerimaan visual v2 belum diasumsikan.
Video lengkap/lisensi komersial/publikasi tetap menunggu. Runtime/data/aset/HP/
model/paket/gerbang rilis tetap. Source dan bukti saja masuk checkpoint GitHub.

Pengguna meminta contoh editing berdasarkan video KamiTolong yang diberikan,
setelah memilih sampel suara perempuan Putri Maharani dari ElevenLabs. Ini
cuplikan pembuka untuk ditinjau, belum video promo lengkap atau publikasi.

## Hasil

- Video lokal `D:/Download/Zikra-App-main/Zikra-Promo-Preview-15s-Putri.mp4`.
- 15 detik, 720×1280, H.264 30 fps, AAC 48 kHz mono; 2.553.295 byte.
- Latar putih/mint, teks bertahap, perangkat dengan proyeksi perspektif ringan,
  bayangan, pergerakan kiri/kanan dan rekaman Home/Quran APK beta 1.9.40 asli.
- Narasi baru 14,68 detik dibuat dari naskah pembuka di halaman resmi ElevenLabs
  Indonesian Text to Speech, Putri Maharani dipilih. Bukan audio contoh profil
  suara yang dipakai ulang. Tidak mengaktifkan paket berbayar atau API key.
- Suara dinormalisasi; kecepatan tidak diubah. Tanpa musik, tilawah atau adzan
  tambahan. Suara perempuan dan tempo narasi tetap perlu penilaian pengguna.

## Naskah cuplikan

Kenalkan, Zikra. Aplikasi pendamping ibadah dan belajar dalam satu tempat.
Baca Al-Quran lengkap dengan terjemahan dan audio. Lanjutkan hafalanmu,
cek arah kiblat, dan aktifkan pengingat salat sesuai kebutuhan.

## Sumber dan batas

- Rekaman Home/Quran dari sesi emulator terisolasi 11 November yang sudah ada;
  tidak merekam ulang HP, data pengguna atau layar pribadi.
- Master logo tetap. Turunan putih ditint hijau hanya dalam komposisi video;
  file aset aplikasi tidak berubah.
- Inspirasi tata gerak berasal dari referensi pengguna, tidak memakai ulang
  rekaman, merek, musik atau narasinya.
- Mengadaptasi referensi mendatar menjadi vertikal. Rotasi/pergeseran perangkat
  adalah komposisi 2D dengan perspektif; bukan render perangkat 3D penuh.
- Narasi dibuat melalui demo publik gratis ElevenLabs; jangan menganggap hak
  komersial sudah terpenuhi. Lisensi sesuai paket/output harus dipastikan sebelum
  promosi komersial. Audio/video hanya lokal, tidak masuk Git atau dipublikasikan.
- Tidak ada klaim sudah di Play Store, notifikasi selalu tepat, atau seluruh
  A–D/E/gerbang rilis selesai. Tidak mengubah APK/backend/model AI/paket/cron.

## Verifikasi

`checks.json` menyimpan ukuran/hash, hasil decode penuh dan preservasi. Keempat
frame 0,5/3,5/7,5/13 detik ditinjau: judul, Home, Quran dan daftar manfaat tidak
bertumpuk; layar Quran memakai bagian reader sesudah sheet pengaturan ditutup.
450 frame dan audio didekode penuh tanpa error; rata-rata audio -16,7 dB dan
puncak -1,5 dB. Ini bukan penilaian mendengar secara langsung oleh model.

Source: `tools/render_zikra_promo_teaser.py`. Bahan lokal di
`.local/promo-teaser-20261112/`, rekaman sumber di `.local/promo-zikra-20261111/`.
Render memakai Pillow, NumPy, FFmpeg dan font Windows; metadata sumber/hash
memungkinkan bahan dipasangkan kembali tanpa menyimpan audio/video di Git.
