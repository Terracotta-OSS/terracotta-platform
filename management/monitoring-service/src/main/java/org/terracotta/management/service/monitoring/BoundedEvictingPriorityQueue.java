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
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;

/**
 * A special version of {@link PriorityBlockingQueue} that is bounded.
 * Threads won't wait to insert element.
 * <p>
 * If the queue is full, the oldest element is discarded and the new one enters the queue
 *
 * @author Mathieu Carbou
 */
final class BoundedEvictingPriorityQueue<V> extends AbstractQueue<V> {

  private final Queue<V> queue;
  private final Consumer<V> evictionListener;

  public BoundedEvictingPriorityQueue(int maxSize) {
    this.queue = new BoundedPriorityQueue<>(maxSize);
    this.evictionListener = v -> {};
  }

  public BoundedEvictingPriorityQueue(int maxSize, Comparator<? super V> comparator) {
    this.queue = new BoundedPriorityQueue<>(maxSize, comparator);
    this.evictionListener = v -> {};
  }

  public BoundedEvictingPriorityQueue(int maxSize, Comparator<? super V> comparator, Consumer<V> evictionListener) {
    this.queue = new BoundedPriorityQueue<>(maxSize, comparator);
    this.evictionListener = evictionListener;
  }

  public BoundedEvictingPriorityQueue(int maxSize, Consumer<V> evictionListener) {
    this.queue = new BoundedPriorityQueue<>(maxSize);
    this.evictionListener = evictionListener;
  }

  @Override
  public Iterator<V> iterator() {
    return queue.iterator();
  }

  @Override
  public int size() {
    return queue.size();
  }

  /**
   * Forces an offer to be done.
   * If the queue is full, calls {@link #poll()} to remove the oldest element of the queue;
   *
   * @return true if the element can be inserted. Returns false if the current thread has been interrupted.
   */
  @Override
  public boolean offer(V v) {
    boolean offered;
    while (!(offered = queue.offer(v)) && !Thread.currentThread().isInterrupted()) {
      V evicted = queue.poll();
      if (evicted != null) {
        evictionListener.accept(evicted);
      }
    }
    return offered;
  }

  @Override
  public V poll() {
    return queue.poll();
  }

  @Override
  public V peek() {
    return queue.peek();
  }

}
