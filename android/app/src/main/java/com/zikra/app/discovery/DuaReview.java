package com.zikra.app.discovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Corrections apply to a reviewed source snapshot, never blindly to a reused provider ID. */
public enum DuaReview {
  NONE, SOURCE_CHANGED, WAKE_UP, WIND, RAIN_NUMBERING, GRADING_CONTEXT, AFTER_MEAL_NUMBERING;

  public static DuaReview forSource(int id, String title, String arabic, String meaning, String reference) {
    String expected; DuaReview review;
    switch (id) {
      case 6: expected = "e38beae72f57b70e3675893dadde7fcfa46e5fa56d879163fedfd03210af4c04"; review = WAKE_UP; break;
      case 18: expected = "88a82df458a0e89ad9ed4340462cf722db19ad34e91e3a469df96ac9bf49ad14"; review = WIND; break;
      case 20: expected = "1ab612312d815d856ca5735b07954b75b2f6cb1d830dc326a754e834872238d5"; review = RAIN_NUMBERING; break;
      case 96: expected = "811109d3e416464781ca6da504788eb2cdc09e4f79daf946905b91640a42e549"; review = GRADING_CONTEXT; break;
      case 139: expected = "baf12d0972ec1ba32eaa4aad2da62184752ea465cb2e430ceb86730e485df096"; review = AFTER_MEAL_NUMBERING; break;
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

  String displayArabic(String source) {
    return this == WAKE_UP ? source.replace("النُّشُوْرِ", "النُّشُورُ") : source;
  }
}
