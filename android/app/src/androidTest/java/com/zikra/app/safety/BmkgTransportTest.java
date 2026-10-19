package com.zikra.app.safety;

import android.test.InstrumentationTestCase;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/** Bound both declared and chunked bodies before JSON allocation. */
@SuppressWarnings("deprecation")
public final class BmkgTransportTest extends InstrumentationTestCase {
  public void testBoundedBodyAcceptsActualJsonAndRejectsOversizedDeclaredBody() throws Exception {
    String json = "{\"Infogempa\":{\"gempa\":{\"Wilayah\":\"Laut\",\"Magnitude\":\"4.1\"}}}";
    try (ResponseBody body = ResponseBody.create(MediaType.parse("application/json"), json)) {
      assertEquals("4.1", BmkgParser.earthquake(BmkgClient.readBounded(body)).magnitude);
    }
    byte[] excessive = new byte[262145];
    try (ResponseBody body = ResponseBody.create(MediaType.parse("application/json"), excessive)) {
      try {
        BmkgClient.readBounded(body);
        fail("Oversized declared body accepted");
      } catch (IOException expected) {
        assertTrue(expected.getMessage().contains("too large"));
      }
    }
  }

  public void testChunkedResponseIsBoundedEvenWithoutContentLength() throws Exception {
    Buffer bytes = new Buffer().write(new byte[262145]);
    try (ResponseBody body =
        new ResponseBody() {
          @Override
          public MediaType contentType() {
            return MediaType.parse("application/json");
          }

          @Override
          public long contentLength() {
            return -1;
          }

          @Override
          public BufferedSource source() {
            return bytes;
          }
        }) {
      try {
        BmkgClient.readBounded(body);
        fail("Oversized chunked body accepted");
      } catch (IOException expected) {
        assertTrue(expected.getMessage().contains("too large"));
      }
    }
  }
}
