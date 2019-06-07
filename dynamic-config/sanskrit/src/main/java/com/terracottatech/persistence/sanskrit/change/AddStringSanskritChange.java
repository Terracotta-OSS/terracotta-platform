/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.change;

/**
 * A data change corresponding to adding a mapping between a (String) key and a String value.
 */
public class AddStringSanskritChange implements SanskritChange {
  private final String key;
  private final String value;

  public AddStringSanskritChange(String key, String value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public void accept(SanskritChangeVisitor visitor) {
    visitor.setString(key, value);
  }
}
