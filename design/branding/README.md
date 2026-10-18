# Satu master branding Zikra

Master tunggal: [`zikra-logo-primary.jpg`](zikra-logo-primary.jpg). Bentuk bulan sabit, buku terbuka dan **spark** dipertahankan. SHA-256 master: `7fa6744a626bbb27e1d944c18090730a64d37658a0e8f5aaea5212f14d52d963`. Master tidak digambar ulang.

## Turunan dan proporsi

Varian Android terpusat di `android/app/src/main/res/drawable-nodpi/`:

- `zikra_symbol_pine.png`: Pine **#15332F**, latar transparan.
- `zikra_symbol_white.png`: putih, latar transparan untuk permukaan gelap.

Keduanya memakai **mask alpha identik**, kanvas1024×1024 yang sama, dan posisi/proporsi simbol yang sama dari master. Spark tidak dihilangkan. Dibuat dengan konversi piksel deterministik yang diizinkan pengguna; perintah reproduksi `python3 tools/branding/derive_logo.py` memerlukan Pillow/NumPy. Tidak ada tracing atau logo substitusi.

`view_zikra_brand.xml` adalah komponen bersama untuk **simbol + tulisan “zikra.”**. Gunakan theme overlay `ZikraBrandLight` atau `ZikraBrandDark`; jangan mengganti simbol dengan `ic_moon`, `ic_spark` atau ilustrasi dekoratif. Jangan meregangkan bitmap: selalu `fitCenter`, tanpa tint berbeda atau pemotongan spark. Ukuran keseluruhan boleh diskalakan seragam.

## Penerapan

| Tempat | Aturan |
| --- | --- |
| Splash | Komponen lengkap putih pada Pine; Android12+ juga memakai simbol yang sama pada system splash |
| Onboarding | Komponen lengkap putih di kiri atas |
| Home | Komponen lengkap putih ringkas; lokasi pada baris di bawahnya |
| Tentang | Komponen lengkap Pine pada dialog terang |
| Ikon aplikasi | Simbol putih saja, latar Pine; adaptive icon memakai master yang sama, tanpa wordmark |
| Halaman fitur | Judul dan tombol kembali; ikon tindakan boleh sesuai fungsinya |
| Profil | Avatar/initial pengguna, tidak diganti logo aplikasi |

Ilustrasi taman onboarding adalah aset adegan baru di `onboarding_garden.png`, bukan logo dan bukan pengganti master. Aset adegan dibuat ulang berdasarkan brief/screenshot percakapan; tidak diklaim sebagai file asli dari pengerjaan lama. Awan dan gerakan dirender terpisah oleh Canvas native.
