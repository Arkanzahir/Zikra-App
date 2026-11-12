package com.zikra.app.discovery;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bounded RFC4180 reader for the publisher's versioned Indonesian export. */
public final class HadeethEncCsv {
  public static final String VERSION = "1.22.0";
  public static final int COUNT = 2260;
  public static final long BYTES = 10706985L;
  public static final String SHA256 = "6049e4f92c6c1d40f06993a7c57e5808aafffb5b65da2b12f2f6ede4b056a2c8";
  public static final String URL = "https://hadeethenc.com/downloads/csv/HadeethEnc.com_id-v1.22.0.csv";
  public static final String UPDATE_URL = "https://hadeethenc.com/en/check/id/v1.22.0";
  private static final String[] COLUMNS = {"id", "title_ar", "title", "hadith_text_ar",
      "hadith_text", "explanation_ar", "explanation", "benefits_ar", "benefits", "grade_ar",
      "takhrij_ar", "grade", "takhrij", "lang", "link"};
  private static final int FIELD_LIMIT = 200000;

  private HadeethEncCsv() {}

  public static final class Record {
    private final String[] cells;
    public final String metadata;
    public final int number;

    private Record(String metadata, String[] cells) throws IOException {
      validateMetadata(metadata);
      if (cells.length != COLUMNS.length) throw new IOException("Invalid source columns");
      for (String cell : cells) {
        if (cell == null || cell.length() > FIELD_LIMIT || cell.indexOf('\0') >= 0)
          throw new IOException("Invalid source field");
      }
      if (!cells[0].matches("[1-9][0-9]{0,5}")) throw new IOException("Invalid source ID");
      number = Integer.parseInt(cells[0]);
      for (int required : new int[]{1, 2, 3, 4, 11, 12})
        if (cells[required].trim().isEmpty()) throw new IOException("Missing source content");
      if (!"id".equals(cells[13]) || !sourceUrl(number).equals(cells[14]))
        throw new IOException("Invalid source language or link");
      this.metadata = metadata;
      this.cells = cells.clone();
    }

    public String id() { return "hadeethenc:id:" + number; }
    public String cell(int column) { return cells[column]; }
    public String json() throws org.json.JSONException {
      return new JSONObject().put("metadata", metadata)
          .put("cells", new JSONArray(Arrays.asList(cells))).toString();
    }
    public static Record fromJson(String value) throws Exception {
      if (value == null || value.length() > 1000000) throw new IOException("Invalid source record");
      JSONObject object = new JSONObject(value);
      JSONArray fields = object.getJSONArray("cells");
      if (fields.length() != COLUMNS.length) throw new IOException("Invalid source record");
      String[] cells = new String[COLUMNS.length];
      for (int i = 0; i < cells.length; i++) {
        if (!(fields.get(i) instanceof String)) throw new IOException("Invalid source field type");
        cells[i] = fields.getString(i);
      }
      return new Record(object.getString("metadata"), cells);
    }
  }

  public interface Sink { void accept(Record record) throws Exception; }

  public static String sourceUrl(int number) {
    return "https://hadeethenc.com/id/browse/hadith/" + number;
  }

  private static void validateMetadata(String metadata) throws IOException {
    if (metadata == null || metadata.length() > 4096 || metadata.indexOf('\0') >= 0
        || !Arrays.asList(metadata.split("\\r?\\n", -1)).containsAll(Arrays.asList(
            "# Language: Indonesia", "# Source: https://hadeethenc.com/id",
            "# Last update: 2026-05-10 17:47:10 (v" + VERSION + ")",
            "# Check for updates: " + UPDATE_URL,
            "# PLEASE DON'T REMOVE THIS IMPORTANT INFORMATION!")))
      throw new IOException("Invalid source version metadata");
  }

  /** Original cell contents, including line breaks and version notice, are never rewritten. */
  public static void parse(Reader input, Sink sink) throws Exception {
    Csv reader = new Csv(input);
    List<String> notice = reader.row();
    if (notice == null || notice.size() != COLUMNS.length) throw new IOException("Missing source notice");
    String metadata = notice.get(0);
    validateMetadata(metadata);
    for (int i = 1; i < notice.size(); i++)
      if (!notice.get(i).isEmpty()) throw new IOException("Invalid source notice");
    if (!Arrays.asList(COLUMNS).equals(reader.row())) throw new IOException("Invalid CSV header");
    Set<Integer> ids = new HashSet<>();
    int minimum = Integer.MAX_VALUE, maximum = 0;
    List<String> row;
    while ((row = reader.row()) != null) {
      Record record = new Record(metadata, row.toArray(new String[0]));
      if (ids.size() >= COUNT || !ids.add(record.number)) throw new IOException("Duplicate or excess ID");
      minimum = Math.min(minimum, record.number); maximum = Math.max(maximum, record.number);
      sink.accept(record);
    }
    if (ids.size() != COUNT || minimum != 1751 || maximum != 66541)
      throw new IOException("Incomplete source package");
  }

  private static final class Csv {
    private final PushbackReader reader;
    private boolean first = true;
    Csv(Reader reader) { this.reader = new PushbackReader(reader, 1); }

    List<String> row() throws IOException {
      List<String> fields = new ArrayList<>();
      StringBuilder field = new StringBuilder();
      boolean quoted = false, endedQuote = false, any = false;
      int characters = 0;
      while (true) {
        int next = reader.read();
        if (first) { first = false; if (next == 0xfeff) next = reader.read(); }
        if (next == -1) {
          if (quoted) throw new IOException("Unclosed CSV quote");
          if (!any && fields.isEmpty()) return null;
          fields.add(field.toString()); return fields;
        }
        any = true;
        if (++characters > 1000000 || field.length() > FIELD_LIMIT || fields.size() >= COLUMNS.length)
          throw new IOException("CSV budget exceeded");
        char value = (char) next;
        if (value == '\0') throw new IOException("Invalid CSV control character");
        if (quoted) {
          if (value == '"') {
            int following = reader.read();
            if (following == '"') field.append('"');
            else { quoted = false; endedQuote = true; if (following != -1) reader.unread(following); }
          } else field.append(value);
        } else if (value == ',') {
          fields.add(field.toString()); field.setLength(0); endedQuote = false;
        } else if (value == '\r' || value == '\n') {
          if (value == '\r') { int following = reader.read(); if (following != '\n' && following != -1) reader.unread(following); }
          fields.add(field.toString()); return fields;
        } else if (endedQuote) throw new IOException("Invalid text after CSV quote");
        else if (value == '"') {
          if (field.length() != 0) throw new IOException("Invalid CSV quote");
          quoted = true;
        } else field.append(value);
      }
    }
  }
}
