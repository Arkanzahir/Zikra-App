# Pemulihan indeks Quran — 14 November 2026

Kandidat internal **1.9.49/code76**,49.537.679byte,SHA256
`f01398824c5c9463b64cffae171bc6e35ea714a7869020830790ac6d4de410ef`.
APK lokal `D:/Download/Zikra-App-main/Zikra-Polish-v1.9.49-beta-debug.apk`.
Debug/HOLD. HP masih1.9.48; USB tidak tersedia saat batch ini. Website ditunda
atas permintaan pengguna, pratinjau tetap1.9.48; tidak membuka unduhan/publikasi.

## Cacat yang direproduksi dan diperbaiki

Pada1.9.48, tiga dari empat kasus awal menghasilkan error yang diharapkan sebagai
bukti cacat (`before-fix-native.txt`): JSON pointer rusak tidak dapat diperbaiki
dengan unduhan valid, schema/future timestamp tidak sah memblokir refresh, dan
pointer previous yang keliru dapat menghapus generasi committed surah lain.
Kasus penolakan source lebih lama sudah lulus sebelum perbaikan dan tetap lulus.

`QuranSyncStore` kini memvalidasi manifest serta mencocokkan waktu sumber dengan
halaman pertamanya sebelum mempercayai pointer lama. Manifest tidak valid tidak
dipakai untuk cleanup/penolakan versi. Pengganti hanya dipublikasikan setelah
semua halaman baru valid dan tersimpan. Pembersihan generasi lama memeriksa
referensi current/previous seluruh pointer; ketidakpastian mempertahankan file.
Pembatalan diperiksa ulang di dalam lock tepat sebelum write pointer.
Tidak mengubah teks/terjemahan, qari, rekaman, desain, provider atau hak sumber.

## Hasil pada biner final yang sama

| Pemeriksaan | Hasil | Rincian |
|---|---|---|
| JVM |207lulus/0skip/0gagal|Fixture privat tersedia, bukan teks/rekaman di Git/APK|
| Lint |0error/890warning|Warning lain tetap belum seluruhnya dituntaskan|
| Pemulihan manifest |7native lulus|Rusak/malformed,invalidschema,oversize,waktusumbertidakcocok,validnewer,crosschapter,cancel/network,writefailure dalam7metode|
| Regresi store/peta |5native lulus|4store:atomic/cancel/sourcechange/corruptpage/midwrite/lowspace;1petaexpiry|
| Withdrawal |2native lulus|Sumber dicabut memblokir cache reader/Mushaf tanpa menghapus data;outage/recovery dibedakan|
| Proses mati |1native recovery lulus|PID teramati,force-stop aktual,PID lama hilang,proses baru memulihkan commit dan membersihkan orphan miliknya|
| Upgrade terisi |1native verifikasi lulus|Seed pada1.9.41,laluinstall-r1.9.48→1.9.49;profil/tugas/Hifz/chat+draf/bacaan/tema tetap|
| Audit APK |Privat0/credential dikenal0|Signer debug valid/sama;audio pilihan berhash sama;backup/cleartext nonaktif|

Total **16metode native berbeda** pada1.9.49. Satu seed pada1.9.41 dan prepare
yang sengaja dihentikan bukan tambahan metode lulus kandidat baru. Backend
tidak berubah;134tes terakhir pada checkpoint4420cc7 tidak dijalankan ulang.
`checks.json`, audit dan log terarah ada di direktori ini. Raw fixture/log build
privat di `.local/manifest-recovery-20261114/`; tidak di Git/APK.

## Batas dan keselamatan

- Emulator baru ZikraManifestQA,loopback5583,ADB5042,API37.1,720×1560.
  RAM diminta1536MB tetapi image menaikkan ke4096MB. Tidak menutup aplikasi/game.
  Run pertama dihentikan Android LOW_MEMORY saat proses tidak foreground;
  tidak dihitung sebagai bug/regresi lulus. Harness diberi aktivitas foreground,
  kemudian empat kasus baseline berhasil mereproduksi tiga error tersebut.
- Semua operasi korupsi/penghapusan pada context scratch UUID dengan fixture
  privat dan marker host. Tidak pernah pada direktori Quran/data HP.
  Tidak ada audio, restart HP atau perubahan izin/daya pengguna.
- Pointer rusak tidak langsung membuat data dipercaya: unduhan tidak lengkap
  tetap gagal dan file lama tetap utuh. Timestamp valid yang lebih baru tetap
  menolak source lebih lama. File generasi/unknown milik pengguna tidak dihapus
  secara massal untuk meluluskan tes.
- Kegagalan write nyata memakai path direktori(EISDIR), lowspace guard memakai
  probe terkontrol; bukan memenuhi disk fisik. Kill aktual saat staging bukan
  power-loss perangkat atau pengujian setiap titik kegagalan filesystem.
- Snapshot/withdrawal, fixture mapping dan decoder/font/audio sebelumnya tetap
  bukti sesuai versinya; tidak menjumlahkan hasil1.9.47/48 sebagai hasil1.9.49.
  Belum tes HP1.9.49,seluruh604font/semua timing/lafaz atau background semuaOS.

J02 kriteria teknis kini terpenuhi dengan batas di atas; J03 baru pemetaan
teknis yang dicentang. Hak sumber/rekaman/B01,privasi remaja/retensi,signer dan
migrasi,J13/J14 tetap terpisah. Website tetap ditunda. Pengguna memilih migrasi
lewat cadangan lokal; implementasi/enkripsi/restore lintas signer belum selesai
pada checkpoint ini. F06backupcloud/sync/admin tetap ditunda.

Emulator task dan data sementara sudah dibersihkan setelah validasi path/marker.
Hash7konfigurasi privat, WIP handoff, arsip audio, logo/art dan stash tetap.
Website tidak diubah/diuji ulang; checker versi preview menunggu penyelarasan
saat pekerjaan website dilanjutkan, karena preview48 dibekukan dan kandidat49.
