package com.zikra.app.account;

import java.util.HashSet;
import java.util.Set;

/** A failed disk write must not reopen AI access in the running process. */
final class AccountPrivacyHold {
  interface Storage {
    boolean read(String key);
    boolean write(String key, boolean blocked);
  }

  private final Storage storage;
  private final Set<String> pending = new HashSet<>();

  AccountPrivacyHold(Storage storage) { this.storage = storage; }

  synchronized boolean held(String key) {
    if (pending.contains(key)) return true;
    try { return storage.read(key); }
    catch (RuntimeException unavailable) { return true; }
  }

  synchronized boolean set(String key, boolean blocked) {
    pending.add(key);
    try {
      if (!storage.write(key, blocked)) return false;
      pending.remove(key);
      return true;
    } catch (RuntimeException unavailable) {
      return false;
    }
  }
}
