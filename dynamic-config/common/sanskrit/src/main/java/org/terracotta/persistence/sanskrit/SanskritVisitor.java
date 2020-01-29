/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit;

public interface SanskritVisitor {
  default void setString(String key, String value) {}

  default void setLong(String key, long value) {}

  default void setObject(String key, SanskritObject value) {}

  default <T> void setExternal(String key, T value) {}
}
