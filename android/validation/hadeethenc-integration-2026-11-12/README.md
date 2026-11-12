# HadeethEnc opsional — kandidat 1.9.41, 12 November 2026

Pengguna meminta melanjutkan setelah [audit kandidat](../hadeethenc-source-2026-11-12/README.md).
Integrasi menambah ensiklopedia pilihan, **tanpa menghapus koleksi/data lama atau
mengubah target Bukhari/Muslim lengkap**. B01 induk dan gerbang beta/publik tetap
terbuka; ini bukan perubahan cakupan A02 atau tashih seluruh koleksi.

## Perilaku yang diterapkan

- Tab **Pilihan**, opsi unduhan 2.260 entri/10,7 MB, pencarian dan paging lokal.
  Tidak ada unduhan otomatis saat membuka halaman. URL CSV resmi berversi1.22.0,
  ukuran dan SHA dipatok; TLS bawaan, tanpa redirect/retry otomatis, dengan
  deadline dan batas respons. HTML/error atau perubahan sumber gagal verifikasi.
- Parser RFC4180 streaming mempertahankan seluruh15 kolom, metadata asli,
  kutip ganda dan baris baru. Header versi/bahasa/tautan, ID unik, jumlah dan
  rentang diverifikasi; UTF-8 rusak, field berlebihan atau dokumen terpotong ditolak.
  Berkas CSV asli disimpan privat, bukan hanya isi utamanya. Tidak menambah teks
  atau penilaian agama dari AI.
- Model terpisah `hadeethenc:id:*`, tanpa mengubah `tafsirq-v1:*`/ID kutipan lama.
  SQLitev1→v2 menambah kolom nullable, tanpa drop tabel. Impor satu transaksi
  mempertahankan sumber lama ketika gagal/batal. Arsip dipasang atomik dengan
  penguncian antar-store; arsip tanpa paket aktif dibersihkan ketika store dibuka.
  Hapus paket mempertahankan penanda yang menunggu unduhan ulang.
- Reader menampilkan atribusi, versi, penilaian sumber, uraian/faedah yang tersedia,
  naskah lengkap seluruh kolom/metadata, salin/bagikan dan tautan sumber. ID
  ensiklopedia tidak disebut nomor kitab. Bibliografi rinci tersedia lewat situs
  penerbit; **belum disimpan sebagai bibliografi luring tambahan** karena tidak
  ada dalam ekspor Indonesia ini.
- **Periksa versi** membuka halaman resmi. Ini pemeriksaan manual oleh pengguna,
  bukan pembaruan otomatis atau klaim data selalu mutakhir. Perubahan pin paket
  berikutnya harus diaudit terlebih dahulu; jangan memperbarui hash saja tanpa
  memeriksa isi/metadata dan mempertahankan ID/bookmark.

## Pembuktian

[Ringkasan](checks.json) dan [paket](apk.json) terikat APK final yang sama.

- [x] Parser dan round-trip snapshot resmi penuh, termasuk metadata.
- [x] Migrasi aditif, rollback, bookmark dan unduhan/pasang ulang di emulator.
- [x] Reader terang/gelap serta kontrol teks 200% pada skenario terarah.
- [x] Audit paket, hash audio dan preservasi WIP/konfigurasi privat.
- [ ] Keputusan sumber/cakupan B01 dan kelayakan distribusi beta/publik.

- Unit198 total: **194 lulus/4 skip**, tanpa gagal/error. Lima regresi baru
  mencakup kutip/baris baru/BOM, versi/link/bahasa palsu, ID duplikat, potongan/
  field berlebihan, pembatalan sink serta ekspor resmi penuh. Snapshot asli
  privat dipasok eksplisit lewat `ZIKRA_HADEETHENC_FIXTURE`; seluruh2.260 record
  dan15 kolom beserta metadata round-trip tanpa perubahan. Jika snapshot tidak
  dipasok pada lingkungan lain, tes ekspor resmi dilewati secara eksplisit.
- Build debug/helper dan lint berhasil; **0 error,878 warning**. Backend tidak
  berubah dan tidak diuji ulang tanpa alasan. Tidak menyatakan A–D/G selesai.
- Native final: dua metode pada ukuran normal, lalu satu metode UI pada320dp/
  font200%, terang dan gelap. Uji meliputi databasev1 berisi, unduhan HTTPS
  sesungguhnya dengan pin,2260 entri/paging/cari angka Arab/SQL-literal,
  impor gagal/rollback, pembatalan, buka ulang, bookmark sumber lama/baru,
  hapus/pasang ulang, reader/copy/versi serta layout.
- Pemeriksaan awal visibilitas dialog hanya mengecek sebagian area terlihat;
  screenshot font200% menemukan tombol footer terpotong. Pemeriksaan diperketat
  menjadi seluruh tinggi tombol terlihat dan label dock lengkap dalam selnya.
  Tombol versi dipindah ke area gulir; footer hanyaOK. Baseline alignment pada
  bar berbobot dinonaktifkan agar dock tidak mengukur label di luar sel.
  Tab memakai label utuh, susunan vertikal ketika teks besar; judul sumber
  panjang diperkecil tanpa memotong isi. Aksi reader juga ditumpuk pada teks
  besar agar Simpan/Bagikan tidak memecah kata; diuji seluruh tombol terlihat
  dan label satu baris.
- Percobaan penempatan fixture pada folder luar ditolak Android sebelum tes
  berjalan. Fixture dipindah ke files internal lewat `run-as` emulator, tanpa
  menambah izin aplikasi. Hasil teknis awal yang lulus tetapi belum lolos
  pemeriksaan visual tetap dicatat dalam riwayat lokal, bukan disebut bukti final.
- Satu build iterasi tidak menyertakan properti audio privat; audit menangkap
  ketiadaan kedua aset. Build berikutnya memakai salinan privat dengan hash
  pilihan pengguna. Percobaan path relatif ditolak sebelum packaging; build
  final memakai path absolut dan tiga eksekusi native diulang pada APK final.

Log final normal/font200%, build dan XML unit tersimpan pada direktori bukti.
Screenshot asli dan log iterasi tetap privat di
`.local/hadeethenc-native-20261112/`; [daftar hash](screenshots.json) mencatat
hasil yang ditinjau. Tidak memasukkan korpus atau fixture asli ke Git/APK.

## Paket, preservasi dan batas

APK **1.9.41/code68**, debug bertanda tangan yang sama dengan baseline; lihat
[receipt](apk.json) untuk ukuran/SHA final. Salinan lokal untuk pengujian internal
`D:\Download\Zikra-App-main\Zikra-HadeethEnc-v1.9.41-beta-debug.apk`.
Unduhan awal <100.000.000byte; CSV10,7MB opsional tidak dibundel dalam APK.
Tidak menemukan nilai credential server yang dikenal atau konfigurasi privat;
identitas client Firebase tetap diharapkan. Dua hash adzan/signer tetap.

HP pengguna **tidak diakses atau diperbarui**, terakhir dibuktikan1.9.39. Bukti
migrasi memakai fixture databasev1 pada emulator baru, bukan seluruh data HP
nyata atau pengujian upgrade APK berisi seluruh fitur. WIP PROJECT_HANDOFF,
stash/arsip audio, env, logo dan artwork cocok hash sebelumnya. Konfigurasi
privat tidak dirotasi/diunggah. Tidak ada deploy, email, Drive, biaya atau
publikasi APK/website baru. Emulator/fixture dipulihkan/dibersihkan setelah uji.

Sumber lama masih kandidat yang belum mendapat kejelasan B01. Sebelum distribusi,
tentukan cakupan/sumber konten final secara eksplisit, tutup hak/lafaz adzan,
C02/sisa fisik D02, privasi/kontak/syarat layanan, signing/migrasi distribusi
dan gerbang E/F/G yang relevan. Jangan mengganti target dua kitab lengkap dengan
2.260 entri pilihan atau menganggap build debug sebagai paket siap publik.
