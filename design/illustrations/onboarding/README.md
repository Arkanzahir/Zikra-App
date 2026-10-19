# Ilustrasi onboarding dan parallax sentuh

Arahan pengguna 18 Oktober 2026: pilih tiga adegan dari kolase 2D yang dibagikan dalam chat;
karakter diam. Karakter berbeda boleh mewakili beragam pengguna Zikra.
Pada 19 Oktober, setelah menyetujui komposisi/copy, pengguna meminta parallax.
Karakter tetap tidak dianimasikan; gambar utuh dan blok teks bergerak sedikit saat interaksi.

| Cerita | Pilihan referensi | Resource Android |
| --- | --- | --- |
| Belajar lebih terarah. | Kiri atas, pelajar berkacamata menulis di meja | `onboarding_study_static.webp` |
| Ibadah tetap terjaga. | Kiri bawah, perempuan bermukena di sajadah | `onboarding_prayer_static.webp` |
| Dekat, ayat demi ayat. | Tengah bawah, laki-laki berpeci membaca Quran dengan rehal | `onboarding_quran_static.webp` |

Aset terpusat pada `android/app/src/main/res/drawable-nodpi/`. Gambar terpisah 1536×1024 (3:2)
disiapkan dengan alat gambar dari panel pilihan, mempertahankan adegan dan gaya garis/warna
referensi; ini bukan crop yang identik piksel dengan kolase kecil. Tidak ada UI, teks atau logo
pada aset ilustrasi. Encoding WebP lossless mempertahankan RGBA dari PNG hasil edit persis;
Pada ronde 19 Oktober 2026, kanvas diperluas dengan alat gambar agar adegan mencapai tepi layar dan lebih menyatu dengan halaman. Karakter tetap statis. Dimensi/ukuran/hash dicatat di [validasi aset](../../../android/validation/quality-2026-10-19/asset-verification.json).

Lebar ilustrasi mengikuti viewport dengan rasio asli. Tidak menggambar ulang aset atau
membuat animasi karakter, tilt, sensor, orbit atau pemutar ulang. Parallax memakai matrix
ImageView dengan pembesaran 4% sebagai ruang geser: pinggir panel terpotong sangat sedikit,
sedangkan kepala/tangan/buku/sajadah tetap terlihat. Pergerakan dibatasi ruang gambar agar
tidak membuka tepi kosong. Saat efek nonaktif, gambar kembali ke geometri `fitCenter` penuh.
Ilustrasi, judul dua baris dan deskripsi dapat menggulung bersama pada font besar.
Navigasi tetap di bawah dengan nomor langkah 01 / 03.
Tidak memuat tiga gambar sekaligus atau atlas frame; gambar aktif disampling sesuai display.

Pembaruan tipografi 19 Oktober: tiap cerita memakai pengantar 11sp, judul 32sp dan
paragraf manfaat 16sp dengan jarak antarbaris. Blok teks berada di tengah sisa ruang
di bawah ilustrasi; pada layar pendek/font besar semua isi tetap bisa digulir,
sementara Lewati dan tombol lanjut tetap tersedia. Kembali di kiri, nomor langkah
di kanan; catatan tanpa akun tampil konsisten. Copy disimpan terpusat di resource
`values/onboarding_static.xml`, tanpa klaim fitur yang masih dalam rencana.

`ui/OnboardingParallax.java` mengamati sentuhan tanpa mengambil alih gesture navigasi/
ScrollView. Gambar bergeser maksimal 7dp horizontal/6dp vertikal (dibatasi overscan),
teks bergerak berlawanan maksimal 2dp. Saat dilepas, keduanya kembali dalam 320ms.
Gulir panjang memberi tambahan pergeseran gambar terbatas. Tidak ada loop idle atau sensor.
`AmbientMotionPolicy` menonaktifkan efek ketika animasi sistem dimatikan atau penghemat
daya aktif; efek direset saat kehilangan fokus, berpindah cerita atau meninggalkan halaman.

Ivory `#FAF7EF` dengan warna dinding ilustrasi diteruskan ke header/status bar, teks/CTA Pine `#15332F`, teks sekunder `#53655D`. Logo master tetap memakai
`view_zikra_brand.xml` di kiri atas. Ilustrasi bukan pengganti logo. Lihat [branding](../../branding/README.md).
Sumber percobaan 3D disimpan sebagai arsip; aset/pemutarnya tidak ikut APK terbaru.
