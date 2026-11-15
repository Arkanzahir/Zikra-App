# Pengingat harian — perbaikan terarah, 15 November 2026

Status: source internal 1.9.56, HOLD; bukan pembaruan HP atau rilis publik.

## Perubahan

Halaman memakai kelompok rutinitas/cuaca/ringkasan/akses cepat, baris waktu dan navigasi dengan target sentuh minimum56dp, warna tema bersama dan dock global. Judul duplikat serta tumpukan tombol hijau penuh dihapus. Detail waktu/retensi notifikasi/cuaca tetap dapat dibaca melalui Cara kerja pengingat; ringkasan memakai data yang sama, bukan contoh statis. Pintasan Pengingat salat membuka PrayerActivity agar pengingat harian tidak disamakan dengan adzan.

Penjadwalan, izin, opt-in dan data tersimpan tidak diubah. Dialog persetujuan lokasi Open-Meteo tetap dipertahankan. Tidak ada perubahan server.

## Verifikasi

Build debug public-scope berhasil. Pemeriksaan fungsional dan gambar emulator API36 berada di receipt dan PNG pada folder ini. Sembilan pemeriksaan fungsional (3fungsi ×3kondisi: terang1×, gelap1×, terang2×) lulus. Gambar ditinjau langsung; font200% tetap memerlukan gulir dan header membungkus. Ini bukan pengukuran seluruh batas kontrol/TalkBack atau semua halaman. Pemeriksaan awal alat uji salah membaca penanda activity API36 dan gulir melewati kontrol; diperbaiki pada harness lalu run akhir lulus. APK44.864.650byte, SHA256`4143f04f7524011a064dc8af938664132c7bfcdc859116ad9ede93fc3b89167a`, tanpa MP3. Pemeriksaan terarah ini tidak menggantikan suite akun pada checkpoint fed6d50; APK memiliki konfigurasi berbeda (account sign-in/emulator defaultfalse). Tidak menggunakan HP, tidak mengaktifkan pengingat pengguna, tidak memutar audio.

## Masih terbuka

HP tetap release1.9.55. Rekaman adzan tidak dibundel pada cakupan publik, sehingga mode lama ADHAN diturunkan menjadi notifikasi. Ini menjelaskan hilangnya rekaman, tetapi tidak membuktikan notifikasi/izin/alarm HP saat ini sudah benar. Solusi berikutnya yang direncanakan: pemilihan MP3 lokal pengguna, validasi, simpan privat dan fallback aman. Belum diimplementasikan; jangan mengklaim audio pulih.

Pengguna membatalkan review usia manual. Alur otomatis/izin wali digital belum terintegrasi; tanggal lahir yang diisi dan email terverifikasi tidak membuktikan identitas/usia/relasi. Tidak membuat queue pengelola atau membuka akses AI publik otomatis. HOLD tetap.
