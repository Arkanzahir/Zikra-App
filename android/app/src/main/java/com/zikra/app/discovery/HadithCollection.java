package com.zikra.app.discovery;

/** Curated excerpts with a stable collection identifier and precise source numbering. */
public final class HadithCollection {
  private HadithCollection() {}

  public static final class Entry {
    public final String id, title, arabic, meaning, source, collection;
    public final boolean excerpt;

    private Entry(
        String id, String title, String arabic, String meaning, String source, String collection) {
      this(id, title, arabic, meaning, source, collection, true);
    }

    private Entry(String id, String title, String arabic, String meaning, String source,
        String collection, boolean excerpt) {
      this.id = id;
      this.title = title;
      this.arabic = arabic;
      this.meaning = meaning;
      this.source = source;
      this.collection = collection;
      this.excerpt = excerpt;
    }
  }

  public static Entry sourced(String book, int number, String arabic, String meaning) {
    if (!("bukhari".equals(book) || "muslim".equals(book)) || number < 1 || number > 100000)
      throw new IllegalArgumentException("Hadith identity");
    String name = "bukhari".equals(book) ? "Bukhari" : "Muslim";
    return new Entry("tafsirq-v1:" + book + ":" + number, "Hadis " + number, arabic, meaning,
        "HR. " + name + " · " + number + " · penomoran Tafsirq", name, false);
  }

  public static String bookForId(String id) {
    if (id == null || !id.matches("tafsirq-v1:(bukhari|muslim):[1-9][0-9]{0,5}")) return null;
    return id.split(":")[1];
  }

  public static final Entry[] ENTRIES = {
    new Entry(
        "intentions",
        "Niat yang baik",
        "إِنَّمَا الْأَعْمَالُ بِالنِّيَّاتِ",
        "Sesungguhnya amal bergantung pada niatnya.",
        "HR. Bukhari 1 · Muslim 1907",
        "Bukhari Muslim"),
    new Entry(
        "kind_words",
        "Berkata baik",
        "فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ",
        "Barang siapa beriman kepada Allah dan hari akhir, hendaklah ia berkata baik atau diam.",
        "HR. Bukhari 6018 · Muslim 47",
        "Bukhari Muslim"),
    new Entry(
        "brotherhood",
        "Mencintai sesama",
        "لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
        "Tidak sempurna iman salah seorang di antara kalian hingga ia mencintai untuk saudaranya"
            + " apa yang ia cintai untuk dirinya sendiri.",
        "HR. Bukhari 13 · Muslim 45",
        "Bukhari Muslim"),
    new Entry(
        "learning",
        "Jalan menuntut ilmu",
        "وَمَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى"
            + " الْجَنَّةِ",
        "Siapa yang menempuh jalan untuk mencari ilmu, Allah memudahkan baginya jalan menuju"
            + " surga.",
        "HR. Muslim 2699",
        "Muslim"),
    new Entry(
        "gentleness",
        "Keindahan kelembutan",
        "إِنَّ الرِّفْقَ لَا يَكُونُ فِي شَىْءٍ إِلَّا زَانَهُ",
        "Sesungguhnya kelembutan tidak berada pada sesuatu kecuali menghiasinya.",
        "HR. Muslim 2594",
        "Muslim"),
    new Entry(
        "quran",
        "Belajar Al-Quran",
        "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ",
        "Sebaik-baik kalian adalah yang mempelajari Al-Quran dan mengajarkannya.",
        "HR. Bukhari 5027",
        "Bukhari"),
    new Entry(
        "anger",
        "Menjaga diri dari marah",
        "لَا تَغْضَبْ",
        "Jangan marah. Nasihat ini diulangi Nabi ketika seseorang meminta nasihat kepada beliau.",
        "HR. Bukhari 6116",
        "Bukhari")
  };
}
