# F03a — Tema global Zikra, 29 Oktober 2026

Beta **1.9.23 / code 50**. Permintaan pengguna: pilihan terang/gelap konsisten di seluruh fitur. Implementasi subset ini selesai dan teruji di emulator; bukan penutupan seluruh F03 atau rilis publik.

## Hasil

- [x] Satu pilihan **Terang / Gelap / Ikuti perangkat**, dapat dibuka dari Home, Profil dan Mushaf; pilihan pembaca Quran memakai sumber yang sama.
- [x] Pilihan Home lama tetap berlaku. Penyimpanan memakai nama/key lama tanpa menghapus preferensi atau data pembaca.
- [x] Semua halaman produk turunan BaseActivity menerima konfigurasi sebelum layout diinflasi. Halaman di back stack dibuat ulang bila pilihan berubah; pilihan tersimpan setelah proses berhenti.
- [x] Palet malam mencakup permukaan, teks, kartu, input, kontrol, dialog dan navigasi. Warna merek/on-brand dibedakan dari warna permukaan/teks. Notifikasi tetap mengikuti tema sistem Android.
- [x] Quran/Mushaf tidak lagi memiliki tema halaman terpisah. Otomatis pada Quran sekarang mengikuti tema perangkat; sensor lux tidak membalik tema sendiri. Preferensi reader lama dipertahankan sebagai data legacy.
- [x] Keadaan sakelar Quran lama tidak dipulihkan di atas pilihan tema baru. Draf AI tetap tersimpan selama pergantian halaman/restart. Banner Doa mengikuti tinggi teks, termasuk font 200%.
- [x] Geometri delapan ikon fitur, logo utama, ilustrasi masjid, pilihan audio dan konfigurasi privat dipertahankan. HP tidak dipasang/diubah pada checkpoint ini.

## Bukti dan pemeriksaan

[Preview native](preview.jpg), [hasil native](native-results.json), [unit/lint](checks.json), [APK](apk.json), [preservasi](preservation.json).

**9 eksekusi native / 6 metode unik**: matriks 24 tampilan pada terang/gelap, matriks ulang layar kecil/font besar, dialog Home dan pemulihan back stack Home–AI–Profil, draf AI, sistem terang/gelap, preferensi legacy, proses baru, dialog tema reader Quran, serta pengukuran banner Doa pada ukuran normal/besar. Matriks memeriksa 96 pembukaan tampilan; perbaikan lokal terakhir diuji ulang secara terarah pada reader/Doa, tanpa mengulang seluruh matriks yang tidak terpengaruh.

Ukuran normal **1080×2400 / 420 dpi, font100%**; ukuran sempit **640×1380 / 320 dpi (320dp), font200%**. Emulator khusus ZikraApproved, jaringan dimatikan, isi sintetis bertanda DATA UJI. Paket tes dihapus dan emulator ditutup. Gambar Mushaf/peta yang membutuhkan unduhan diuji pada kondisi luring/gagal, bukan diklaim berhasil mengunduh/render isi daring.

Tampilan matriks: Home, Profil, Study, Hifz, kalender, Ibadah/Faith, katalog Quran, Mushaf, AI, Doa, pustaka Doa, Hadis, reader Hadis luring, salat, lokasi, Kiblat, masjid terdekat, Protector, cuaca, gempa, fokus, jadwal kelas, onboarding dan Infaq legacy.

**153 unit lulus / 4 dilewati / 0 gagal**. Build main/androidTest lulus; **lint 0 error / 860 warning**. Warning belum dinyatakan bersih; mayoritas sumber lama/unused resource, teks dan penataan. Percobaan unit saat emulator masih hidup sempat gagal karena memori Windows; eksekusi final dilakukan setelah emulator ditutup dengan batas heap dan berhasil. Perbaikan harness menunggu instance Activity baru dan mengetuk opsi dialog asli; kegagalan awal tidak dihitung sebagai kelulusan.

Tes: `GlobalAppearanceTest` (hanya emulator dengan marker fixture) dan `RebuiltQuranTest#testDefaultLightHeaderAndPersistedNightOption`. Tes lama Quran/Mushaf diselaraskan dengan kontrak tema global; suite audio/render Mushaf daring secara keseluruhan tidak dijalankan ulang.

## Paket dan batas

APK lokal folder pembungkus: `Zikra-Theme-v1.9.23-beta-debug.apk`. **44.070.983 byte**, SHA-256 `9aa208f726729127bd77f70dfc29abf0488015bfdd9699a88ab476f7baefd5d1`. Sertifikat debug sama dengan beta sebelumnya; pembaruan dapat memakai install-r. Semua entry APK didekompresi dan diperiksa terhadap nilai kredensial privat yang diketahui: 0 temuan. Ini pemeriksaan terarah, bukan audit keamanan publik menyeluruh.

Audio privat tetap rekaman biasa/Subuh pilihan pengguna, hanya untuk build debug lokal; tidak dimasukkan ke Git. APK/MP3/DB/backup/log privat tidak dipublikasikan.

Langit Home tetap mengikuti waktu lokasi; ilustrasi dan warna peta domain tidak dibalik secara paksa. Masih perlu penilaian tema pada HP/vendor nyata. Pada teks200% beberapa judul/label tetap membungkus dan strip jadwal Home dapat digulir; aksesibilitas seluruh produk F03 tetap terbuka. Checkpoint ini tidak menambahkan dock AI atau animasi splash native, tidak memperbaiki lampu malam, dan tidak menutup B01, C02/C04/C07, D02/D02a, E/F/G atau publik.

Pemeliharaan: gunakan `ui/AppAppearance` dan role resource sesuai konteks; putih/pine merek tidak otomatis berarti surface/ink. Letakkan warna UI dalam resource siang/malam, bukan nilai tetap pada halaman. Hash source/push dicatat dalam receipt checkpoint.
