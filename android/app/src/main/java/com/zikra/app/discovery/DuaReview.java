package com.zikra.app.discovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Corrections apply to a reviewed source snapshot, never blindly to a reused provider ID. */
public enum DuaReview {
  NONE, SOURCE_CHANGED, WAKE_UP, WIND, RAIN_NUMBERING, GRADING_CONTEXT, AFTER_MEAL_NUMBERING, ARABIC_INSTRUCTIONS, WITR_LAYOUT, TITLE_TYPO, TITLE_SCOPE,
  AYYUB_ADAPTED, MARKET_SCOPE, ANAS_ADAPTED, BIRTH_GREETING, CHILD_ADAPTED, CHILD_REFERENCE, FIQH_ADAPTED, JARIR_ADAPTED, IBN_MASUD_ATHAR, IMAN_ROUTES, EID_GREETING, MEANING_REPEAT, LABBAIK_LETTER, WORD_SPACING, COMPOSITE_KHUTBA, SLEEP_GRADING, DEBT_GRADING, MEANING_PERSON, DEATH_NUMBERING, POVERTY_NUMBERING, GOOD_DEEDS_NUMBERING, GHAYB_HARAKAT;

  public static DuaReview forSource(int id, String title, String arabic, String meaning, String reference) {
    String expected; DuaReview review;
    switch (id) {
      case 6: expected = "e38beae72f57b70e3675893dadde7fcfa46e5fa56d879163fedfd03210af4c04"; review = WAKE_UP; break;
      case 18: expected = "88a82df458a0e89ad9ed4340462cf722db19ad34e91e3a469df96ac9bf49ad14"; review = WIND; break;
      case 20: expected = "1ab612312d815d856ca5735b07954b75b2f6cb1d830dc326a754e834872238d5"; review = RAIN_NUMBERING; break;
      case 96: expected = "811109d3e416464781ca6da504788eb2cdc09e4f79daf946905b91640a42e549"; review = GRADING_CONTEXT; break;
      case 139: expected = "baf12d0972ec1ba32eaa4aad2da62184752ea465cb2e430ceb86730e485df096"; review = AFTER_MEAL_NUMBERING; break;
      case 57: expected = "433e8629514ef9043ff2a4abb1b14ef5cbe97a5bf2cd084cde7e9fdb3da899da"; review = ARABIC_INSTRUCTIONS; break;
      case 209: expected = "5ed03893e9255b9b5dbb5e348211976f0e5c087ce8fa0a855c0df966381561a8"; review = ARABIC_INSTRUCTIONS; break;
      case 225: expected = "0b975000eec579c5e3d1c5b5f688334381ab28987b0c0d3c782e704b16177ac8"; review = WITR_LAYOUT; break;
      case 16: expected = "fdcb8bb467ae5cc20d3dd13ac262e0aa4b7dbe08fc665d0df9f5a96d6f9da78f"; review = TITLE_SCOPE; break;
      case 66: expected = "8a24f0f9b8671c611d7c09a3ab5cd6e13f447b9d755b75c24dc8e334457f34dc"; review = TITLE_TYPO; break;
      case 103: expected = "963f3a81d8dec76266ec0b6138b50116bb3c0e50a12f21a0765fc327ac36ebdb"; review = TITLE_TYPO; break;
      case 26: expected = "8ee1d0bba9ddb6f998a885c7f28bf5df1e4531dcd7d47eec0d3b200718db4540"; review = AYYUB_ADAPTED; break;
      case 55: expected = "ac1142e94c32999b25d48cb2104929aa56b5762f96d9fff6a2d53fff1c6dd020"; review = MARKET_SCOPE; break;
      case 70: expected = "d7781583cb631a4e74184b147077b4c05eed5ad58309abb047f574d93dd77208"; review = ANAS_ADAPTED; break;
      case 74: expected = "b6b969d8058cf68fa8674d65f2fd75dbc28916eddeecc7be47cb7c7edfaba180"; review = BIRTH_GREETING; break;
      case 76: expected = "5d0ea829c4b141121df3f8909c0f0d3523c50ce22f9cbdaa512b94b43387124c"; review = CHILD_ADAPTED; break;
      case 77: expected = "c24cdde114b8f0b231ad844980f998c59c162d483b9d738eb168961c06510f6b"; review = CHILD_REFERENCE; break;
      case 98: expected = "f80c3f0c57e7d0edb558ae09b0e62f40f40ce09f3470e3fd9e4867817018d5b9"; review = FIQH_ADAPTED; break;
      case 121: expected = "380db7f08e5a580667356890af14077052b901f25023d31022ebd8576c9063f4"; review = JARIR_ADAPTED; break;
      case 122: expected = "bb3a923a81ef0c67c9a8afe918500032e425f8f999633408fd17e8ecd0f8a18a"; review = IBN_MASUD_ATHAR; break;
      case 124: expected = "dbab235375279c51db067498c996b754862b2633d727d7dafa8964d2709f22e6"; review = IMAN_ROUTES; break;
      case 145: expected = "ef4b5a3f7b85ce47b55d6e1dc538ef90cade68bd072385d0dfca55d4bd0db2ab"; review = EID_GREETING; break;
      case 154: expected = "9faab0d00c5b5178b33eed4479bfcea4af2e59ffdc55517622f2cf94ad477721"; review = MEANING_REPEAT; break;
      case 186: expected = "b57f1adb7697a0b6758de58fe59487a3dc7f918001b5b2f045788e130ffc91bc"; review = LABBAIK_LETTER; break;
      case 188: expected = "f2b4a365037fa762a25c571952dcbe3cca9135e9252febb7d5e68539590666b7"; review = WORD_SPACING; break;
      case 211: expected = "2528dc0bdb4dd9438a85a617a6c11e8df7484d7238c4b486648b7aa79ae3910e"; review = COMPOSITE_KHUTBA; break;
      case 3: expected = "553875f954269c352218889a32fc21ec04fc8179c05d8d9ee95ae4e04933fc4c"; review = SLEEP_GRADING; break;
      case 159: expected = "a57aabd5fa41af203b3c52109983b221fcbcfb2ab29349e3548ba014433ac07a"; review = DEBT_GRADING; break;
      case 123: expected = "b930f531d37fb0f9db238ef5c5b14823c67806f51eca88531b8f7256d28b4a57"; review = MEANING_PERSON; break;
      case 94: expected = "a3d5b54942a2e63941b1166f4200fc3b2dde6891192f648bb4c906b7e733843b"; review = DEATH_NUMBERING; break;
      case 168: expected = "be9aaaf41dd4f85f49ab1a6bb330fe2356d2df54558176dbf2dc894caa46fab7"; review = POVERTY_NUMBERING; break;
      case 176: expected = "0bfb3683525364c8d43a49783d03f86447e936de1956dfcf063bed392667fb67"; review = GOOD_DEEDS_NUMBERING; break;
      case 111: expected = "7bf212a1368d09cd37513913a548d28f49702385e313e1e759639f3c1d6e44a7"; review = GHAYB_HARAKAT; break;
      default: return DuaSourceSnapshots.matches(id, fingerprint(title, arabic, meaning, reference)) ? NONE : SOURCE_CHANGED;
    }
    return expected.equals(fingerprint(title, arabic, meaning, reference)) ? review : SOURCE_CHANGED;
  }

  static String fingerprint(String title, String arabic, String meaning, String reference) {
    for (String field : new String[]{title, arabic, meaning, reference})
      if (field.indexOf(0) >= 0) throw new IllegalArgumentException("Invalid source separator");
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(
          (title + "\0" + arabic + "\0" + meaning + "\0" + reference).getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (byte value : digest) out.append(String.format(Locale.ROOT, "%02x", value & 255));
      return out.toString();
    } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
  }

  public String displayTitle(String source) {
    if (this == TITLE_SCOPE) return "Berlindung dari malas, pengecut, usia tua dan kikir";
    if (this == MARKET_SCOPE) return "Dzikir ketika masuk pasar";
    if (this == ANAS_ADAPTED) return "Memohon keberkahan harta, anak, umur dan amal";
    if (this == TITLE_TYPO || this == JARIR_ADAPTED) return source.replace("Nabil Ibrahim", "Nabi Ibrahim")
        .replace("Nabu Musa", "Nabi Musa").replace("Sebagai pembeli Petunjuk", "Sebagai pemberi Petunjuk");
    return source;
  }

  String displayArabic(String source) {
    if (this == WAKE_UP) return source.replace("النُّشُوْرِ", "النُّشُورُ");
    if (this == ARABIC_INSTRUCTIONS || this == CHILD_ADAPTED || this == CHILD_REFERENCE) return source.substring(source.indexOf('\n') + 1).trim();
    if (this == WITR_LAYOUT) return source.replace("(", "").replace(")", "").trim();
    if (this == LABBAIK_LETTER) return "لَ" + source;
    if (this == GHAYB_HARAKAT) return source.replace("بِعِلْمِكَ الْغَيْبِ", "بِعِلْمِكَ الْغَيْبَ");
    if (this == WORD_SPACING) return source.replace("اَلأَ حَدُ", "اَلأَحَدُ");
    return source;
  }

  String displayLatin(String source) {
    return this == GHAYB_HARAKAT ? source.replace("bi'ilmikal ghoibi", "bi'ilmikal ghoiba") : source;
  }

  String displayMeaning(String source) {
    if (this == MEANING_PERSON) return source.replace("kokohkanlah kami", "kokohkanlah aku").replace("kami menemui-Mu", "aku menemui-Mu");
    return this == MEANING_REPEAT ? source.replace("berlindung berlindung", "berlindung") : source;
  }
}
