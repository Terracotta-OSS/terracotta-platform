/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit;

import java.util.List;

public class LogInfo {
  private final List<String> texts;
  private final List<String> hashes;

  public LogInfo(List<String> texts, List<String> hashes) {
    this.texts = texts;
    this.hashes = hashes;
  }

  public String getText() {
    return texts.get(texts.size() - 1);
  }

  public String getHash() {
    return hashes.get(hashes.size() - 1);
  }

  public String getText(int record) {
    return texts.get(record);
  }

  public String getHash(int record) {
    return hashes.get(record);
  }
}
