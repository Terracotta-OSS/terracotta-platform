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

package org.terracotta.entity.map;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.map.common.BooleanResponse;
import org.terracotta.entity.map.common.ClearOperation;
import org.terracotta.entity.map.common.ConcurrentClusteredMap;
import org.terracotta.entity.map.common.ConditionalRemoveOperation;
import org.terracotta.entity.map.common.ConditionalReplaceOperation;
import org.terracotta.entity.map.common.ContainsKeyOperation;
import org.terracotta.entity.map.common.ContainsValueOperation;
import org.terracotta.entity.map.common.EntrySetOperation;
import org.terracotta.entity.map.common.EntrySetResponse;
import org.terracotta.entity.map.common.GetOperation;
import org.terracotta.entity.map.common.KeySetOperation;
import org.terracotta.entity.map.common.KeySetResponse;
import org.terracotta.entity.map.common.MapOperation;
import org.terracotta.entity.map.common.MapResponse;
import org.terracotta.entity.map.common.MapValueResponse;
import org.terracotta.entity.map.common.PutAllOperation;
import org.terracotta.entity.map.common.PutIfAbsentOperation;
import org.terracotta.entity.map.common.PutIfPresentOperation;
import org.terracotta.entity.map.common.PutOperation;
import org.terracotta.entity.map.common.RemoveOperation;
import org.terracotta.entity.map.common.SizeOperation;
import org.terracotta.entity.map.common.SizeResponse;
import org.terracotta.entity.map.common.ValueCollectionResponse;
import org.terracotta.entity.map.common.ValuesOperation;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
class TerracottaClusteredMap<K, V> implements ConcurrentClusteredMap<K, V> {
  private final EntityClientEndpoint<MapOperation, MapResponse> endpoint;

  TerracottaClusteredMap(EntityClientEndpoint<MapOperation, MapResponse> endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public void close() {
    this.endpoint.close();
  }

  @Override
  public int size() {
    Long size = ((SizeResponse)invokeWithReturn(new SizeOperation())).getSize();
    if (size > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else if (size <= 0) {
      return 0;
    } else {
      return size.intValue();
    }
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    return ((BooleanResponse)invokeWithReturn(new ContainsKeyOperation(key))).isTrue();
  }

  @Override
  public boolean containsValue(Object value) {
    return ((BooleanResponse)invokeWithReturn(new ContainsValueOperation(value))).isTrue();
  }

  @Override
  public V get(Object key) {
    return (V) ((MapValueResponse)invokeWithReturn(new GetOperation(key))).getValue();
  }

  @Override
  public V put(K key, V value) {
    return (V) ((MapValueResponse)invokeWithReturn(new PutOperation(key, value))).getValue();
  }

  @Override
  public V remove(Object key) {
    return (V) ((MapValueResponse)invokeWithReturn(new RemoveOperation(key))).getValue();
  }

  private MapResponse invokeWithReturn(MapOperation operation) {
    try {
      return endpoint.beginInvoke()
          .message(operation)
          .replicate(operation.operationType().replicate())
          .invoke()
          .get();
    } catch (Exception e) {
      throw new RuntimeException("Exception while processing map operation " + operation, e);
    }
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    invokeWithReturn(new PutAllOperation((Map<Object, Object>) m));
  }

  @Override
  public void clear() {
    invokeWithReturn(new ClearOperation());
  }

  @Override
  public Set<K> keySet() {
    return (Set<K>) ((KeySetResponse)invokeWithReturn(new KeySetOperation())).getKeySet();
  }

  @Override
  public Collection<V> values() {
    return (Collection<V>) ((ValueCollectionResponse)invokeWithReturn(new ValuesOperation())).getValues();
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return (Set<Map.Entry<K, V>>)(Object)((EntrySetResponse)invokeWithReturn(new EntrySetOperation())).getEntrySet();
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return (V) ((MapValueResponse)invokeWithReturn(new PutIfAbsentOperation(key, value))).getValue();
  }

  @Override
  public boolean remove(Object key, Object value) {
    return ((BooleanResponse)invokeWithReturn(new ConditionalRemoveOperation(key, value))).isTrue();
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return ((BooleanResponse) invokeWithReturn(new ConditionalReplaceOperation(key, oldValue, newValue))).isTrue();
  }

  @Override
  public V replace(K key, V value) {
    return (V) ((MapValueResponse) invokeWithReturn(new PutIfPresentOperation(key, value))).getValue();
  }
}
