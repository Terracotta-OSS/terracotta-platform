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
package org.terracotta.management.service.buffer.impl;


import org.terracotta.management.service.buffer.PartitionedRingBuffer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Multiple producer, multi consumer implementation of the ring buffer.
 *
 * A pool of ring buffers, one per message producer. By providing a partitionNo
 * each producer can write to its own buffer. This ensure support of multiple producer,
 * multiple consumer model for the {@link PartitionedRingBuffer}, as long as there is
 * no ordering requirement across the partitions.
 *
 * @author RKAV
 */
public class MultiPartitionLockFreeRingBuffer<E> implements PartitionedRingBuffer<E> {
  private final PartitionedRingBuffer<E>[] ringBuffers;
  private final int perBufferSize;
  private final int maxPartitions;

  public MultiPartitionLockFreeRingBuffer(int maxPartitions, int perBufferSize) {
    @SuppressWarnings("unchecked")
    PartitionedRingBuffer<E>[] buffers = new PartitionedRingBuffer[maxPartitions];
    for (int i = 0; i < maxPartitions; i++) {
      buffers[i] = new SinglePartitionLockFreeRingBuffer<>(perBufferSize);
    }
    this.perBufferSize = perBufferSize;
    this.ringBuffers = buffers;
    this.maxPartitions = maxPartitions;
  }

  @Override
  public void insert(int partitionNo, E item) {
    if (partitionNo >= maxPartitions || partitionNo < 0) {
      throw new IllegalArgumentException("Invalid Partition Number " + partitionNo + " specified.");
    }
    ringBuffers[partitionNo].insert(0, item);
  }

  @Override
  public int capacity(int partitionNo) {
    if (partitionNo >= maxPartitions) {
      throw new IllegalArgumentException("Invalid Partition Number " + partitionNo + " specified.");
    }
    return (partitionNo < 0) ? perBufferSize * maxPartitions : perBufferSize;
  }

  @Override
  public int size(int partitionNo) {
    if (partitionNo >= maxPartitions) {
      throw new IllegalArgumentException("Invalid Partition Number " + partitionNo + " specified.");
    }
    int size = 0;
    if (partitionNo < 0) {
      for (PartitionedRingBuffer<E> buffer : ringBuffers) {
        size += buffer.size(0);
      }
    } else {
      size = ringBuffers[partitionNo].size(0);
    }
    return size;
  }

  @Override
  public boolean hasOverSpillThresholdReached() {
    boolean nearOverSpill = false;
    for (PartitionedRingBuffer<E> buffer : ringBuffers) {
      if (buffer.hasOverSpillThresholdReached()) {
        nearOverSpill = true;
        break;
      }
    }
    return nearOverSpill;
  }

  @Override
  public Collection<E> removeAll() {
    Collection<E> items = new ArrayList<>();
    for (PartitionedRingBuffer<E> buffer : ringBuffers) {
      items.addAll(buffer.removeAll());
    }
    return items;
  }

  @SuppressWarnings("unchecked")
  @Override
  public E[] toArray(Class<E[]> type) {
    E[][] objs = (E[][])new Object[ringBuffers.length][];
    int i = 0;
    int totalLength = 0;
    for (PartitionedRingBuffer<E> buffer : ringBuffers) {
      E[] items = buffer.toArray(type);
      if (items.length > 0) {
        objs[i++] = items;
        totalLength += items.length;
      }
    }
    return (totalLength > 0) ? concatArrays(totalLength, objs) :
        (E[])Array.newInstance(type.getComponentType(), 0);
  }

  /**
   * Concatenate an array of arrays..
   * <p>
   * TODO: Could ideally be moved into a ArrayUtils kind of clazz..
   *
   * @param totalLength total final length of returned arrays
   * @param items The array of arrays
   * @return concatenated array copy
   */
  @SafeVarargs
  private final E[] concatArrays(int totalLength, E[]... items) {
    E[] copy = Arrays.copyOf(items[0], totalLength);
    int lastPos = items[0].length;
    for (int i = 1; i < items.length; i++) {
      if (items[i] == null) {
        break;
      }
      System.arraycopy(items[i], 0, copy, lastPos, items[i].length);
      lastPos += items[i].length;
    }
    return copy;
  }
}
