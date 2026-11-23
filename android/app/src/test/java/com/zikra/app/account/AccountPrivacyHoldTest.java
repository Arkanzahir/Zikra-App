package com.zikra.app.account;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public final class AccountPrivacyHoldTest {
  private static final class Disk implements AccountPrivacyHold.Storage {
    final Map<String, Boolean> durable = new HashMap<>();
    final Map<String, Boolean> memory = new HashMap<>();
    boolean fail, throwWrite, throwRead;
    public boolean read(String key) {
      if (throwRead) throw new IllegalStateException("Unreadable preferences");
      return memory.getOrDefault(key, durable.getOrDefault(key, false));
    }
    public boolean write(String key, boolean blocked) {
      // SharedPreferences can update memory even when commit returns false.
      memory.put(key, blocked);
      if (throwWrite) throw new IllegalStateException("Disk unavailable");
      if (fail) return false;
      durable.put(key, blocked);
      return true;
    }
    void restart() { memory.clear(); }
  }

  @Test public void savedWithdrawalSurvivesNewProcessAndExplicitRenewalClearsIt() {
    Disk disk = new Disk();
    assertTrue(new AccountPrivacyHold(disk).set("account-a", true));
    disk.restart();
    AccountPrivacyHold restarted = new AccountPrivacyHold(disk);
    assertTrue(restarted.held("account-a"));
    assertTrue(restarted.set("account-a", false));
    disk.restart();
    assertFalse(new AccountPrivacyHold(disk).held("account-a"));
  }

  @Test public void failedWithdrawalKeepsProcessBlockedWithoutAssumingPersistence() {
    Disk disk = new Disk(); disk.fail = true;
    AccountPrivacyHold hold = new AccountPrivacyHold(disk);
    assertFalse(hold.set("account-a", true));
    disk.memory.clear();
    assertTrue(hold.held("account-a"));
    disk.restart();
    assertFalse(new AccountPrivacyHold(disk).held("account-a"));
  }

  @Test public void failedRenewalCannotReopenAccessThroughUpdatedPreferenceMemory() {
    Disk disk = new Disk();
    AccountPrivacyHold hold = new AccountPrivacyHold(disk);
    assertTrue(hold.set("account-a", true));
    disk.fail = true;
    assertFalse(hold.set("account-a", false));
    assertFalse(disk.read("account-a"));
    assertTrue(hold.held("account-a"));
    disk.restart();
    assertTrue(new AccountPrivacyHold(disk).held("account-a"));
    disk.fail = false;
    assertTrue(hold.set("account-a", false));
    assertFalse(hold.held("account-a"));
  }

  @Test public void writeExceptionBlocksUntilSuccessfulRetry() {
    Disk disk = new Disk(); disk.throwWrite = true;
    AccountPrivacyHold hold = new AccountPrivacyHold(disk);
    assertFalse(hold.set("account-a", true));
    assertTrue(hold.held("account-a"));
    disk.throwWrite = false;
    assertTrue(hold.set("account-a", true));
    disk.restart();
    assertTrue(new AccountPrivacyHold(disk).held("account-a"));
  }

  @Test public void unreadableStorageDeniesAccess() {
    Disk disk = new Disk(); disk.throwRead = true;
    assertTrue(new AccountPrivacyHold(disk).held("account-a"));
  }

  @Test public void failedWriteForOneAccountDoesNotBlockAnotherAccount() {
    Disk disk = new Disk(); disk.fail = true;
    AccountPrivacyHold hold = new AccountPrivacyHold(disk);
    assertFalse(hold.set("project-one:account-a", true));
    assertTrue(hold.held("project-one:account-a"));
    assertFalse(hold.held("project-one:account-b"));
    assertFalse(hold.held("project-two:account-a"));
  }
}
