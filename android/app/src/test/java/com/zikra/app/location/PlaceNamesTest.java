package com.zikra.app.location;

import static org.junit.Assert.*;

import org.junit.Test;

public final class PlaceNamesTest {
  @Test
  public void keepsDistrictAndCityWithoutDuplicateAdministrativeNames() {
    assertEquals(
        "Sukolilo, Surabaya, Jawa Timur",
        PlaceNames.label("Sukolilo", "Surabaya", "Surabaya", "Jawa Timur"));
  }

  @Test
  public void missingAdministrativeNamesNeverInventAPlace() {
    assertEquals("Lokasi perangkat", PlaceNames.label(null, "", " ", null));
    assertEquals("Surabaya, Jawa Timur", PlaceNames.label(null, "Surabaya", null, "Jawa Timur"));
  }

  @Test
  public void sanitizesControlCharactersAndBoundsUntrustedLabels() {
    assertEquals(
        "Suko lilo, Surabaya",
        PlaceNames.label("Suko\nlilo", "Surabaya", "surabaya", "x".repeat(81)));
    assertEquals("Lokasi perangkat", PlaceNames.label("x".repeat(81), null, null, null));
  }
}
