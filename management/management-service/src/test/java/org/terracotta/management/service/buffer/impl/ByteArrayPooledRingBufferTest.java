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

import org.junit.Test;
import org.terracotta.management.service.buffer.BaseByteArrayBufferTest;
import org.terracotta.management.service.buffer.PartitionedRingBuffer;

import java.util.Collection;
import java.util.Objects;

import static org.terracotta.management.service.TestConstants.NUM_PARTITIONS_FOR_POOLED;

/**
 * Test the pooled buffer implementation with byte arrays as content.
 *
 * @author RKAV
 */
public final class ByteArrayPooledRingBufferTest extends BaseByteArrayBufferTest {
  @Override
  protected PartitionedRingBuffer<byte[]> getBufferUnderTest(int size) {
    return new MultiPartitionLockFreeRingBuffer<>(NUM_PARTITIONS_FOR_POOLED, size);
  }

  @Override
  protected int getNumPartitions() {
    return NUM_PARTITIONS_FOR_POOLED;
  }

  @Test
  public void testMultiProducerSingleConsumerRemoveAll() {
    assertNProducerSingleConsumer((ai) -> {
      Collection<byte[]> itemCollection = bufferUnderTest.removeAll();
      if (itemCollection.size() > 0) {
        ai.addAndGet(itemCollection.size());
        return true;
      }
      return false;
    }, false, Objects::equals, NUM_PARTITIONS_FOR_POOLED);
  }

  @Test
  public void testMultiProducerSingleConsumerToArray() {
    assertNProducerSingleConsumer((ai) -> {
      byte[][] itemCollection = bufferUnderTest.toArray(getArrayType());
      if (itemCollection.length > 0) {
        ai.addAndGet(itemCollection.length);
        return true;
      }
      return false;
    }, false, Objects::equals, NUM_PARTITIONS_FOR_POOLED);
  }

  @Test
  public void testSingleProducerSingleConsumerWithOverflow() {
    assertNProducerSingleConsumer(
        (ai) -> {
          byte[][] itemCollection = bufferUnderTest.toArray(getArrayType());
          if (itemCollection.length > 0) {
            ai.addAndGet(itemCollection.length);
            return true;
          }
          return false;
        },
        true,
        (i, r) -> r <= i,
        NUM_PARTITIONS_FOR_POOLED);
  }
}
