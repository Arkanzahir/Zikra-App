# Audio adzan — arah yang dipilih pengguna

## Pilihan aktif 27 Oktober — pasangan Subuh dan Mecca

Beta1.9.12 meneruskan pasangan yang sama dengan banner dan kontrol berhenti;
[receipt/tampilan/batas terbaru](../../android/validation/adhan-banner-2026-10-27/README.md).

Pengguna memberikan **fajr_128_44.mp3 untuk Subuh** dan
**mecca_56_22 (1).mp3 untuk Dzuhur/Ashar/Maghrib/Isya**, menggantikan pilihan
Misyari sebelumnya agar karakter rekaman serupa. Subuh tidak lagi menunggu
pilihan file. Nama/pemakaian mengikuti keputusan pengguna, bukan verifikasi
identitas muazin atau lafaz dari metadata.

- Subuh: 2.996.349byte, sekitar3:07; SHA256
  `106a7b2ca4e8f85fb44f7b150d42e7ca11237c961642ad877dd02e45f8add530`.
- Mecca: 1.423.360byte, sekitar3:23; SHA256
  `43ade766272ff9c89c86908959df6ddd7f3a34961897caf09f965a3841e1bcd2`.
  Byte identik dengan file Mecca terdahulu. Semua asli/salinan lama tetap.
- Pemutar lokal, mode per salat, preview/stop, audio focus dan jeda timer disiapkan
  pada beta privat1.9.11. Audio hanya dari aset debug generated dengan properti
  lokal, binary tidak masuk Git/release. [Bukti dan batas terbaru](../../android/validation/prayer-adhan-d02-2026-10-27/README.md).
- D02/D02a tetap[ ] untuk suara/reboot/Doze/vendor HP dan sumber/ketentuan
  redistribusi. Pemilihan file tidak membuktikan hak distribusi atau audit lafaz.

Bagian berikut adalah riwayat yang digantikan keputusan terbaru.

## Riwayat pilihan — Adzan-Misyari-Rasyid.mp3

Pengguna memberikan **Adzan-Misyari-Rasyid.mp3** lalu memilihnya dengan “itu aja
ga si bagus itu”. Berkas ini menggantikan mecca_56_22.mp3 sebagai pilihan aktif
D02/D02a. File Makkah lama tetap disimpan sebagai riwayat, bukan diputar bersama
atau dimasukkan otomatis sebagai pilihan kedua. Jangan memaksakan label lokasi
Makkah pada berkas baru tanpa sumber yang membuktikannya.

- Durasi metadata232,0195625detik (sekitar3menit52detik),3.713.152byte,
  128kbps/stereo/44.100Hz menurut Windows Shell.
- SHA256 `545da9c890916ce918ed0e82df9b5e37169aafd638cdb3dfe61d41caa39b016f`.
- Salinan siap integrasi privat:
  `.local/d02-audio-20261026/selected/adhan_misyari_user.mp3`.
  Hash sama dengan sumber; kedua file asli dan salinan Makkah lama tetap utuh.
  selection.json menunjuk pilihan baru, receipt sebelumnya di history/.
- “Misyari Rasyid” berasal dari nama berkas pengguna; identitas muazin, sumber
  distribusi dan lafaz Subuh belum dibuktikan melalui pemeriksaan isi/metadata.
  Jangan menempelkan lisensi kandidat Commons pada berkas ini.
- Belum ada kode pemutar/alarm atau APK baru dari pergantian pilihan audio.
  D02/D02a tetap[ ]; versi khusus Subuh belum dikonfirmasi.

Bagian selanjutnya adalah riwayat keputusan yang sudah digantikan.

## Riwayat pilihan — mecca_56_22.mp3

Pengguna kemudian memberikan **mecca_56_22.mp3** dan meminta “pake ini aja”.
File ini menggantikan kandidat Commons Makkah2013 untuk pekerjaan D02/D02a.
Pilihan bukan lagi sekadar gaya Makkah; berkas spesifik sudah dipilih pengguna.

- Durasi metadata203,2587142detik (sekitar3menit23detik),1.423.360byte,
  MPEG audio56kbps/stereo/22.050Hz menurut Windows Shell.
- SHA256 `43ade766272ff9c89c86908959df6ddd7f3a34961897caf09f965a3841e1bcd2`.
- Salinan siap integrasi disimpan privat di
  `.local/d02-audio-20261026/selected/adhan_makkah.mp3`; hash sama dan file asal
  tidak diubah. Metadata/receipt privat di selection.json pada direktori tersebut.
- Nama muazin, asal rekaman/izin distribusi dan keberadaan lafaz khusus Subuh
  belum dibuktikan dari metadata. Jangan menempelkan lisensi kandidat Commons
  pada file berbeda ini. Untuk uji lokal gunakan pilihan pengguna ini; verifikasi
  distribusi tetap bagian gerbang rilis D02. Tidak mengunggah berkas ke layanan lain.
- Belum ada service/alarm pemutar adzan atau APK baru dari pemilihan ini.
  D02/D02a tetap[ ]; file Subuh khusus belum ditentukan/dikonfirmasi.

Bagian kandidat di bawah merupakan riwayat penelitian, bukan pilihan aktif.

26 Oktober2026: pengguna menolak kandidat “Beautiful adhan” unggahan
Adam-synagda, kemudian memilih **adzan Makkah**. Jangan memakai kandidat yang
ditolak sebagai audio bawaan. Pilihan ini menetapkan arah suara/asal, belum
menyetujui satu rekaman atau menetapkan nama muazin tertentu.

## Kandidat untuk didengarkan

[Adhan, Great Mosque of Mecca — 21 Januari2013](https://commons.wikimedia.org/wiki/File:Adhan,_Great_Mosque_of_Mecca_-_Jan_21,_2013.webm)
berdurasi sekitar3menit17detik. Deskripsi mencatat rekaman Makkah, pembuat/kanal
Seyfula Islam; nama muazin tidak dicantumkan. Jangan mengatribusikannya kepada
Ali Ahmad Mulla atau muazin lain tanpa bukti identitas.

Halaman sumber mencantumkan CC BY3.0 dan pemeriksaan lisensi manusia pada
19Juli2018. Untuk distribusi turunan, simpan nama sumber, URL, lisensi dan catatan
perubahan yang sebenarnya. Kejelasan/kebisingan, kelengkapan lafaz dan kecocokan
selera pengguna belum dinilai dari audio; metadata tidak membuktikan kualitas.
Halaman sumber menyediakan pemutar untuk preview; belum ada file ditambahkan
ke APK atau diputar dalam pengujian Zikra.

[Katalog Makkah di Assabile](https://www.assabile.com/adhan-call-prayer/makkah)
juga tersedia sebagai referensi gaya/suara. Ketersediaan unduh di katalog tidak
dipakai sebagai bukti izin redistribusi ke APK.

## Batas implementasi D02/D02a

- Utamakan rekaman Makkah yang jelas dan lafaz utuh. Periksa isi sebelum aset
  dikunci; jangan mengganti rekaman dengan suara sintetis secara diam-diam.
- Subuh perlu rekaman khusus Subuh yang juga sesuai arah Makkah. Sumbernya belum
  dipilih; jangan menganggap kandidat adzan biasa sudah cocok untuk Subuh.
- Setelah dipilih, kemas audio agar pemutaran salat tidak bergantung jaringan.
  Hindari memasukkan video penuh. Ukuran awal APK tetap di bawah100.000.000byte.
- Preview/audio dipilih tidak menutup D02/D02a. Mode adzan/notifikasi/getar/mati,
  master, stop, audio focus, penyimpanan, reboot dan uji HP tetap persyaratannya.

Tidak ada perubahan kode, APK, rahasia, hosting, aset desain atau data pengguna
pada keputusan ini. D01 masih menunggu validasi fisik; D04 selesai beta.
