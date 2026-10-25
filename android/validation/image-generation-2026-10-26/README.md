# C09 — gambar Cloudflare, beta 1.9.6

26 Oktober 2026. Pengguna memilih Cloudflare Free dan mengizinkan implementasi.
Runtime `717a9c5`, pematangan harness keyboard `1c42900`.
Token Workers AI dimasukkan sendiri pada file privat; paket Free dikonfirmasi
pengguna. Tidak mengaktifkan billing, paket berbayar, domain/Worker/database baru
atau mengganti key Featherless. Backend Vercel yang sama tetap melayani chat.

## Hasil

- Kemampuan gambar diperiksa melalui endpoint terautentikasi. Ketika siap,
  **Buat gambar** tersedia pada AI dan menu +. Mode gambar terpisah dari chat;
  hanya deskripsi dikirim setelah tinjauan. Profil/riwayat/lampiran tidak ikut.
  Gambar tidak dibuat otomatis saat aplikasi dibuka atau jaringan pulih.
- Hasil JPEG/PNG ditampilkan dengan label **Ilustrasi buatan AI**, preview,
  **Simpan gambar** dan **Bagikan gambar**. Riwayat hanya menyimpan referensi;
  file berada di folder privat aplikasi, maksimal32MB. Simpan memakai pemilih
  dokumen Android; berbagi memakai chooser dan URI baca saja. Tidak ada izin
  penyimpanan luas atau model/key provider dalam APK.
- Batas **3 percobaan per instalasi per hari, 20 total aplikasi per hari**;
  Redis Lua atomik, reset UTC00:00/WIB07:00. Nonce mencegah satu request memanggil
  provider dua kali. Percobaan gagal/batal/timeout dapat tetap memakai kuota;
  retry memerlukan tinjauan lagi, tanpa fallback berbayar. Kuota provider juga
  dibagi dengan pemakaian lain pada akun.
- Koneksi pulih segera memperbarui ketersediaan gambar. Ganti chat/batal menekan
  callback terlambat. Berkas AtomicFile `.new`/`.bak` ikut dibersihkan ketika
  chat dihapus. Penghapusan hanya menyentuh folder/ref gambar chat yang dipilih.
- Model tetap FLUX.1 Schnell, empat langkah, text-to-image. Body prompt≤4.096
  byte, hasil≤2.000.000 byte, JPEG/PNG64–1.536 piksel per sisi/≤2.100.000 piksel,
  tanpa URL gambar eksternal. Android memeriksa decode. Ini bukan editor foto,
  dukungan semua model, atau jaminan tulisan/anatomi/isi gambar selalu benar.

## Bukti

| Pemeriksaan | Hasil |
| --- | --- |
| [Backend + Lua](backend-tests-lua.txt) | **47 lulus, 0 gagal/skip**. Lua asli dijalankan lewat fakeredis/Lupa lokal untuk atomisitas, batas3/20, duplikasi, reset hari dan data rusak. Ini bukan uji konkurensi Upstash produksi. |
| [Provider langsung](provider-probe.json) | REST Cloudflare nyata200, JPEG1024×1024 tervalidasi. Prompt lanskap sintetis, tanpa data pribadi. |
| [Kapabilitas hosting](hosted-capabilities.json) | Gateway Vercel nyata200, gambar aktif, batas3/20. |
| [APK → Vercel → Cloudflare](native-image-live.txt), [receipt](native/ImageC09-live.json) | Model nyata tanpa interceptor, gambar diterima, didecode dan disimpan ke chat. Ini juga melewati registry/lease/kuota Upstash produksi. Bukan hasil mockup. |
| [Consent/save/share/reopen](native-consent-save-share.txt) | Sebelum konfirmasi0 request. Body hanya prompt+requestId; gambar tidak masuk JSON riwayat. Export menghasilkan byte yang sama pada ContentProvider fixture dalam helper terpisah. Intent pemilih dokumen/chooser diblokir oleh monitor tes; tidak mengirim ke aplikasi orang lain. Reopen memulihkan ref dan file. |
| [Storage/provider](native-image-storage.txt) | MIME/base64/decode salah ditolak; traversal ditolak; URI dapat dibaca tetapi tidak ditulis; hapus membersihkan gambar serta `.new` sintetis. |
| [Kuota/batal/pulih](native-image-quota-cancel.txt) | Respons kuota terkendali memberi pesan yang benar; batal lalu chat baru menekan hasil terlambat. Mode offline→auto memulihkan kemampuan tanpa POST gambar baru. |
| [Regresi AI](native-ai-regression.txt), [client](native-client-regression.txt) | 7+6 metode sebelumnya lulus: migrasi/multi-chat/lampiran/consent/jaringan/draf, serta auth/kuota/timeout/error/batal/tugas atomik/sumber. Respons terkendali dibedakan dari uji live. |
| [Font200%](native-large-text.txt), [layar sempit](native-narrow-large-text.txt) | Mode gambar dengan keyboard: seluruh tinggi target kirim/+ terlihat. Screenshot wide/narrow terpisah. |
| [Build](final-awake-build.txt), [lint](lint-summary.txt), [unit](unit-results.json) | Debug/helper terbangun. Lint0 error; jumlah warning dan hasil unit persis ada pada receipt, tidak disamakan semua lulus. |
| [Preservasi](preservation.json), [audit](audit.json) | Data asli emulator dipulihkan cocok hash; helper/marker dihapus, font/ukuran/jaringan/pengaturan tetap-nyala dikembalikan; emulator ditutup. HP tidak diubah. WIP/key lama/audio/stash/logo/APK lama tetap. |

19 eksekusi native/18 metode unik pada kandidat final. Pemilihan penerima share
dan pemilih dokumen di HP fisik tetap uji perangkat G02; hasil export fixture
tidak disebut sebagai pengiriman ke WhatsApp/Drive nyata.

Percobaan awal **tidak dihitung lulus**: builder Intent memakai chaining pada
`setClipData` yang return-void; filter monitor ekspor belum cocok MIME/category;
emulator sempat tertidur dan menghasilkan screenshot hitam. Builder/filter,
persiapan layar aktif/fokus, dan pemulihan setting diperbaiki sebelum tes akhir.
Pemeriksaan juga memperbaiki cache kapabilitas setelah offline serta pembersihan
file tulis terputus. Assertion keyboard diperketat agar IME benar-benar terlihat;
uji tata letak sempit tidak menunggu internet karena tidak melakukan inference.
Satu startup setelah resize sempat crash dan pengulangan sempat menunggu jaringan;
restart proses/fokus/keyboard fixture dibenahi. Hasil itu tidak dimasukkan hitungan
lulus. Uji akhir sempit benar-benar menampilkan keyboard dan seluruh target.

Probe Upstash langsung dari komputer timeout; uji Lua lokal dan alur live lewat
Vercel dibedakan. Probe memakai prefix sintetis terpisah; cleanup langsung tidak
terkonfirmasi. Nonce/counter probe yang mungkin sempat dibuat memiliki TTL48jam,
tanpa profil/prompt/data pengguna dan tanpa mengubah registry pengguna. Tidak
mengklaim seluruh pengujian operasional C02 selesai.

## Paket dan batas

[Receipt APK/source/signer](apk-receipt.json) mencatat byte/hash/source final.
File luar `Zikra-AI-v1.9.6-beta-debug.apk`, versionCode33, installer<100.000.000
byte (**37.013.519 byte**), SHA-256
`dc55e1e817f17dece0765d06b434b846291d53663fde6220adb05a899ff67c80`.
SQLite utama4 dan chat1; kandidat1.9.5/lama tetap ada. Sertifikat debug sama
baseline, bukan signed release publik.

Deployment existing `https://zikra-ai.vercel.app`, ID
`Du6X24ysdLE3Zc6Ua6A6Q5Nt6CLR`, immutable
`https://zikra-1i4jk6stt-arkanzahir86-3257s-projects.vercel.app`.
Konfigurasi gambar server-only dapat dimatikan tanpa mematikan chat. C09 ditutup
sebatas implementasi/pembuktian beta ini; C02/C03/C04/C06/C07, B01, adzan D02a,
UI04 keseluruhan/F/G dan publik tetap mengikuti checkboxnya masing-masing.

Rujukan resmi: [FLUX.1 Schnell](https://developers.cloudflare.com/workers-ai/models/flux-1-schnell/),
[REST/token](https://developers.cloudflare.com/workers-ai/get-started/rest-api/),
[kuota/harga](https://developers.cloudflare.com/workers-ai/platform/pricing/).

Preview: [hasil nyata](native/ImageC09-live-result-100.png),
[siap online](native/ImageC09-online-ready-100.png),
[tinjauan](native/ImageC09-consent-100.png),
[riwayat pulih](native/ImageC09-restored-100.png),
[font200% sempit](native/ImageC09-composer-narrow-200.png).
