# D06 — penyimpanan dan pembaruan berisi data

27 Oktober 2026. **Selesai teknis beta**, pada emulator API37.1; HP tidak disentuh.
Runtime tetap1.9.14/code41/source5288e03. Tidak ada perubahan skema/kode produksi
atau APK baru. [Receipt](release.json), [kasus](upgrade-cases.json),
[hasil native](native-results.json), [preservasi](preservation.json).

## Yang benar-benar diuji

Helper yang sama memakai refleksi terhadap **kelas pada APK lama yang terpasang**,
bukan membuat skema lama palsu memakai kode terbaru. Host memasang APK1.7.6/code24,
1.9.13/code40, atau1.9.14/code41 pada data emulator yang diisolasi; native kemudian
mengisi data sintetis melalui helper/penyimpanan versi tersebut. APK1.9.14 dipasang
sebagai pembaruan dengan `install -r`, tanpa menghapus aplikasi atau datanya.
Downgrade persiapan memakai `-r -d`, hanya pada fixture emulator bersigner debug sama.
Keberhasilan pemasangan ketiga APK membuktikan kompatibilitas signer yang dipakai;
bukan dukungan downgrade data pengguna atau jaminan signer rilis publik.

- Tugas Study lengkap/tidak lengkap dan target Hifz, catatan ibadah dua tanggal;
  metadata tenggat/prioritas/mata pelajaran bila tersedia pada versi lama.
- Nama, jenjang, bacaan Quran terakhir, tema, kota/koordinat/zona Makassar,
  pilihan qari/follow/repeat serta pilihan pengingat lama/empat mode modern.
- Bookmark Hadis bawaan dengan ID stabil; riwayat AI lama diimpor sekali,
  percakapan modern beserta judul/draf/isi bila sudah tersedia.
- Data jadwal kelas pada1.9.13/1.9.14, jurnal/rencana khatam/latihan/catatan
  pada1.9.14. Modul yang belum ada pada1.7.6 tidak diklaim sudah berisi.
- Gateway **dummy**, dienkripsi oleh AndroidKeyStore lama, masih dapat dibaca
  setelah upgrade dan process restart. Tidak ada key provider/token nyata.
- Dua berkas tahan lama cocok SHA256; file WAV sintetis bernama`.mp3` hanya
  sentinel unduhan, tidak diputar atau dianggap rekaman Quran.

Semua nilai kolom/baris lama dibandingkan sebagai multiset, bukan hanya jumlah.
Kolom/baris baru boleh ditambahkan oleh migrasi; nilai lama tidak boleh hilang.
Seluruh nilai preference pada empat grup fixture dibandingkan beserta tipenya.
Enam halaman dibuka dua kali per kasus (36 pembukaan total), lalu proses dihentikan
paksa dan dibuka kembali untuk pemeriksaan. Hanya `conversations.updated` boleh
berubah **setelah** pembukaan halaman, karena penyimpanan UI memperbarui waktu;
isi/draf/judul/ID tetap dibandingkan. Sebelum pembukaan, waktunya juga diperiksa.

11 eksekusi/5 metode native lulus; log di[native](native/). Helper dan lint lulus
[build](build.txt), lint produksi tetap0 error/780 warning menurut receipt D05.
Unit aplikasi tidak diulang untuk penambahan helper saja; hasil143 lulus/4 skip
terakhir milik[checkpoint runtime D05](../faith-d05-2026-10-27/README.md).

## Isolasi dan pengulangan

Test hanya mau berjalan bila host sudah membuat marker `files/d06-authorized`.
Sebelumnya host menghentikan proses, mencadangkan data internal dan eksternal
secara privat, memindahkan direktori asli keluar dari lokasi fixture, serta
merekam alias KeyStore. Jangan menjalankannya pada HP/data pengguna aktif.
Urutan metode: `testRecordKeystoreBaseline`, lalu untuk setiap APK lama
`testSeedPopulatedOldVersion` → pasang APK terbaru →
`testVerifyUpgradeAndRepeatedOpen` → force-stop → `testVerifyProcessRestart`;
terakhir `testRestoreFixtureKeystore` sebelum memulihkan data asli.

Backup/snapshot lengkap hanya di`.local/d06-20261027`, di luar Git; laporan
publik hanya hasil/nama sintetis/hashes APK. Baseline emulator10 berkas0byte
bukan bukti data pengguna terisi; **fixture terisi di atas adalah bukti upgrade**.
126 berkas eksternal dan seluruh internal asli dipulihkan cocok hash. Alias
Keystore asli dipertahankan; alias fixture baru saja dihapus bila dibuat pengujian.
Wi-Fi/data/stay-awake/izin notifikasi dipulihkan; helper dihapus, emulator ditutup.

Percobaan awal berhenti saat preflight: metadata paket versi41 ada tetapi
`codePath` APK tidak tersedia (`pkg=null`), sebelum isolasi/penulisan fixture.
Memasang ulang APK1.9.14 dengan `-r` mengembalikan paket tanpa menghapus data;
putaran lengkap berikutnya lulus. Bukan kegagalan migrasi aplikasi yang dibuktikan.

D06 lokal tidak menjanjikan backup/sinkronisasi antar-HP, pemulihan setelah
uninstall, pemulihan HP hilang atau downgrade. Akun/cloud tetap ditunda sesuai
RELEASE_SCOPE. Unduhan awal41.592.382byte (<100.000.000); APK debug, belum publik.
D01/D02/D02a dan gerbang sumber/audio/HP/G tetap terbuka.
