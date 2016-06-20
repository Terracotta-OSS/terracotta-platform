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

import org.terracotta.connection.entity.Entity;

import java.util.concurrent.ConcurrentMap;

public interface ConcurrentClusteredMap<K, V> extends ConcurrentMap<K, V>, Entity {
  long VERSION = 1;

  /**
   * Records the key and value classes to enable optimizations.
   *
   * @param keyClass the key class
   * @param valueClass the value class
   */
  void setTypes(Class<K> keyClass, Class<V> valueClass);
}
