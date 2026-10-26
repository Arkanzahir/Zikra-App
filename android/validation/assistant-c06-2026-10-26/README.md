# C06 — pengujian suara fisik disiapkan, belum selesai

26 Oktober2026. Pengguna memilih melanjutkan pekerjaan tanpa HP setelah koneksi
USB terputus. C06 tetap[ ]; tidak menyebut callback simulasi sebagai mikrofon nyata.

`PhoneVoiceTest` disertakan hanya pada helper opt-in dengan
`assembleDebugAndroidTest -PzikraPhoneTests=true`. [Build helper](build-helper.txt)
lulus pada source APK1.9.7/code34. Tidak mengubah APK utama atau backend.

Harness menolak emulator, HP terkunci, versi APK yang tidak cocok, dan host yang
belum menegaskan otorisasi/cadangan baru/isolasi data asli. Alur yang akan diuji:
tolak consent, buka recognizer Android nyata lalu batalkan, ucapkan kalimat
sintetis yang mengandung belajar/matematika, pastikan hasil tetap draf maksimal
500 karakter/tidak auto-kirim dan pulih saat layar dibuka ulang. Tidak memanggil
callback hasil suara lewat refleksi atau menyisipkan audio rekaman. Hasil publik
hanya boolean/panjang teks; transkrip tidak ditulis ke laporan.

12 berkas asli HP sebelumnya telah dicadangkan privat. Percobaan menjalankan
harness berhenti pada pemeriksaan koneksi, sebelum isolasi, pemasangan atau
perubahan pengaturan. APK HP terakhir teramati code26. File privat perangkat,
arsip, hash per berkas dan driver berada di `.local/phone-c06/`, tidak di Git.
Cadangan itu wajib dibandingkan dengan keadaan terkini sebelum pengujian ulang;
perubahan pengguna yang lebih baru harus dicadangkan lagi, bukan ditimpa.

Sesudah HP tersedia: verifikasi cadangan baru dan root app-private yang tepat,
sisihkan data asli di perangkat, pasang APK sebagai pembaruan/helper sementara,
lakukan dua metode secara eksplisit, lalu pulihkan data asli dengan pemeriksaan
hash dan hapus helper. Jangan uninstall APK utama atau mengirim profil/chat asli.
Kalimat uji perlu diucapkan pengguna saat recognizer mendengarkan. Bukti sebelumnya
tentang consent/draf/batas teks tetap simulasi/native terarah, bukan penutupan C06.
