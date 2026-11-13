# Tinjauan retensi penyedia — 14 November 2026

Peninjauan dokumentasi primer, bukan audit dashboard/kontrak seluruh akun.
Tidak mengubah paket, konfigurasi log, backup, TTL produksi atau menghapus data.
Angka di sini tidak boleh dijadikan janji seluruh lapisan nol log.

| Layanan | Yang terverifikasi dari sumber primer | Yang masih harus diperiksa pada akun |
|---|---|---|
| Workers AI | Inferensi standar memproses konten selama diperlukan untuk request/response dan tidak menggunakannya untuk melatih model. Fitur stateful/log/cache mempunyai lifecycle sendiri; metadata operasional serta pengecualian keamanan/hukum tetap ada | Konfigurasi aktif, integrasi lain dan retensi metadata; tidak ada angka universal penghapusan seluruh metadata |
| Vercel Runtime Logs | Dokumentasi mencantumkan retensi menurut paket: Hobby1jam,Pro1hari,Enterprise3hari; opsi Observability Plus dapat berbeda | Paket/fitur akun aktif, log drains, build/security/access logs dan salinan di layanan lain. Tabel Runtime Logs saja bukan seluruh retensi Vercel |
| Upstash Redis | TTL key aplikasi berbeda dari backup. Dokumentasi menjelaskan backup langsung/terjadwal serta penghapusan backup; pilihan daily backup1atau3hari | Apakah backup aktif, daftar backup dan akses anggota akun. Menghapus satu key tidak membuktikan seluruh salinan ikut hilang |

Sumber yang dibuka pada14November2026:

- [Cloudflare Workers AI data usage](https://developers.cloudflare.com/workers-ai/platform/data-usage/).
- [Vercel Runtime Logs](https://vercel.com/docs/logs/runtime).
- [Upstash Backup/Restore](https://upstash.com/docs/redis/features/backup).

Retensi yang terlihat pada kode gateway dicatat di [draf privasi](PRIVACY_DRAFT.md).
Penghapusan registry/usage instalasi teruji lokal melalui
[prosedur operator](INSTALLATION_ERASURE.md), bukan API penghapusan otomatis
semua data penyedia. Sertakan sumber/akun/fitur/waktu dan batas pada bukti
permintaan nyata; jangan mengisi durasi yang belum terverifikasi.

Sasaran beta dipilih pengguna: **13+**, termasuk remaja13–17tahun. Pemilihan
sasaran bukan verifikasi usia/izin orang tua. Peninjauan ketentuan layanan untuk
remaja, alur pendampingan/persetujuan dan penjelasan publik masih gate sebelum
undangan remaja. Tidak membuat tanggal lahir atau identitas palsu untuk uji.
