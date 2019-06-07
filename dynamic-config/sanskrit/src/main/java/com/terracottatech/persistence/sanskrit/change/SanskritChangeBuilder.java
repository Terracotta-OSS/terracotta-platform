/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.change;

import com.terracottatech.persistence.sanskrit.SanskritObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows construction of a change to be applied atomically to the append log.
 */
public class SanskritChangeBuilder {
  private List<SanskritChange> changes = new ArrayList<>();

  public static SanskritChangeBuilder newChange() {
    return new SanskritChangeBuilder();
  }

  private SanskritChangeBuilder() {}

  public SanskritChangeBuilder setString(String key, String value) {
    changes.add(new AddStringSanskritChange(key, value));
    return this;
  }

  public SanskritChangeBuilder setLong(String key, long value) {
    changes.add(new AddLongSanskritChange(key, value));
    return this;
  }

  public SanskritChangeBuilder setObject(String key, SanskritObject value) {
    changes.add(new AddObjectSanskritChange(key, value));
    return this;
  }

  public SanskritChangeBuilder removeKey(String key) {
    changes.add(new UnsetKeySanskritChange(key));
    return this;
  }

  public SanskritChange build() {
    return new MuxSanskritChange(changes);
  }
}
