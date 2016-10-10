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
package org.terracotta.management.service.monitoring.buffer;

import java.io.Serializable;
import java.util.Collection;
import java.util.Spliterators;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterator.SUBSIZED;

/**
 * A ring buffer for best-effort push in {@link org.terracotta.monitoring.IMonitoringProducer#pushBestEffortsData(String, Serializable)}.
 * Some data are discarded when the queue is full.
 *
 * @author Mathieu Carbou
 */
public class RingBuffer<V> implements ReadWriteBuffer<V> {

  private final LinkedBlockingQueue<V> queue;

  public RingBuffer(int maxSize) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("Bad size: " + maxSize + ". Max. size must be greater than 0.");
    }
    this.queue = new LinkedBlockingQueue<>(maxSize);
  }

  @Override
  public V put(V value) {
    if (value == null) {
      throw new NullPointerException();
    }
    V removed = null;
    while (!queue.offer(value)) {
      removed = queue.poll();
    }
    return removed;
  }

  @Override
  public V take() throws InterruptedException {
    return queue.take();
  }

  @Override
  public V take(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
    V v = queue.poll(timeout, unit);
    if (v == null) {
      throw new TimeoutException();
    }
    return v;
  }

  @Override
  public V read() {
    return queue.poll();
  }

  @Override
  public void drainTo(Collection<? super V> to) {
    queue.drainTo(to);
  }

  @Override
  public int size() {
    return queue.size();
  }

  @Override
  public void clear() {
    queue.clear();
  }

  @Override
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  @Override
  public Stream<V> stream() {
    return StreamSupport.stream(new Spliterators.AbstractSpliterator<V>(queue.size(), NONNULL | ORDERED | SIZED | SUBSIZED | DISTINCT | IMMUTABLE) {
      @Override
      public boolean tryAdvance(Consumer<? super V> action) {
        V v = read();
        if (v == null) {
          return false;
        }
        action.accept(v);
        return true;
      }
    }, false);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RingBuffer<?> that = (RingBuffer<?>) o;
    return queue.equals(that.queue);
  }

  @Override
  public int hashCode() {
    return queue.hashCode();
  }

  @Override
  public String toString() {
    return queue.toString();
  }

}
