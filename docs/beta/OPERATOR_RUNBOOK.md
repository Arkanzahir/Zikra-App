# Operasional beta terbatas

Prosedur source13November2026, bukan bukti kapasitas tak terbatas atau retensi
penyedia yang sudah disahkan. Tidak memerlukan dashboard baru atau paket berbayar.
Tidak menjalankan tindakan administratif hanya karena dokumen ini tersedia.

## Akses per penguji

Gunakan backend/redis-client-admin.mjs melalui lingkungan operator privat yang
sudah memuat konfigurasi Redis sah. Perintah issue menerima path absolut output
privat yang belum ada; token disimpan ke file eksklusif, bukan dicetak. Catat ID
instalasi dari hasilnya pada daftar operator privat; bagikan akses hanya melalui
kanal privat kepada penguji yang dituju. Jangan masukkan token bersama ke APK,
website, Git atau formulir dukungan publik. Token provider AI/QF/FCM tidak dibagikan.

Perintah revoke menerima ID instalasi yang tepat dan memeriksa indeks hash.
Revoke menolak permintaan baru, bukan menghapus chat HP, seluruh record registry
atau data di penyedia. Jangan mengklaim penghapusan menyeluruh dari keberhasilan
revoke. Issue gagal dapat meninggalkan file token yang belum terdaftar: simpan
ID/hasil privat, periksa kejadian itu dahulu; jangan menyebarkan ulang sembarang
file token atau menghapus registry secara massal.

## Pemantauan dan kapasitas

Pantau health, kelas status401/429/5xx dan latensi agregat; jangan log isi chat,
lampiran, koordinat tepat, Authorization, token atau snapshotQuran. Batas source
chat adalah10permintaan/menit/instalasi dan kuota harian yang dikonfigurasi
(default100, bukan janji paket produksi aktif bernilai100). Slot memiliki lease55
detik dan TTL60detik; usage TTL172800detik. Quran memakai batas terpisah60/menit;
gambar dan batas penyedia berbeda. Nilai ini bukan jaminan throughput atau jumlah
pengguna serentak. Kuota penyedia/global bisa habis lebih dahulu.

Saat429: tunggu sesuai petunjuk UI/Retry-After; jangan mereset counter/global key
atau berulang kali menekan Kirim. Saat401: verifikasi ID akses/versi/kondisi revoke
secara privat. Saat5xx/timeout: bedakan DNS/TLS laptop dari hosted, health dari
inference dan hasil cache dari pembaruan; jangan otomatis mengganti model/paket.
Gunakan identitas sintetis tersimpan sebelum request, dengan TTL uji dan cleanup
tepat pada key milik ID tersebut. Timeout cleanup dicatat, bukan disamarkan lulus.

## Dukungan dan insiden

Kontak publik yang disetujui: arkanzahir46@gmail.com. Minta versi/hash, Android,
model perangkat, waktu dan langkah mengulang; screenshot harus disunting pengguna.
Jangan minta PIN, password, key, token, dokumen pribadi atau ekspor chat sebagai
syarat dasar pelaporan. Nomor tiket dan hasil tindak lanjut disimpan privat;
tidak menjanjikan SLA atau retensi yang belum ditetapkan pemilik.

Jika ada kebocoran akses yang terkonfirmasi: hentikan pembagian versi/akses yang
terdampak, identifikasi cakupan privat, siapkan pencabutan ID dan pembaruan paket.
Rotasi credential server atau tindakan di layanan luar mengikuti izin yang relevan;
jangan menulis nilai credential ke laporan. Jika data pengguna mungkin terdampak,
catat fakta terverifikasi dan kebutuhan pemberitahuan; jangan menjanjikan nol log.

Penghapusan server: identifikasi layanan/record pemohon dan kewenangannya terlebih
dahulu. Chat lokal, registry, usage, cache dan metadata penyedia memiliki aturan
berbeda. Bukti penghapusan/cadangan di penyedia masih gate J11. Jangan menghapus
record orang lain atau cache bersama hanya karena satu penguji berhenti.

## Rollback dan distribusi

Paket baru harus punya versi/hash/signer dan receipt audit. Jika Android menolak
update, jangan uninstall atau mengganti DB dari cadangan tanpa guard. Tahan kanal
unduhan saat ada regresi/data hilang; siapkan paket perbaikan berversi lebih tinggi
dengan signer yang sama, lalu validasi migrasi. Backend rollback harus mempertahankan
registry/data/kuota dan kompatibilitas APK; jangan mereset Redis sebagai rollback.
Tidak menurunkan gerbang HOLD atau menyatakan beta siap dari dokumen ini saja.
