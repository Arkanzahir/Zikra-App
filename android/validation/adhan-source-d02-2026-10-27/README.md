# D02 — asal berkas adzan pilihan pengguna

27 Oktober 2026. **Asal unduhan teridentifikasi; ketentuan redistribusi belum
terbukti.** Pilihan audio pengguna dipertahankan, tidak diganti.

[Halaman penerbit](https://islamdownload.net/123801-download-suara-adzan.html)
menautkan berkas yang dilabeli Adzan Subuh dan Adzan Makkah (1).
Dua respons MP3 diunduh ke direktori audit privat dan dibandingkan byte/hash
dengan berkas pengguna. [Hasil](file-match.json):

| Penggunaan | Berkas publik | Byte | SHA256 |
| --- | --- | ---: | --- |
| Subuh | `fajr_128_44.mp3` | 2.996.349 | `106a7b2ca4e8f85fb44f7b150d42e7ca11237c961642ad877dd02e45f8add530` |
| Empat waktu lainnya | `mecca_56_22.mp3` | 1.423.360 | `43ade766272ff9c89c86908959df6ddd7f3a34961897caf09f965a3841e1bcd2` |

URL tepat tercatat pada laporan; file kedua cocok dengan nama lokal pengguna
`mecca_56_22 (1).mp3`. Kesamaan nama saja tidak dipakai sebagai bukti.
Halaman menyatakan unduhan gratis, tetapi pemeriksaan halaman tersebut belum
menemukan lisensi eksplisit untuk menyertakan rekaman ke distribusi aplikasi,
identitas pemilik rekaman, atau ketentuan redistribusi komersial/nonkomersial.
Ini **bukan kesimpulan bahwa pemakaian dilarang**; bukti yang dibutuhkan belum ada.

Label Subuh pada penerbit bukan pengganti mendengarkan seluruh rekaman untuk
memeriksa lafaz/tambahan Subuh. Belum menyimpulkan keduanya dilantunkan muazin
yang sama atau memverifikasi identitas muazin. Decoder/durasi/posisi playback
sudah teruji pada beta privat; kualitas suara terdengar tetap perlu diperiksa.

Tidak menghubungi penerbit atau mengirim email baru. MP3/hasil unduhan tetap
privat di luar Git; hanya build debug privat yang memasukkan audio melalui
properti Gradle eksplisit. D02/D02a dan distribusi publik tetap belum dicentang.
