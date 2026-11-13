# Ketahanan Quran — 14 November 2026

Kandidat tetap **1.9.47/code74**,49.511.880 byte,SHA256
`03f9b61fc7ad88ad9049683a138dc3ae77b92982c3062a6d0fd152b884a07670`.
Tidak ada perubahan produksi atau APK baru. Lima kasus tambahan lulus pada
binary yang sama; bersama27kasus sebelumnya menjadi32kasus native berbeda.
Persiapan yang sengaja dihentikan **tidak** dihitung sebagai test lulus.

| Kasus | Hasil yang diamati |
|---|---|
| Resource dicabut | Kedua kode503 yang relevan mengaktifkan status withdrawn; reader dan Mushaf menolak menampilkan cache, tetapi file valid/bookmark tetap utuh |
| Pulih/gagal biasa |200malformed tidak membuka kembali sumber;200valid reader/Mushaf memulihkannya.401/429/500/503generik tidak disamakan dengan penarikan sumber |
| Gagal tulis halaman | FileOutputStream benar-benar gagal pada halaman2 akibat path direktori; manifest lama dan halaman valid tetap identik |
| Ruang rendah | Probe5MiB terkontrol menghentikan pekerjaan sebelum request sumber/staging baru; manifest dan data lama tetap identik |
| Proses mati saat staging | Proses aplikasi benar-benar force-stop setelah halaman1 refresh ditulis; PID lama hilang. Proses baru membaca generasi committed, membersihkan orphan miliknya, dan mempertahankan bookmark/file pengguna sintetis |

Empat kasus pertama: `native-withdrawal-storage-final.txt` →4lulus.
Kasus pemulihan: `recovery.txt` →1lulus; `proof.json` mencatat penghentian aktual
dan hasil pemulihan. Bukan sekadar exception atau membuat orphan secara manual.

## Batas bukti

- Penarikan sumber memakai interceptor OkHttp dengan respons terkontrol, tanpa
  mencabut akses publisher/server produksi. Token sintetis tidak keluar emulator.
- Ruang rendah memakai override probe File.getUsableSpace pada test saja; disk
  laptop/HP tidak dipenuhi. Gagal tulis nyata adalah EISDIR, bukan ENOSPC.
- Force-stop terjadi saat staging halaman, sebelum commit. Ini bukan simulasi
  power-loss perangkat atau semua titik kegagalan AtomicFile/filesystem.
- Fixture Quran dipush privat, tidak masuk testAPK/Git. Bukan uji baru seluruh
  audio/font/qari/timing atau delivery alarm fisik. Tidak memakai HP/AI/layanan
  produksi. Hasil JVM202lulus/4skip dan lint0error/890warning adalah run sebelumnya;
  tidak dijalankan ulang karena delta hanya test/harness/dokumentasi.

## Cara mengulang dengan aman

1. Buat emulator disposable baru; pasang APK1.9.47 dan testAPK hasil source ini.
   Pakai ADBterisolasi5042,serial loopback127.0.0.1:5583. Jangan pakai HP pengguna.
2. Push reader-pages dan mushaf-pages privat ke app files/quran-failure-fixtures
   dan files/quran-sync-fixtures. Tambahkan marker
   files/quran-failure-emulator-authorized pada emulator milik tugas saja.
3. Jalankan QuranWithdrawalTest, serta dua metode baru QuranSyncIntegrationTest:
   testMidPageFilesystemWriteFailureKeepsCommittedGeneration dan
   testLowSpaceGuardDoesNotFetchOrReplaceCommittedData. Jangan rerun seluruh kelas
   yang memiliki persyaratan fresh store tanpa membaca setup-nya.
4. Untuk kill/recovery, pastikan files/quran-process-owned belum ada, lalu jalankan:

```powershell
./tools/verify_quran_process_recovery.ps1 -OutputDirectory .local/quran-crash-new-evidence
```

Script memeriksa marker/model/serial, menunggu ready marker dengan PID yang
sesuai, force-stop hanya paket uji emulator, memeriksa PID hilang, kemudian
menjalankan recovery pada proses baru. Output harus ke direktori baru.
Marker lama ditolak; serial bukan loopback ditolak. Jangan menghapus data
pengguna untuk memenuhi syarat fresh test. Guard negatif juga telah dijalankan.

Metode prepare sengaja menghasilkan `Process crashed` ketika dihentikan; itu
bukan bukti pemulihan lulus. Hanya `OK (1 test)` di recovery beserta proof yang
menutup kasus. Versi awal helper PowerShell salah mengikat argumen-w; binding
dibetulkan dan harness lengkap diuji kembali. Percobaan awal tidak dihitung lulus.

## Pembersihan dan kelanjutan

Emulator milik tugas ZikraQuranFailure,port5582,dataE:/Zikra-QuranFailure-20261114
dan serverADB5042 dihentikan/dibersihkan setelah bukti disalin. Hanya sandbox
sintetis yang direset saat menguji ulang helper. WIP PROJECT_HANDOFF,stash audio,
konfigurasi privat,logo/aset,APK dan data HP tidak diubah.

J02 membaik sesuai lima skenario ini; parent belum otomatis selesai. Lanjutkan
font/audio/timing dan matriks perangkat. ENOSPCfisik,power-loss,signing,hak/B01,
privasi serta J13/J14/J15 tetap terbuka sesuai handoff.
