# E03 — implementasi pengingat,13November2026

Status: kode dan APK internal tersedia; uji gabungan ditunda pengguna. E03/J08 belum dicentang selesai.

Protector → Atur notifikasi dan pengingat membuka halaman sekeluarga dengan Protector (tema global/dock/typography sama): empat opt-in terpisah, waktu pagi/petang, ringkasan hari ini, tautan Study, Doa pagi/petang dan perjalanan, gempa dan setelan Android. Kategori Doa dipilih melalui daftar kategori yang sudah ada; naskah/rujukan tidak dibuat atau diganti. Perjalanan adalah tindakan manual, tidak mengaku mendeteksi perjalanan.

Ringkasan memakai jumlah tugas Study yang belum selesai/tenggat hingga hari ini, tabel salat bersama atau fallback lokal berlabel, dan prakiraan cache lokasi yang valid maksimal6jam dengan waktu pengambilan. Tidak menampilkan judul tugas pada notifikasi atau mengirimnya ke AI. Data gagal tidak dikarang sebagai nol/cuaca cerah.

JobScheduler lokal persisten15menit/flex5menit, best-effort bukan alarm tepat: jendela harian maksimal2jam dan tidak melewati hari. Pagi+briefing digabung satu pesan, petang terpisah. Deduplikasi bounded perjenis/tanggal serta cooldown6jam, ditulis sebelum notify; revisi setting/lokasi ditinjau saat delivery. Job generasi lama tidak menyelesaikan generasi baru. Channel privat/no adzan. Semua pilihan default mati. Opt-out membatalkan job saat semua mati dan menghapus notifikasi kontekstual yang aktif; tidak mengubah salat/gempa.

Cuaca meminta persetujuan koordinat terpilih→Open-Meteo saat aplikasi ditutup; tanpa GPS latar belakang. Internet tervalidasi diperlukan untuk fetch. Pemeriksaan maksimal sekali/jam/lokasi pukul06–21; berhenti memeriksa sesudah satu alert cuaca pada hari itu. Data HTTP/parser baru yang valid, slot tiga jam ke depan dan label hujan/petir; bukan warning resmi BMKG atau jaminan bahaya. Tidak memakai data lama untuk alert baru. Ketentuan/batas penggunaan Open-Meteo tetap gate J04, termasuk agregat pengguna.

Kompilasi dan assembleDebug berhasil; paket akhir mencakup routing notifikasi cuaca langsung ke detail prakiraan. Tidak menjalankan unit/lint/runtime/emulator/HP, tidak mengaktifkan preferensi, mengirim notifikasi atau mengganti backend. Perubahan source harus diuji sebagai satu kandidat, bukan mengandalkan kelulusan lama. Uji berikut: opt-in/izin ditolak/channel diblokir; duplikasi/jadwal/zona/lokasi/hari/clock rollback; job stop-resume/generasi; offline/data rusak/cache usang; seluruh Doa/link; teks besar/dark/keyboard/scroll; burst dan quota. Evaluasi also batas airtime/keterlambatan vendor HP pada J13.

APK lokal D:/Download/Zikra-App-main/Zikra-Contextual-v1.9.44-beta-debug.apk,50.262.119byte (<100MB), SHA256a23813d63284927c75f03a570ce1baba9f8ca8dcc3cbbc10d01d10dd24872cc1. Tidak menemukan credential privat dikenal/berkas privat, dua hash audio/signer debug tetap. Bukan distribusi final. J02/J03 verifikasi gabungan, B01/hak konten/audio/signing/HP/E04/F/G/beta tetap terbuka.
