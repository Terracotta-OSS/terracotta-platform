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
package org.terracotta.management.service.monitoring;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * A special version of {@link PriorityBlockingQueue} that is bounded.
 * Threads won't wait to insert element, and they can insert only if the queue has capacity.
 *
 * @author Mathieu Carbou
 */
final class BoundedPriorityQueue<V> extends AbstractQueue<V> {

  private final Semaphore permits;
  private final PriorityBlockingQueue<V> queue;

  public BoundedPriorityQueue(int maxSize) {
    this.queue = new PriorityBlockingQueue<>(11);
    this.permits = new Semaphore(maxSize);
  }

  public BoundedPriorityQueue(int maxSize, Comparator<? super V> comparator) {
    this.queue = new PriorityBlockingQueue<>(11, comparator);
    this.permits = new Semaphore(maxSize);
  }

  @Override
  public int size() {
    return queue.size();
  }

  @Override
  public V peek() {
    return queue.peek();
  }

  @Override
  public Iterator<V> iterator() {
    // read-only iterators
    Iterator<V> iterator = queue.iterator();
    return new Iterator<V>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public V next() {
        return iterator.next();
      }
    };
  }

  /**
   * Returns false immediately if an element cannot be inserted
   */
  @Override
  public boolean offer(V v) {
    if (permits.tryAcquire()) {
      try {
        boolean offered = queue.offer(v);
        if (!offered) {
          permits.release();
        }
        return offered;
      } catch (RuntimeException e) {
        permits.release();
        throw e;
      }
    }
    return false;
  }

  @Override
  public V poll() {
    V v = queue.poll();
    if (v != null) {
      permits.release();
    }
    return v;
  }

}
