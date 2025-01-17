/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.server.configuration.nomad.persistence.sanskrit;

import org.terracotta.json.Json;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.SanskritObject;
import org.terracotta.persistence.sanskrit.change.SanskritChangeVisitor;

import java.util.LinkedHashMap;
import java.util.Map;

public class SanskritMapVisitor implements SanskritChangeVisitor {

  private final Map<String, Object> result = new LinkedHashMap<>();

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
  public void setObject(String key, SanskritObject value) throws SanskritException {
    if (value == null) {
      result.put(key, null);
    } else {
      SanskritMapVisitor childVisitor = new SanskritMapVisitor();
      value.accept(childVisitor);
      Map<String, Object> map = childVisitor.getMap();
      result.put(key, map);
    }
  }

  @Override
  public void set(String key, Object value, String version) throws SanskritException {
    if (value instanceof SanskritObject) {
      setObject(key, (SanskritObject) value);
    } else if (value instanceof Long) {
      setLong(key, (Long) value);
    } else if (value instanceof String) {
      setString(key, (String) value);
    } else {
      // null or complex object or other numbers
      result.put(key, value);
    }
  }

  @Override
  public void removeKey(String key) {
    result.put(key, Null.NULL);
  }

  public static final class Null implements Json.Null {
    public static final Null NULL = new Null();
  }
}
