package com.zikra.app.assistant;

import java.util.Locale;

/** Small, deterministic local guide. It never claims to be a remote or generative AI. */
public final class OfflineAssistant {
  public static final int MAX_INPUT_LENGTH = 500;

  private OfflineAssistant() {}

  public static String reply(String input, int completed, int total, String lastSurah) {
    if (input == null || input.trim().isEmpty())
      throw new IllegalArgumentException("Pesan wajib diisi");
    if (input.length() > MAX_INPUT_LENGTH)
      throw new IllegalArgumentException("Pesan maksimal 500 karakter");
    String normalized = input.toLowerCase(Locale.ROOT);
    if (contains(normalized, "gempa", "darurat", "bmkg", "cuaca")) {
      return "Untuk data cuaca dan gempa, buka Protector BMKG dari Home. Halaman itu menampilkan"
          + " sumber serta waktu pembaruan data.\n\n"
          + "Dalam keadaan darurat, gunakan Bantuan cepat untuk membuka telepon 112."
          + " Pendamping offline ini tidak memantau kondisi darurat.";
    }
    if (contains(normalized, "hafal", "hifz", "quran", "qur'an", "tilawah", "murojaah")) {
      return "Kita mulai dari bacaan terakhirmu: "
          + lastSurah
          + ".\n\n"
          + "1. Baca satu ayat perlahan sambil melihat teks.\n"
          + "2. Ulangi tiga kali, lalu coba tanpa melihat.\n"
          + "3. Sambungkan dengan ayat sebelumnya.\n"
          + "4. Catat target di kategori Hifz pada Study Hub.\n\n"
          + "Sepuluh menit yang rutin lebih mudah dijaga. Untuk tajwid dan koreksi bacaan, tetap"
          + " belajar bersama guru.";
    }
    if (contains(normalized, "sholat", "salat", "shalat", "ibadah", "doa", "dzikir", "kiblat")) {
      return "Mulai dengan satu ibadah yang bisa kamu jaga hari ini.\n\n"
          + "1. Periksa jadwal di menu Jadwal Sholat.\n"
          + "2. Siapkan waktu tenang di antara kegiatan kuliah.\n"
          + "3. Baca doa harian dan tandai checklist setelah selesai.\n\n"
          + "Progresmu hari ini "
          + completed
          + " dari "
          + total
          + " checklist. Panduan ini untuk membantu rutinitas, bukan fatwa keagamaan.";
    }
    if (contains(
        normalized, "progres", "progress", "checklist", "target hari", "selesai", "tugas hari")) {
      int remaining = Math.max(0, total - completed);
      return "Hari ini kamu telah menyelesaikan "
          + completed
          + " dari "
          + total
          + " checklist. "
          + (remaining == 0
              ? "Semua target hari ini sudah selesai. Luangkan waktu untuk istirahat dan refleksi."
              : "Masih ada "
                  + remaining
                  + " target yang bisa dilanjutkan.\n\n"
                  + "Pilih satu tugas paling penting, kerjakan selama 25 menit, lalu istirahat 5"
                  + " menit.")
          + "\n\n"
          + "Buka Study Hub di bawah untuk melihat, menambah, atau menandai tugas. Pesan di sini"
          + " tidak otomatis mengubah checklist.";
    }
    if (contains(
        normalized, "cemas", "capek", "lelah", "stres", "stress", "bingung", "malas", "sedih")) {
      return "Tidak apa-apa untuk mengambil jeda sebentar.\n\n"
          + "Coba letakkan ponsel, tarik napas pelan, dan minum air. Setelah itu pilih satu"
          + " tugas yang bisa selesai dalam lima menit.\n\n"
          + "Kalau perasaan ini menetap atau mengganggu aktivitas, ceritakan kepada orang"
          + " tepercaya atau layanan konseling kampus. Aku hanya panduan offline dan tidak"
          + " dapat menilai kesehatanmu.";
    }
    if (contains(normalized, "hai", "halo", "assalam", "hello")) {
      return "Waalaikumsalam, Arkan. Aku pendamping offline untuk membantu menyusun rutinitas"
          + " belajar, ibadah, dan hafalan.\n\n"
          + "Coba tulis “bantu susun jadwal belajar”, “lihat progres checklist”, atau pilih"
          + " salah satu topik di atas.";
    }
    return "Mari ubah targetmu menjadi langkah yang lebih ringan.\n\n"
        + "1. Tulis satu hasil yang ingin dicapai dari materi itu.\n"
        + "2. Pelajari satu bagian selama 25 menit tanpa distraksi.\n"
        + "3. Istirahat 5 menit, lalu kerjakan satu latihan.\n"
        + "4. Catat hal yang belum dipahami untuk ditanyakan ke dosen atau teman.\n\n"
        + "Tambahkan target itu di Study Hub agar progresnya tercatat. Aku memakai panduan"
        + " lokal; aku belum bisa menjawab soal khusus atau mencari informasi di internet.";
  }

  private static boolean contains(String text, String... words) {
    for (String word : words) if (text.contains(word)) return true;
    return false;
  }
}
