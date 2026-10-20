# Audit provider, Mushaf dan timing — 21 Oktober 2026 (WIB)

Observasi cloud pada tahap ini: managed environment connected, observasi current,
HTTP restricted/enforced. Tidak ada perubahan policy, credential, route atau proxy.
Request memakai proxy/CA bawaan; tidak ada percobaan melewati destination denial.

| Pemeriksaan | Hasil aktual |
| --- | --- |
| `https://api.quran.com/api/v4/chapters/1` | CONNECT tunnel ditolak proxy 403; HTTP origin tidak tercapai |
| `https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3` | CONNECT tunnel ditolak proxy 403; rekaman asli tidak terunduh |
| `https://api-docs.quran.foundation/docs/api/resources/recitations` | CONNECT tunnel ditolak proxy 403; dokumentasi dari host tersebut tidak terbaca |
| Quran.com frontend README melalui raw GitHub | 200; dibaca sebagai dokumentasi, bukan source aplikasi Zikra |
| Quran for Android README dan LICENSE melalui raw GitHub | 200; README menyebut sumber gambar Madani dan data, kode berlisensi GPL v3 |
| Quran Image Generator README melalui raw GitHub | 200; menghasilkan gambar halaman beserta bounds glyph, font/page milik King Fahd Quran Complex |
| Dugaan lokasi README repo `quran/quran.com-api` | 404; tidak dianggap sumber valid |
| Dugaan LICENSE/LICENSE.txt repo image generator | 404; tidak menyimpulkan lisensi font/gambar dari license kode |

Dokumentasi yang benar-benar dibaca:

- [Quran for Android README](https://github.com/quran/quran_android/blob/master/README.md)
- [Lisensi kode Quran for Android](https://github.com/quran/quran_android/blob/master/LICENSE)
- [Quran Image Generator README](https://github.com/quran/quran.com-images/blob/master/README.md)
- [Quran.com frontend README](https://github.com/quran/quran.com-frontend-next/blob/master/README.md)

README image generator membedakan kode GPL dari kepemilikan font/page. Tidak ada
kode, font, database, halaman atau rekaman dari proyek tersebut yang disalin ke APK.
Dokumentasi repo bukan pengganti verifikasi data, hak pemakaian dan API yang dibutuhkan.

## Keputusan implementasi tahap ini

- Pertahankan sumber teks Quran.com/resource 33 dan empat surah bawaan/cache lama.
  Parser keys/nomor/terjemahan dan penyimpanan tidak diubah.
- Pertahankan Al Quran Cloud CDN dan tiga reciter lama; tidak mengganti provider
  secara diam-diam. Unduhan/per ayat, service, MediaSession dan transfer tetap dipakai.
- Uji playback/seek/background/range dan transfer lokal memakai WAV sintetis serta
  server lokal milik tes. Itu menguji decoder, state dan I/O; **bukan rekaman Quran**,
  bukan bukti CDN/live reciter atau kebenaran pelafalan sudah terverifikasi.
- Sorotan yang sekarang tersedia tetap **ayat aktif di pemutar** dengan latar sage,
  termasuk ketika player sedang dijeda/memuat. Bukan sorotan kata/karaoke.
- Belum menambahkan mode Mushaf, tab halaman/Juz baru atau toggle karaoke. Tampilan
  ayat-terjemahan tidak disebut Mushaf. Tidak menebak baris/halaman atau timing kata.

## Persyaratan konkret tahap berikutnya

1. Sumber resmi yang dapat diakses beserta syarat pemakaian font/gambar/layout dan
   koordinat kata/ayat/baris untuk satu edisi. Referensi foto Al-Mulk mengharuskan
   teks menyambung dalam baris dengan nomor ayat inline, bukan satu kartu per ayat.
2. Timing kata yang cocok dengan reciter dan versi rekaman yang benar-benar diputar.
   Efek emas bertahap wajib memakai posisi player aktual saat seek/pause/buffering.
   Tidak membagi durasi ayat berdasarkan jumlah huruf/kata atau slider gulir baca.
3. Verifikasi mapping nomor global/surah/ayat, batas halaman, RTL, basmalah/waqaf,
   panjang bacaan dan atribusi sebelum menyatakan fitur lengkap.
4. Verifikasi rekaman asli, unduhan lewat jaringan, headset/telepon serta runtime
   API 35/HP fisik setelah akses aktual tersedia. Request yang ditolak cloud tidak
   menyimpulkan provider gagal pada semua perangkat pengguna.
