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
 * The Covered Software is Connection API.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.consensus.entity.server;

import java.util.List;

/**
 * @author Alex Snaps
 */
public interface LeaderElector<K, V> {
  Object enlist(K key, V value);

  void releasePermit(K key, Object permit);

  void dropPermit(K key);

  void delist(K key, V value);

  boolean isLeader(K key, V participant);

  List<V> getAllWaitingOn(K key);

}
