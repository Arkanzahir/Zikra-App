package com.zikra.app.assistant;

/** Voice is a draft only. Keep the same UTF-16 input limit without splitting a surrogate pair. */
public final class VoiceDraft {
  private VoiceDraft() {}
  public static String normalize(String text) {
    if (text == null) return "";
    String value = text.trim();
    int end = Math.min(value.length(), OfflineAssistant.MAX_INPUT_LENGTH);
    if (end < value.length() && end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) end--;
    return value.substring(0, end);
  }
}
