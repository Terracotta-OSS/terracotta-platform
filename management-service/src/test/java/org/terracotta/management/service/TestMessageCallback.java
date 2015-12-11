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
package org.terracotta.management.service;

import org.junit.Assert;
import org.terracotta.voltron.management.consumer.MessageListener;
import org.terracotta.voltron.management.consumer.PreviousMessageAckPendingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.terracotta.management.service.TestConstants.DEFAULT_MESSAGE_SIZE;

/**
 * Test message callback.
 *
 * @author RKAV
 */
public class TestMessageCallback implements MessageListener<byte[]> {
  private final Semaphore consumedMessageTracker;
  private final Collection<byte[]> messageCollection;
  private final int maxMessages;
  private final AtomicInteger failureCount;

  public TestMessageCallback(int maxMessages) {
    this.consumedMessageTracker = new Semaphore(0, false);
    // insertion/retrieval order is important for test assertion, use array list
    this.messageCollection = new ArrayList<>();
    this.maxMessages = maxMessages;
    failureCount = new AtomicInteger(0);
  }

  @Override
  public void postMessages(Collection<byte[]> nextCollection) throws PreviousMessageAckPendingException {
    if (failureCount.get() > 0) {
      failureCount.decrementAndGet();
      throw new PreviousMessageAckPendingException("Simulating a failure");
    }
    messageCollection.addAll(nextCollection);
    // release the number of messages that are consumed, so that test waiting for consumption can come out
    consumedMessageTracker.release(nextCollection.size());
  }

  public void verifyMessages() {
    Assert.assertEquals(maxMessages, messageCollection.size());
    int i = 0;
    for (byte[] msg : messageCollection) {
      Assert.assertArrayEquals(createTestMessage(i++), msg);
    }
  }

  public Collection<byte[]> createMessagesToInsert(int start, int numMessages) {
    Collection<byte[]> createdMessages = new ArrayList<>(numMessages);
    for (int i = 0; i < numMessages; i++) {
      createdMessages.add(createTestMessage(start + i));
    }
    return createdMessages;
  }

  public void waitToConsume(long maxTimeToWait, TimeUnit unit, int numMessages) {
    try {
      Assert.assertTrue("Unexpected timeout waiting for messages",
          consumedMessageTracker.tryAcquire(numMessages, maxTimeToWait, unit));
    } catch (InterruptedException ignored) {
    }
  }

  public void failCallback(int times) {
    failureCount.addAndGet(times);
  }

  private byte[] createTestMessage(int start) {
    byte[] message = new byte[DEFAULT_MESSAGE_SIZE];
    for (int i = 0; i < DEFAULT_MESSAGE_SIZE; i++) {
      message[i] = (byte)('A' + (i + start) % 26);
    }
    return message;
  }
}
