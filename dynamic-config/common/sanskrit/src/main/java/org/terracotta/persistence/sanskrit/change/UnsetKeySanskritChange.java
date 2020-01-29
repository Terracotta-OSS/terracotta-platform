/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit.change;

/**
 * A data change corresponding to removing a mapping.
 */
public class UnsetKeySanskritChange implements SanskritChange {
  private final String key;

  public UnsetKeySanskritChange(String key) {
    this.key = key;
  }

  @Override
  public void accept(SanskritChangeVisitor visitor) {
    visitor.removeKey(key);
  }
}
