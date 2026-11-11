# C02: penolakan antrean teramati — 11 November 2026

Log produksi menunjukkan tiga assistant_rejected/429/try_later pada09.34.17,
09.34.19 dan09.34.27WIB. Pada source aktif, try_later berasal dari pemeriksaan
lease gateway sebelum inference; bukan kode provider_busy atau kuota harian
Cloudflare. Metadata tidak membuktikan setiap request berasal dari HP ini,
tetapi memberi kategori penolakan nyata yang belum tersedia sebelumnya.

Pada09.37WIB, snapshot read-only tidak menemukan lease global aktif. Registry
operator memiliki14percobaan hari ini dan0lease, tetapi token operator belum
dicocokkan secara independen ke koneksi HP; angka14bukan klaim penggunaan HP.
Data operator tersebut tetap privat. Belum dapat dibedakan apakah penolakan
tadi berasal dari satu slot instalasi atau dua slot global. Tidak menghapus
lease, mereset counter, mengubah batas atau mengirim inference baru.

Pengguna mengusulkan Groq dan menyebut model Hugging Face. Groq adalah penyedia,
model harus tersedia pada katalognya. Pemeriksaan dokumentasi11November:
Qwen3.8-27B Free memiliki ringkasan30RPM/1.000RPD/8.000TPM/200.000TPD; batas
aktual organisasi perlu diperiksa. Vision model itu menerima sampai3gambar,
masing-masing2.048token input. Mengganti penyedia belum memperbaiki try_later
yang terjadi sebelum provider dipanggil. Tidak ada migrasi Groq, key baru,
perubahan paket, build atau uji HP pada pemeriksaan ini.

Referensi: https://console.groq.com/docs/rate-limits,
https://console.groq.com/docs/models, https://console.groq.com/docs/vision.
C02 tetap[ ], C03b hanya bukti beta terarah sebelumnya; bukan A–D/publik selesai.
B01/D02/D02a/E03/E04/F/G tetap mengikuti checklist. WIP/data/rahasia/aset tetap.
