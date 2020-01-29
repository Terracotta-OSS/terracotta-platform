/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit.change;

/**
 * A data change corresponding to adding a mapping between a (String) key and a long value.
 */
public class AddLongSanskritChange implements SanskritChange {
  private final String key;
  private final long value;

  public AddLongSanskritChange(String key, long value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public void accept(SanskritChangeVisitor visitor) {
    visitor.setLong(key, value);
  }
}
