# C06 — input suara fisik lulus beta1.9.7

26 Oktober 2026. Dua metode native final lulus pada HP/API36 dengan APK1.9.7/code34:
[consent/pembatalan recognizer nyata](phone-c06-cancel.json) dan
[mikrofon/dikte nyata](phone-c06-microphone.json). Hasil29 karakter mengandung
kata uji belajar/matematika, tetap draf maksimal500 karakter, tidak otomatis
mengirim ke AI dan bertahan setelah layar dibuka ulang. Tidak memalsukan callback
activityResult, memasukkan audio rekaman atau menerbitkan transkrip.

[Preservasi](preservation.json):12 berkas asli dipindahkan sementara di perangkat
sesudah cadangan baru, kemudian dipulihkan dengan hash identik. Helper dihapus,
APK utama dipertahankan sebagai pembaruan1.9.7; Wi-Fi/data seluler tetap dan
stay-awake kembali ke snapshot percobaan final. Snapshot final tercatat privat;
jangan memperluasnya menjadi audit semua pengaturan Android sebelum seluruh
percobaan. Recognizer Android dapat memakai jaringan/layanan suara tersendiri;
mode AI offline bukan jaminan pengenalan suara sepenuhnya offline.

Percobaan awal gagal mendapat fokus activity; host kemudian membuka launcher
resmi sebelum instrumentation. Satu pemasangan helper juga gagal; data12 berkas
sudah dikembalikan dan diverifikasi sebelum pemasangan ulang. Dua metode final
lulus setelah hambatan alat ini ditangani. Saat pengguna bermain game, pengujian
berhenti dan data dipulihkan; dilanjutkan hanya setelah diminta dan konfirmasi
siap berbicara. Driver/raw log/arsip perangkat tetap privat.

Source APK249b523, SHA256686101f643a13b49ec9ff05623a8b52c8ea3e9f2f1689763ee24ee7d952ea833,
37218400 byte. Tidak membangun APK produksi baru atau mengubah backend. C02 masih
menunggu HP saat laptop mati. Kamera C07 kemudian dilaporkan berfungsi oleh
pengguna; audit picker/izin/dokumen/OCR belum ditutup oleh laporan manual itu.
Pengguna mengalihkan prioritas ke referensi desain AI; tidak mengulang uji kamera.

# Riwayat persiapan C06

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
