/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.entity.map.common;

import java.io.Serializable;
import java.util.Map;

public class MapEntry<K, V> implements Map.Entry<K, V>, Serializable {

  private K key;
  private V value;

  public MapEntry() {
  }

  public MapEntry(K key, V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public K getKey() {
    return key;
  }

  public void setKey(K key) {
    this.key = key;
  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public V setValue(V newValue) {
    V oldValue = value;
    value = newValue;
    return oldValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MapEntry<?, ?> mapEntry = (MapEntry<?, ?>) o;

    if (!key.equals(mapEntry.key)) return false;
    return value != null ? value.equals(mapEntry.value) : mapEntry.value == null;

  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }
}
