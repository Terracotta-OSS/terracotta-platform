/**
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
package org.terracotta.consensus.entity.server;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.terracotta.consensus.entity.ElectionResult;
import org.terracotta.consensus.entity.ElectionResponse;
import org.terracotta.consensus.entity.LeaderOffer;

import static java.util.Collections.emptyList;

/**
 * @author Alex Snaps
 */
public class LeaderElector<K, V> {

  private static final boolean INITIAL_STATE_CLEAN = false;
  
  private final OfferFactory<V> offerFactory;
  private final ConcurrentMap<K, ElectionQueue> leaderQueues = new ConcurrentHashMap<K, ElectionQueue>();
  private ElectionChangeListener<K, V> listener;

  public LeaderElector(OfferFactory<V> factory) {
    this.offerFactory = factory;
  }

  public ElectionResponse enlist(K key, V value) {
    ElectionQueue queue = new ElectionQueue(key);
    ElectionResponse response = queue.runElectionOrAdd(value);
    ElectionQueue existing = leaderQueues.putIfAbsent(key, queue);
    if (existing == null) {
      return response;
    } else {
      return existing.runElectionOrAdd(value);
    }
  }

  public void setListener(ElectionChangeListener<K, V> listener) {
    this.listener = listener;
  }

  public void accept(K key, LeaderOffer permit) {
    final ElectionQueue queue = leaderQueues.get(key);
    if (queue == null) {
      throw new IllegalStateException("No election under that key");
    } else {
      queue.accept(permit);
    }
  }

  public void delist(final K key, V value) {
    final ElectionQueue queue = leaderQueues.get(key);
    if (queue != null) {
      queue.remove(value, new Runnable() {
        public void run() {
          leaderQueues.remove(key, queue);
        }
      });
    }
  }

  public List<V> getAllWaitingOn(final K key) {
    ElectionQueue queue = leaderQueues.get(key);
    if (queue == null) {
      return emptyList();
    } else {
      return queue.tail();
    }
  }

  public void delistAll(V value) {
    for (final Iterator<Entry<K, ElectionQueue>> it = leaderQueues.entrySet().iterator(); it.hasNext();) {
      Entry<K, ElectionQueue> entry = it.next();
      entry.getValue().remove(value, new Runnable() {
        public void run() {
          it.remove();
        }
      });
    }
  }

  private class ElectionQueue {

    private final LinkedList<V> leaderQueue = new LinkedList<V>();
    private final K key;
    
    private boolean hasLeader = false;
    private boolean clean = INITIAL_STATE_CLEAN;
    private LeaderOffer openOffer = null;

    ElectionQueue(K key) {
      this.key = key;
    }

    synchronized ElectionResponse runElectionOrAdd(V value) {
      int position = leaderQueue.indexOf(value);
      if (position == 0) {
        openOffer = offerFactory.createOffer(value, clean);
        clean = false;
        return openOffer;
      } else if (position > 0) {
        if (hasLeader) {
          return ElectionResult.NOT_ELECTED;
        } else {
          return ElectionResult.PENDING;
        }
      } else {
        leaderQueue.add(value);
        if (hasLeader) {
          return ElectionResult.NOT_ELECTED;
        } else if (leaderQueue.size() == 1) {
          openOffer = offerFactory.createOffer(value, clean);
          clean = false;
          return openOffer;
        } else {
          return ElectionResult.PENDING;
        }
      }
    }

    synchronized List<V> tail() {
      return Collections.unmodifiableList(leaderQueue.subList(1, leaderQueue.size()));
    }

    synchronized void accept(LeaderOffer offer) {
      if (openOffer.equals(offer)) {
        hasLeader = true;
        clean = true;
        openOffer = null;
      } else {
        throw new IllegalArgumentException("Leader offer not active");
      }
    }

    synchronized void remove(V departing, Runnable onEmpty) {
      V leader = leaderQueue.peek();
      if (leader != null && leader.equals(departing)) {
        hasLeader = false;
        openOffer = null;
        leaderQueue.remove(departing);
        V val = leaderQueue.peek();
        if (val != null) {
          listener.onDelist(key, val);
        }
      } else {
        leaderQueue.remove(departing);
      }
      if (clean == INITIAL_STATE_CLEAN && leaderQueue.isEmpty()) {
        onEmpty.run();
      }
    }
  }

}
