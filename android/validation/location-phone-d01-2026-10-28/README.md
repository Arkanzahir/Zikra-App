# D01 — lokasi dan kompas pada HP fisik

28 Oktober2026. **D01 selesai teknis beta terarah**, melengkapi
[hitungan270 peristiwa dan uji emulator sebelumnya](../location-qibla-d01-2026-10-26/README.md).
POCO F7/API36 nyata, tanpa lokasi/sensor buatan. APK1.9.7 diperbarui dengan
`install -r` ke1.9.15/code42/sourcec68bdde; APK utama tidak dihapus. Tidak ada
perubahan kode produksi/versi APK pada pemeriksaan ini. [Receipt](release.json),
[hasil](results.json), [preservasi](preservation.json), [diagnostik lokasi](coarse-result.json).

## Bukti fisik dan batas pengamatannya

| Pemeriksaan | Hasil dan cara pembuktian |
| --- | --- |
| Putaran HP mendatar | Pengguna mengonfirmasi kompas/angka berubah mengikuti gerakan; tangkapan privat menunjukkan status sensor aktif |
| HP ditegakkan lalu diratakan | Pengguna mengonfirmasi petunjuk meratakan muncul, kemudian kompas kembali aktif |
| Arah terhadap acuan setempat | Pengguna menyatakan memiliki acuan Kiblat dan arah Zikra sesuai. Ini pembandingan manual, bukan pengukuran toleransi memakai alat terkalibrasi |
| Lokasi presisi | Pengguna melihat konfirmasi zona Asia/Jakarta; pembacaan izin sistem menunjukkan fine+coarse aktif pada percobaan itu |
| Lokasi Perkiraan | Fine benar-benar dicabut sementara dan coarse tetap aktif. Percobaan pertama mencapai timeout; setelah pemeriksaan provider di depan, pengguna mengulang alur dan mengonfirmasi zona waktu muncul |
| Empat rotasi | Android diminta0/90/180/270; halaman Kiblat benar-benar di depan pada semua kasus, dimensi tangkapan berganti1280×2772/2772×1280. Gambar ditelaah; pengaturan rotasi semula dipulihkan |

Percobaan rotasi pertama **tidak valid** karena pengguna berpindah aplikasi.
Tidak dihitung lulus; gambar di luar Zikra dibuang. Putaran kedua menambahkan
pemeriksaan aplikasi di depan sebelum tangkapan dan lulus empat rotasi.
Di landscape halaman memakai gulir; bukti ini tidak menutup F03 semua kontrol,
font/ukuran atau ketepatan heading terhadap acuan fisik pada setiap rotasi.

Awalnya antarmuka otomatis tidak dapat membuka Activity internal dari shell
karena tidak diekspor, sesuai pembatasan aplikasi. Launcher resmi dipakai dan
pengguna membuka menu. UIAutomator tidak mencapai idle saat layar berubah;
pengamatan memakai tangkapan app-only dengan pemeriksaan fokus dan konfirmasi
pengguna. Kegagalan alat tersebut bukan tes native aplikasi yang lulus.

## Diagnostik Perkiraan nyata

Satu metode native final `PhoneCoarseLocationTest` berjalan sekitar28detik,
dengan **LocationActivity benar-benar di depan**, coarse diberikan/fine ditolak.
Provider network menerima pembaruan nyata yang bukan mock; provider gps/fused
tidak mengirim fix baru pada jendela yang sama. Lokasi tersimpan dibandingkan
sebelum/sesudah dan tetap sama. Laporan hanya status/boolean, tanpa koordinat.

Percobaan diagnostik pertama tidak membuka Activity; tidak dipakai untuk
membuktikan perilaku izin saat aplikasi digunakan. Versi final memperbaiki
precondition foreground dan memeriksa nilai lokasi tersimpan. Kelulusan metode
berarti diagnostik selesai/keutuhan terjaga; keberhasilan alur pengguna Perkiraan
dibuktikan terpisah oleh percobaan ulang pengguna. Tidak ada perbaikan produksi
yang diklaim: penyebab pasti timeout awal belum ditetapkan. Permintaan lokasi
tetap dapat gagal/lambat; pilihan kota manual tersedia sebagai fallback.

Helper berada di `src/phoneTest`, hanya dibangun dengan `-PzikraPhoneTests=true`;
tidak masuk APK aplikasi. Marker memerlukan cadangan yang sudah diverifikasi.
Helper tidak membersihkan database, tidak menyuntik lokasi/sensor dan tidak
memakai key provider AI. Setelah pemeriksaan helper dan dua berkas hasil/marker
sementara di HP dihapus. [Log native final](native-foreground.txt), [build helper](build.txt).

Rujukan perilaku provider/izin:
[LocationManager Android](https://developer.android.com/reference/android/location/LocationManager).
Dokumentasi menyebut lokasi dapat dikaburkan ketika hanya coarse diberikan;
hasil ini hanya untuk perangkat/kondisi yang diperiksa, bukan jaminan setiap
vendor atau kondisi tanpa sinyal/jaringan.

## Preservasi

19 berkas internal dan APK lama dicadangkan privat sebelum upgrade. Database
sebelum/sesudah dibaca dari snapshot setelah proses dihentikan: semua nilai
baris lama pada11 tabel dipertahankan, dan integrity_check lulus. Perubahan
skema/berkas SQLite akibat upgrade tidak diklaim cocok hash. Delapan grup
preferensi dibandingkan; lokasi uji sempat tersimpan dan empat nilai lokasi
berbeda, lalu berkas preferensi lokasi asli dipulihkan serta dicocokkan byte.
Nilai preferensi awal akhirnya terpelihara. Database asli tidak dikembalikan
ke skema lama atau ditimpa fixture. File asli lain yang dibandingkan tidak berubah.

Izin fine/coarse/kamera/notifikasi awal, rotasi dan pengaturan jaringan/stay-awake
dipertahankan/dipulihkan. Aplikasi utama tetap1.9.15 dan launcher dibuka kembali.
Cache/berkas eksternal tidak diisolasi dan **tidak diklaim telah dibandingkan hash**.
Cadangan, serial, gambar layar dan lokasi rinci tetap di`.local`, tidak di Git.

Unit produksi/lint/emulator tidak diulang untuk penambahan helper saja. Hasil
143 unit/4 skip dan lint0 error/815 warning milik kandidat
[1.9.15](../refined-ui-2026-10-28/README.md), bukan hasil baru pemeriksaan ini.
D01[x] sebatas syarat beta terarah dengan gabungan bukti lama dan pemeriksaan
fisik di atas. D02/D02a, sumber/ketentuan audio, B01, C dan E/F/G/publik tetap
mengikuti gerbang masing-masing. Tidak menjamin semua lokasi/tanggal dunia,
akurasi sensor absolut atau penetapan fiqh/jadwal resmi.
