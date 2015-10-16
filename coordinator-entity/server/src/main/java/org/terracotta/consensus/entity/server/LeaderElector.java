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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Alex Snaps
 */
public class LeaderElector<K, V> {

  private final PermitFactory<V> factory;
  private final ConcurrentMap<K, BlockingDeque<V>> leaderQueues = new ConcurrentHashMap<K, BlockingDeque<V>>();

  public LeaderElector(PermitFactory<V> factory) {
    this.factory = factory;
  }

  public Object enlist(K key, V value) {
    return null;
  }

  public void releasePermit(K key, Object permit) {
    if (permit == null) {
      throw new NullPointerException("Permit can't be null");
    }
  }

  public void dropPermit(K key) {
  }

  public void delist(K key, V value) {
  }

  public boolean isLeader(final K key, final V participant) {
    final BlockingDeque<V> vs = leaderQueues.get(key);
    return vs == null ? false : vs.peek().equals(participant);
  }

  public List<V> getAllWaitingOn(final K key) {
    return null;
  }

  public void delistAll(org.terracotta.entity.ClientDescriptor clientDescriptor) {

  };

}
