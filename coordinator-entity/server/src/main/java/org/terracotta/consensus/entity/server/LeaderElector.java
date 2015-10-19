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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.terracotta.consensus.entity.Nomination;

/**
 * @author Alex Snaps
 */
public class LeaderElector<K, V> {

  private final PermitFactory<V> factory;
  private final ConcurrentMap<K, ElectionQueue<V>> leaderQueues = new ConcurrentHashMap<K, ElectionQueue<V>>();
  private final ConcurrentMap<K, ReentrantLock> locks = new ConcurrentHashMap<K, ReentrantLock>();
  private DelistListener<K, V> listener;

  public LeaderElector(PermitFactory<V> factory) {
    this.factory = factory;
  }

  public Object enlist(K key, V value) {
    leaderQueues.putIfAbsent(key, new ElectionQueue<V>(
        new LinkedBlockingQueue<V>(), ElectionState.NOT_ELECTED));
    locks.putIfAbsent(key, new ReentrantLock());
    return runElection(key, value);
  }
  
  public void setListener(DelistListener<K, V> listener) {
    this.listener = listener;
  }

  private Object runElection(K key, V value) {
    Lock lock = locks.get(key);
    lock.lock();
    try {
      ElectionQueue<V> queue = leaderQueues.get(key);
      switch (queue.getState()) {
      case ELECTED:
        addToQueue(queue, key, value);
        return null;
      case RUNNING:
        addToQueue(queue, key, value);
        return new Nomination();
      case NOT_ELECTED:
        addToQueue(queue, key, value);
        if (isLeader(key, value)) {
          queue.setState(ElectionState.RUNNING);
          return factory.createPermit(value);
        }
        // this should not happen
        return null;
      default:
        throw new IllegalStateException("Illegal Election state");
      }
    } finally {
      lock.unlock();
    }
  }

  private void addToQueue(ElectionQueue<V> queue, K key, V value) {
    BlockingQueue<V> valueQueue = queue.getQueue();
    valueQueue.offer(value);
  }

  public void accept(K key, Object permit) {
    Lock lock = locks.get(key);
    lock.lock();
    try {
      ElectionQueue<V> queue = leaderQueues.get(key);
      if (queue.getState() == ElectionState.RUNNING) {
        queue.setState(ElectionState.ELECTED);
      }
    } finally {
      lock.unlock();
    }
  }

  public void delist(K key, V value) {
    Lock lock = locks.get(key);
    lock.lock();
    try {
      ElectionQueue<V> queue = leaderQueues.get(key);
      if (isLeader(key, value)) {
        queue.setState(ElectionState.NOT_ELECTED);
        queue.getQueue().remove(value);
        V val = queue.getQueue().peek();
        if(val != null) {
          queue.setState(ElectionState.RUNNING);
          listener.onDelist(key, val, factory.createPermit(val));
        }
      }
      else {
        queue.getQueue().remove(value);
      }
    } finally {
      lock.unlock();
    }
  }

  private boolean isLeader(final K key, final V participant) {
    Lock lock = locks.get(key);
    lock.lock();
    try {
      BlockingQueue<V> vs = leaderQueues.get(key).getQueue();
      return vs == null ? false : vs.peek().equals(participant);
    } finally {
      lock.unlock();
    }
  }

  public List<V> getAllWaitingOn(final K key) {
    Lock lock = locks.get(key);
    lock.lock();
    try {
      BlockingQueue<V> queue = leaderQueues.get(key).getQueue();
      List<V> list = new ArrayList<V>(queue);
      list.remove(0);
      return list;
    } finally {
      lock.unlock();
    }
  }

  public void delistAll(V value) {
    for (K key : leaderQueues.keySet()) {
      delist(key, value);
    }
  }

  private enum ElectionState {
    RUNNING, ELECTED, NOT_ELECTED;
  }

  private static class ElectionQueue<V> {
    private final BlockingQueue<V> leaderQueue;
    private ElectionState state;

    public ElectionQueue(BlockingQueue<V> leaderQueue, ElectionState state) {
      this.leaderQueue = leaderQueue;
      this.state = state;
    }

    BlockingQueue<V> getQueue() {
      return this.leaderQueue;
    }

    ElectionState getState() {
      return this.state;
    }

    void setState(ElectionState state) {
      this.state = state;
    }
  }

}
