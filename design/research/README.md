# Riset dan keputusan kegunaan — 19 Oktober 2026

Audit dilakukan sebelum implementasi ronde ini. Laporan `*-audit.md` adalah temuan pada baseline, bukan daftar bug yang masih terbuka setelah perbaikan. Hasil implementasi dan bukti pengujian berada di [validasi ronde ini](../../android/validation/quality-2026-10-19/README.md).

## Referensi yang benar-benar diperiksa

- Gambar referensi pengguna di `design/references/`: katalog/reader Suhayel, halaman ibadah, Kiblat, BMKG dan asisten; kolase onboarding 2D dari chat.
- [Android: membuat aplikasi lebih mudah diakses](https://developer.android.com/guide/topics/ui/accessibility/apps): target sentuh minimal 48 dp, label tindakan, teks/kontras dan struktur informasi.
- [Android 14: pembesaran font 200%](https://developer.android.com/about/versions/14/features#non-linear-font-scaling): ukuran `sp`, tinggi yang dapat berkembang dan uji ukuran besar. Runtime API 35 belum diuji dalam ronde ini.
- [WCAG kontras](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html) dan [pembesaran teks](https://www.w3.org/WAI/WCAG22/Understanding/resize-text.html) menjadi acuan desain, bukan sertifikasi kesesuaian menyeluruh aplikasi native.
- [Structured](https://structured.app/) memberi konteks produk tentang rencana, urutan aktivitas dan fokus; halaman teks dapat diakses. Ini bukan hasil pengujian produk tersebut.
- Dokumentasi BMKG, Open-Meteo, perizinan/alarm Android dan sumber Quran diperiksa pada audit masing-masing fitur.

Catatan akses HTTP di `2026-10-19-web-sources.json`. Material 3 mengembalikan shell JavaScript; Pillars tidak memberi konten yang dapat dibaca dan Tarteel hanya judul. Tidak ada klaim telah melihat atau menyalin UI ketiganya.

## Keputusan yang diterapkan

1. **Utamakan data pengguna:** hari baru kosong; tugas akademik/target hafalan tidak menghilang sesudah tengah malam; status simpan gagal tidak boleh mengklaim berhasil. Data lama dipertahankan.
2. **Satu keluarga tampilan:** header bersama, Pine/ivory/sage, teks pendukung lebih gelap, dock dengan penanda pilihan, ruang konten mengikuti ukuran dock saat font membesar.
3. **Onboarding ringkas:** tiga adegan 2D statis 3:2 memenuhi lebar halaman, dua baris judul, deskripsi singkat, satu CTA dan nomor langkah. Wordmark tanpa titik; simbol master tetap.
4. **Bacaan dapat disesuaikan:** ukuran Arab dan terjemahan terpisah, lompat ayat tervalidasi, pencarian nama Arab tanpa harakat. Teks Quran yang ditampilkan tidak dinormalisasi/diubah.
5. **Kemampuan yang jujur:** asisten berbasis aturan offline, lokasi acuan Sukolilo, sumber/waktu data jaringan terlihat. Request gagal tidak menghapus hasil valid yang masih boleh dipakai.

## Yang tetap memerlukan tahap berikutnya

Uji langsung dengan pengguna lintas usia, perangkat fisik/font besar, sensor, izin/alarm Android modern dan konsumsi baterai. Pilihan kota/GPS, murottal, Mushaf, unduhan Quran penuh, konten doa/hadis tambahan dan AI generatif belum menjadi fitur lengkap. Build dan screenshot bukan bukti seluruh fitur sudah optimal.
