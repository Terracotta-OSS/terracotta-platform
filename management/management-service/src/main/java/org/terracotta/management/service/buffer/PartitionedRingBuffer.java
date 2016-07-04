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
package org.terracotta.management.service.buffer;

import java.util.Collection;

/**
 * The Partitioned Ring buffer interface used by management service to pass messages
 * between the managed entities and the management entities.
 * <p>
 * TODO: Some of these methods may be redundant and can be removed from the interface post
 * performance analysis.
 * <p>
 * TODO: To make this layer more generic (and used outside the {@code ManagementService}),
 * separate interfaces could be provided to control the producer and consumer as well
 * as to separate the producer and consumer access to the ring buffer and to ensure that these
 * interfaces are used only as prescribed.
 * <p>
 * Note: More than one implementations of this interface are currently made available for
 * performance comparison when large amount of statistics passes through the management service.
 * Only one of these implementations may be eventually put into production.
 *
 * @author RKAV
 */
public interface PartitionedRingBuffer<E> {
  /**
   * Insert an item into the ring buffer in the given partition.
   *
   * @param partitionNo Partition number, if the buffer is partitioned.
   * @param item item to be inserted.
   */
  void insert(int partitionNo, E item);

  /**
   * Consume all the 'pending' items and return it as a {@code Collection}. This retrieves
   * from all partitions in a multi-partitioned ring buffer.
   *
   * @return a collection, an empty collection if no 'pending' items.
   */
  Collection<E> removeAll();

  /**
   * Consume all the 'pending' items and return it as a java object {@code Array}.
   * <p>
   * Retrieves/Consumes from all partitions.
   *
   * @param type Type of the array.
   * @return an array of 'pending' items.
   */
  E[] toArray(Class<E[]> type);

  /**
   * Return the total capacity of the partitioned buffer.
   *
   * @param partitionNo The partition number. If this is negative, total
   *                    capacity of all partitions will be returned.
   * @return total capacity
   */
  int capacity(int partitionNo);

  /**
   * Return the current used size of the buffer.
   * <p>
   * {@code capacity(partitionNo) - size(partitionNo)} provides the remaining
   * capacity of the given partition.
   *
   * @param partitionNo The partition number. If this is negative, total
   *                    capacity
   * @return used size of the buffer
   */
  int size(int partitionNo);

  /**
   * Returns true if any of the partitions are about to overspill (or has already overspilled)
   *
   * @return true, if any of the buffer has crossed the threshold.
   */
  boolean hasOverSpillThresholdReached();
}
