/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.persistence.sanskrit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Matches up the record hashes to the hashes found in the hash files.
 */
public class HashChecker {
  private static final Logger LOGGER = LoggerFactory.getLogger(HashChecker.class);

  private final Map<String, String> hashes = new HashMap<>(2);
  private boolean laterHash;
  private String removedFirst;
  private int hashCount;

  public HashChecker(String hash0, String hash1) {
    LOGGER.trace("HashChecker({}, {})", hash0, hash1);
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
      LOGGER.trace("check({}): {}", hash, false);
      return false;
    }

    String removed = hashes.remove(hash);
    LOGGER.trace("check({}): removed: {}", hash, removed);

    if (removed != null) {
      if (removedFirst == null) {
        removedFirst = removed;
      }
    } else {
      if (removedFirst != null) {
        throw new SanskritException("Found extra hash between final hashes: " + hash);
      }
    }

    LOGGER.trace("check({}): {}", hash, true);
    return true;
  }

  public String done() throws SanskritException {
    if (!hashes.isEmpty()) {
      throw new SanskritException("Hash existed but no matching record: " + hashes);
    }

    if (hashCount == 2) {
      LOGGER.trace("done(): {}", removedFirst);
      return removedFirst;
    }

    LOGGER.trace("done(): <none>");
    return null;
  }

  public String nextHashFile() {
    if (hashCount == 2) {
      LOGGER.trace("nextHashFile(): {}", removedFirst);
      return removedFirst;
    }

    if (hashCount == 1) {
      final String inverted = invert(removedFirst);
      LOGGER.trace("nextHashFile(): inverted: {}", inverted);
      return inverted;
    }

    LOGGER.trace("nextHashFile(): hash0");
    return "hash0";
  }

  private String invert(String removedFirst) {
    if ("hash0".equals(removedFirst)) {
      return "hash1";
    }

    return "hash0";
  }
}
