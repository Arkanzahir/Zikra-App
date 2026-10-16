package com.zikra.app.reading;

/** Complete local texts for the four surahs advertised by the offline reader. */
public final class SurahData {
  public final String name;
  public final String arabicName;
  public final int number;
  public final int juz;
  public final String[] arabic;
  public final String[] translations;

  private SurahData(
      String name, String arabicName, int number, int juz, String[] arabic, String[] translations) {
    this.name = name;
    this.arabicName = arabicName;
    this.number = number;
    this.juz = juz;
    this.arabic = arabic;
    this.translations = translations;
  }

  public static final SurahData[] ALL = {
    new SurahData(
        "Al-Fatihah",
        "سُورَةُ الْفَاتِحَةِ",
        1,
        1,
        new String[] {
          "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
          "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
          "الرَّحْمَٰنِ الرَّحِيمِ",
          "مَالِكِ يَوْمِ الدِّينِ",
          "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
          "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
          "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا"
              + " الضَّالِّينَ"
        },
        new String[] {
          "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.",
          "Segala puji bagi Allah, Tuhan seluruh alam.",
          "Yang Maha Pengasih, Maha Penyayang.",
          "Pemilik hari pembalasan.",
          "Hanya kepada Engkaulah kami menyembah dan hanya kepada Engkaulah kami memohon"
              + " pertolongan.",
          "Tunjukilah kami jalan yang lurus.",
          "(Yaitu) jalan orang-orang yang telah Engkau beri nikmat, bukan jalan mereka yang"
              + " dimurkai dan bukan pula jalan mereka yang sesat."
        }),
    new SurahData(
        "Al-Ikhlas",
        "سُورَةُ الْإِخْلَاصِ",
        112,
        30,
        new String[] {
          "قُلْ هُوَ اللَّهُ أَحَدٌ",
          "اللَّهُ الصَّمَدُ",
          "لَمْ يَلِدْ وَلَمْ يُولَدْ",
          "وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ"
        },
        new String[] {
          "Katakanlah, Dialah Allah Yang Maha Esa.",
          "Allah tempat meminta segala sesuatu.",
          "Dia tidak beranak dan tidak pula diperanakkan.",
          "Dan tidak ada sesuatu yang setara dengan Dia."
        }),
    new SurahData(
        "Al-Falaq",
        "سُورَةُ الْفَلَقِ",
        113,
        30,
        new String[] {
          "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ",
          "مِنْ شَرِّ مَا خَلَقَ",
          "وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ",
          "وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ",
          "وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ"
        },
        new String[] {
          "Katakanlah, aku berlindung kepada Tuhan yang menguasai subuh (fajar).",
          "Dari kejahatan (makhluk yang) Dia ciptakan.",
          "Dan dari kejahatan malam apabila telah gelap gulita.",
          "Dan dari kejahatan para peniup pada buhul-buhul (talinya).",
          "Dan dari kejahatan orang yang dengki apabila ia dengki."
        }),
    new SurahData(
        "An-Nas",
        "سُورَةُ النَّاسِ",
        114,
        30,
        new String[] {
          "قُلْ أَعُوذُ بِرَبِّ النَّاسِ",
          "مَلِكِ النَّاسِ",
          "إِلَٰهِ النَّاسِ",
          "مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ",
          "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ",
          "مِنَ الْجِنَّةِ وَالنَّاسِ"
        },
        new String[] {
          "Katakanlah, aku berlindung kepada Tuhannya manusia.",
          "Raja manusia.",
          "Sembahan manusia.",
          "Dari kejahatan (bisikan) setan yang bersembunyi.",
          "Yang membisikkan (kejahatan) ke dalam dada manusia.",
          "Dari (golongan) jin dan manusia."
        })
  };
}
