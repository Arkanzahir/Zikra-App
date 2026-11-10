# Migrasi Cloudflare — beta 1.9.37, 10 November 2026

Pengguna memilih Cloudflare Workers AI sesudah langganan Featherless berakhir,
dan meminta APK dahulu, HP nanti. Backend produksi sudah READY dan menerima
permintaan sintetis melalui HTTPS. Chat/vision dikonfigurasi ke
`@cf/qwen/qwen3.8-27b`; paket tetap Free, token AI lama dipakai hanya di server.
Tidak mengklaim seluruh A–D, C02 laptop mati atau rilis publik selesai.

## Perubahan

- Adapter server terpisah, endpoint tetap, input/model/account tervalidasi,
  reasoning rendah dan batas output 512 token; tanpa fallback provider berbayar.
- APK mengikat request/respons ke Cloudflare. Izin baru sekali per endpoint,
  izin Featherless lama tidak memberi akses ke penerima baru. Server menolak
  APK lama sebelum mengambil kuota atau meneruskan konten.
- Kuota harian instalasi, alokasi gratis akun, pembatasan per menit dan antrean
  memiliki galat/pesan berbeda. Registry/counter pengguna tidak direset.

## Verifikasi dan batas

| Pemeriksaan | Hasil |
| --- | --- |
| Backend akhir | 86 lulus, 0 gagal/skip, termasuk Redis/Lua |
| Unit Android | 177 lulus, 4 skip, 0 gagal/error |
| Native emulator | 3 metode lulus: client/header/error lama, enam galat Cloudflare, izin penyedia |
| Lint | Lihat `android-checks.json`; tidak mengabaikan warning baseline |
| Gateway lokal → provider nyata | Chat, foto contoh, tiga gambar halaman PDF dan teks hasil ekstraksi DOCX, masing-masing strict pass |
| HTTPS produksi awal | 11/12 pemeriksaan strict lulus; PDF timeout 504 pada 36.210 ms |
| HTTPS ulang manual | 8/9 strict lulus; PDF HTTP 200 membaca kode benar tetapi menambahkan teks di luar format |
| Usulan tugas | Dua sampel hosted menghasilkan add_task tervalidasi, tanpa menyimpan tugas |
| Penolakan penerima lama | HTTP 409, tidak membuat counter penggunaan instalasi uji |
| Endpoint gempa | 401 tanpa auth dan 405 GET tetap; tidak memicu broadcast sintetis |
| Cleanup | Dua identitas uji Redis beserta usage/lease dihapus; 32 berkas emulator pulih cocok hash, setting/izin/helper dipulihkan dan emulator ditutup |

Kegagalan PDF tidak dihapus atau diberi label lulus sempurna. Sampel PDF memakai
tiga salinan gambar fixture yang sama, DOCX memakai teks yang sudah diekstrak;
ini membuktikan jalur provider, bukan picker/parser native dokumen baru atau
ketepatan semua dokumen. Pembuktian parser/picker terdahulu tetap pada C07.
Penggunaan model baru pada HP, kelancaran jaringan HP, OCR kompleks, kepatuhan
format semua jawaban dan C02 laptop mati belum dibuktikan. Tidak ada auto-retry.
Tiga uji native memakai respons sintetis, terpisah dari inference hosted.

Backend awal pernah 83 lulus/1 gagal karena fixture lama menyamakan batas menit
dengan kuota harian; fixture diperbaiki sesuai kontrak baru. Berikutnya 85 lalu
86 tes lulus setelah menambah kasus error HTTP 200. Build/helper awal terhambat
akses cache Gradle; helper akhir berhasil. Riwayat galat ini tidak menandakan
seluruh suite awal lulus. Metadata empat env baru bertipe sensitive tidak dapat
dibaca ulang lewat API; receipt penambahan, manifest deploy, gate startup dan
capabilities hosted menjadi bukti konfigurasi, bukan klaim nilai didekripsi.

## APK dan privasi

APK debug 1.9.37/code 64: **49.377.416 byte**, SHA256
`e65767269d53d91fc126dde387dd7f57c805da7cd35dcfecb283676463e55e94`.
Salinan lokal `D:\Download\Zikra-App-main\Zikra-Cloudflare-v1.9.37-beta-debug.apk`.
Di bawah batas awal 100 MB, signer debug dan dua audio adzan tetap.
Audit seluruh entry APK tak menemukan nilai credential server yang diketahui
atau file konfigurasi privat. Identitas Firebase client memang tersedia untuk
SDK, berbeda dari private key pengirim. Audit bukan bukti keamanan mutlak.

HP tidak diakses atau dipasang ulang. WIP PROJECT_HANDOFF, stash/arsip audio,
env privat lama, logo dan master Home cocok hash. Gambar/desain/cuaca/alarm/
font Quran tidak diubah oleh migrasi ini. E02 cron Cloudflare tidak diubah;
FCM/Redis/secret pemicu tetap. Tidak ada pembelian atau upgrade paket.

## Bukti

- `backend-tests.txt`, `android-checks.json`, `native-attempts.json`
- `gateway-probe.json`, `hosted-probe.json`, `hosted-pdf-retry.json`
- `configuration-added.json`, `live-config.json`, `deployment.json`
- `apk.json`, `preservation.json`

[Runbook penyedia dan privasi](../../../backend/deploy/ASSISTANT_PROVIDER.md).
B01 hak Hadis, C02 laptop mati, D02/D02a hak audio/sisa fisik/vendor, E03/E04,
F/G dan publik tetap mengikuti checklist; UI10/mode orang tua ditunda.
