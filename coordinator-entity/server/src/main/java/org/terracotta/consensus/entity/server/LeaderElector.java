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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.terracotta.consensus.entity.messages.Nomination;

/**
 * @author Alex Snaps
 */
public class LeaderElector<K, V> {

  private final PermitFactory<K, V> factory;
  private final ConcurrentMap<K, ElectionQueue<K, V>> leaderQueues = new ConcurrentHashMap<K, ElectionQueue<K, V>>();
  private DelistListener<K, V> listener;

  public LeaderElector(PermitFactory<K, V> factory) {
    this.factory = factory;
  }

  public Nomination enlist(K key, V value) {
    ElectionQueue queue = leaderQueues.putIfAbsent(key, new ElectionQueue<K, V>(key, factory, listener));
    return leaderQueues.get(key).runElectionOrAdd(value, queue == null);
  }

  public void setListener(DelistListener<K, V> listener) {
    this.listener = listener;
  }

  public void accept(K key, Nomination permit) {
    leaderQueues.get(key).accept(permit);
  }

  public void delist(final K key, V value) {
    leaderQueues.get(key).remove(value);
  }

  public List<V> getAllWaitingOn(final K key) {
    return leaderQueues.get(key).tail();
  }

  public void delistAll(V value) {
    for (K key : leaderQueues.keySet()) {
      delist(key, value);
    }
  }

  private enum ElectionState {
    RUNNING, ELECTED, NOT_ELECTED;
  }

  private static class ElectionQueue<K, V> {
    private final BlockingQueue<V> leaderQueue = new LinkedBlockingQueue<V>();
    private final K key;
    private final PermitFactory<K, V> factory;
    private ElectionState state = ElectionState.NOT_ELECTED;
    private final DelistListener listener;
    private final ReentrantLock lock = new ReentrantLock();
    private Nomination currentPermit;

    public ElectionQueue(K key, PermitFactory<K, V> factory,
        DelistListener listener) {
      if (listener == null) {
        throw new IllegalArgumentException("Listener cannot be null.");
      }
      this.key = key;
      this.listener = listener;
      this.factory = factory;
    }

    private Nomination runElectionOrAdd(V value, boolean elected) {
      lock.lock();
      try {
        switch (state) {
        case ELECTED:
          leaderQueue.offer(value);
          return null;
        case RUNNING:
          leaderQueue.offer(value);
          return factory.createPermit(this.key);
        case NOT_ELECTED:
          leaderQueue.offer(value);
          if (leaderQueue.peek().equals(value)) {
            state = ElectionState.RUNNING;
            return currentPermit = factory.createPermit(this.key, value, elected);
          }
          // this should not happen
          return null;
        default:
          throw new AssertionError("Illegal Election state");
        }
      } finally {
        lock.unlock();
      }
    }

    private List<V> tail() {
      lock.lock();
      try {
        List<V> list = new ArrayList<V>(leaderQueue);
        list.remove(0);
        return list;
      } finally {
        lock.unlock();
      }
    }

    private void accept(Object permit) {
      if (permit == null ) {
        throw new IllegalStateException("Null Permits cannot be accepted");
      }
      lock.lock();
      try {
        if(!currentPermit.equals(permit)) {
          throw new IllegalArgumentException("Wrong Nomination accepted"); 
        }
        if (state == ElectionState.RUNNING) {
          state = ElectionState.ELECTED;
        } else {
          throw new AssertionError("Illegal Election state");
        }
      } finally {
        lock.unlock();
      }
    }

    private void remove(V value) {
      lock.lock();
      try {
        V leader = null;
        if ((leader = leaderQueue.peek()) != null && leader.equals(value)) {
          state = ElectionState.NOT_ELECTED;
          leaderQueue.remove(value);
          V val = leaderQueue.peek();
          if (val != null) {
            state = ElectionState.RUNNING;
            listener.onDelist(key, val,
                currentPermit = factory.createPermit(this.key, val, false));
          }
        } else {
          leaderQueue.remove(value);
        }
      } catch (Exception e) {
        throw new IllegalStateException(e);
      } finally {
        lock.unlock();
      }
    }
  }

}
