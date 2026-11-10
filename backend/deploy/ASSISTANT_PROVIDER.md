# Penyedia chat Zikra — Cloudflare, 10 November 2026

Pengguna memilih Cloudflare Workers AI untuk menggantikan langganan Featherless
yang berakhir. Android 1.9.37 → HTTPS Vercel → Workers AI menggunakan
`@cf/qwen/qwen3.8-27b` untuk teks dan vision. Gambar generatif tetap memakai
jalur Cloudflare yang sudah ada. Ini layanan online; panduan offline tetap
berbasis aturan, bukan model generatif lokal.

## Konfigurasi server

| Variabel | Nilai atau aturan |
| --- | --- |
| `ZIKRA_AI_PROVIDER` | `cloudflare` |
| `CLOUDFLARE_ACCOUNT_ID` | Identitas akun privat, 32 hex; hanya server |
| `CLOUDFLARE_API_TOKEN` | Token AI yang sudah tersedia, hanya secret store server |
| `CLOUDFLARE_CHAT_MODEL` | `@cf/qwen/qwen3.8-27b` |
| `CLOUDFLARE_VISION_MODEL` | `@cf/qwen/qwen3.8-27b` |
| `ZIKRA_ASSISTANT_FREE_PLAN_CONFIRMED` | `true` hanya setelah operator memeriksa paket Free |
| `ZIKRA_DAILY_LIMIT` | Tetap 100 percobaan per instalasi per hari UTC |

Template mempertahankan konfirmasi Free `false` agar konfigurasi baru berhenti
sampai diverifikasi. Provider/model dipilih server; request klien tidak dapat
memilih URL, model atau key. Tidak ada fallback otomatis ke Featherless atau
paket berbayar. Env Featherless lama tidak dirotasi, dicabut atau dipakai saat
Cloudflare dipilih. Jangan menyalin token Workers cron sebagai token AI.

Batas tetap: 10 permintaan/menit per instalasi, satu aktif per instalasi dan dua
aktif bersama. Free Workers AI menyediakan 10.000 neurons per akun per hari,
dibagi dengan generation gambar dan penggunaan AI lain pada akun yang sama;
bukan 10.000 pesan atau kuota per pengguna. Kuota harian UTC reset pukul
07.00 WIB. Backend tidak mengaktifkan upgrade berbayar.

## Privasi dan pembaruan APK

APK mengirim `X-Zikra-Provider: cloudflare`. Server menolak APK lama tanpa header
atau header penyedia berbeda dengan 409 `provider_changed`, sebelum kuota dan
inference. Pembatalan/rollback tidak boleh diam-diam mengirim pesan ke penerima
lain. Header adalah pengikat protokol, bukan pengganti autentikasi instalasi
atau bukti kriptografis persetujuan manusia.

Izin dasar baru disimpan terpisah dari izin Featherless lama dan diminta sekali
per endpoint. Kiriman berikutnya langsung; konteks profil/tugas/jadwal/riwayat
tambahan tetap opt-in. Chat/draf lama tidak dihapus. Hanya lampiran pilihan
yang dikirim, berupa gambar inline atau teks hasil ekstraksi; tidak mengirim
URL berkas pribadi. `store:false`, tanpa user ID/metadata ke model. Batas tetap
satu berkas 8 MB, PDF tiga halaman, DOCX/TXT 12.000 karakter. Ini konteks request,
bukan indeks RAG permanen atau memori cloud seluruh perangkat.

Dokumentasi Cloudflare menyatakan konten pelanggan tidak dipakai melatih model;
halaman model menyebut zero data retention. Ini tidak membuktikan seluruh
metadata/log operasional setiap layanan nihil. Retensi, kebijakan publik dan
Data Safety tetap gerbang G03.

## Galat dan operasi

| Kode wire | Arti |
| --- | --- |
| `daily_quota_exceeded` | Jatah harian instalasi habis |
| `rate_limited` | Batas sementara per menit |
| `try_later` / `provider_busy` | Permintaan aktif atau kapasitas penyedia |
| `provider_quota_exceeded` | Alokasi gratis akun Cloudflare habis, kode upstream 3036 |
| `provider_changed` | APK/protokol penyedia perlu diperbarui |

Cloudflare 3040 berarti kapasitas sementara. Detail error upstream tidak
ditampilkan ke pengguna. Tidak ada pengulangan otomatis; kegagalan mempertahankan
draf untuk dicoba lagi secara eksplisit. Deadline server tetap 35 detik.

Sebelum deploy: tes backend dengan Redis/Lua, unit/lint Android, audit upload
allowlist dan APK. Sesudah deploy: health, 401 tanpa akses, capabilities,
409 APK lama sebelum quota, serta inference sintetis memakai instalasi uji
terpisah; hapus hanya kunci/lease instalasi uji itu. Jangan mengatur ulang
kuota pengguna. Jangan memasukkan kejadian gempa palsu ke topic broadcast.

Rollback harus eksplisit: pilih provider/model dengan hak akses yang masih
berlaku, deploy ulang, lalu gunakan APK dengan izin penerima yang sesuai.
APK Cloudflare menolak respons Featherless; jangan menonaktifkan gate demi
kompatibilitas. Langganan Featherless yang berakhir bukan fallback andal.

[Bukti dan kegagalan yang dipertahankan](../../android/validation/assistant-cloudflare-2026-11-10/README.md).
Uji HP baru dan C02 saat laptop mati masih tertunda atas pilihan pengguna.

Referensi resmi:
[model](https://developers.cloudflare.com/workers-ai/models/qwen3.8-27b/),
[OpenAI compatibility](https://developers.cloudflare.com/workers-ai/configuration/open-ai-compatibility/),
[data usage](https://developers.cloudflare.com/workers-ai/platform/data-usage/),
[pricing](https://developers.cloudflare.com/workers-ai/platform/pricing/),
[errors](https://developers.cloudflare.com/workers-ai/platform/errors/).
