# Zikra AI — Seimbang, disetujui 26 Oktober 2026

Pengguna meninjau referensi interaktif Seimbang/Ekspresif setelah riset mobile AI,
menyetujui hasilnya, meminta teks “Zikra AI bisa keliru. Periksa jawaban penting.”
dihapus, lalu menanyakan kapan eksekusi dimulai. Implementasi diprioritaskan sekarang
dengan varian Seimbang. Acuan ini menggantikan usulan Fokus/Studio terdahulu untuk AI;
Home dan ikon pintasan yang sudah diterima tidak didesain ulang.

## Acuan yang diterapkan

- Latar putih hangat `#FDFDFB`, teks `#20332E`, sekunder `#68766F`, mint tipis
  `#E8F2ED`, hijau tombol `#164F40`; tema malam memiliki pasangan khusus layar AI.
- Header riwayat di kiri, judul/status mode di tengah, chat baru di kanan.
  Area AI memakai layar penuh; tombol Back Android tetap mengembalikan ke layar sebelumnya.
- Sapaan rata kiri, ikon tipis, empat pintasan dalam dua baris. Fitur membuat
  gambar terlihat tetapi tidak aktif ketika layanan online belum tersedia.
- Jawaban asisten menyatu dengan latar; pesan pengguna memakai mint tipis.
  Tombol salin asli tersedia tanpa kontrol penilaian/suara yang belum ada implementasinya.
- Composer multiline menyatukan lampiran, input, tambah/Alat, dikte dan kirim/batal.
  Target tindakan minimal48dp; tampilan tetap menggulir pada teks besar.
- Riwayat: pencarian berisi, baris ringkas, chat aktif, judul/tanggal asli,
  rename dan hapus dengan konfirmasi, status penyimpanan lokal.
- Lampiran: sheet bawah dengan tiga pilihan Kamera/Galeri/Dokumen; rincian format
  dilipat. Memilih tidak mengirim. Preview/hapus/konfirmasi kirim tetap dipertahankan.
- Footer yang ditolak pengguna dihapus; tidak diganti disclaimer lain.
  Konfirmasi dan pilihan konteks saat mengirim tetap merupakan alur privasi fungsional.

## Referensi riset

- [Gemini: pembaruan desain dan interaksi](https://blog.google/innovation-and-ai/products/gemini-app/next-evolution-gemini-app/).
- [Claude Android: contoh mobile dan akses foto/berkas](https://claude.com/resources/articles/android-app).

Keputusan warna, susunan, dan ukuran adalah adaptasi desain Zikra. Tidak mengklaim
fitur/model Gemini atau Claude tersedia di Zikra. Backend, kuota, memori lokal,
model dan izin tidak diubah untuk menyerupai fitur layanan lain.

Hasil native, APK dan batas pemeriksaan dicatat pada
[laporan implementasi](../../android/validation/ai-seimbang-2026-10-26/README.md).
Mockup disetujui bukan berarti seluruh UI04/F/G atau rilis publik sudah selesai.
