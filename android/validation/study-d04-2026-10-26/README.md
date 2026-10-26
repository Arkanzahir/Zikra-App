# D04 — Study, jadwal mingguan dan pengingat lokal

26 Oktober 2026, beta1.9.9/code36. D04 selesai untuk implementasi dan uji beta
terarah pada emulator; pengujian HP/penghemat daya lintas vendor tetap G02.
[Receipt versi/source/hash](release.json), [hasil native](native-results.json),
[riwayat percobaan](native-attempts.json), [preservasi](preservation.json),
[audit nilai rahasia yang diketahui](audit.json).

## Perubahan

- Tugas akademik mempunyai pelajaran/mata kuliah, prioritas rendah/sedang/tinggi,
  tenggat dan kalender. Urutan berdasarkan tenggat, prioritas lalu ID stabil;
  tugas tanpa tenggat berada setelah tugas bertenggat. Tambah/edit/selesai/hapus
  tetap tindakan pengguna, bukan efek timer atau notifikasi.
- Jenjang opsional Siswa SMP/Siswa SMA/Mahasiswa/Umum tersedia pada cerita ketiga
  onboarding, Study dan Profil. Pilihan tersimpan lokal, mengubah istilah, dan
  tidak menulis ulang tugas/kelas. Mahasiswa dapat mengisi SKS1–30; mengganti
  jenjang menyembunyikan field SKS tanpa menghapus nilai yang sudah tersimpan.
- Jadwal Senin–Minggu menyimpan nama, ruang/lokasi, guru/dosen, jam dan pengingat
  mati/H-15/H-30. Bentrok waktu ditolak atomik; edit/hapus memakai revisi untuk
  menolak formulir basi. Jam selesai harus setelah jam mulai pada hari yang sama.
  Batas64 kelas dijelaskan; tidak menghapus kelas lama untuk menerima kelas baru.
- Home menampilkan maksimal tiga kelas hari ini dari store yang sama dan zona
  kota pilihan, disertai jumlah sisanya/link jadwal. Tidak memasukkan kelas contoh.
  Hero/ikon/aset Home dan source AiActivity Seimbang dipertahankan.
- Pengingat tenggat lama tetap saat tenggat saja. Opsi H-1/Hari-H harus dipilih:
  H-1 pada jam lokal yang sama; Hari-H satu jam sebelum tenggat, paling awal00.00.
  Tenggat00.00 tidak mendapat dua notifikasi identik. Jadwal baru yang sudah
  terlewat tidak membuat alert retrospektif; alarm lama yang tertunda masih
  dibatasi jendela validitas dan deduplikasi.
- Maksimal128 alarm tenggat terdekat aktif; sisa tugas tetap disimpan dan dipromosi
  setelah alarm sebelumnya diselesaikan. Ada penjelasan kapasitas di Study/editor.
  Satu kejadian terdekat per kelas dijadwalkan. Ganti kota/zona, edit/selesai/hapus
  membatalkan alarm basi; receiver memeriksa data/revisi/zona/izin lagi.
  Notifikasi kelas membawa identitas dan hari kelas, termasuk lead lintas tengah malam.

## Arsitektur dan keamanan

Android Java/XML/SQLite tetap. `WeeklyClass` dan `DeadlinePlan` memisahkan aturan
kalender murni; `WeeklyClassStore` menyimpan transaksi/revisi; `ClassReminders`,
`StudyReminders` dan `StudyAlarmPolicy` mengurus jadwal/delivery; `ClassSummary`
mengikat data Home; `EducationLevel` mengurus preferensi istilah.
Daftar memakai adapter native yang mendaur ulang view tanpa dependensi UI baru.

SQLite utama4→5 menambah subject/priority/advance_reminder secara aditif.
Store jadwal terpisah memakai versi2 (v1→2 menambah SKS dengan default0);
SQLite chat tetap1. Tidak ada reset data untuk menangani kegagalan baca/simpan.
Pilihan kelas tidak dikirim ke backend/AI; integrasi konteks mengikuti C04.
Activity/receiver jadwal tidak diekspor, PendingIntent immutable, query memakai
parameter, label/jam/lead/SKS/bentrok dibatasi. Notifikasi ber-visibility private.
Tidak ada key/token/provider/billing baru atau perubahan layanan hosting.

## Pembuktian

- Unit127 tercatat:123 lulus,4 skip,0 failure/error. Tujuh tes aturan akademik
  meliputi H-15/H-30, pergantian minggu/zona, lead lintas tengah malam, bentrok,
  batas input, deadline dini/DST dan urutan. [XML terarah](academic-unit.xml).
- Build aplikasi/helper dan lint lulus;0 error/773 warning. Warning proyek belum
  seluruhnya dibersihkan. [Build](build.txt), [build helper akhir](build-harness.txt).
- Native akhir23 eksekusi/22 metode unik:13 jadwal/migrasi/editor/metadata/Home/
  pengingat/kapasitas/zona,7 regresi akademik/timer,1 fallback tanpa izin alarm
  tepat waktu,1 pengulangan editor font200%,1 izin notifikasi ditolak.
- AlarmManager asli mengirim tenggat dan dua lead kelas H-15/H-30 pada trigger
  berdekatan, dengan Wi-Fi/data dimatikan dan jaringan VALIDATED tidak tersedia.
  Tidak memanggil receiver secara langsung dalam metode alarm nyata.
  [Receipt alarm](native/native-d04-offline-D04-actual-alarms.json) dan
  [observasi waktu](native/native-d04-offline-D04-alarm-timing.json).
  H-1/Hari-H dan stale/duplikat juga diuji dengan pemanggilan delivery terkontrol;
  tidak mengklaim menunggu satu hari nyata.
- Migrasi memakai SQLite v4 berisi tugas/Hifz/receipt serta store jadwal v1
  berisi kelas/revisi; metadata/progres tidak ditukar menjadi data contoh.
  Batas130 tugas membuktikan128 alarm aktif dan promosi berikutnya tanpa kehilangan tugas.
- Timer fokus berjalan saat background, pause/recreation mempertahankan waktu,
  alarm sesi lama ditolak, selesai tidak menandai tugas dan tidak memulai break
  otomatis. Aturan transisi fokus/break juga diperiksa unit FocusClock.
- Kalender/editor/draf rotasi, batal/hapus dengan konfirmasi dan validasi bentrok
  diuji native. Error digulir ke area terlihat; tombol Simpan tetap terlihat pada
 640×1280/font200%. Kontrol timer/editor tugas diperiksa pada320dp/teks200%.
  Ini bukan pemeriksaan seluruh layar aplikasi pada semua ukuran/font.

Percobaan gagal tetap dicatat pada native-attempts.json: salah target kalender
di skrip, assertion tombol reset legacy yang sudah tersembunyi, dan batas tunggu
alarm80detik yang terlampaui. Percobaan akhir mencatat waktu/hasil tiap notifikasi
dan memakai jendela180detik untuk observasi kelas; tidak mempercepat/memalsukan
AlarmManager atau mengubah kebijakan alarm runtime untuk membuat tes lulus.
Timeout terdahulu tidak menetapkan akar masalah secara pasti.

## Batas dan preservasi

Izin notifikasi dapat ditolak tanpa menghapus record. Tanpa izin exact, aplikasi
tetap menjadwalkan alarm inexact dan menampilkan kemungkinan penundaan. Pemberian
izin exact juga bukan jaminan bebas pembatasan perangkat. Android mendokumentasikan
batching/pembatasan frekuensi alarm while-idle dan penundaan saat Doze:
[AlarmManager](https://developer.android.com/reference/android/app/AlarmManager#setExactAndAllowWhileIdle(int,long,android.app.PendingIntent)),
[panduan alarm](https://developer.android.com/develop/background-work/services/alarms).
Uji ini tidak membuktikan mode Doze, reboot nyata, force-stop pengguna, atau ketepatan
alarm pada semua HP/vendor. Pemeriksaan tersebut tetap G02/D06/D02 sesuai lingkup.

Data emulator dicadangkan privat dan diisolasi sebelum fixture. Sepuluh berkas
baseline ternyata berukuran0byte; hash sesudah pemulihan cocok baseline tersebut.
Jangan mengartikan hasil ini sebagai pembuktian upgrade data nyata di HP.
Migrasi record berisi dibuktikan melalui kasus SQLite sintetis di atas.
Helper dihapus; izin, jaringan, font/keyboard/wake/tema/ukuran dikembalikan dan
diverifikasi; emulator ditutup. HP tidak disentuh. Log mentah, arsip dan konfigurasi
tetap privat; bukti publik memakai fixture dan metadata pemeriksaan.

WIP PROJECT_HANDOFF.md, stash/arsip audio, env privat, logo/art Home, source AI
dan APK1.9.7/1.9.8 tetap cocok hash. Audit terarah tidak menyatakan seluruh sejarah
Git bebas rahasia. APK debug1.9.9 berukuran di bawah100.000.000byte; signer cocok
versi sebelumnya. Ini bukan signed release publik.

C02 laptop mati, C04 konteks/tindakan AI, C07 cakupan lampiran penuh, B01 sumber,
D02/D02a adzan/jeda fokus, E/F/G dan rilis publik tetap terbuka. Progres yang
ditampilkan memakai record nyata; tidak menambahkan statistik fokus lintas hari
atau mengklaim morning briefing E03 sudah tersedia. Berikutnya C04, dengan
persetujuan konteks kelas dan telaah Tadabbur yang masih tersisa.
