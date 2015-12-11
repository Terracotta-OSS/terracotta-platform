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
