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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * A thread safe no wait simple un partitioned ring buffer.
 * <p>
 * In this implementation, the producer does not wait for the consumer to empty when the buffer is full. Instead
 * it simply continues to overwrite. This implementation guarantees a FIFO ordering.
 * <p>
 * This implementation supports multiple producer, multiple consumer thread safe concurrent access of
 * the ring buffer and is thread safe.
 *
 * @author RKAV
 */
public class SinglePartitionSafeRingBuffer<E> implements PartitionedRingBuffer<E> {
  private final E[] buffer;
  private final int mask;
  private final int overSpillThreshold;
  private int head;
  private int tail;

  @SuppressWarnings("unchecked")
  public SinglePartitionSafeRingBuffer(int size) {
    this.buffer = (E[]) new Object[size];
    this.head = -1;
    this.tail = -1;
    this.mask = size - 1;
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
  public synchronized int size(int partitionNo) {
    if (partitionNo > 0) {
      throw new IllegalArgumentException("Invalid Partition Number " + partitionNo + " specified.");
    }
    return (head > tail) ? buffer.length - head + tail : tail - head;
  }

  @Override
  public boolean hasOverSpillThresholdReached() {
    return size(0) >= overSpillThreshold;
  }

  @Override
  public synchronized void insert(int partitionNo, E item) {
    if (partitionNo != 0) {
      throw new IllegalArgumentException("Invalid Partition Number " + partitionNo + " specified.");
    }
    tail = (tail + 1) & mask;
    if (tail == head) {
      // reader is not fast enough, move the head
      head = (head + 1) & mask;
    }
    buffer[tail] = item;
  }

  @Override
  public synchronized Collection<E> removeAll() {
    if (head == tail) {
      return Collections.emptyList();
    }
    Collection<E> itemCollection = new ArrayList<>(size(0));
    int startIdx = (head + 1) % buffer.length;
    int endIdx = (tail + 1) % buffer.length;
    head = tail;
    while (startIdx != endIdx) {
      itemCollection.add(buffer[startIdx]);
      startIdx = (startIdx + 1) & mask;
    }
    return itemCollection;
  }

  /**
   * Safely returns the 'pending' items in the buffer as an array of this type
   *
   * @param type array type
   * @return the 'pending' unread items as an array of elements
   */
  public synchronized E[] toArray(Class<E[]> type) {
    if (head == tail) {
      @SuppressWarnings("unchecked")
      E[] retVal = (E[])Array.newInstance(type.getComponentType(), 0);
      return retVal;
    }
    int startIdx = (head + 1) % buffer.length;
    int endIdx = (tail + 1) % buffer.length;
    head = tail;
    if (startIdx < endIdx) {
      return Arrays.copyOfRange(buffer, startIdx, endIdx, type);
    } else {
      E[] copy = Arrays.copyOfRange(buffer, startIdx, endIdx + buffer.length, type);
      System.arraycopy(buffer, 0, copy, buffer.length - startIdx, endIdx);
      return copy;
    }
  }
}
