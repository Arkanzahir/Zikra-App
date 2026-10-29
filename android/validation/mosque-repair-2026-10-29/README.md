# UI08 — Interior masjid dan pencarian terdekat

29 Oktober2026, kandidat **1.9.26/code53**. Pengguna memprioritaskan interior
malam dan melaporkan pencarian masjid gagal. Selesai teknis beta terarah pada
emulator proyek API37.1; HP pengguna tetap1.9.25 dan tidak disentuh sesi ini.
E04/F04/A–D/publik tidak otomatis selesai.

## Perubahan

- Cahaya hangat terarah di delapan lengkungan, memakai mask pada ilustrasi
  masjid yang sama. Gradasi radial menjaga detail pintu, dengan pantulan lembut.
  Tidak mengecat pilar atau area terakhir yang tertutup dedaunan. Siang mati;
  memakai lampStrength/matahari lokasi serta fade20menit yang sudah ada, bebas
  dari pilihan tema. Path/Paint/shader disimpan, tanpa bitmap atau alokasi shader
  baru setiap frame. Ini ilustrasi cahaya, bukan pengamatan interior bangunan.
- Dua host pencarian lama timeout pada probe titik publik. Transport POST
  dipisahkan ke OverpassClient; urutan baru VK Maps, Private.coffee, FOSSGIS,
  batas total50detik, timeout per koneksi/call dan identitas User-Agent tetap.
  HTTP429/406 menghentikan seluruh failover dan menghormati Retry-After.
  Respons terdekompresi dibatasi1MiB; JSON invalid/remark/HTTPgagal beralih ke
  kandidat berikut. Cancel/close tidak mengembalikan hasil atau menyimpan cache
  yang sudah dibatalkan. Tidak ada provider key, biaya atau layanan hosting baru.
- Cache segar30menit, cadangan non-kosong maksimal7hari hanya untuk kunci lokasi
  yang sama. Saat layanan gagal/ditunda, tanggal asli dipertahankan dan status
  menyatakan hasil tersimpan. Cache kedaluwarsa/lokasi berbeda tidak dipakai.
  UI menjelaskan pengiriman koordinat saat Cari ditekan dan menyebut operator
  pada keterangan sumber. Tidak memulai GPS atau mengunggah profil/chat.

[Dokumentasi OpenStreetMap tentang Overpass dan instans publik](https://wiki.openstreetmap.org/wiki/Overpass_API#Public_Overpass_API_instances)
mencantumkan endpoint VK/Private.coffee/FOSSGIS serta batas layanan. Atribusi
OpenStreetMap/ODbL dipertahankan; keberadaan/jamaah/fasilitas tidak dikarang.

## Bukti

[Perbandingan siang/malam](preview.jpg), [render malam](home-21.jpg),
[render siang](home-12.jpg), [daftar nyata](live-list.jpg), [peta jalan nyata](live-map.jpg),
[cache bertanggal](stale-list.jpg), [native](native-results.json),
[unit/lint](checks.json), [preservasi](preservation.json), [APK](apk.json).

**160 unit lulus/4 skip/0 gagal**, termasuk5 kasus server loopback: formPOST,
HTTP503+JSONmalformed→failover,429/406 tanpa pindah server, respons terlalu besar,
dan transport tertutup tidak mengirim lokasi. Server uji memakai ServerSocket
standar; harness HttpServer awal tidak tersedia pada classpath Android dan
diganti tanpa mengubah perilaku produksi. **Lint0 error/862 warning**.

**Empat native final lulus** pada APK akhir: mask interior, piksel hangat dalam
lengkungan/pilar tidak berubah/siang mati serta renderHome12.00/21.00; pencarian
nyata100elemen→60hasil valid dalam3km berurut jarak, tampilan Daftar/Peta dan
road tiles, fallback cache bertanggal, lokasi berbeda/cache8hari ditolak;
regresi refresh gagal mempertahankan hasil/tanggal, dan throttle tanpa cache
menunjukkan Retry tanpa mengubah lokasi. Cache/throttle beberapa kasus disetel
oleh harness; bagian itu bukan klaim radio HP offline diuji.

Semua pencarian memakai LocationPoint.INITIAL publik dari repo, diberi label
acuan publik di layar. Tidak mengambil GPS HP. Gambar lampu adalah Canvas
Android dengan waktu fixture; screenshot daftar/peta adalah Activity Android
dan data API sebenarnya. JPEG hanya diperkecil; preview menggabungkan crop
dua render tersebut. Tidak ada piksel jalan atau hasil masjid yang dibuat-buat.

## Keandalan yang tidak disembunyikan

[Riwayat uji](native-attempts.json), [probe host lama](old-provider-probes.json),
[probe kode Java](production-java-probe.json). Satu pencarian native baru tanpa
cache sempat tidak memberikan hasil; satu retry terarah setelah jeda lulus.
Penyedia publik dapat lambat/gagal; bukan SLA atau janji selalu tersedia.
Cache membantu setelah hasil pernah didapat; instal baru tanpa jaringan/data
tetap dapat gagal dan menawarkan Maps. Ketahanan skala publik/provider/kuota,
Android lama dan HP/vendor lain masih bagian E04/G03/G02.

JBR17 lokal mula-mula tidak mengenali rantai sertifikat provider baru. Probe
Java tambahan memakai trust store Windows yang sudah dipercaya sistem, bukan
trust-all/CA tambahan. APK memakai trust store Android bawaan; pencarian HTTPS
native API37.1 lulus tanpa modifikasi sertifikat atau bypass TLS. Screenshot
peta pertama terlalu awal (indikator tile sudah masuk, frame belum selesai);
helper final menunggu notice hilang/bingkai berikutnya dan jalan diperiksa.

## Paket, data dan batas

APK lokal folder pembungkus: `Zikra-Mosque-v1.9.26-beta-debug.apk`.
Ukuran43.857.302byte (<100.000.000byte),
SHA256`f2b45e3982d076fb76fb06b2ebd00c1efae6e17f5c7915de56ea8586c92f181f`.
Signer debug tetap`c7888fe2ca40e14698726ed20b2f93947033cef0bdee9cfd127c429d0ea0ca1b`.
Seluruh entry APK didekompresi/diperiksa terhadap nilai privat yang diketahui:
0temuan. Dua audio cocok hash pilihan terakhir dan tetap hanya input debug
privat. Tidak mengunggah APK/MP3/DB/backup/log mentah/GPS ke Git.

31 berkas asli emulator dicadangkan sebelum update lalu dipulihkan cocok hash;
pengaturan jaringan/font/animasi/viewport dikembalikan. Marker/helper/gambar
uji di emulator dibersihkan, emulator ditutup. HP1.9.25 tidak dipasang/diubah.
WIP PROJECT_HANDOFF, stash/arsip audio, env privat, artwork dan ikon asli tetap.
Font Arab reader/gerbang B01/C02/C04/C07/D02/D02a/E/F/G tetap mengikuti plan.

## Source/checkpoint

[Receipt](receipt.json) mengikat source`355350601f60bb8ba9f93d2e063e5d6b002dcd51` dengan APK/uji.
Commit receipt hanya dokumentasi. Hash remote diverifikasi sesudah push dan
dilaporkan di chat; APK/MP3/data privat tetap lokal.
