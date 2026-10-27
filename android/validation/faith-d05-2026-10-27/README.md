# D05 — Jurnal ibadah, latihan Hifz dan target khatam

**Selesai teknis beta,27 Oktober2026.** Kandidat1.9.14/code41, source
`5288e033cf5d3e195177fe7538baf3c4bdd00398`. APK lokal folder luar
`Zikra-Faith-v1.9.14-beta-debug.apk`, **41.592.382byte**, SHA256
`70e2b24916fafb6beb634ef998dcff892c9070d356478544bbd875962f5652cd`.
Signer debug sama1.9.13; unduhan awal <100.000.000byte. Ini checkpoint lokal,
belum push baru atau signed release publik. Pengguna menunda uji HP.

## Yang diterapkan

- **Jurnal bertanggal:** pilih tanggal, tulis/ubah/hapus isi jurnal, lihat jumlah
  checklist ibadah pada tanggal itu dan riwayat bacaan/latihan. Tombol checklist
  jelas membuka **hari ini**; riwayat tanggal lama tidak dipindah ke hari baru.
- **Latihan Hifz:** catat surah/rentang/catatan opsional dari jurnal atau menu
  target Diary Hifz. Target yang sudah ada mengisi rentang awal. Latihan disimpan
  sebagai snapshot, sehingga ubah/hapus target tidak membuang riwayat latihan.
  Catatan latihan tidak otomatis menandai target selesai atau menambah khatam.
  Penyelesaian target tetap lewat centang pengguna di Diary Hifz.
- **Target khatam30/90/365hari:** rekomendasi kumulatif berdasarkan jumlah
  6.236 ayat Hafs, bukan panjang bacaan yang seragam. Hanya rentang bacaan yang
  disimpan eksplisit dihitung; overlap/pengulangan dihitung sekali. Sisa target
  hari ini mengikuti jadwal, sedangkan progres aktual tidak naik karena waktu,
  bookmark atau pemutaran audio. Buka ayat berikutnya menuju ayat pertama yang
  belum tercatat. Ubah durasi mempertahankan bacaan; mulai putaran baru perlu
  konfirmasi, mengarsipkan target lama tanpa menghapus riwayatnya.
- **Streak/ringkasan:** satu hari aktif bila ada ibadah bertanggal yang dicentang
  atau entri bacaan/latihan. Jurnal teks saja, tugas akademik, checklist kosong
  dan flag Hifz lama tanpa tanggal penyelesaian tidak diberi hari aktif fiktif.
  Streak boleh berakhir kemarin bila hari ini belum dicatat; statistik mingguan
  memakai tujuh tanggal lokal terakhir. Home/Profil/jurnal membaca sumber yang
  sama melalui FaithSummary.
- **Katalog target luring:** editor Hifz memakai114 metadata surah bawaan yang
  sudah diaudit D03; tidak menunggu API untuk memilih Al-Mulk/rentang lain.
  Reader tetap mengikuti kemampuan/cache sumbernya sendiri.

Home mempertahankan hero/ikon/tata letak lama dengan satu pintasan ringkasan di
bawah kartu ibadah. Profil dan header Diary Hifz/checklist memberi jalan menuju
jurnal. Desain AI/logo/masjid/audio pilihan pengguna tidak diganti.

## Penyimpanan dan pengamanan

FaithStore memakai `faith.db` versi1 terpisah: plans, entries dan day_notes.
Database utama tetap versi5, chat1, jadwal kelas2. Tidak mengubah/mengonversi
status target lama menjadi catatan latihan atau menebak tanggal selesai.

Mutasi memakai transaksi dan parameter SQL. Token entri membuat kirim ulang
identik tidak menambah duplikat; revisi menjaga edit/hapus yang basi. Rencana
memeriksa ID target saat formulir dibuka agar perubahan dari layar lain tidak
ditimpa. Callback Simpan setelah dialog ditutup diabaikan. Validasi surah/rentang,
tanggal tidak melebihi hari lokal saat menulis, catatan maks1.000 unit UTF-16,
batas100 entri/tanggal dan5.000 total; penuh memberi error tanpa menghapus data
lain. Batas100 dan idempotensi pada kapasitas itu diuji; bukan uji beban5.000.

Tanggal historis tidak ditulis ulang saat berganti zona. Cakupan bacaan tersimpan
tetap ada ketika pindah ke tanggal lokal yang lebih awal; streak tetap kalender
lokal. Tidak menjanjikan sinkronisasi lintas HP, pemulihan setelah uninstall atau
enkripsi database tambahan. Data berada di sandbox aplikasi; Activity tidak
diekspor. Jurnal tidak dimasukkan ke konteks/upload AI atau server baru.

## Bukti

**143 unit lulus/4 skip,23 eksekusi native/21 metode unik lulus,lint0 error/
780 warning.** Empat skip adalah satu snapshot katalog Doa, satu dokumen Mushaf
dan dua metrik font host opsional, bukan tes D05 yang dianggap lulus.

| Kelompok native | Hasil |
| --- | --- |
| native-storage |10 metode: tujuh FaithStorage + tiga HifzRecovery; transaksi/replay/revisi basi, overlap/edit/delete,30/90/365, arsip/snapshot, note tidak menambah streak, undo, kapasitas100, hari/zona, validasi dan koeksistensi database lama|
| native-journal |8 metode UI: kosong/batal, pencatatan overlap/edit/hapus/batal, bookmark, draf recreation/invalid/ulang Simpan, Hifz prefill/manual completion, Home/Profil, tanggal lama dan putaran baru|
| native-large-keyboard |2 metode diulang pada320dp/font200% dengan IME nyata: draf dan tombol Simpan tetap terlihat di atas keyboard, recreation dan validasi|
| native-hifz-worship-regression |3 metode lama: editor114 surah/rentang/draf/batal dikte, centang target dan reader Al-Ikhlas2–4, template ibadah/stale editor lintas hari tanpa simpan palsu|

Metode historis bernama `testCatalogLive...` pada putaran ini memakai katalog
bawaan; **Wi-Fi/data emulator dimatikan**. Batal dikte memakai callback pengujian,
bukan bukti mikrofon baru. Tidak ada permintaan AI/Quran provider pada putaran ini.
Uji fisik/baterai/OEM dan upgrade HP berisi tetap terpisah.

- [Receipt source/APK/hash](release.json),[unit](unit-results.json),
  [empat uji perhitungan](TEST-com.zikra.app.faith.FaithProgressTest.xml),
  [native final](native-results.json),[audit privat](audit.json),
  [preservasi terakhir](preservation.json).
- [Storage/Hifz lama](native-storage.txt),[jurnal](native-journal.txt),
  [keyboard200%](native-large-keyboard.txt),
  [regresi editor lama](native-hifz-worship-regression.txt).
- [Ringkasan khatam](screenshots/D05-recorded-reading.png),
  [jurnal teks tanpa streak palsu](screenshots/D05-empty-note.png),
  [Profil](screenshots/D05-profile-summary.png),
  [form biasa](screenshots/D05-keyboard-draft.png),
  [form200%/keyboard/footer](screenshots/D05-keyboard-200.png).
- [Build kandidat](build-release-candidate.txt),
  [build perbaikan editor](build-editor-final.txt).

Putaran pertama20 tes fungsi lulus, tetapi **ditolak saat review gambar** karena
label catatan panjang dan custom view AlertDialog memenuhi viewport keyboard200%.
Editor diganti dengan body bergulir, label ringkas, footer tetap dan ukuran yang
mengikuti area terlihat/IME. Assertion ditambah untuk tombol Simpan minimal48dp
dan berada di atas keyboard; callback tombol setelah dismiss juga diuji.
[Catatan penolakan visual awal](initial-visual-review.json) dan hasil/preservasi
putaran1/2 disimpan; hanya gambar kandidat akhir diterbitkan di sini.
Pembangunan awal juga sempat menemukan encoding karakter sisipan; source UTF-8
dibetulkan sebelum build kandidat. Log awal tetap lokal, tidak dihitung lulus.

## Preservasi dan batas

Seluruh10 berkas asli internal emulator cocok hash setelah pemulihan, tetapi
baseline itu0byte: **bukan bukti upgrade HP berisi**. Pengaturan font/keyboard,
tema/ukuran, jaringan, DND/ringer, izin notifikasi/exact alarm dan pengaturan
terkait dikembalikan. Helper dihapus/emulator ditutup. Folder output validation
sebelumnya dipindah sementara/dikembalikan; screenshot editor legacy pada agenda/
merupakan output tes emulator, tidak dinyatakan sebagai data pengguna yang di-hash.

WIP PROJECT_HANDOFF, stash/arsip audio A03, env privat, desain AI/Home/logo,
audio asli dan APK lama dipertahankan.96 entry media1.9.13 termasuk dua MP3 adzan
identik pada kandidat ini. Nilai rahasia privat yang diketahui tidak ditemukan
pada source/evidence/entry APK; ini bukan audit seluruh sejarah Git atau G03 penuh.
Audio privat tidak masuk Git dan izin distribusinya tetap gerbang D02/G03.

D05[x] untuk cakupan beta di atas. D01/D02/D02a dan G02 fisik tetap ditunda;
D06 migrasi/backup lokal menyeluruh, B01/C02/C04/C07/E/F/G serta publik tidak
ditutup oleh laporan ini. Berikutnya D06; jurnal baru perlu ikut pemeriksaan
preservasi pembaruan, tanpa menambahkan akun/cloud sync yang ditunda A02.
