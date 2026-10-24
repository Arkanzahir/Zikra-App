package com.zikra.app.discovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Corrections apply to a reviewed source snapshot, never blindly to a reused provider ID. */
public enum DuaReview {
  NONE, SOURCE_CHANGED, WAKE_UP, WIND, RAIN_NUMBERING, GRADING_CONTEXT, AFTER_MEAL_NUMBERING, ARABIC_INSTRUCTIONS, WITR_LAYOUT, TITLE_TYPO, TITLE_SCOPE;

  public static DuaReview forSource(int id, String title, String arabic, String meaning, String reference) {
    String expected; DuaReview review;
    switch (id) {
      case 6: expected = "e38beae72f57b70e3675893dadde7fcfa46e5fa56d879163fedfd03210af4c04"; review = WAKE_UP; break;
      case 18: expected = "88a82df458a0e89ad9ed4340462cf722db19ad34e91e3a469df96ac9bf49ad14"; review = WIND; break;
      case 20: expected = "1ab612312d815d856ca5735b07954b75b2f6cb1d830dc326a754e834872238d5"; review = RAIN_NUMBERING; break;
      case 96: expected = "811109d3e416464781ca6da504788eb2cdc09e4f79daf946905b91640a42e549"; review = GRADING_CONTEXT; break;
      case 139: expected = "baf12d0972ec1ba32eaa4aad2da62184752ea465cb2e430ceb86730e485df096"; review = AFTER_MEAL_NUMBERING; break;
      case 57: expected = "433e8629514ef9043ff2a4abb1b14ef5cbe97a5bf2cd084cde7e9fdb3da899da"; review = ARABIC_INSTRUCTIONS; break;
      case 76: expected = "5d0ea829c4b141121df3f8909c0f0d3523c50ce22f9cbdaa512b94b43387124c"; review = ARABIC_INSTRUCTIONS; break;
      case 77: expected = "c24cdde114b8f0b231ad844980f998c59c162d483b9d738eb168961c06510f6b"; review = ARABIC_INSTRUCTIONS; break;
      case 209: expected = "5ed03893e9255b9b5dbb5e348211976f0e5c087ce8fa0a855c0df966381561a8"; review = ARABIC_INSTRUCTIONS; break;
      case 225: expected = "0b975000eec579c5e3d1c5b5f688334381ab28987b0c0d3c782e704b16177ac8"; review = WITR_LAYOUT; break;
      case 16: expected = "fdcb8bb467ae5cc20d3dd13ac262e0aa4b7dbe08fc665d0df9f5a96d6f9da78f"; review = TITLE_SCOPE; break;
      case 66: expected = "8a24f0f9b8671c611d7c09a3ab5cd6e13f447b9d755b75c24dc8e334457f34dc"; review = TITLE_TYPO; break;
      case 103: expected = "963f3a81d8dec76266ec0b6138b50116bb3c0e50a12f21a0765fc327ac36ebdb"; review = TITLE_TYPO; break;
      case 121: expected = "380db7f08e5a580667356890af14077052b901f25023d31022ebd8576c9063f4"; review = TITLE_TYPO; break;
      default: return NONE;
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
    if (this == TITLE_TYPO) return source.replace("Nabil Ibrahim", "Nabi Ibrahim")
        .replace("Nabu Musa", "Nabi Musa").replace("Sebagai pembeli Petunjuk", "Sebagai pemberi Petunjuk");
    return source;
  }

  String displayArabic(String source) {
    if (this == WAKE_UP) return source.replace("النُّشُوْرِ", "النُّشُورُ");
    if (this == ARABIC_INSTRUCTIONS) return source.substring(source.indexOf('\n') + 1).trim();
    if (this == WITR_LAYOUT) return source.replace("(", "").replace(")", "").trim();
    return source;
  }
}
