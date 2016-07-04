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
