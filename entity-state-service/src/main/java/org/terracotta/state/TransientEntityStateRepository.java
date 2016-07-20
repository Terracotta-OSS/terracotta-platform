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

package org.terracotta.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 */
public class TransientEntityStateRepository implements EntityStateRepository {

  private ConcurrentMap<String, Map> stateMap = new ConcurrentHashMap<String, Map>();

  @Override
  public <K, V> Map<K, V> getOrCreateState(String name, Class<K> kClass, Class<V> vClass) {
    Map state = new ConcurrentHashMap<K, V>();
    Map actual =  stateMap.putIfAbsent(name, state);
    if (actual == null) {
      actual = state;
    }
    return actual;
  }

  @Override
  public void destroyState(String name) {
    if (stateMap.remove(name) == null) {
      throw new IllegalArgumentException("State for " + name + "does not exist.");
    }
  }
}
