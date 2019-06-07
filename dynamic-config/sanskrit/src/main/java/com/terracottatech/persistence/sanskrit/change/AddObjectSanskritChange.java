/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.change;

import com.terracottatech.persistence.sanskrit.SanskritObject;

/**
 * A data change corresponding to adding a mapping between a (String) key and a SanskritObject.
 */
public class AddObjectSanskritChange implements SanskritChange {
  private final String key;
  private final SanskritObject value;

  public AddObjectSanskritChange(String key, SanskritObject value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public void accept(SanskritChangeVisitor visitor) {
    visitor.setObject(key, value);
  }
}
