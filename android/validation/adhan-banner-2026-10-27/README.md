# D02 — banner adzan dan kontrol berhenti, 27 Oktober2026

Kelanjutan [beta1.9.11](../prayer-adhan-d02-2026-10-27/README.md) atas persetujuan
[konsep banner](../../../design/concepts/adhan-banner-2026-10-27.md). Ini pekerjaan
tampilan/kontrol adzan, bukan penutupan D02/D02a atau rilis publik.

Kandidat final **1.9.12/code39**, source `73ff1b25c642b1ffb2847ec11b16259439a6861e`, APK
**Zikra-Adzan-v1.9.12-beta-debug.apk**, **41.333.887byte** (<100.000.000), SHA256
`d505fb59613c0f1f5331019473a3a3eef5064f40ed645a38ea4f59301f1bf8df`. **134 unit lulus/4 skip,20 eksekusi native/17 metode unik lulus**,
lint0 error/780 warning; schema5/1/2, signer debug sama dengan1.9.11.
[Receipt](release.json), [hasil native](native-results.json), [input sentuh](touch-inputs.json),
[preservasi](preservation.json), [audit](audit.json), [aset](media.json).

## Perubahan

`AdhanNotification` memusatkan pembuatan kartu, action dan kategori. Pemutaran
otomatis memakai kategori `zikra_adhan_alerts` berprioritas HIGH tanpa nada/getar
sistem tambahan. Preview memakai kategori playback LOW lama. Kategori lama tidak
dihapus/dinaikkan; blok playback lama dan blok kategori adzan baru tetap menahan
pemutaran otomatis. Pengguna boleh menurunkan prioritas; aplikasi tidak menaikkannya
kembali atau melewati DND/senyap/izin.

Kartu memakai dekorasi/header Android, konten XML ringkas dengan tombol mint
**Hentikan adzan**. Banner dan kartu diperluas menggunakan satu action Android;
konten heads-up tidak mengulang tombol ringkas. Pada font>=150%, kartu ringkas
memakai **Stop** dengan contentDescription lengkap dan menyembunyikan subjudul
agar tinggi ringkas terjangkau; kartu diperluas tetap menampilkan teks lengkap. Teks dan warna
dipisah ke resource, ada varian night. Tombol ringkas minimal48dp; judul/status
ringkas ellipsis bila sempit, judul lengkap tersedia pada kartu diperluas. Kartu
menampilkan nama salat/status, tanpa nama pengguna, kota, profil atau jadwal kelas.
Isi playback dibuat PUBLIC untuk kontrol layar kunci yang tidak memuat data pribadi;
pengaturan layar kunci pengguna tetap berlaku.

Notifikasi foreground ditampilkan segera (API31+). Pembaruan status memakai
only-alert-once; tidak mengulang banner tiap tick. Tombol stop ringkas dan action
Android memakai PendingIntent immutable yang sama, token terikat sesi; stop tidak
membuka Activity. Klik kartu membuka Waktu Sholat. Tidak ada deleteIntent yang
menghentikan audio ketika banner digeser, overlay atau full-screen intent.

Pengaturan **Pengaturan banner adzan** membuka kategori adzan melalui Settings,
dengan petunjuk pop-up/mengambang dan keadaan izin/kategori diblokir yang berbeda.
Banner, durasi muncul dan tampilan layar kunci tetap ditentukan sistem/OEM dan
pengaturan pengguna. [Kanal](https://developer.android.com/develop/ui/compose/notifications/channels),
[layout custom terdekorasi](https://developer.android.com/develop/ui/views/notifications/custom-notification).

## Bukti

Receipt final mencatat source/hash/APK, jumlah unit/native/lint. Hanya putaran yang
seluruh fasenya lulus menjadi kandidat. Pengujian memakai emulator API37.1
terisolasi dengan kedua MP3 pengguna dari beta1.9.11, jaringan mati dan tanpa
keluaran audio host. Ini bukan uji kualitas suara terdengar atau HP fisik.

- AlarmManager exact waktu fixture +3detik, aplikasi di belakang/launcher:
  pemutar nyata, kartu sistem/banner dan aksi stop tanpa membuka aplikasi.
- Pemutaran otomatis tetap aktif setelah beberapa detik; buka panel sistem dan
  tekan stop. Ulang pada tema gelap dan font200%/lebar320dp. Preview LOW diperiksa
  lewat regresi pemutaran dua rekaman dan audio focus, tidak memaksa banner preview.
- Regresi mode/master/revisi/kota/deduplikasi, dua rekaman/hash/decoder, token stop
  lama, audio focus Quran WAV sintetis, jeda timer/tugas tetap belum selesai,
  senyap kelas, alarm layar noninteraktif, DND dan izin ditolak.
- Data fixture terpisah dari baseline emulator. Semua file awal dan pengaturan
  jaringan/font/ukuran/tema/DND/ringer/heads-up/izin dikembalikan, helper dihapus,
  emulator ditutup. Baseline10file0byte bukan pembuktian upgrade HP berisi.
- Audit nilai privat saat ini pada source/evidence dan APK, pemeriksaan dua MP3,
  media lama, WIP/stash/env/desain/APK lama. Binary audio tetap di luar Git;
  properti debug memasok aset, sourceSet release tidak membundel file privat.

## Percobaan dan batas

Build awal memakai metode action dari API compat pada builder native; kompilasi
menolaknya dan pemakaian tersebut dihapus. Native pertama belum menemukan tombol
di banner/kartu ringkas walau pemutaran serta regresi audio/alarm lulus. Layout
kemudian memakai konten ringkas bertombol dan konten diperluas terpisah; penangkapan
screenshot sistem dilakukan langsung agar tidak melewati masa banner singkat.
Hasil dan screenshot akhir harus dibaca bersama batas ini, bukan gambar konsep.

D02/D02a masih memerlukan HP untuk suara/getar/notifikasi mengambang vendor,
layar kunci nyata/reboot/Doze/perubahan jam-zona dan upgrade berisi, serta audit
lafaz dan asal/ketentuan redistribusi rekaman. D01/B01/C lain/D03/E/F/G tidak
otomatis ditutup oleh hasil banner ini.

## Cara membaca bukti visual

Empat kasus kontrol sistem memakai **screenshot Android asli yang ditinjau lalu
input tap pada koordinat tombol yang terlihat**, bukan memanggil PendingIntent
langsung. Instrumentasi kemudian memverifikasi MediaPlayer/service berhenti,
kartu hilang dan stop banner tidak membuka Activity.16 eksekusi lain otomatis.
Kasus UI menunggu status `ui_stop_ready` dan input host maksimal90detik; tidak
dapat dijalankan tanpa operator/input fixture yang sesuai.

Pada uji sentuh, pengaturan aksesibilitas emulator untuk waktu tindakan sementara
120.000ms agar banner bisa ditinjau sebelum disentuh, kemudian dikembalikan ke
nilai awal. Ini bukan durasi banner produksi. Log SystemUI putaran awal tanpa
pengaturan tersebut mencatat bound/pinned/visible lalu auto-remove sekitar5–6detik,
tetapi pembacaan node UiAutomation dan snapshot awal belum cukup membuktikan
posisi/tombol. Tidak menyebut uji aksesibilitas/TalkBack menyeluruh telah lulus.

Percobaan1–4 gagal pada pembacaan elemen banner/panel; render kartu dan log
heads-up akhirnya diperiksa langsung. Pergantian renderer emulator dan pembacaan
ulang cache tidak dianggap akar masalah yang pasti. Ada race antar-fixture saat
notifikasi sebelumnya masih diproses; reset fixture dipindah ke main looper.
Putaran5 lulus19 kasus dengan input sentuh. Kartu kemudian dirapikan agar satu
tombol pada banner. Putaran6–7 memuat input host yang melewati tenggat90detik
dan guard yang masih membaca notifikasi kasus sebelumnya. Uji khusus mematikan
notifikasi getar lewat pengaturan berhasil; guard kemudian dimulai dari keadaan
layar pengaturan yang sama. Plan/delivery juga diserialkan agar penjadwalan ulang
dan penerimaan tidak menimpa state bersama. Tidak menetapkan akar internal NMS
atau masalah pembacaan node sebagai kepastian. Putaran9 final **20 kasus lulus**.
Empat input memakai koordinat dari frame yang telah ditinjau, verifikasi region
aksi sudah dirender, lalu adb input tap; screenshot sebelum input disimpan dan
ditinjau. Tidak memanggil service/PendingIntent sebagai pengganti klik UI.

Frame final: [banner](native/native-banner-D02-banner-heads-up-100.png),
[panel terang](native/native-banner-D02-notification-shade-100.png),
[font200%](native/native-large-text-D02-notification-shade-200.png),
[panel gelap](native/native-dark-D02-notification-shade-100.png).
Waktu salat dalam kasus banner adalah event fixture (+3detik), bukan Maghrib/Subuh
astronomis yang benar-benar sedang masuk pada jam screenshot.
