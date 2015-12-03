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
package org.terracotta.management.service.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.management.service.buffer.PartitionedRingBuffer;
import org.terracotta.management.service.buffer.impl.SinglePartitionLockFreeRingBuffer;

import static org.terracotta.management.service.TestConstants.BUFFER_SIZE;

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
    producerUnderTest.pushManagementMessage(new byte[1024]);
    byte[][] bufferedMessageArray = messageCache.toArray(byte[][].class);
    Assert.assertEquals(1, bufferedMessageArray.length);
    Assert.assertEquals(1024, bufferedMessageArray[0].length);
    bufferedMessageArray = messageCache.toArray(byte[][].class);
    Assert.assertEquals(0, bufferedMessageArray.length);
  }
}
