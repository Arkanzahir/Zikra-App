# Kandidat signed Zikra 1.9.67

24 November 2026. Kandidat code94 berstatus **HOLD**. APK dibangun dari arsip
Git `2414b876a0034ff294e812f5e0d90530b7e3887d` dengan perubahan versi pada
`android/app/build.gradle`. Build release memakai
`zikraReleaseAdultAccounts=true` dan `zikraReleaseAccountDeletion=true`.
Pemeriksaan BuildConfig memastikan Auth Emulator nonaktif.

| Pemeriksaan | Hasil |
|---|---|
| Tombol Hapus Akun | APK signed pada emulator API 36 memperlihatkan tombol setelah masuk. Batal dan sandi salah menghasilkan nol permintaan hapus. Sandi benar menghasilkan satu permintaan, akun dihapus, layar kembali ke kondisi keluar, dan Firebase menolak login ulang. Satu uji native lulus dalam 10,599 detik. |
| Staging | Instrumen mengganti proyek Firebase dan mengalihkan hanya request hapus ke deployment Preview milik Zikra. Firebase Admin mengonfirmasi identitas sintetis hilang, Redis uji kosong, dan cookie akses lama ditolak dengan 302 setelah pencabutan tersebar. |
| JVM dan lint | 248 tes release lulus tanpa skip. Lint mencatat 0 error dan 913 warning. |
| Upgrade dan Murottal | Update signed 1.9.66 → 1.9.67 pada emulator API 36 menjaga waktu pemasangan awal dan empat nilai baca sintetis. Dua uji Murottal lulus dalam 18,498 detik, mencakup memutar ulang ayat yang tersendat dan menjaga unduhan offline saat cache sorotan kata kedaluwarsa. |
| Paket dan signer | SHA-256 dan ukuran cocok dengan [manifest](../../../docs/beta/internal-artifact.json). Sertifikat sama dengan 1.9.66. Audit menemukan 0 konfigurasi privat dan 0 kecocokan kredensial yang dipindai. Dua rekaman adzan bawaan cocok dengan hash yang telah tercatat. |
| Saluran unduhan lokal | HEAD dan GET HTTPS loopback mengembalikan 200. Sertifikat TLS diverifikasi. Ukuran, tipe APK, dan SHA-256 sesuai manifest. |

Kandidat privat berada di `.local/releases/Zikra-v1.9.67-beta-candidate.apk`.
Instrumen uji tombol memiliki SHA-256
`b6c38b018322f8ae150ae07ff77290861d800c0bd91882474034a197d756de97`.
Produksi belum memiliki flag dan Secret Hapus Akun. Uji HP, aktivasi
produksi, dan verifikasi URL publik tetap tercatat di
[persyaratan rilis](../../../docs/beta/PUBLIC_RELEASE_BLOCKERS.md).
