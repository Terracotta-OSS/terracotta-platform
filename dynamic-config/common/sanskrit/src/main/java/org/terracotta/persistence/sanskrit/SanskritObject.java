/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit;

/**
 * An interface representing complex data to be held against a key.
 */
public interface SanskritObject {
  void accept(SanskritVisitor visitor);

  <T> T getObject(String key, Class<T> type);

  String getString(String key);

  Long getLong(String key);

  SanskritObject getObject(String key);
}
