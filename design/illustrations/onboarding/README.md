# Ilustrasi onboarding statis

Arahan pengguna 18 Oktober 2026: pilih tiga adegan dari kolase 2D yang dibagikan dalam chat;
gambar diam sepenuhnya. Karakter berbeda boleh mewakili beragam pengguna Zikra.

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

Gunakan ImageView `fitCenter`, tanpa memotong kepala/tangan/buku/sajadah, tanpa lingkaran latar,
animasi, float, parallax, tilt, sensor atau pemutar ulang. Lebar ilustrasi mengikuti viewport dengan rasio asli. Ilustrasi, judul dua baris dan deskripsi dapat menggulung bersama pada font besar. Navigasi tetap di bawah dengan nomor langkah 01 / 03.
Tidak memuat tiga gambar sekaligus atau atlas frame; gambar aktif disampling sesuai display.

Pembaruan tipografi 19 Oktober: tiap cerita memakai pengantar 11sp, judul 32sp dan
paragraf manfaat 16sp dengan jarak antarbaris. Blok teks berada di tengah sisa ruang
di bawah ilustrasi; pada layar pendek/font besar semua isi tetap bisa digulir,
sementara Lewati dan tombol lanjut tetap tersedia. Kembali di kiri, nomor langkah
di kanan; catatan tanpa akun tampil konsisten. Copy disimpan terpusat di resource
`values/onboarding_static.xml`, tanpa klaim fitur yang masih dalam rencana.

Ivory `#FAF7EF` dengan warna dinding ilustrasi diteruskan ke header/status bar, teks/CTA Pine `#15332F`, teks sekunder `#53655D`. Logo master tetap memakai
`view_zikra_brand.xml` di kiri atas. Ilustrasi bukan pengganti logo. Lihat [branding](../../branding/README.md).
Sumber percobaan 3D disimpan sebagai arsip; aset/pemutarnya tidak ikut APK terbaru.
