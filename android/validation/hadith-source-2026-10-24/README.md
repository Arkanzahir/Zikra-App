# B01 — Pemeriksaan awal sumber Hadis

24 Oktober 2026. **B01 masih aktif, belum dicentang.** [A02](../../../RELEASE_SCOPE.md) menetapkan Bukhari/Muslim sebagai koleksi rilis pertama; [plan](../../../IMPLEMENTATION_PLAN.md) tetap mengharuskan sumber, edisi, penomoran, penggunaan dan cakupan diperiksa sebelum impor distribusi.

## Pemeriksaan nyata

Kandidat Gading diperiksa pada commit tetap **`8e15b4f9e7585822426a0d470e8dfec27e2a1707`**, bukan hanya branch bergerak. Dua JSON berhasil dibaca HTTP 200; checksum sama dengan pemeriksaan A01. [Metadata, contoh pendek respons, nomor dan hash](source-audit.json). Tidak ada seluruh teks kitab yang ditambahkan ke repo, server atau APK; tidak menjalankan build/emulator.

| Dataset | Entri / nomor unik | Rentang nomor sumber | Nomor tak muncul dalam rentang | Nomor tidak sama dengan posisi baris |
| --- | --- | --- | --- | --- |
| Bukhari | 6.638 / 6.638 | 1–7.008 | 370 | 3.661 |
| Muslim | 4.930 / 4.930 | 1–5.362 | 432 | 4.883 |

Nomor berupa integer, tanpa duplikat nomor pada masing-masing kitab. Semua baris mempunyai string `arab`/`id` tidak kosong. Ini pemeriksaan bentuk data, **bukan pembuktian ketepatan seluruh teks/terjemahan atau kelengkapan kitab**. Celah nomor tidak otomatis berarti teks hilang; penyebab dan edisi perlu diketahui.

Field `id` adalah teks Indonesia, bukan identitas record. Simpan identitas seperti `source/edition/book/number`; jangan memakai urutan baris atau mencampur nomor dari edisi berbeda.

## Temuan penting untuk integrasi

[Model API sumber](https://github.com/gadingnst/hadith-api/blob/8e15b4f9e7585822426a0d470e8dfec27e2a1707/api/Models/Hadith.ts) menggunakan **posisi array** untuk range, sementara pencarian satu nomor membandingkan field `number`. Akibatnya range dan nomor yang tampil dapat berbeda ketika ada celah:

- Baris ke-2.978 Bukhari mempunyai nomor sumber **3.001**.
- Baris ke-48 Muslim mempunyai nomor sumber **49**.

Ini hasil pemeriksaan source dan isi JSON, bukan klaim respons endpoint range telah diuji live. Integrasi Zikra jangan menampilkan posisi range sebagai nomor Hadis, jangan menolak nomor sah karena lebih besar dari jumlah entri, dan jangan membuat entri rekaan untuk mengisi celah. Query nomor/paging harus memakai pemetaan yang eksplisit. Pembaruan paket harus dipatok versi/hash dan menjaga nomor asli.

## Asal data dan penggunaan

- [README Gading](https://github.com/gadingnst/hadith-api/blob/8e15b4f9e7585822426a0d470e8dfec27e2a1707/README.md) menyatakan sumber tafsirq.com serta Arab/Indonesia; [crawler](https://github.com/gadingnst/hadith-api/blob/8e15b4f9e7585822426a0d470e8dfec27e2a1707/crawler/script.ts) membaca halaman HTML Hadis. [LICENSE](https://github.com/gadingnst/hadith-api/blob/8e15b4f9e7585822426a0d470e8dfec27e2a1707/LICENSE) adalah MIT untuk software. Identitas edisi/penerbit/terjemahan serta ketentuan konten upstream belum ditemukan dalam bukti yang diperiksa. Ini kekurangan bukti asal/penggunaan, bukan kesimpulan hukum bahwa isinya tidak boleh digunakan.
- Halaman upstream tafsirq.com tidak berhasil diakses melalui alat web pada pemeriksaan ini. Jangan menyimpulkan halaman tidak ada atau layanan mati untuk semua pengguna. API host Gading juga sebelumnya gagal TLS/timeout dari lingkungan ini, sementara raw GitHub berhasil.
- Alternatif [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api) pada branch `1` mengarah ke commit **`df57907be35291c91ad6a6691180e22ca9920784`**. [Lisensinya](https://github.com/fawazahmed0/hadith-api/blob/df57907be35291c91ad6a6691180e22ca9920784/LICENSE) Unlicense software; [References](https://github.com/fawazahmed0/hadith-api/blob/df57907be35291c91ad6a6691180e22ca9920784/References.md) juga menyebut Gading di antara banyak sumber. Jadi alternatif ini tidak otomatis pembanding independen atau menyelesaikan asal terjemahan Indonesia. Dataset edisi alternatif belum diaudit pada putaran ini.

## Keputusan checkpoint

**Gading dipertahankan sebagai kandidat teknis, belum ditetapkan sumber produksi/kitab lengkap.** B01 belum memenuhi syarat untuk ditutup; B02–B04 belum diimplementasikan. Tujuh kutipan lokal tetap jumlah Hadis APK saat ini.

Pekerjaan B01 berikutnya:

1. Cari identitas edisi/asli terjemahan dan ketentuan penggunaan konten yang dapat ditautkan; bandingkan sumber alternatif dengan asal yang jelas.
2. Cocokkan sampel awal, sekitar celah dan akhir kedua kitab ke edisi rujukan; dokumentasikan perbedaan pemecahan/penggabungan nomor, jangan menambah teks dengan AI.
3. Pilih sumber final serta kontrak ID/pembaruan; catat atribusi, cakupan yang benar-benar tersedia dan keadaan nomor tidak ditemukan. Baru kemudian centang B01 dan mulai B02.

Tidak ada pengiriman pesan ke pengelola sumber atau perubahan layanan eksternal pada pemeriksaan ini. Jika konfirmasi langsung diperlukan, siapkan pertanyaan konkret terlebih dahulu; delegasi menentukan cakupan bukan izin menghubungi pihak lain.
