package com.zikra.app.safety;

import java.time.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ContextualWindowTest {
  @Test public void usesSelectedLocalClockAndSkipsLateDelivery() {
    Instant instant = Instant.parse("2026-11-13T00:00:00Z");
    assertTrue(ContextualReminders.inWindow(instant.atZone(ZoneId.of("Asia/Jakarta")), 420));
    assertFalse(ContextualReminders.inWindow(instant.atZone(ZoneId.of("UTC")), 420));
    assertFalse(ContextualReminders.inWindow(instant.plusSeconds(7200).atZone(ZoneId.of("Asia/Jakarta")), 420));
  }
  @Test public void neverReplaysYesterdayWindowAcrossMidnight() {
    ZonedDateTime late = ZonedDateTime.of(2026, 11, 13, 23, 45, 0, 0, ZoneId.of("Asia/Jakarta"));
    assertTrue(ContextualReminders.inWindow(late, 23 * 60));
    assertFalse(ContextualReminders.inWindow(late.plusMinutes(30), 23 * 60));
  }
}
