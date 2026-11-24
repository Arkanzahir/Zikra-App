## Tinjauan dokumen penyedia — 24 November 2026

| Jalur aktif | Bukti penyedia | Batas untuk persetujuan rilis |
|---|---|---|
| Firebase Authentication | IP yang tercatat disimpan beberapa minggu. Informasi autentikasi lain disimpan sampai penghapusan dimulai. Setelah itu, Firebase menyebut hingga 180 hari untuk sistem aktif dan cadangan. [Privasi Firebase](https://firebase.google.com/support/privacy) | Uji hapus akun di Preview dan verifikasi sesi lama masih terbuka. |
| Cloudflare Workers AI | Konten inferensi standar hanya diproses selama diperlukan untuk memberi layanan. [Data usage](https://developers.cloudflare.com/workers-ai/platform/data-usage/) | Ketentuan ini berlaku khusus Workers AI. Retensi metadata dan konfigurasi lain belum dikonfirmasi. |
| Vercel Hobby | Runtime logs satu jam. Traces pada Hobby dapat disimpan tiga hari. [Runtime logs](https://vercel.com/docs/logs/runtime), [Tracing](https://vercel.com/changelog/vercel-tracing-is-now-generally-available) | Tidak menyimpulkan sampling, akses atau retensi drain dan log lain dari angka paket. |
| Upstash | [Dokumentasi Redis](https://upstash.com/docs/redis/features/backup) membedakan cadangan langsung dan terjadwal dari data aktif. | TTL aplikasi tidak berlaku otomatis pada cadangan. Status cadangan proyek belum diverifikasi. |

Draf privat pada situs mencantumkan batas ini. Pemeriksaan dokumen vendor tidak
menutup pemeriksaan konfigurasi akun atau persetujuan privasi. Untuk AI online,
gunakan aturan akun 18+ yang tercatat di [status aktif](CURRENT_STATUS.md).
Pembahasan AI remaja di bawah berasal dari cakupan lama.

## Akses akun dipulihkan — 15 November 2026

Login CLI diperbarui pengguna; metadata proyek/tim kembali200 dan paket Hobby terverifikasi. Ini menggantikan hambatan403 di bawah. Retensi drain/backup/seluruh lapisan masih belum terverifikasi; tidak mengklaim nol log. Laporan aplikasi aktif dengan TTL7hari, diuji sintetis.

## Pemeriksaan akun15November2026 — BLOCKED

Probe hanya-GET ke metadata tim/proyek Vercel menggunakan sesi CLI yang sudah
ada mendapatHTTP403 pada kedua permintaan. Dashboard browser meminta login.
Paket/fitur/drain/backup dan retensi akun aktif **belum terverifikasi**; metadata
Free lama24Oktober tidak menjadi bukti paket15November. Tidak membaca isi log,
mengubah akses/TTL/paket, membuat token atau menghapus data produksi.
Bukti tersanitasi ada pada [receipt audit](../../android/validation/product-audit-2026-11-15/README.md).
Dokumentasi vendor14November di bawah tetap rujukan umum, bukan konfigurasi akun.

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

## Peninjauan kontrol1.9.51

[Pemeriksaan terarah](PRIVACY_SECURITY_REVIEW.md) memisahkan revokasi izin
konten dari penghapusan chat/registry/salinan penyedia. Revisi izin disimpan
lokal tanpa timestamp/nama dan tidak dikirim sebagai profil pengguna. Request
yang sudah diterima penyedia tidak dapat ditarik oleh guard antrean. Status
retensi dashboard/log/backup tetap belum final; tidak mengubah TTL/produksi.
[Tinjauan remaja](TEEN_PRIVACY_REVIEW.md) tetap membutuhkan mekanisme usia/wali
dan peninjauan lokasi presisi; basicconsent bukan persetujuan wali.
