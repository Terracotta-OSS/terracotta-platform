/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import java.util.HashMap;
import java.util.Map;

public class MapSanskritVisitor implements SanskritVisitor {
  private Map<String, Object> result = new HashMap<>();

  public Map<String, Object> getMap() {
    return result;
  }

  @Override
  public void setString(String key, String value) {
    result.put(key, value);
  }

  @Override
  public void setLong(String key, long value) {
    result.put(key, value);
  }

  @Override
  public void setObject(String key, SanskritObject value) {
    MapSanskritVisitor childVisitor = new MapSanskritVisitor();
    value.accept(childVisitor);
    Map<String, Object> map = childVisitor.getMap();
    result.put(key, map);
  }

  @Override
  public <T> void setExternal(String key, T value) {
    result.put(key, value);
  }
}
