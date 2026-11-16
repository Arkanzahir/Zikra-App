package com.zikra.app.account;
import static org.junit.Assert.*;
import java.time.LocalDate;
import org.junit.Test;
public final class AdultAgePolicyTest {
 @Test public void exactBirthdayAndLeapBoundary(){
  assertTrue(AdultAgePolicy.adult(LocalDate.of(2008,11,15),LocalDate.of(2026,11,15)));
  assertFalse(AdultAgePolicy.adult(LocalDate.of(2008,11,16),LocalDate.of(2026,11,15)));
  assertFalse(AdultAgePolicy.adult(LocalDate.of(2008,2,29),LocalDate.of(2026,2,28)));
  assertTrue(AdultAgePolicy.adult(LocalDate.of(2008,2,29),LocalDate.of(2026,3,1)));
 }
 @Test public void invalidAndFutureDatesCannotRegister(){
  for(LocalDate born:new LocalDate[]{null,LocalDate.of(1899,1,1),LocalDate.of(2027,1,1)}){
   try{AdultAgePolicy.adult(born,LocalDate.of(2026,11,15));fail("Invalid birth date");}catch(IllegalArgumentException expected){}
  }
 }
}
