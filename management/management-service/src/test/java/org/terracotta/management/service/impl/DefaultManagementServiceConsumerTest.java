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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.management.service.TestMessageCallback;
import org.terracotta.management.service.buffer.PartitionedRingBuffer;
import org.terracotta.management.service.buffer.impl.SinglePartitionLockFreeRingBuffer;

import java.util.concurrent.TimeUnit;

import static org.terracotta.management.service.TestConstants.BUFFER_SIZE;

/**
 * Test the default implementation of the service consumer interface of management service.
 * <p>
 * These tests also tests/asserts and verifies the message ordering guarantee provided by the message consumer
 * interface. See {@link TestMessageCallback#verifyMessages()}
 *
 * @author RKAV
 */
public class DefaultManagementServiceConsumerTest {
  private PartitionedRingBuffer<byte[]> messageCache;
  private DefaultMessageConsumer<byte[]> consumerUnderTest;

  @Before
  public void setup() {
    messageCache = new SinglePartitionLockFreeRingBuffer<>(BUFFER_SIZE);
    consumerUnderTest = new DefaultMessageConsumer<>(messageCache);
  }

  @Test
  public void testScheduledConsumption() {
    TestMessageCallback messageCallback = new TestMessageCallback(2);
    messageCallback.createMessagesToInsert(0, 2).forEach((msg) -> messageCache.insert(0, msg));
    consumerUnderTest.setupPeriodicManagementMessageCollector(messageCallback);
    messageCallback.waitToConsume(10, TimeUnit.SECONDS, 2);
    messageCallback.verifyMessages();
    consumerUnderTest.shutdown();
  }

  @Test
  public void testScheduledConsumptionTwice() {
    TestMessageCallback messageCallback = new TestMessageCallback(4);

    messageCallback.createMessagesToInsert(0, 2).forEach((msg) -> messageCache.insert(0, msg));
    consumerUnderTest.setupPeriodicManagementMessageCollector(messageCallback);
    messageCallback.waitToConsume(10, TimeUnit.SECONDS, 2);

    messageCallback.createMessagesToInsert(2, 2).forEach((msg) -> messageCache.insert(0, msg));
    messageCallback.waitToConsume(10, TimeUnit.SECONDS, 2);

    messageCallback.verifyMessages();
    consumerUnderTest.shutdown();
  }

  @Test
  public void testScheduledConsumptionWithFailure() {
    TestMessageCallback messageCallback = new TestMessageCallback(8);

    messageCallback.createMessagesToInsert(0, 4).forEach((msg) -> messageCache.insert(0, msg));
    messageCallback.failCallback(1);
    consumerUnderTest.setupPeriodicManagementMessageCollector(messageCallback);
    messageCallback.waitToConsume(10, TimeUnit.SECONDS, 4);

    messageCallback.failCallback(1);
    messageCallback.createMessagesToInsert(4, 4).forEach((msg) -> messageCache.insert(0, msg));
    messageCallback.waitToConsume(10, TimeUnit.SECONDS, 4);

    messageCallback.verifyMessages();
    consumerUnderTest.shutdown();
  }
}
