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

import org.terracotta.management.service.buffer.BaseStatisticsBufferTest;
import org.terracotta.management.service.buffer.PartitionedRingBuffer;
import org.terracotta.management.stats.ContextualStatistics;

/**
 * Contextual statistics stored inside a thread safe ordered ring buffer.
 *
 * @author RKAV
 */
public class StatisticsSafeBufferTest extends BaseStatisticsBufferTest {
  @Override
  protected PartitionedRingBuffer<ContextualStatistics> getBufferUnderTest(int size) {
    return new SinglePartitionSafeRingBuffer<>(size);
  }
}
