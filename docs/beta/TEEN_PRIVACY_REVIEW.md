# Tinjauan privasi beta13+ — 14 November 2026

**Status: belum final, tetap HOLD.** Sasaran13+ dipertahankan sesuai pengguna;
penetapan sasaran bukan bukti usia, izin wali, atau kelayakan seluruh layanan.
Tidak mengubah beta menjadi18+, mengumpulkan identitas anak, mengundang penguji,
menyetujui ketentuan, atau menghapus data produksi dalam peninjauan ini.

## Sumber primer dan batas interpretasi

[PP17/2025, teks resmi](https://peraturan.bpk.go.id/Download/377994/PP%20Nomor%2017%20Tahun%202025.pdf)
membahas pengguna di bawah18tahun, penilaian risiko, privasi tinggi bawaan,
persetujuan orang tua/wali, geolokasi presisi dan pemrofilan. Pasal9 membedakan
mekanisme pengguna17tahun; Pasal49 memuat penyesuaian paling lama dua tahun.
Peninjauan penerapan, aturan pelaksana terbaru dan proses pembuktian belum selesai;
ini bukan penetapan kepatuhan hukum atau alasan menunda perlindungan.
[Status pada JDIH BPK](https://peraturan.bpk.go.id/Details/316698/pp-no-17-tahun-2025).

[Workers AI data usage](https://developers.cloudflare.com/workers-ai/platform/data-usage/)
menjelaskan inferensi standar, pengecualian penyimpanan stateful, metadata operasi,
dan tidak memakai konten untuk pelatihan. Itu bukan jaminan seluruh rantai nol log
atau bukti layanan/model cocok untuk remaja.
[Perjanjian Cloudflare](https://www.cloudflare.com/terms/) dan
[ketentuan Developer Platform](https://www.cloudflare.com/service-specific-terms-developer-platform/)
tetap harus dipadankan dengan akun, model dan penggunaan aktual. Tidak menyetujui
ketentuan baru dalam sesi ini. [Retensi](RETENTION_REVIEW.md) tetap berbatas akun.

## Matriks keputusan yang masih terbuka

| Area | Fakta implementasi | Pekerjaan sebelum undangan remaja |
|---|---|---|
| Usia | Belum ada verifikasi usia; tingkat sekolah/role bukan bukti usia | Tentukan mekanisme proporsional, perlakuan13–17 dan penanganan di bawah13; hindari DOB/KTP/wajah sebagai syarat bawaan |
| Wali | Izin AI adalah persetujuan berbagi data oleh pemakai, bukan bukti wali | Alur pemberitahuan, pembuktian, penolakan/pencabutan dan catatan minimum; review proses sebelum dibuatkan UI |
| Lokasi | Lokasi presisi dapat disimpan/dikirim ke cuaca/pencarian/geokode setelah tindakan pengguna; kota manual tersedia | Telaah pembatasan geolokasi untuk remaja dan jalur kota manual/coarse. Izin Android saja tidak menutup isu ini |
| AI | Backend memproses isi pesan/lampiran terpilih; konteks tambahan opt-in | Evaluasi keselamatan model, lampiran sensitif, penanganan insiden, pengawasan sesuai usia dan ketentuan model |
| Retensi/penghapusan | Hapus chat lokal berbeda dari revokasi/erase registry dan salinan penyedia | Verifikasi pemohon/wali, retensi akun/log/backup, akses petugas, bukti penyelesaian dan batas layanan |
| Cadangan | Lokal terenkripsi; consent/token/OSgrants tidak ikut | Jangan mengimpor persetujuan wali sebagai checkbox lokal atau mengaktifkan ulang izin secara otomatis |

## Rancangan yang perlu diputuskan, belum dieksekusi

- Pertahankan target13+, bukan menebak usia dari nama/kelas atau mengubah usia
  pengguna lama. Jangan mengumpulkan dokumen identitas hanya untuk meloloskan beta.
- Pisahkan izin berbagi pesan, persetujuan orang tua/wali, izin Android, dan
  pilihan data tambahan. Masing-masing punya cakupan dan pencabutan berbeda.
- Tentukan bukti wali dan pengecekan layanan di backend sebelum memberi akses AI
  kepada penguji remaja. Flag lokal/OTP email saja belum dibuktikan memadai.
- Gunakan batas data minimum; jangan meminta GPS latar belakang atau mengirim
  seluruh profil/kalender/chat otomatis. Pertimbangkan pilihan kota manual untuk
  jalur remaja, tanpa mengubah jadwal/lokasi pengguna secara diam-diam.
- Tulis penjelasan sederhana untuk penguji dan wali: data lokal, penerima online,
  hal yang tidak boleh dikirim, batas AI, cara mencabut dan meminta penghapusan.
- Tinjau skenario usia tak diketahui/di bawah13/13–17/dewasa, bukti kedaluwarsa,
  penolakan/pencabutan, pergantian perangkat dan restore tanpa membawa consent.

## Yang sudah diperkuat pada1.9.51

[Receipt dan batas](../../android/validation/privacy-consent-2026-11-14/README.md):
izin chat terikat layanan/credential/revisi, diperiksa saat meninjau dan sebelum
transport antre berjalan. Mencabut lalu menyetujui lagi tidak membangkitkan request
lama. Konfirmasi gambar terpisah perprompt, terikat layanan/credential/revisi;
tidak memberi izin chat diam-diam. Balasan chat yang tiba sesudah revokasi tidak
ditampilkan. Request yang sudah mencapai penyedia tidak dapat ditarik kembali.
Izin valid versi sebelumnya tetap sah; tidak ada dialog setiap pesan.

Ini memperbaiki kontrol teknis berbagi data, bukan mekanisme persetujuan wali.
J11/G03, teenPrivacyFinalized dan keputusan publikasi tetap terbuka.
