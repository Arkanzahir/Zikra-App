# Pengiriman data Hadis dan Doa

24 Oktober 2026. **Usulan arsitektur untuk B01–B04; belum diimplementasikan atau dihosting.** Sumber API adalah kandidat sampai isi, penomoran, penggunaan/redistribusi dan ketersediaannya diperiksa. APK 1.6.x masih berisi tujuh kutipan Hadis dan empat Doa lokal.

## Alur yang dituju

`Sumber yang disetujui → proses impor/validasi server Zikra → paket data berversi → cache/database HP → tampilan dan pencarian`

- Server menyimpan salinan versi yang sudah diperiksa beserta asal data, nomor/ID, bahasa dan versi. Server tidak mengunduh ulang seluruh koleksi untuk setiap pengguna membuka layar. Pembaruan sumber harus melalui validasi sebelum mengganti versi aktif.
- HP menyimpan data yang sudah diunduh agar dapat dibaca dan dicari luring. Perubahan versi diterapkan secara atomik; koneksi putus tidak merusak versi sebelumnya atau bookmark.
- Doa relatif kecil: setelah hak distribusi dan ukuran diverifikasi, paket awal dapat dibundel agar ada bacaan sejak instalasi pertama; pembaruan berikutnya diunduh. Ini belum keputusan jumlah/ukuran paket.
- Hadis memerlukan pemuatan per koleksi/bagian dan unduhan luring eksplisit. Sebelum seluruh kitab tersimpan, UI harus membedakan pencarian seluruh koleksi dari pencarian data di perangkat. Jangan mengklaim seluruh kitab dapat dicari luring jika baru sebagian diunduh.
- Identitas bookmark memakai ID sumber/edisi yang stabil, bukan posisi baris. Penomoran antar-edisi tidak digabung hanya karena nomornya sama.

## Kenapa ada server dan juga simpanan HP?

Server mengelola versi, pemeriksaan sumber dan pembaruan bersama. HP menyediakan akses cepat/luring. Server tersebut tidak wajib secara teknis untuk membaca API publik; akses langsung plus cache HP tetap opsi untuk beta bila belum ada hosting. Penyimpanan ulang/redistribusi hanya diterapkan setelah ketentuan konten mengizinkannya.

Penyimpanan **konten bacaan** di server berbeda dari menyinkronkan **data pribadi pengguna**. Rencana ini tidak otomatis mengunggah profil, tugas, riwayat membaca atau bookmark; akun/sinkronisasi tetap keputusan A02/D06.

## Kandidat yang sudah ditemukan

- Hadis: [Gading Hadith API](https://github.com/gadingnst/hadith-api), sumber upstream yang dinyatakan proyek adalah tafsirq.com; alternatif [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api). Jangan menyamakan lisensi kode repository dengan hasil pemeriksaan hak seluruh konten/terjemahannya.
- Doa: [EQuran.id API Doa](https://equran.id/apidev/doa), dokumentasi mencantumkan Arab, Latin, arti, kelompok/tag dan rujukan. Belum dijadikan sumber produksi Zikra.

## Bukti yang diperlukan sebelum B01–B04 dicentang

Catatan sumber/ketentuan; sampel respons nyata; cakupan koleksi/bahasa; pemetaan ID/nomor; ukuran paket; impor data rusak/duplikat; pembaruan atomik; pencarian online/luring yang jelas; serta bookmark tetap benar setelah versi berubah. Isi Arab/terjemahan tidak diganti atau dilengkapi oleh AI.
