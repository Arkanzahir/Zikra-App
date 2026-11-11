# Draf video promo Zikra — 11 November 2026

Video konsep 30 detik, vertikal 720×1280, H.264 30fps + AAC48kHz mono. Narasi
bahasa Indonesia sintetis dan subtitle berdasarkan timestamp kalimat, dengan
warna/logo Zikra yang sudah disetujui. Belum dipublikasikan atau diunggah ke
layanan lain; tidak memakai badge tersedia di Play Store.

## Berkas penyerahan lokal

- `D:\Download\Zikra-App-main\Zikra-Promo-v1-20261111.mp4`
- `D:\Download\Zikra-App-main\Zikra-Promo-v1-20261111.srt`
- `D:\Download\Zikra-App-main\Zikra-Promo-Project-20261111.zip`

ZIP memuat empat rekaman layar mentah, narasi terpisah, metadata subtitle,
storyboard, logo turunan yang sama dan script render. Font Windows tidak ikut
didistribusikan. Video/audio/ZIP disimpan lokal, tidak dimasukkan ke Git.

## Sumber dan pemeriksaan

Semua cuplikan final direkam langsung dari APK1.9.40 di emulator baru khusus
`ZikraPromo`, bukan animasi layar palsu atau rekaman HP pengguna. Empat halaman:
Home, Quran, sesi fokus dan Protector. Tidak memasukkan contoh respons AI palsu.
Cuaca memakai lokasi contoh publik bawaan; tidak mengambil GPS atau data akun.

Pengguna mengosongkan RAM. Percobaan emulator pertama gagal karena drive D tidak
cukup ruang; data sementara baru dibuat di E, terpisah dari emulator lama.
Rekaman Home pertama ternyata onboarding; ditolak setelah peninjauan visual,
onboarding diselesaikan dan Home direkam ulang. Lima eksekusi capture/empat
metode menghasilkan empat cuplikan final. Guard persiapan onboarding dan
aktivitas tetap terlihat ditambahkan pada helper akhir; build akhir lulus,
guard tambahan itu tidak diklaim sebagai eksekusi native baru.

Narasi memakai preset sintetis `id-ID-ArdiNeural`, teks promo asli, tanpa data
pribadi/paket berbayar. Tidak memakai soundtrack, audio qari atau adzan dari
rekaman layar. Subtitle dibuat dari timestamp kalimat dengan tempo yang sama
seperti audio final. [Storyboard/naskah](storyboard.json).

Enam frame final ditinjau untuk komposisi/teks; video dan audio didekode penuh
tanpa error, durasi30detik. Pengukuran audio mean−17,6dB/peak−1,3dB pada ekspor
awal; ekspor akhir dinormalkan sama dan memakai48kHz untuk kompatibilitas.
Pemeriksaan ini bukan penilaian manusia atas pelafalan/selera suara.
[Metadata/hash/capture/cleanup](checks.json).

Emulator sementara ditutup, direktorinya di E dihapus setelah lokasi absolut
dan marker kepemilikan diverifikasi. HP dan emulator lama tidak diubah. Tidak
ada runtime APK/backend/kuota/provider AI berubah atau gerbang D02/E03/publik
ditutup. Ini draf untuk meninjau arah promo, bukan persetujuan rilis publik.

Reproduksi komposisi memakai `tools/render_zikra_promo.py`, Pillow, font Windows
Segoe UI/Georgia dan FFmpeg. Helper native `PromoCaptureTest` hanya dapat dipakai
pada emulator dengan marker khusus; jangan menjalankannya pada HP atau profil
yang memiliki data pengguna.
