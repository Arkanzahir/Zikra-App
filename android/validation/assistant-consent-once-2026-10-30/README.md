# C03a — izin sekali, beta1.9.31

30 Oktober2026. Pengguna meminta izin di awal sekali, lalu pesan langsung
dikirim seperti alur chat biasa. Sebelumnya setiap request membuka tinjauan.
Perubahan ini diprioritaskan ketika sesi C07 sedang berlangsung; **tidak menutup
C02/C07/D02/D02a/E atau rilis publik**.

## Perilaku

- Kiriman online pertama meminta persetujuan pesan dan lampiran yang dipilih.
  Batal tidak mengirim dan tidak menyimpan izin. Setelah Setuju, kiriman biasa
  berikutnya, lampiran pilihan, dan retry langsung memakai request tanpa data
  tambahan. Izin tersimpan lokal dan terikat endpoint; endpoint berbeda meminta
  izin baru. Tidak ada provider key atau token baru di APK.
- Profil/tugas/salat/history/proposal/kelas tidak otomatis disertakan. **Alat →
  Kirim dengan konteks** membuka tinjauan opsional, enam pilihan defaultfalse
  dan snapshot per kiriman seperti sebelumnya. Pesan biasa tidak memanggilnya.
- **Alat → Izin data AI** atau menu mode menyediakan Cabut izin. Pengiriman aktif
  dibatalkan; kiriman selanjutnya meminta izin lagi. Tidak mengubah mode jaringan
  yang dipilih atau konfigurasi gateway. Persetujuan generasi gambar/dikte/izin
  sistem tetap alur tersendiri; bukan penghapusan semua izin Android.

## Pengujian dan APK

[Ringkasan](checks.json):163 unit lulus/4 skip,lint0 error/862 warning. [Build
aplikasi](build.txt) dan [kompilasi helper lengkap](helper-build.txt) lulus.
Tes konteks lama disesuaikan memilih jalur review opsional secara eksplisit;
kompilasi helper bukan klaim semuanya dieksekusi ulang.

Satu metode native HP API36 dieksekusi sukses tiga kali: versi awal tiga request,
versi akhir empat request termasuk lampiran, dan pemeriksaan ulang sesudah
perbaikan mode. [Hasil akhir](native-final.txt), [hasil ulang](native-online-recheck.txt),
[proof](native-proof.json). HTTP interceptor sintetis, **bukan inference provider
nyata**. Diuji: tolak pertama tanpa upload; setuju sekali; pesan kedua langsung;
lampiran langsung; buka ulang Activity mempertahankan izin; endpoint lain tidak
meminjam izin; tinjauan konteks hanya ketika dipilih; default enam pilihanfalse;
cabut izin meminta persetujuan kembali. Request biasa memiliki context kosong.
Tombol persetujuan terlihat seluruh tingginya di HP yang diuji; bukan audit
seluruh ukuran/font/tema. [Izin pertama](consent-first.jpg), [kiriman langsung](consent-direct.jpg).

APK folder pembungkus `Zikra-AI-v1.9.31-beta-debug.apk`,45.418.699byte,
code58, SHA256`77f91b1d3846a194725308815940772573eb69401562894c6c75b3cc708aea4b`.
[Audit APK](apk.json), [signer](signature.txt): <100MB,sertifikat pembaruan sama,
0 nilai credential privat dikenal di entri APK dan kedua audio cocok hash.
HP aktual1.9.29→1.9.30→1.9.31, tanpa uninstall aplikasi utama. Backend tidak
diubah/dideploy dan tidak ada konfigurasi provider/token/paket baru.

## Temuan sesi, koreksi dan preservasi

[Audit awal](preservation-initial-finding.json) **gagal** karena flag gateway
`online` berubah menjadifalse; mode routing juga berada diOffline. Asal perubahan
tidak ditetapkan. Pengguna menjawab tidak memilih Offline. Upaya menandai temuan
sebagai aman ditolak pemeriksaan otomatis; laporan asli dipertahankan dan tidak
ditulis ulang menjadi lulus.

Dua nilai mode kemudian dipulihkan secara terarah ke Online yang sebelumnya
terlihat saat percobaan PDF berhasil. Sebelum menulis ada backup dan guard byte
terkini; tidak mengembalikan seluruh preferences/DB. [Bukti perbaikan](mode-repair.json).
Uji izin diulang; [Online tetap aktif](mode-recheck.json), tanpa perbaikan kedua.

[Verifikasi akhir baru](final-data-check.json) membandingkan snapshot akhir
sesudah perbaikan dengan33 berkas/17 tabel baseline serta snapshot percobaan PDF
pengguna: isi tabel lama dan chat PDF terpelihara, tidak ada berkas awal hilang
atau preference lama tak terjelaskan yang masih berubah, setting/izin/baterai
tetap. Timestamp akses chat boleh berubah. Pemilih chat aktif terbaru dipertahankan;
rencana alarm turunan dibedakan. Audit awal gagal tetap terpisah dari hasil akhir.
Helper/marker/dua screenshot uji dan empat fixture publik yang cocok hash
dibersihkan. WIP PROJECT_HANDOFF, stash audio, env, ikon/artwork dan pilihan audio
tetap; backup/log mentah dan layar yang memuat data pribadi tetap privat.

Pembukaan AiActivity melalui shell sempat ditolak karena Activity tidak
exported; launcher resmi SplashActivity berhasil digunakan tanpa mengubah manifest.

## Batas C07 dan pekerjaan semula

Percobaan pemilih otomatis sebelumnya dihentikan saat bertabrakan dengan
penggunaan HP; percobaan berikutnya timeout sebelum DOCX contoh terpilih. Keduanya
**bukan hasil lulus**; helper eksperimen disimpan privat dan tidak dimasukkan
checkpoint. Pengguna sendiri memilih PDF: dokumen lebih dari3halaman ditolak,
PDF dua halaman berikutnya tampil dan balasan Online teramati. Ini bukti manual
terarah, bukan jaminan akurasi isi atau penutupan semua picker/grant/OCR C07.

C02 laptop mati masih perlu tindakan pengguna. D02/D02a tetap memerlukan sisa
uji vendor/Doze/reboot/interaksi dan ketentuan redistribusi audio; halaman penerbit
[masih menyatakan unduhan gratis](https://islamdownload.net/123801-download-suara-adzan.html),
belum menjadi bukti izin bundling aplikasi. Tidak mengirim pesan ke pihak lain.
B01 tetap menunggu sumber; UI10 sorotan ditunda, E belum dimulai.

Source runtime/helper `fd098bff92c4c80922b12d0668426e7bbb629e96`; [receipt/source hash](receipt.json).

Audit commit menemukan satu pola credential pada fixture penolakan key sintetis
yang sudah ada di origin/main. Temuan otomatis tetap disimpan; [review khusus
berdasarkan hash/konteks](history-audit-review.json) membedakannya dari credential
nyata. Tidak ada pengecualian umum pada scanner atau penghapusan temuannya.
