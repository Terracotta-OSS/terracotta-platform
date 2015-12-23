/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity Management Service.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.management.service.buffer.impl;

import org.terracotta.management.service.buffer.PartitionedRingBuffer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * A single producer single or multiple consumer <i>lock free</i> and <i>wait free</i> ring
 * buffer implementation.
 * <p>
 * There are several assumptions made for this implementation as follows:
 *     1. producer can continue overwriting even if consumers cannot catch up
 *     2. The size of the buffer is large enough to handle occasional blips in network
 *     3. There is a basic FIFO order guarantee with this implementation
 * <p>
 * This implementation is safe to use only in a single producer, single consumer scenario. Please
 * see {@link MultiPartitionLockFreeRingBuffer} for safe usage across multiple producers.
 *
 * @author RKAV
 */
public class SinglePartitionLockFreeRingBuffer<E> implements PartitionedRingBuffer<E> {
  private final static class Item<E> {
    private final AtomicLong writeSequence;
    private E item;

    private Item() {
      this.writeSequence = new AtomicLong(-1L);
      this.item = null;
    }
  }

  private final AtomicLong writeSequence;
  private final AtomicLong readSequence;
  private final Item<E>[] buffer;
  private final int mask;
  private final int overSpillThreshold;

  @SuppressWarnings("unchecked")
  public SinglePartitionLockFreeRingBuffer(int size) {
    this.buffer = (Item<E>[])new Item[size];
    this.writeSequence = new AtomicLong(-1L);
    this.readSequence = new AtomicLong(-1L);
    this.mask = size - 1;
    for (int i = 0; i < size; i++) {
      buffer[i] = new Item<>();
    }
    overSpillThreshold = Math.round(0.95f * (float)size);
  }

  @Override
  public int capacity(int partitionNo) {
    if (partitionNo > 0) {
      throw new IllegalArgumentException("Invalid Partition Number " + partitionNo + " specified.");
    }
    return buffer.length;
  }

  @Override
  public int size(int partitionNo) {
    if (partitionNo > 0) {
      throw new IllegalArgumentException("Invalid Partition Number " + partitionNo + " specified.");
    }
    int sz = (int)(writeSequence.get() - readSequence.get());
    return (sz > buffer.length) ? buffer.length : sz;
  }

  @Override
  public boolean hasOverSpillThresholdReached() {
    return size(0) >= overSpillThreshold;
  }

  /**
   * Insert a list of items into the ring buffer.
   * <p>
   * Assumptions:
   *   1. Only a single producer thread.
   *   2. Overflow between consumer and producer(s) is possible if the consumer is too slow.
   *      That is handled by simply overwriting unread contents with new contents.
   *
   * @param partitionNo the partition number..only valid value is 0 for this implementation.
   * @param item the list of items that needs to be inserted
   */
  @Override
  public void insert(int partitionNo, E item) {
    if (partitionNo != 0) {
      // this implementation does not support multi-partition
      throw new IllegalArgumentException("Invalid Partition Number " + partitionNo + " specified.");
    }
    long next = writeSequence.get() + 1;
    try {
      // use the better performing lazy set as this implementation follows the single writer priciple.
      int i = (int)(mask & next);
      buffer[i].item = item;
      buffer[i].writeSequence.lazySet(next);
    } finally {
      long lastRead = readSequence.get();
      while (next - lastRead > mask + 1) {
        // attempt to adjust read sequence as there is an overflow
        if (readSequence.compareAndSet(lastRead, next - mask - 1)) {
          break;
        }
        lastRead = readSequence.get();
      }
      writeSequence.lazySet(next);
    }
  }

  /**
   * Removes multiple items from the ring buffer and returns a collection of type {@code E}.
   * <p>
   * Assumes a single consumer.
   *
   * @return the next item in the ring buffer, null if empty
   */
  public Collection<E> removeAll() {
    @SuppressWarnings("unchecked")
    final Collection<E>[] ret = (Collection<E>[])new Collection[1];
    removeAllItems((sz) -> ret[0] = (sz > 0) ? new ArrayList<>(sz) : Collections.emptyList(),
        (j, item) -> ret[0].add(item));
    return ret[0];
  }

  /**
   *
   * Removes multiple items from the ring buffer and returns an array
   *
   * @param type type of array
   * @return array with consumed items.
   */
  @SuppressWarnings("unchecked")
  @Override
  public E[] toArray(Class<E[]> type) {
    final Object[] ret = new Object[1];
    removeAllItems((sz) -> ret[0] = Array.newInstance(type.getComponentType(), sz > 0 ? sz : 0),
        (j, item) -> ((E[])ret[0])[j] = item);
    return (E[])ret[0];
  }

  /**
   * Remove all items from the ring buffer.
   * <p>
   * Use the consumers to create and add the appropriate collection.
   *
   * @param creator The consumer that creates a collection based on type and size
   * @param adder The consumer that adds to the collection.
   */
  private void removeAllItems(IntConsumer creator, BiConsumer<Integer, E> adder) {
    long end = writeSequence.get();
    long start = readSequence.get();
    int sz = (int)(end - start);
    creator.accept(sz);
    if (sz <= 0) {
      return;
    }
    int startIdx = (int)(start + 1 & mask);
    int endIdx = (int)(end + 1 & mask);
    int j = 0;
    do {
      Item<E> item = buffer[startIdx];
      if (item.writeSequence.get() > start) {
        adder.accept(j, item.item);
        j++;
      } else {
        break;
      }
      startIdx = (startIdx + 1) & mask;
    } while (startIdx != endIdx);
    if (j > 0) {
      readSequence.lazySet(start + j);
    }
  }
}
