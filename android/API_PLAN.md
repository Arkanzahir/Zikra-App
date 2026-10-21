> **Mushaf online dan audio asli — 21 Oktober 2026:** renderer memakai public v4 yang sudah ada, font QCF V2 dari CDN resmi dengan atribusi/cache terbatas. Lima metode emulator API 37 (termasuk decoder/unduhan/offline qari asli) lulus. [Laporan](validation/mushaf-reader-2026-10-21/README.md). Gateway backend/Content Sync dan sinkron kata belum diintegrasikan.

> **Production Foundation — 21 Oktober 2026, 06:59 WIB:** autentikasi dan empat probe konten resmi HTTP 200: 114 surah, Al-Mulk, glyph/baris mushaf 1 halaman 1 dan timing sampel Alafasy. [Bukti tanpa kredensial](validation/foundation-access-2026-10-21/README.md). Akses Content Sync, renderer dan playback Android belum diuji. Status cloud/akun sebelumnya di bawah adalah riwayat.

> **Audit Windows — 21 Oktober 2026:** API teks/CDN qari dapat diakses HTTP 200 dari komputer lokal. Timing sampel tersedia tetapi kecocokan recording belum terbukti; [audit/izin terbaru](validation/local-qari-2026-10-21/SOURCE_AUDIT.md). Penolakan proxy di bawah adalah riwayat cloud.

> **Audit 21 Oktober 2026:** reader/panel audio native diperbarui. API teks, CDN qari dan host dokumentasi Foundation masih mendapat CONNECT 403 dari proxy cloud. [Bukti request, dokumentasi Mushaf dan prasyarat timing](validation/quran-reader-panel-2026-10-21/SOURCE_AUDIT.md). Provider lama dipertahankan.

# Provider dan status integrasi — 20 Oktober 2026

Aplikasi tetap Java/XML native. Daftar ini membedakan layanan yang sudah digunakan

dari rencana berikutnya. [Bukti lokasi/audio](validation/location-quran-2026-10-19/README.md) · [Perapian lokasi berikutnya](validation/location-polish-2026-10-20/README.md).

| Kebutuhan | Layanan aktual | Kondisi |
| --- | --- | --- |
| Kota/koordinat/zona | Open-Meteo Geocoding / GeoNames, Android LocationManager | Kota online, 15 pilihan kota offline, posisi foreground satu kali dan pilihan zona eksplisit saat resolusi online gagal |
| Jadwal sholat | Kalkulasi matahari lokal NOAA | Koordinat/zona pilihan; Subuh 20°, Isya 18°, Ashar bayangan 1×, ihtiyati 2 menit kecuali terbit; bukan jadwal resmi Kemenag |
| Cuaca | Open-Meteo Forecast | Model sesuai lokasi/zona pilihan, timestamp Unix untuk menghindari ambiguitas DST; cache maksimal empat lokasi, segar 10 menit, fallback enam jam |
| Masjid | OpenStreetMap melalui Overpass | Radius 3 km, maksimal 100 entri respons/60 hasil tampil, validasi/deduplikasi/urut jarak garis lurus; cache satu lokasi terakhir 30 menit |
| Peta masjid | OpenStreetMap tiles + osmdroid Java 6.1.20 | Peta native di Zikra, dimuat saat dibuka; atribusi terlihat; tanpa Google key/billing |
| Petunjuk arah | Google Maps URL / Android Intent | Membuka navigasi eksternal; tidak menanam navigasi belokan di Zikra |
| Langit Home | USNO harian + perkiraan lokal berlabel | Lokasi/tanggal/offset zona diperiksa; fallback eksplisit bila layanan/peristiwa matahari tidak tersedia |
| Quran | Quran.com public v4 / metadata bawaan | 114 metadata surah, reader Arab/terjemahan resource 33, empat surah bawaan, delapan bagian cache × 50 ayat |
| Gempa | BMKG Open Data | Laporan terakhir Indonesia, bukan prediksi atau peringatan pribadi berdasarkan posisi pengguna |
| Audio Quran | Al Quran Cloud CDN, HTTPS 128kbps | Alafasy/Al-Husary/Al-Minshawi; satu file per ayat; background, notifikasi, rentang/repeat dan unduhan eksplisit per surah/qari sudah di main `1045fa7`. Decoder diuji dengan WAV sintetis; CDN qari live/full download belum terbukti |
| Timer/pengingat akademik | Android AlarmManager + notifikasi lokal | Timer monotonic 25/5 atau 50/10; tenggat Study opsional, alarm inexact dapat tertunda sistem; tanpa provider baru |
| AI | Aturan lokal offline | Featherless/Gemini belum terhubung |

## Lokasi bersama

`LocationStore` menyimpan koordinat, nama, zona IANA, sumber dan akurasi yang tersedia.
Sukolilo adalah **acuan awal berlabel**, sampai pengguna mengonfirmasi pilihan.
Home, jadwal/alarm sholat, cuaca, langit dan Kiblat membaca sumber pilihan yang sama.
Akademik/checklist tetap memakai tanggal lokal perangkat sebagai konteks agenda.

- Izin `ACCESS_COARSE_LOCATION`/`ACCESS_FINE_LOCATION` hanya diminta setelah tindakan
  Gunakan lokasi saya. Izin perkiraan tetap didukung; perangkat tanpa GPS memakai kota.
- Mengambil fix baru atau posisi terakhir yang masih muda (maksimal tiga menit),
  dengan batas tunggu 25 detik. Listener/pekerjaan dibatalkan ketika halaman ditinggalkan;
  tidak ada background location, pelacakan perjalanan atau izin storage.
- Pencarian kota: `https://geocoding-api.open-meteo.com/v1/search`; atribusi Open-Meteo/
  GeoNames. Koordinat kota adalah pusat kota, bukan GPS rumah pengguna.
- Zona posisi perangkat diperiksa melalui metadata Forecast dengan `timezone=auto`.
  Bila gagal, pengguna memilih zona eksplisit atau kota; jangan menebak zona dari bujur.
- Pergantian lokasi menjadwalkan ulang pengingat. Broadcast jadwal lama mempunyai
  identitas lokasi, sehingga tidak diberitahukan sebagai jadwal kota baru.
- Peristiwa matahari yang tidak dapat dihitung di lintang tinggi ditandai `—`;
  jangan menciptakan jam atau alarm. Langit pada kondisi tersebut berlabel ilustrasi.

## OpenStreetMap versi awal

Pengguna memilih OSM karena Google Cloud/billing belum aktif. Ini pilihan sumber,
**bukan klaim gratis tanpa batas atau kelengkapan setara semua layanan berbayar**.

Pencarian memakai `https://overpass.private.coffee/api/interpreter`, dengan satu
fallback terurut ke `https://overpass-api.de/api/interpreter`. Request dibatasi
waktu/ukuran; respons parsial `remark` ditolak. HTTP 429 menghentikan fallback dan
menghormati jeda `Retry-After`; UI membatasi pencarian ulang. Bila layanan gagal,
hasil yang sudah tampil tetap tersedia dan status kegagalan terlihat.

Peta memakai `https://tile.openstreetmap.org/{z}/{x}/{y}.png`, user-agent aplikasi
yang jelas dan cache internal mengikuti kebijakan library/HTTP. Peta dibuat saat
dibuka, koneksi tile dihentikan saat peta disembunyikan/Activity berhenti; tidak
menyediakan bulk download, prefetch area atau unduhan peta offline.

Daftar/peta mengatribusikan **© OpenStreetMap contributors**, dengan tautan ODbL.
Alamat yang tidak ada ditandai belum tercantum. Tidak menyimpulkan fasilitas,
jam jamaah atau status buka. Jarak adalah garis lurus. Data komunitas dapat tidak
lengkap, dan infrastruktur publik tidak mempunyai SLA untuk aplikasi ini.

- [Kebijakan tile OSM](https://operations.osmfoundation.org/policies/tiles/)
- [Pemakaian Overpass publik](https://dev.overpass-api.de/overpass-doc/en/preface/commons.html)
- [Atribusi data](https://www.openstreetmap.org/copyright)
- [osmdroid, Apache 2.0](https://github.com/osmdroid/osmdroid), lisensi ikut aset APK.

## Integrasi berikutnya

- **Aladhan:** verifikasi metode Kemenag dari `/v1/methods`; endpoint koordinat dan
  tanggal/zona/metode harus konsisten antara Home, daftar dan alarm. Belum terpasang;
  kalkulasi lokal sekarang tetap berlabel dan bukan publikasi resmi Kemenag.
- **Quran Foundation:** Content Sync production belum dimiliki pengguna. Mushaf
  membutuhkan layout edisi/font berlisensi yang konsisten. Audio per ayat kini memakai Al Quran Cloud CDN dengan atribusi/ketentuan di pengaturan. Mengikuti audio memakai identitas berkas ayat dan event MediaPlayer, bukan estimasi waktu atau sinkron kata. TTS doa tidak menjadi murottal. Content Sync/Mushaf dan verifikasi audio qari live masih terbuka.
- **AI Featherless/Gemini:** membutuhkan backend HTTPS dan model/kredensial yang valid.
  Kunci provider disimpan di backend, bukan APK/Git. Jangan mengklaim AI telah membaca
  seluruh fitur hanya karena tersedia panduan aturan atau navigasi lokal.
- **Google Places/Maps tertanam:** memerlukan project, billing, pembatasan kunci dan
  kepatuhan atribusi. Data Google Places pada peta mengikuti ketentuan Google, bukan
  ditumpangkan diam-diam pada OSM. Belum termasuk versi OSM ini. Google Maps eksternal
  untuk arah tidak memerlukan kunci milik aplikasi.
- **Open-Meteo:** akses publik nonkomersial mengikuti ketentuan provider, bukan gratis
  tanpa batas untuk segala skala/penggunaan. Simpan sumber dan waktu prakiraan model.

Tidak ada secret baru yang diperlukan build versi OSM ini. Alarm, sensor, GPS nyata,
layanan suara, performa/baterai dan runtime API 35 tetap perlu diuji di perangkat fisik.

## Pemeriksaan provider — 20 Oktober 2026

Tile OSM, Overpass dan CDN qari masih mendapat penolakan proxy CONNECT 403 pada
checkpoint `feature/map-audio-study-2026-10-20`. Tidak mengganti sumber atau melewati
policy. [Log](validation/map-audio-study-2026-10-20/live-provider-status.txt).
Peta kini menunjukkan loading/belum termuat dan tombol coba ulang; status keberhasilan
mengikuti callback library, bukan menganggap penanda sebagai bukti tile jalan tersedia.
Transfer unduhan menolak cache rusak dan melakukan validasi decoder sebelum memakai
berkas lengkap atau menyimpan hasil unduhan. Pengujian HTTP memakai fixture WAV sintetis;
keberhasilan server live/qari dan kebenaran suara tetap perlu diuji pada jaringan yang diizinkan.
