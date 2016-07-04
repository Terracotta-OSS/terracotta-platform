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
package org.terracotta.management.service.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.management.service.buffer.PartitionedRingBuffer;
import org.terracotta.management.service.buffer.impl.SinglePartitionLockFreeRingBuffer;

import static org.terracotta.management.service.TestConstants.BUFFER_SIZE;
import static org.terracotta.management.service.TestConstants.DEFAULT_MESSAGE_SIZE;

/**
 *
 * @author RKAV
 */
public class DefaultManagementServiceProducerTest {
  private PartitionedRingBuffer<byte[]> messageCache;
  private DefaultMessageProducer<byte[]> producerUnderTest;

  @Before
  public void setup() {
    messageCache = new SinglePartitionLockFreeRingBuffer<>(BUFFER_SIZE);
    producerUnderTest = new DefaultMessageProducer<>(messageCache, 0);
  }

  @Test
  public void testMessageProduction() {
    producerUnderTest.pushManagementMessage(new byte[DEFAULT_MESSAGE_SIZE]);
    byte[][] bufferedMessageArray = messageCache.toArray(byte[][].class);
    Assert.assertEquals(1, bufferedMessageArray.length);
    Assert.assertEquals(DEFAULT_MESSAGE_SIZE, bufferedMessageArray[0].length);
    bufferedMessageArray = messageCache.toArray(byte[][].class);
    Assert.assertEquals(0, bufferedMessageArray.length);
  }
}
