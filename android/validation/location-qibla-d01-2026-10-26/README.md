# D01 — verifikasi hitungan salat dan kepercayaan sensor Kiblat

26 Oktober2026, beta1.9.10/code37. **D01 masih[ ]**: pemeriksaan sensor/GPS HP
fisik belum dilakukan. [Source/versi/hash](release.json), [native](native-results.json),
[referensi hitungan](reference.json), [preservasi](preservation.json), [audit](audit.json).

## Perubahan dan alasan

Kiblat sebelumnya hanya menilai akurasi ketika ada event sensor baru. Ketika
event berhenti atau callback akurasi turun tanpa event berikutnya, status sejajar
dapat bertahan. `CompassReading` memisahkan keadaan waiting/invalid/stale/tilted/
low accuracy/live dari Activity. Hanya bacaan baru dan memadai yang boleh memberi
status sejajar/haptik; callback akurasi rendah segera membatalkannya.

Umur bacaan dihitung dari timestamp monotonic milik event. Batas internal3detik
dan pemeriksaan500ms mengembalikan diagram statis bila bacaan terhenti. Ini
kebijakan aplikasi, bukan angka akurasi/jaminan sensor dari Android. Vektor tidak
finite/tidak wajar, timestamp lama/masa depan dan sampel tak berurutan ditolak.
Kemiringan lebih45° dari posisi mendatar memerlukan pengguna meratakan ponsel.
Smoothing sudut, hysteresis5° masuk/8° keluar dan pembatasan haptik tetap.
Pause melepas sensor/timer; resume menunggu bacaan baru. Dialog kalibrasi ditutup
saat Activity dihancurkan. Diagram statis dilabeli jelas, termasuk deskripsi aksesibel.

Perhitungan salat tetap metode yang dinyatakan aplikasi: Subuh20°, Isya18°,
Ashar bayangan1×, horizon-0,833°, pembulatan ke atas dan ihtiyati2menit kecuali
terbit, mendekati permukaan laut. **Bukan feed jadwal resmi Kemenag.** Pembanding
menemukan Ashar kutub saat pusat matahari tidak mencapai horizon positif;
nilai itu kini tidak dibentuk dan ditampilkan“—”. Tidak menerapkan aturan fiqh
penyesuaian lintang tinggi secara diam-diam.

## Pembuktian hitungan

Generator [tools/generate_prayer_d01_reference.py](../../../tools/generate_prayer_d01_reference.py)
memakai **Astral3.2**, bukan memanggil fungsi matahari aplikasi.45 tanggal-kota
meliputi Jakarta/Surabaya/Makassar/Jayapura/London/New York/Sydney/Apia/Tromso,
empat musim2026 dan29Feb2028.270 peristiwa diperiksa:261 nilai waktu dalam
toleransi2menit,9 keadaan peristiwa tidak tersedia cocok. Metode/refraction dan
hash generator, fixture, serta source Astral dicatat pada reference.json.
Fixture berada di test resources dan tidak dikemas ke APK.

Ini pembanding implementasi astronomi terpisah yang masih memakai keluarga
rumus terkait, bukan model astronomi sepenuhnya independen, penilaian fiqh atau
jaminan waktu pengamatan. Oracle Ashar memakai akar elevasi terhadap target
bayangan; waktu untuk deklinasi dievaluasi di UTC. Referensi awal gagal pada
kutub musim dingin sebelum guard keberadaan matahari ditambahkan pada aplikasi
dan oracle. Percobaan awal juga sempat menjalankan generator dari direktori
yang salah sehingga fixture belum ditemukan; sudah diperbaiki sebelum build akhir.

Rujukan primer:

- [NOAA calculation details](https://gml.noaa.gov/grad/solcalc/calcdetails.html):
  asal persamaan dan batas kondisi atmosfer/perhitungan. Laman kalkulator NOAA
  menyatakan tidak lagi dipelihara aktif; hasil tidak dipakai sebagai layanan produksi.
- [Astral API](https://astral.readthedocs.io/en/latest/package.html#astral.sun.time_at_elevation):
  elevation/time-at-elevation/noon dan pilihan refraction eksplisit.
- [Android SensorEvent](https://developer.android.com/reference/android/hardware/SensorEvent#timestamp):
  timestamp event memakai acuan elapsedRealtimeNanos, bukan jam kalender.
- [Android position sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_position):
  acuan utara magnetik, orientasi dan remap. Koreksi deklinasi ke utara sejati tetap
  memakai GeomagneticField; ketepatan lapangan belum dibuktikan.

## Pengujian aplikasi

- Unit134 tercatat:130 lulus,4 skip,0 failure/error. Tujuh tes baru memeriksa
  umur/akurasi/kemiringan/invalid/wrap/hysteresis serta referensi270 peristiwa.
  Regresi jadwal/lokasi/tugas/kelas lain tetap dijalankan.
- Build aplikasi/helper dan lint lulus,0 error/770 warning. Warning proyek belum
  seluruhnya diselesaikan. [Build](build.txt), [unit terarah](targeted-unit-corrected.txt).
- Native5 metode/6 eksekusi lulus di emulator: izin fine/coarse benar-benar
  ditolak dan Wi-Fi/data dimatikan, tetapi kota Makassar/Jayapura/Jakarta dapat
  dipilih/disimpan/recreate; Prayer/Qibla/Home mengikuti kota yang sama.
  Baris salat cocok hitungan tanggal/zona aktif, label metode tetap jujur.
- Pilihan tanggal/pekan bertahan setelah recreate. Keadaan akurasi rendah,
  tilted/invalid dan berhentinya pembaruan mematikan klaim sejajar. Watchdog
  berjalan nyata sambil input sensor dihentikan; heading/alignment awal memakai
  **injeksi terkontrol**, bukan pembuktian orientasi fisik.
- Keadaan sensor tidak tersedia dipaksa pada fixture, bukan membuktikan perangkat
  tanpa sensor sungguhan. Kalibrasi/scroll/tombol dialog diperiksa pada1080×2400
  font100% dan640×1280 font200%. Tangkapan200% tidak selalu memperlihatkan semua
  isi sekaligus; pemeriksaan membuktikan kontrol dapat dijangkau dengan gulir,
  bukan kelulusan aksesibilitas seluruh aplikasi/F03.

## Preservasi, keamanan dan sisa gerbang

HP tidak disentuh. Baseline emulator10 berkas semuanya0byte dicadangkan privat,
diisolasi sebelum fixture dan dipulihkan cocok hash. Ini bukan bukti upgrade data
berisi di HP. Izin fine/coarse/notifikasi/exact, jaringan, font/IME/wake dan tema/
ukuran dipulihkan dan diverifikasi; helper dihapus dan emulator ditutup.
Tidak ada provider inference, unggahan lokasi pengguna atau perubahan hosting/key.

Audit nilai rahasia yang dikenal source/evidence/APK nol temuan; bukan sertifikasi
seluruh sejarah Git. WIP PROJECT_HANDOFF.md, stash/arsip audio, env, desain Home,
source AiActivity dan APK sebelumnya dipertahankan. SQLite5/chat1/jadwal2 tetap.
Perbandingan isi APK1.9.9→1.9.10 juga memeriksa96 entri gambar/audio/font bersama:
tidak berubah atau hilang. [Receipt media APK](apk-media-preservation.json).
APK debug di bawah100.000.000byte dan signer sama; belum dipasang HP/publik.

Sisa D01: HP nyata untuk izin approximate/GPS, magnet/kalibrasi, arah terhadap
acuan yang diketahui dan empat rotasi layar. Sensitivitas watchdog/kemiringan
perlu diperiksa pada sensor vendor. Belum memvalidasi seluruh tanggal/lokasi dunia
atau mengganti sumber jadwal menjadi otoritas resmi. D02/D02a adzan, D03 audio,
D05 Hifz/jurnal, D06 upgrade menyeluruh, C02/C04/C07/B01 dan publik tetap terbuka.
