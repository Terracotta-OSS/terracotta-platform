/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import com.terracottatech.utilities.Json;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogUtil {
  @SafeVarargs
  public static LogInfo createLog(Map<String, Object>... records) throws Exception {
    StringBuilder log = new StringBuilder();
    String lastHash = null;

    List<String> texts = new ArrayList<>();
    List<String> hashes = new ArrayList<>();

    for (Map<String, Object> record : records) {
      String timestamp = Instant.now().toString();
      String json = Json.toPrettyJson(record);

      String entryString = timestamp + System.lineSeparator() + json;

      String hashString = "";
      if (lastHash != null) {
        hashString = lastHash + System.lineSeparator() + System.lineSeparator();
      }

      hashString += entryString;

      lastHash = HashUtils.generateHash(hashString.getBytes("UTF-8"));

      log.append(entryString);
      log.append(System.lineSeparator());
      log.append(lastHash);
      log.append(System.lineSeparator());
      log.append(System.lineSeparator());

      texts.add(log.toString());
      hashes.add(HashUtils.generateHash(lastHash));
    }

    return new LogInfo(texts, hashes);
  }
}
