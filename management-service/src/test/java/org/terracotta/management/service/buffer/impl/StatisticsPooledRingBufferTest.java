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

import org.terracotta.management.service.buffer.BaseStatisticsBufferTest;
import org.terracotta.management.service.buffer.PartitionedRingBuffer;
import org.terracotta.management.stats.ContextualStatistics;

import static org.terracotta.management.service.TestConstants.NUM_PARTITIONS_FOR_POOLED;

/**
 * Test the pooled ring buffer with statistics data..
 *
 * @author RKAV
 */
public class StatisticsPooledRingBufferTest extends BaseStatisticsBufferTest {
  @Override
  protected PartitionedRingBuffer<ContextualStatistics> getBufferUnderTest(int size) {
    return new MultiPartitionLockFreeRingBuffer<>(NUM_PARTITIONS_FOR_POOLED, size);
  }

  @Override
  protected int getNumPartitions() {
    return NUM_PARTITIONS_FOR_POOLED;
  }
}
