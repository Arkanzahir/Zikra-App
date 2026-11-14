package com.zikra.app.migration;

import java.util.*;

/** Positive inventory: newly added preferences/databases are excluded until reviewed. */
public final class BackupPolicy {
  public static final int SNAPSHOT_BYTES = 8 * 1024 * 1024, MAX_ROWS = 50000;
  public static final Map<String, List<String>> DATABASES;
  private static final Map<String, Set<String>> PREFERENCES;
  static {
    Map<String, List<String>> databases = new LinkedHashMap<>();
    databases.put("zikra.db", Arrays.asList("checklist_days", "tasks", "assistant_actions"));
    databases.put("study-schedule.db", Collections.singletonList("weekly_classes"));
    databases.put("assistant_chats.db", Arrays.asList("conversations", "metadata"));
    databases.put("faith.db", Arrays.asList("plans", "entries", "day_notes"));
    databases.put("zikra_hadith.db", Collections.singletonList("bookmarks"));
    for (Map.Entry<String, List<String>> entry : databases.entrySet())
      entry.setValue(Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
    DATABASES = Collections.unmodifiableMap(databases);
    Map<String, Set<String>> preferences = new LinkedHashMap<>();
    preferences.put("zikra_preferences", set("profile_display_name", "user_role", "education_level",
        "onboarding_complete", "onboarding_story_version", "shake_enabled", "last_chapter", "last_ayah",
        "last_surah", "last_verse_key", "last_mushaf_page", "last_mushaf_chapter", "last_mushaf_verse",
        "last_hadith_id", "hadith_bookmarks", "reading_theme", "reading_arabic_size",
        "reading_translation_size", "reader_word_sync", "mushaf_zoom",
        "prayer_pause_focus", "prayer_quiet_classes", "prayer_mode_subuh", "prayer_mode_dzuhur",
        "prayer_mode_ashar", "prayer_mode_maghrib", "prayer_mode_isya"));
    preferences.put("zikra_home_appearance", set("theme"));
    preferences.put("quran_audio_options", set("reciter", "repeats", "loop", "follow", "scroll_speed", "reader_audio_folded"));
    preferences.put("zikra_location", set("latitude", "longitude", "zone", "label", "source", "selected_at", "accuracy"));
    PREFERENCES = Collections.unmodifiableMap(preferences);
  }
  private BackupPolicy() {}
  private static Set<String> set(String... values) { return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values))); }
  public static Set<String> preferenceGroups() { return PREFERENCES.keySet(); }
  public static boolean preferenceAllowed(String group, String key) {
    return PREFERENCES.containsKey(group) && PREFERENCES.get(group).contains(key);
  }
  public static String preferenceType(String group, String key) {
    if (!preferenceAllowed(group, key)) throw new IllegalArgumentException("Preference not transferable");
    if (group.equals("zikra_location")) {
      if (set("latitude", "longitude", "selected_at").contains(key)) return "long";
      return key.equals("accuracy") ? "float" : "string";
    }
    if (group.equals("quran_audio_options")) {
      if (key.equals("reciter")) return "string";
      return set("repeats", "scroll_speed").contains(key) ? "int" : "boolean";
    }
    if (key.equals("hadith_bookmarks")) return "set";
    if (set("onboarding_complete", "shake_enabled", "reader_word_sync", "prayer_pause_focus", "prayer_quiet_classes").contains(key)) return "boolean";
    if (set("onboarding_story_version", "last_chapter", "last_ayah", "last_mushaf_page", "last_mushaf_chapter",
        "last_mushaf_verse", "reading_arabic_size", "reading_translation_size", "mushaf_zoom").contains(key)) return "int";
    return "string";
  }
}
