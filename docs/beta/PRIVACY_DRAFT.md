# Draf informasi privasi Zikra beta

12 November 2026. **Draf untuk ditinjau, belum kebijakan publik final.**
Kontak dukungan yang diizinkan pengguna: arkanzahir46@gmail.com. Nama aplikasi
Zikra; nama penerbit publik **Zikra** dikonfirmasi pengguna pada 14 November 2026. Cakupan usia, retensi
metadata seluruh penyedia dan prosedur permintaan penghapusan server harus
dipastikan sebelum dokumen diterbitkan. Ini bukan pernyataan semua lapisan nol log.

## Data di perangkat

Profil, tugas/jadwal Study, bacaan/bookmark, Hifz, preferensi dan riwayat chat
disimpan lokal. Login Google, sinkronisasi dan cadangan cloud pengguna belum
tersedia. Menutup aplikasi tidak menghapus data; uninstall/clear data dapat
menghilangkannya. Backup Android dinonaktifkan dan domain cloud/D2D dikecualikan,
tetapi kepatuhan semua vendor belum diuji. Chat/lampiran belum memakai enkripsi
database tersendiri; token akses AI dilindungi Android Keystore.

## AI online dan lampiran

Setelah izin dasar, pesan/lampiran yang dipilih pengguna dikirim melalui
backend HTTPS Zikra di Vercel kepada Cloudflare Workers AI. Tidak memakai
Featherless untuk chat produksi sekarang. Pembuatan gambar juga melalui backend
ke Cloudflare. Mode offline memakai aturan lokal, bukan model generatif di HP.

Nama, tugas, jadwal kelas, jadwal salat dan riwayat tambahan mengikuti pilihan
konteks eksplisit. Tidak otomatis mengunggah seluruh profil, GPS atau folder
HP. Lampiran hanya yang dipilih. Foto dinormalisasi dan metadata EXIF asli tidak
diteruskan; isi visual/teksnya masih dapat mengandung data pribadi. PDF menjadi
gambar halaman, DOCX/TXT menjadi teks. Tidak ada redaksi otomatis atau indeks
RAG/ingatan cloud permanen. Hapus chat/lampiran lokal tidak menarik kembali data
yang telah diproses penyedia.

Batas saat ini satu berkas per pesan, 8 MB, PDF sampai 3 halaman, DOCX/TXT sampai
12.000 unit karakter Java; penyimpanan lampiran lokal dibatasi 25 MB. File yang
memenuhi batas belum tentu terbaca akurat. Kuota AI bersifat terbatas dan dapat
gagal; pengguna perlu memilih coba lagi, tanpa pengiriman ulang diam-diam.

Kode gateway yang ditinjau tidak menyimpan isi percakapan sebagai log. Upstash
menyimpan registry/kuota/lease akses instalasi. Vercel/Cloudflare dapat memproses
metadata operasional. Pernyataan Cloudflare bahwa konten tidak dipakai melatih
model mengikuti [kebijakan penyedia](https://developers.cloudflare.com/workers-ai/platform/data-usage/),
bukan audit independen terhadap semua layanan.

## Lokasi, peta dan konten

Lokasi pilihan/manual/perangkat dipakai untuk jadwal, Kiblat, cuaca dan pencarian
masjid. Permintaan cuaca/geokode/langit/pencarian masjid dapat mengirim koordinat
atau kata pencarian ke layanan terkait. Permintaan tile peta mengungkap area
peta yang dilihat; HTTPS tidak menyembunyikan IP dari penerima. Layanan pada
[inventaris](SERVICES.md) juga menerima metadata jaringan ketika konten dimuat.
Izin lokasi dapat ditolak dan lokasi manual tetap tersedia.

Pengingat cuaca/harian mati secara bawaan. Jika pemeriksaan cuaca latar belakang
diaktifkan setelah penjelasan di aplikasi, koordinat lokasi pilihan dikirim ke
Open-Meteo sekitar tiap jam pada pukul06–21 waktu lokasi pilihan. Fitur ini tidak
menyalakan GPS atau mendeteksi perjalanan. Pilihan dapat dimatikan kapan saja;
jadwal pemeriksaan dibatalkan ketika seluruh pengingat dimatikan. Ringkasan pagi
membaca jumlah tugas dan jadwal salat secara lokal; tidak mengirim isi tugas ke AI.
Prakiraan yang tersimpan hanya dipakai bila lokasi/waktu dan batas umurnya valid.
Notifikasi ditandai privat; visibilitas akhirnya mengikuti pengaturan Android.

Quran, audio, paket Mushaf, Doa dan kandidat Hadis diunduh saat fitur memerlukannya.
Ensiklopedia HadeethEnc bersifat opsional dan diunduh setelah pilihan pengguna;
berkas sumber/metadata versinya disimpan lokal. Tautan sumber/cek versi membuka
situs penerbit, tanpa mengirim profil atau isi chat.
Tautan rute/panggilan/berbagi membuka aplikasi luar setelah tindakan pengguna;
menu bantuan membuka dialer atau draf pesan tanpa menelepon/mengirim otomatis.
Draf keluarga tidak menyertakan GPS, profil atau percakapan. Salin koordinat
untuk navigasi manual hanya dilakukan setelah tombol Salin ditekan; clipboard
kemudian tunduk pada akses dan masa simpan Android/aplikasi lain.
Kebijakan aplikasi tujuan berlaku. Dikte/kamera/pemilih dokumen menggunakan
layanan Android/aplikasi pilihan perangkat; tidak menjanjikan dikte selalu luring.

## Pengingat

Pengingat salat dan Study dijadwalkan lokal sesuai preferensi/izin Android.
Notifikasi gempa opsional memakai Firebase Cloud Messaging. SDK dapat membuat
identitas instalasi/token dan memproses metadata pengiriman saat diaktifkan.
Analytics Firebase dan auto-init messaging dimatikan di manifest; ini bukan
jaminan Firebase tidak memproses data saat pengguna mengaktifkan push.
[Penjelasan Firebase](https://firebase.google.com/support/privacy).

Gempa bersumber laporan BMKG, bukan prediksi atau jaminan tiba sebelum guncangan.
Cuaca adalah prakiraan untuk waktu/lokasi sumber, bukan pengukuran langsung.

## Kontrol dan hal yang belum selesai

Pengguna dapat memilih mode AI, mencabut izin online, memilih/menghapus chat,
mengatur izin Android dan menonaktifkan pengingat/push. Lokasi dan konteks
tambahan tidak menjadi izin permanen mengirim seluruh data ke AI. Operator
memerlukan prosedur dukungan untuk mencabut akses instalasi dan menangani
permintaan terkait data server; kontaknya harus ditetapkan sebelum rilis.
Durasi retensi final per layanan belum disahkan dalam draf ini.

## Retensi yang terlihat pada kode gateway

Pemeriksaan 12 November 2026 (bukan audit dashboard penyedia):

| Data | Perilaku kode saat ini |
| --- | --- |
| Registry instalasi dan hash token akses | Tidak memiliki TTL otomatis. Pencabutan menandai revoked; bukan penghapusan record |
| Hitungan pemakaian AI | TTL 172.800 detik, diperbarui oleh pemakaian yang diterima; bukan penghapusan mutlak dua hari sejak instalasi |
| Slot aktif AI | Lease 55 detik, key TTL 60 detik; pelepasan normal lebih cepat |
| Kuota/deduplikasi gambar | TTL 172.800 detik pada key yang diatur layanan gambar |
| Deduplikasi laporan gempa | Key event 7 hari; penanda inisialisasi tidak memiliki TTL otomatis |
| Chat dan berkas di HP | Lokal sampai dihapus pengguna/aturan penyimpanan aplikasi; penghapusan lokal tidak menghapus metadata layanan lain |

Retensi log Vercel/Cloudflare/Upstash/FCM harus diverifikasi pada akun/paket dan
ketentuan masing-masing; angka tabel tidak menjamin seluruh salinan/log/backups
penyedia ikut terhapus. Tidak mengubah TTL produksi atau menghapus registry di sini.

## Dukungan dan permintaan terkait data

Hubungi [dukungan Zikra](mailto:arkanzahir46@gmail.com). Sertakan versi aplikasi,
jenis masalah dan waktu kejadian; jangan kirim password, API key, kode akses AI,
PIN, dokumen pribadi atau koordinat tepat. Permintaan akan ditinjau operator;
belum ada layanan penghapusan otomatis atau tenggat penanganan yang dijanjikan.
Operator memverifikasi instalasi melalui kanal aman sebelum mencabut akses atau
menghapus data terkait. Pencabutan akses dan penghapusan data adalah dua langkah
berbeda. Jangan mengklaim data penyedia terhapus tanpa konfirmasi layanan tersebut.
