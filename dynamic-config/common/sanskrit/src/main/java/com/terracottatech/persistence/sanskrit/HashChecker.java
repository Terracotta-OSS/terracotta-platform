/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import java.util.HashMap;
import java.util.Map;

/**
 * Matches up the record hashes to the hashes found in the hash files.
 */
public class HashChecker {
  private final Map<String, String> hashes = new HashMap<>(2);
  private boolean laterHash;
  private String removedFirst;
  private int hashCount;

  public HashChecker(String hash0, String hash1) {
    if (hash0 != null) {
      hashes.put(hash0, "hash0");
      hashCount++;
    }
    if (hash1 != null) {
      hashes.put(hash1, "hash1");
      hashCount++;
    }
  }

  public boolean check(String hash) throws SanskritException {
    if (hashes.isEmpty()) {
      if (laterHash) {
        throw new SanskritException("Found hashes after the last recorded hash");
      }
      laterHash = true;
      return false;
    }

    String removed = hashes.remove(hash);

    if (removed != null) {
      if (removedFirst == null) {
        removedFirst = removed;
      }
    } else {
      if (removedFirst != null) {
        throw new SanskritException("Found extra hash between final hashes: " + hash);
      }
    }

    return true;
  }

  public String done() throws SanskritException {
    if (!hashes.isEmpty()) {
      throw new SanskritException("Hash existed but no matching record: " + hashes);
    }

    if (hashCount == 2) {
      return removedFirst;
    }

    return null;
  }

  public String nextHashFile() {
    if (hashCount == 2) {
      return removedFirst;
    }

    if (hashCount == 1) {
      return invert(removedFirst);
    }

    return "hash0";
  }

  private String invert(String removedFirst) {
    if ("hash0".equals(removedFirst)) {
      return "hash1";
    }

    return "hash0";
  }
}
