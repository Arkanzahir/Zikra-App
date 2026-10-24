# C01 — Audit dan persiapan konfigurasi backend

25 Oktober 2026 (WIB). Baseline Git `83312da`; APK tetap **1.7.6 beta / code 24**.
**C01 belum selesai:** pengguna mengonfirmasi key Featherless yang pernah
dibagikan **belum dirotasi**. Persiapan dan pemeriksaan privat lokal selesai;
tidak ada deployment publik atau perubahan data/aplikasi Android.

## Checklist berdasarkan kriteria C01

- [x] Konfigurasi privat yang sudah tersedia dapat dibaca tanpa menampilkan nilainya;
  validasi key/model/token/bind/port dan penolakan bypass TLS diterapkan.
- [x] Server CLI bisa dihentikan lalu dijalankan ulang pada port loopback yang sama;
  dua start pada uji live dan dua start pemeriksaan final berhasil.
- [x] Health 200 dengan payload minimal, akses tanpa token 401; proses uji ditutup.
- [x] Model yang dikonfigurasi terbukti tersedia untuk satu completion sintetis:
  gateway lokal → Featherless → jawaban valid 200. Tidak mencetak ID model/key/reply.
- [x] Key/token saat ini tidak ditemukan dalam file Git pada working tree yang
  diperiksa atau 399 entri APK yang didekompresi; konfigurasi privat tidak dilacak.
  Ini pemeriksaan nilai yang dikenal, bukan bukti seluruh secret historis tidak ada.
- [ ] Rotasi key yang pernah dibagikan, pembaruan env privat, pencabutan key lama
  dan konfirmasi operator tanpa nilai key; completion setelah rotasi masih perlu.
- [ ] C01 keseluruhan; jangan dicentang sebelum syarat rotasi terpenuhi.

Hosting/domain, HTTPS permanen, identitas/token instalasi yang dapat dicabut,
kuota antarreplika dan layanan tetap berjalan saat laptop mati adalah **C02**.
Shared gateway token tetap khusus uji pribadi. Percakapan APK dari HP adalah
C03/C05; tidak dibuktikan oleh pemeriksaan lokal ini. B01 tetap menunggu sumber
Hadis dan rilis publik tetap terbuka.

## Perubahan yang disiapkan

- [Konfigurasi](../../../backend/config.mjs): startup menolak env salah tanpa
  echo nilai; default loopback; token mengikuti format/32–256 karakter Android,
  terpisah dari key provider; model/endpoint tetap ditentukan server.
- [Gateway](../../../backend/server.mjs): `--check-config`, pesan lifecycle generik,
  shutdown SIGTERM/SIGINT dengan batas 40 detik, batas header. Body JSON `null`
  menjadi client error 400; body chunked >48 KiB dapat menerima 413 terkontrol.
  JSON/body provider rusak atau >128 KiB menjadi 502, bukan menyalahkan client
  dengan 400/413; detail exception provider tidak diteruskan.
- [Pemeriksa privat](../../../backend/check-private.mjs): dua proses CLI nyata,
  bind loopback sementara, health/unauthorized, penghentian child dan opsi satu
  completion sintetis. Output hanya status/boolean, tidak ada data pribadi.
- [Runbook](../../../backend/README.md), template env kosong dan
  [template systemd](../../../backend/deploy/zikra-private.service). Template
  belum dipasang; tidak dianggap bukti restart otomatis Linux atau hosting.

## Bukti terarah

| Pemeriksaan | Hasil / bukti |
| --- | --- |
| Baseline sebelum perubahan | 9 tes backend lulus; source `83312da` |
| Tes final tanpa internet/key nyata | **20 lulus, 0 gagal, 0 skip**; [TAP](backend-tests.tap) |
| Konfigurasi privat | `--check-config` exit 0; tidak mencetak nilai konfigurasi |
| Proses dan provider nyata | [Receipt live](private-live-check.json): 2 start, health 200, tanpa token 401, satu completion 200, kedua proses berhenti |
| Verifikasi source final | [Receipt restart final](restart-final-check.json): 2 start, health 200/401, tidak mengulangi request berbayar |
| Secret dan preservasi | [Hasil pemindaian](secret-and-preservation-check.json), [skrip audit](audit-private.py) |
| Kecocokan source | [Receipt/hash file backend](receipt.json); source checkpoint dapat ditemukan lewat `git log -1 -- backend/server.mjs` |
| APK yang dipertahankan | 31.319.928 byte, SHA-256 `7dc808e6280ac4f4182f4bda89ceae43c88eb0329e25dd8baa3f2caaed0f5b57`; <100.000.000 byte |

Tes mencakup bentuk env/CLI gagal, peran/konteks salah, fixed model/prompt/header
provider, token salah, health, tipe/JSON/body client, body chunked besar,
body/exception/provider 429, timeout/pembatalan, dua request aktif serta kuota
socket termasuk spoof `X-Forwarded-For` dan pulih setelah jendela waktu habis.
Node lokal **v22.17.0**, tanpa dependensi npm baru, emulator atau build Android.
Windows menutup child dengan terminasi proses; drain sinyal Unix/systemd nyata,
TLS/proxy, kuota akun berkelanjutan dan HP masih belum diuji.

Perintah utama dari root repo:

```sh
node --test backend/*.test.mjs
node --env-file=.local/featherless.env backend/server.mjs --check-config
node --env-file=.local/featherless.env backend/check-private.mjs
# Hanya bila perlu validasi akses model/hasil rotasi; memakai kuota satu request:
node --env-file=.local/featherless.env backend/check-private.mjs --live
```

Audit secret memakai env yang diinjeksi lewat loader Node, lalu memindai byte
UTF-8/UTF-16LE key/token saat ini pada file tracked working tree dan file C01 baru,
serta seluruh entry APK terdekompresi. Hasil tidak berisi nilai secret atau hash
konfigurasi privat. Baseline hash preservasi tetap pada `.local/backend-c01-baseline.json`
yang di-ignore; buat snapshot privat baru sebelum sesi audit berikutnya bila ada
perubahan sah. Skrip audit membutuhkan Python 3; bukan dependensi runtime gateway.
Tidak memindai seluruh riwayat Git, encoding/obfuscation tak dikenal atau semua
secret layanan lain; tidak mengklaim sertifikasi keamanan menyeluruh.

## Temuan keamanan dan tindak lanjut

Audit memakai skill security-best-practices. Scope JavaScript ESM + `node:http`,
tanpa Express/frontend web. Skill tidak menyediakan referensi khusus backend
`node:http`; panduan resmi Node dan Featherless dipakai untuk bagian tersebut.

| ID / prioritas | Temuan / dampak | Status dan lokasi |
| --- | --- | --- |
| SEC-01 / tinggi | Key pernah dibagikan dan belum dirotasi; pihak yang pernah memperoleh key dapat memakai kuota provider | **Terbuka C01**, konfirmasi pengguna 25 Oktober; rotasi di dashboard, env lokal, cabut key lama, completion sintetis ulang |
| SEC-02 / tinggi bila dipublikasikan | Shared token, kuota memori/socket dan belum hosting HTTPS tidak memenuhi akses publik per instalasi | **Terbuka C02/G03**; `server.mjs:69–81`, `config.mjs:11–14`; jangan deploy publik template ini |
| SEC-03 / sedang | Env lama hanya memeriksa keberadaan/minimum panjang; salah bind/port/control/header bisa lolos awal | **Diperbaiki**, `config.mjs:5–28`, `server.mjs:132–160`; tes env dan CLI lulus |
| SEC-04 / sedang | Error parser/provider memakai status/teks exception yang sama dengan client; chunk besar bisa memutus socket tanpa respons terkontrol | **Diperbaiki**, `server.mjs:33–50,89–122`; regresi chunked/null/provider 502 dan sanitasi lulus |
| SEC-05 / operasional | README lama menyebut belum pernah live, tanpa cara validasi/restart terarah | **Diperbaiki**, README + checker; otomatisasi dan patch runtime produksi tetap perlu platform nyata C02 |

Keamanan yang dipertahankan: provider URL HTTPS tetap, `redirect: 'error'`,
token dibandingkan constant-time, tidak mempercayai forwarded IP, input dibatasi,
provider secret tidak masuk pesan/model request body ke klien, tidak mencatat chat.
Log proxy/hosting/provider dan mekanisme pencabutan identitas belum diaudit karena
belum ada platform C02.

Rujukan diperiksa saat sesi:
[Node security](https://nodejs.org/en/learn/getting-started/security-best-practices),
[HTTP](https://nodejs.org/api/http.html),
[iterator stream Node 22](https://nodejs.org/docs/latest-v22.x/api/stream.html#readableiteratoroptions),
[siklus LTS](https://nodejs.org/en/about/previous-releases),
[Featherless completion](https://featherless.ai/docs/completions),
[katalog model](https://featherless.ai/docs/api-reference-models).

## Preservasi dan langkah berikutnya

`PROJECT_HANDOFF.md`, env privat, arsip WIP audio dan stash A03 cocok dengan
baseline sebelum sesi; tidak di-stage. Referensi desain, source Android dan data
perangkat tidak diubah. Tidak ada APK baru, pengiriman email atau rilis publik.

Langkah C01 tersisa: pemilik merotasi key melalui dashboard lalu memperbarui env
lokal tanpa chat/Git; catat konfirmasi dan uji satu completion sintetis ulang.
Setelah itu nilai penutupan C01. Pilihan layanan/domain/biaya, penerapan template
dan akses instalasi tetap dikerjakan terpisah pada C02.
