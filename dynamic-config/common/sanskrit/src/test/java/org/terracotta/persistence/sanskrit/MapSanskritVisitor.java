/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.persistence.sanskrit;

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
