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
import org.terracotta.management.service.TestMessageCallback;
import org.terracotta.voltron.management.MessageDeliveryInfrastructureService;
import org.terracotta.voltron.management.consumer.MessageConsumer;
import org.terracotta.voltron.management.producer.MessageProducer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.terracotta.management.service.TestConstants.PAUSE;

/**
 * Test management service default implementation.
 *
 * TODO: Add configuration to management service (file a separate TAB). This will not only help
 * external configuration, but also help in varying the configuration in automated testing and making
 * the tests faster.
 * TODO: Add other tests (e.g. registry) once these functionality is added to management service.
 *
 * @author RKAV
 */
public class DefaultManagementServiceTest {
  private DefaultManagementService mnmServiceUnderTest;

  @Before
  public void setup() {
    mnmServiceUnderTest = new DefaultManagementService();
  }

  @Test
  public void testEndToEndMessageProductionAndConsumption() {
    TestMessageCallback messageCallback = new TestMessageCallback(1024);
    MessageDeliveryInfrastructureService mdis = mnmServiceUnderTest.getMessageDeliveryInfrastructure();
    MessageProducer<byte[]> producer = mdis.createMessageProducer(byte[].class);
    AtomicReference<MessageConsumer<byte[]>> consumerReference = new AtomicReference<>();

    // since we have registered the producer already, the onCreate will be called
    // immediately.
    mdis.registerMessageConsumerListener(byte[].class, consumerReference::set);

    MessageConsumer<byte[]> consumer = consumerReference.get();

    assertEndToEndFunctioning(producer, consumer, messageCallback);

  }


  @Test
  public void testEndToEndMessageProductionWithConsumerRegisteringEarly() {
    TestMessageCallback messageCallback = new TestMessageCallback(1024);
    MessageDeliveryInfrastructureService mdis = mnmServiceUnderTest.getMessageDeliveryInfrastructure();

    AtomicReference<MessageConsumer<byte[]>> consumerReference = new AtomicReference<>();
    // since we have not registered the producer, the onCreate will be called
    // later
    mdis.registerMessageConsumerListener(byte[].class, consumerReference::set);
    Assert.assertNull(consumerReference.get());

    MessageProducer<byte[]> producer = mdis.createMessageProducer(byte[].class);
    Assert.assertNotNull(consumerReference.get());

    MessageConsumer<byte[]> consumer = consumerReference.get();

    assertEndToEndFunctioning(producer, consumer, messageCallback);
  }

  private void assertEndToEndFunctioning(MessageProducer<byte[]> producer,
                                         MessageConsumer<byte[]> consumer,
                                         TestMessageCallback messageCallback) {
    consumer.setupPeriodicManagementMessageCollector(messageCallback);
    PAUSE(500);
    messageCallback.createMessagesToInsert(0, 256).forEach(producer::pushManagementMessage);
    PAUSE(500);
    messageCallback.createMessagesToInsert(256, 256).forEach(producer::pushManagementMessage);
    PAUSE(500);
    messageCallback.createMessagesToInsert(512, 256).forEach(producer::pushManagementMessage);
    PAUSE(500);
    messageCallback.createMessagesToInsert(768, 256).forEach(producer::pushManagementMessage);

    messageCallback.waitToConsume(5, TimeUnit.SECONDS, 1024);
    messageCallback.verifyMessages();
  }
}
