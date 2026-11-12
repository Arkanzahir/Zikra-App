# Inventaris layanan untuk peninjauan beta

12 November 2026. Inventaris source, bukan bukti semua layanan selalu tersedia
atau seluruh lisensi konten sudah disahkan. Tidak mengganti penyedia/paket.

| Jalur | Data yang mungkin diterima | Kesiapan/batas |
| --- | --- | --- |
| Vercel → Cloudflare Workers AI | Pesan, lampiran terpilih, konteks opt-in; metadata jaringan | Key di backend; akses per instalasi. Kuota gratis akun bersama, bukan unlimited tiap pengguna. Konfirmasi C02 dan prosedur penerbitan akses tester masih perlu |
| Upstash Redis | Registry/kuota/lease, state deduplikasi push | Bukan riwayat chat. Operator perlu retensi/pencabutan dan pemulihan terdokumentasi |
| Firebase FCM | Token/identitas instalasi dan metadata pengiriman | Push opt-in dan Analytics mati; periksa konfigurasi client untuk paket final. Bukan server key di APK |
| Open-Meteo forecast/geocoding | Koordinat/zona/kata pencarian dan IP | Atribusi ada; cache/jeda tersedia. Free API memiliki batas dan ketentuan nonkomersial, bukan izin otomatis untuk semua model bisnis |
| USNO sky API | Tanggal dan koordinat lokasi pilihan | Langit ilustratif dibedakan dari data astronomi; audit syarat/beban masih perlu |
| BMKG feed/gambar | Permintaan laporan dan metadata jaringan | Atribusi/peta kejadian diperiksa E01. Push interval 5 menit, bukan prediksi |
| OSM tile/Overpass | Area tile, kotak/radius koordinat pencarian dan IP | Atribusi/User-Agent/fallback. Server komunitas best-effort, tidak boleh prefetch massal. Provider Overpass beragam, masing-masing perlu peninjauan |
| myQuran v3 | ID wilayah dan bulan, metadata jaringan | Deklarasi sumber Kemenag melalui perantara. Jangan menyatakan terhubung langsung ke API resmi Kemenag; jangan tambah offset diam-diam |
| Quran.com/Quran Foundation/CDN Islamic Network | Nomor surah/ayat/halaman/qari dan metadata jaringan | Audit sumber teks/terjemahan/font/timing/audio perlu dirangkum sebelum distribusi final |
| equran.id | Permintaan Doa/tafsir dan metadata jaringan | B04 sudah ditelaah terarah; tetap perlu sumber/atribusi pada paket final |
| raw GitHub kandidat Gading | Kitab/versi paket dan metadata jaringan | B01 belum selesai. Lisensi software tidak mengesahkan sumber konten |
| Android/aplikasi luar | Dikte, kamera/picker, rute/panggilan/berbagi pilihan pengguna | Bergantung perangkat dan aplikasi; jangan menganggap semua pemrosesan berlangsung lokal |

## Sumber primer diperiksa 12 November

- [Cloudflare data usage](https://developers.cloudflare.com/workers-ai/platform/data-usage/).
  Kebijakan penyedia tidak menggantikan inventaris Vercel/Redis/metadata.
- [Open-Meteo terms](https://open-meteo.com/en/terms). Free API nonkomersial
  memiliki batas 10.000/hari, 5.000/jam, 600/menit; persyaratan mencakup aplikasi
  dan IP. Situs mencantumkan langganan/iklan/promosi komersial sebagai penggunaan
  komersial. Status gratis aplikasi saja tidak cukup untuk menyimpulkan memenuhi
  ketentuan. Tentukan bentuk beta/promo sebelum menutup gerbang ini; tidak ada
  paket berbayar diaktifkan.
- [OSM Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/).
  Atribusi terlihat, User-Agent identitas aplikasi, cache sesuai header/setidaknya
  tujuh hari dan tanpa unduhan massal; tidak ada SLA. Dua jalur map memakai
  osmdroid MAPNIK; kepatuhan header/cache aktual masih perlu diperiksa.
- [Firebase privacy](https://firebase.google.com/support/privacy). Tanggung jawab
  pemberitahuan/kontrol pengguna tetap pada penerbit aplikasi.

Kesesuaian syarat Quran/font/audio/Hadis/Doa/USNO/Overpass dan metadata/retensi
platform tetap memerlukan bukti. Jangan mengklaim seluruh G03 selesai dari
empat halaman yang dibaca atau dari pemindaian credential yang bersih.
