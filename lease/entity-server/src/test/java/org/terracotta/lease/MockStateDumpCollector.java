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
package org.terracotta.lease;

import org.terracotta.entity.StateDumpCollector;

import java.util.HashMap;
import java.util.Map;

public class MockStateDumpCollector implements StateDumpCollector {

  private final Map<String, Object> map = new HashMap<>();

  @Override
  public StateDumpCollector subStateDumpCollector(String s) {
    StateDumpCollector dumper = new MockStateDumpCollector();
    map.put(s, dumper);
    return dumper;
  }

  @Override
  public void addState(String s, Object s1) {
    map.put(s, s1);
  }

  public Object getMapping(String key) {
    return map.get(key);
  }

  @Override
  public String toString() {
    return map.toString();
  }
}
