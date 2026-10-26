# D02/D02a — adzan lokal, beta privat 1.9.11

Implementasi 27 Oktober 2026. **D02/D02a tetap terbuka** untuk keluaran suara HP,
reboot nyata/pembatasan vendor dan asal/ketentuan distribusi rekaman. Ini bukti
terarah beta privat, bukan pengesahan rilis publik. Hasil final/hash/source ada di
[receipt](release.json); hanya putaran yang seluruh fasenya lulus menjadi kandidat.

Kandidat final: **1.9.11/code38**, source `f39bff2366661ad3a6317105a9200ae44c4a438c`,
APK `Zikra-Adzan-v1.9.11-beta-debug.apk` **41.355.968byte** (<100.000.000), SHA256
`fd582cfcda447c257e43191e2c18537d909d005d51104deff44b4a40b396f0b5`.
**134 unit lulus/4 skip, 15 eksekusi native/14 metode unik lulus, lint0 error/
778 warning**. Sertifikat debug sama dengan1.9.10; schema main5/chat1/weekly2.

## Keputusan audio terbaru

Pengguna mengganti pilihan Misyari dengan dua file yang diberikan sendiri:

| Waktu | Berkas pengguna | Durasi metadata | Ukuran | SHA-256 |
| --- | --- | --- | --- | --- |
| Subuh | fajr_128_44.mp3 | 187,272 detik | 2.996.349 byte | 106a7b2ca4e8f85fb44f7b150d42e7ca11237c961642ad877dd02e45f8add530 |
| Dzuhur, Ashar, Maghrib, Isya | mecca_56_22 (1).mp3 | 203,259 detik | 1.423.360 byte | 43ade766272ff9c89c86908959df6ddd7f3a34961897caf09f965a3841e1bcd2 |

File Mecca baru identik dengan mecca_56_22.mp3 terdahulu. Asli, salinan Misyari,
manifest lama dan stash audio tetap disimpan. Nama lokasi/jenis rekaman mengikuti
pilihan pengguna; identitas muazin, lafaz lengkap dan hak redistribusi belum
dibuktikan dari nama berkas atau metadata. Tidak mengadopsi lisensi kandidat
Commons untuk kedua file ini. Tidak mengunggah rekaman ke layanan lain.

Build debug mengambil salinan lokal melalui properti `zikraAdhanAudio` dan
`zikraFajrAudio`. Keduanya disalin ke aset debug yang dihasilkan Gradle; source
release tidak menyertakan audio privat. Binary dan manifest/path privat tidak
masuk Git. Build biasa tanpa properti tersebut menampilkan audio tidak tersedia,
tetap menyediakan notifikasi/getar dan tidak mencoba mengambil rekaman dari URL.
[Pemeriksaan aset dan media sebelumnya](media.json).

## Perilaku

- Sakelar utama dan empat mode per salat: adzan penuh, notifikasi biasa, getar,
  mati. Mode lama dimigrasikan ke notifikasi/mati; tidak otomatis mengaktifkan
  suara adzan. Pilihan tersimpan ketika master mati dan saat Activity dibuat ulang.
- Rencana lokal tujuh tanggal kota pilihan, tanpa alarm terbit. Perubahan kota,
  waktu/zona, izin, pembaruan aplikasi dan receiver boot menyusun ulang alarm.
  Tidak membutuhkan backend AI. Notifikasi maksimal terlambat 30 menit; audio
  maksimal 2 menit, tanpa pemutaran susulan dari peristiwa lama.
- Pemutar satu rekaman lokal memakai foreground service dan audio focus.
  Tombol berhenti pada halaman/notifikasi, sesi berhenti lama tidak menghentikan
  sesi baru; selesai/gagal/kehilangan focus melepas pemutar dan tidak auto-resume.
  Pemutar mengikuti ringer, Jangan Ganggu, izin notifikasi dan volume alarm.
- Opsi jeda fokus mem-pause timer Study yang sedang berjalan tanpa menyelesaikan
  tugas atau melanjutkannya sendiri. Opsi senyap selama kelas memakai jadwal lokal
  Study, menampilkan notifikasi tanpa suara/getar. Keduanya default mati.
- Kegagalan izin/start foreground service/audio memakai fallback notifikasi jika
  masih diizinkan. Master mati dan mode mati membatalkan alarm terkait; event
  kota/revisi lama, masa depan, tidak dikenal dan duplikat ditolak.

## Validasi terarah

[Unit](TEST-com.zikra.app.reminders.PrayerModesPlanTest.xml), [build/lint](build.txt),
[hasil native](native-results.json), [preservasi](preservation.json),
[audit nilai rahasia saat ini](audit.json). Receipt mencatat jumlah tepat.

Native memakai API 37.1 emulator, jaringan Wi-Fi/data dimatikan, data fixture
terisolasi. Pemutar benar-benar mendekode dua MP3 pilihan dan posisi maju;
emulator dijalankan tanpa keluaran audio host, jadi ini **bukan uji kualitas suara
yang terdengar**. Cakupan:

1. Migrasi pilihan lama, 30–35 alarm dalam tujuh tanggal, pembatalan master/per
   salat, rearm melalui pemanggilan receiver BOOT terkontrol (bukan reboot nyata).
2. Editor mode menyimpan/membatalkan, draft bertahan recreation, tombol tetap
   terjangkau pada font 200% dan lebar 320dp. Lihat [dialog](native/native-large-text-D02-mode-draft-200.png).
3. Receiver notifikasi/getar memakai kanal yang tepat tanpa memulai pemutar;
   event future/kota lama/mati/duplikat ditolak. Broadcast UI diuji dengan halaman
   aktif; alarm latar diuji terpisah lewat AlarmManager sebenarnya.
4. Dua rekaman cocok hash, decoder/durasi/posisi, tombol notifikasi berhenti dan
   token sesi lama; master/mode/kota berubah menghentikan pemutaran otomatis.
5. Timer fokus terlihat ikut berhenti dan tugas tetap belum selesai. Quran memakai
   WAV sintetis lokal 30 detik sebagai fixture audio focus: pause/resume sekitar
   adzan, audio focus lain menghentikan adzan tanpa resume.
6. AlarmManager exact asli dengan waktu fixture +4 detik memulai pemutar saat
   layar noninteraktif dan jaringan mati. Bukan bukti waktu salat astronomis nyata,
   Doze lama, baterai vendor atau suara di HP.
7. Izin notifikasi ditolak mempertahankan pilihan tanpa alarm/audio; DND none
   menahan audio dan mempertahankan kebijakan perangkat.

## Percobaan dan batas

- Build pertama menemukan dependency lint terhadap direktori aset generated;
  sourceSet sekarang memakai TaskProvider sehingga dependensi semua konsumen
  diketahui Gradle. Build/unit/lint berikutnya lulus.
- Putaran native pertama: dua kasus broadcast aplikasi tanpa halaman aktif
  tidak mengamati notifikasi dalam tenggat; kasus pemutar/alarm nyata/DND/font/
  izin lulus. Harness kemudian membuka halaman sebelum broadcast UI; tidak
  menghapus validasi receiver atau mengganti alarm nyata dengan panggilan langsung.
  Ini penyesuaian kondisi pengujian, bukan bukti pasti penyebab internal Android.
- Putaran kedua menyisakan satu tes pergantian notifikasi→getar. Harness semula
  memulai fixture berikutnya segera setelah notifikasi terlihat, sebelum receiver
  sebelumnya selesai menyusun ulang plan. Harness kini menunggu main looper idle
  dan memeriksa validitas event sebelum broadcast; implementasi alarm tidak
  dilonggarkan untuk meloloskan fixture.
- Baseline emulator 10 file berukuran nol dipulihkan cocok hash. Ini menjaga
  baseline yang tersedia, **bukan pembuktian upgrade data HP yang berisi**.
  Pengaturan jaringan/font/ukuran/tema/izin/DND/ringer dikembalikan, helper dihapus,
  emulator ditutup. HP fisik tidak diubah, provider AI tidak dipanggil.
- WIP PROJECT_HANDOFF, stash/arsip audio, env privat, desain Home/AI dan APK lama
  dipertahankan; audit hanya nilai privat saat ini dalam source/evidence dan APK,
  bukan sertifikasi seluruh sejarah Git atau seluruh keamanan produk.

Android dapat membatasi alarm dan foreground service. Exact alarm mempunyai
pengecualian tertentu untuk start layanan latar; izin atau pembatasan yang gagal
tetap memerlukan fallback. [Dokumentasi alarm](https://developer.android.com/develop/background-work/services/alarms),
[batas start FGS](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start),
[tipe mediaPlayback](https://developer.android.com/develop/background-work/services/fgs/service-types#media),
[audio focus](https://developer.android.com/media/optimize/audio-focus).

Sebelum menutup D02/D02a: dengarkan kedua rekaman penuh/lafaz Subuh, buktikan asal
dan ketentuan distribusi; uji HP nyata untuk suara/getar/senyap/channel, reboot,
perubahan jam/zona, lama layar mati/Doze/vendor serta pembaruan berisi data.
D01/C02/C04/C07/B01 dan rilis publik tidak otomatis selesai oleh hasil ini.
