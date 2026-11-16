package com.zikra.app.account;

import java.time.LocalDate;

public final class AdultAgePolicy {
  private AdultAgePolicy() {}
  public static boolean adult(LocalDate born, LocalDate today) {
    if(born==null||today==null||born.getYear()<1900||born.isAfter(today))throw new IllegalArgumentException("Invalid birth date");
    return !born.isAfter(today.minusYears(18));
  }
}
