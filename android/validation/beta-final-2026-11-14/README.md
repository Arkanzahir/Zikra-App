# Kandidat internal 1.9.47 — 14 November 2026

Code74;49.511.880 byte; SHA256
`03f9b61fc7ad88ad9049683a138dc3ae77b92982c3062a6d0fd152b884a07670`.
APK lokal `D:/Download/Zikra-App-main/Zikra-Polish-v1.9.47-beta-debug.apk`.
Tetap debug/HOLD, bukan paket publik. Ini checkpoint terbaru setelah167b0fb.

## Perubahan produksi

`PrayerRescheduleReceiver` kini hanya menjalankan pemulihan untuk lima action
sistem yang dideklarasikan pada manifest; null, action kosong, penggantian
paket lain dan action tidak dikenal tidak menyentuh penjadwalan. Receiver tetap
non-exported. Peringatan lint ini bukan bukti eksploitasi pihak luar sebelumnya.
Tidak mengubah jadwal, audio, desain, provider, hak konten atau data pengguna.

## Bukti pada APK yang sama

| Pemeriksaan | Hasil | Cakupan |
|---|---|---|
| JVM |202 lulus,4 skip,0 gagal/error|206 total; dua tes allowlist receiver baru|
| lintDebug |0 error,890 warning|Sebelumnya891; warning lain belum dituntaskan|
| Native inti |11 lulus|Upgrade data,receiver,hostedQuran4alur,fullfixtureQuran4tes,font1,tema1,onboarding2; rincian log|
| Native UI/cuaca |15 lulus|WeatherGlass4,UI follow-up5,darurat2,cuaca live1,teks200%1,smoke16layar1,izin ditolak1|
| Native pengingat |1 lulus|Jadwal/cancel,waktu,dedupe,cooldown,revision/lokasi lama,hari berikutnya,opt-out|
| Audit APK |Tanda tangan valid;privat0/secret dikenal0|Debug;release signer belum tersedia;audio/signer sama|
| Website |Desktop dan390CSSpx lulus|Gambar termuat,FAQ,privasi,tidak overflow,download disabled;belum publik|

Native total27 kasus pada1.9.47, bukan penjumlahan retest gagal atau hasil versi
lama. Backend tidak berubah;125tes backend lulus pada sesi sebelumnya, tidak
diklaim dijalankan ulang di sini. Semua build memakai dua audio pilihan lokal.

### Detail dan batas penting

- Instal1.9.41,isi profil/tugas/Hifz/chat+draf/bookmark/tema sintetis;install-r
 1.9.46 lalu1.9.47 tanpa uninstall atau clear data di antara upgrade. Kedua
 verifikasi berhasil. Hash APK awal:
 `29ad8fb40f6449efe1678df949665075a4a54394a06bb44187b21e807e261321`.
 Reset data hanya saat mempersiapkan emulator disposable sebelum seed1.9.41.
 Ini membuktikan rantai debug dengan signer sama; bukan migrasi debug→release.
- Hosted dari Android asli memakai Keystore dan identitas instalasi sintetis:
 reader1/7ayat,download generasi,halamanMushaf1,locate2:1→2,token salah ditolak,
 pulih dengan token benar sertaMushafRepository/font CDN asli berhasil. Refresh
 offline di store diuji memakaiIOException terkontrol; bukan seluruh kasus radio
 mati/proses dibunuh atau semua604font/qari/audio/timing.
- Fixture604halaman/6236ayat dipush privat,tidak masuk APK/Git. Parser/word mapping,
 staging/publish/failure/recovery dan binding reader diuji terpisah.
- Cuaca live default kota publik Sukolilo/Surabaya berhasil dari Open-Meteo dan
 request berikutnya memakai cache fresh. Scene/cuaca gagal memakai fixture
 berlabel; hasil itu tidak dianggap prakiraan nyata. Job latar belakang cuaca
 dalam Doze/HP belum dibuktikan. Status smoke menyimpan sumber/fetched_at.
- Dialer/SMS/share/Maps dicegat ActivityMonitor. Verifikasi nomor,body draf,
 URLdestination,web→geo→dialog salin dan source resmi; tidak menelepon,mengirim,
 membuka rute eksternal sungguhan atau menulis clipboard otomatis.
- Keyboard AI nyata light/dark,draf lintas dock,teks200%,lampu siang/malam,
 cold/warm launch/reduced motion dan pause/resume lulus. Smoke16tampilan fokus
 dan screenshot berhasil; bukan persetujuan visual semua piksel/semua layar.
- Pengingat diuji izin ditolak/diberi,beda lokasi/revision,day journal/cooldown,
 defaultoff/opt-out. Tidak memalsukan tanggal HP; transisi hari jurnal terkontrol
 dan unit zona waktu melengkapi bukti. Belum membuktikan pengiriman fisik semuaOS.

## Kegagalan awal yang ditangani

1. Folder fixture belum dibuat; persiapan diperbaiki sebelum retest.
2. Android37 memblokir jaringan UID instrumentasi tanpa Activity foreground;
 shell DNS berhasil tetapi app mendapatEAI_NODATA/APP_BACKGROUND. Test kini
 menunggu fokus layar lokal, tanpa mengubah DNS,TLS,firewall atau kebijakanOS.
3. Test reduced motion lama mengandalkan harness luar mengatur animation0.
 Test sekarang mengatur/memulihkan nilainya sendiri di emulator berizin.
4. Test font200% masih mengharapkan judul lama “Pendamping Zikra”; diperbarui
 ke resource desain diterima “Zikra AI”. Assertion ukuran/sentuhan tetap.

Log awal privat disimpan untuk audit; hasil akhirnya dicatat tanpa menyembunyikan
gagal awal. Compile typo test fieldstored dibetulkan sebelum pengujian akhir.

## Keselamatan dan pembersihan

Hanya emulator `ZikraNativeHosted`,port5582,ADB5042,serial127.0.0.1:5583.
Tidak memakai HP. Dua identitas sintetis server diketahui,TTL900detik danDEL
khusus key identitas/rate miliknya,EXISTS0 terverifikasi. Credential emulator
dihapus olehfinally;tidak mencetak isi atau mengubah user registry lainnya.
Ketidakpastian identitas dari run historis tetap tercatat di handoff.

Izin notif emulator dicabut kembali;pengingat/job/notif dibersihkan. Skala font/
animator dikembalikan;viewport browser direset. Proses/data emulator task dan
server preview dihentikan setelah bukti disalin. WIP,stash,privat,logo/foreground,
dua audio dan signer debug sesuai baseline. Tidak deploy produksi atau inference.

## Gerbang yang masih terbuka

J02/J03:resourcewithdrawal/prosesmati/storage nyata serta seluruh font/audio/
timing dan kebijakan offline terpisah. J08/J09/F/G masih menyisakan perangkat,
receiverOS/aplikasi eksternal dan audit layar/performa lengkap. J04–J06 hak,
B01/dua kitab,Gading,audio/lafaz;J11 penerbit/retensi/kebijakan;J12 signer dan
migrasi release;J13 HP;J14putusan;J15unduhHTTPS/pemasangan dari kanal nyata.
Website siap pratinjau lokal; tidak dianggap rilis publik selesai.
