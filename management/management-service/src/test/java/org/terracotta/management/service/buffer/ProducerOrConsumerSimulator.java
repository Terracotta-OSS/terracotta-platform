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
package org.terracotta.management.service.buffer;

import org.junit.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.function.IntPredicate;

import static org.terracotta.management.service.TestConstants.BUFFER_SIZE;
import static org.terracotta.management.service.TestConstants.TEST_MAX_WAIT_TIME_MILLIS;

/**
 * The producer or consumer simulator thread.
 *
 * @author RKAV
 */
public final class ProducerOrConsumerSimulator implements Runnable {
  private static final Object LOCK = new Object();

  private final CountDownLatch doneLatch;
  private final CountDownLatch startLatch;
  private final IntPredicate producerOrConsumer;
  private final IntPredicate bufferFull;
  private final boolean allowOverflow;
  private final boolean isProducer;
  private final int partitionNumber;

  public ProducerOrConsumerSimulator(CountDownLatch doneLatch,
                                     CountDownLatch startLatch,
                                     IntPredicate actualProducerOrConsumer,
                                     IntPredicate bufferFull,
                                     boolean allowOverflow,
                                     int partitionNumber) {
    this.doneLatch = doneLatch;
    this.startLatch = startLatch;
    this.producerOrConsumer = actualProducerOrConsumer;
    this.allowOverflow = allowOverflow;
    this.partitionNumber = partitionNumber;
    if (bufferFull == null) {
      this.isProducer = false;
      this.bufferFull = (x) -> false;
    } else {
      this.isProducer = true;
      this.bufferFull = bufferFull;
    }
  }

  @Override
  public void run() {
    try {
      startLatch.await();
      if (isProducer) {
        doProducerLoop();
      } else {
        doConsumerLoop();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      doneLatch.countDown();
    }
  }

  private void doProducerLoop() throws InterruptedException {
    int i = 0;
    while (i < ((BUFFER_SIZE * 4) - 1)) {
      if (producerOrConsumer.test(partitionNumber)) {
        i++;
      } else {
        Assert.fail("Unexpected behaviour from a producer");
      }
      if (!allowOverflow) {
        waitIfBufferFull();
      }
    }
  }

  private void doConsumerLoop() {
    boolean done = false;
    int numFailures = 0;
    while (!done) {
      if (producerOrConsumer.test(partitionNumber)) {
        numFailures = 0;
        notifyAndYield(allowOverflow);
      } else {
        numFailures++;
        yieldLoop();
      }
      if (numFailures++ > 100 && doneLatch.getCount() <= 1) {
        // only the consumer is waiting for buffer..all producers are gone
        done = true;
      }
    }
  }

  /**
   * Wait/block if the buffer is full.
   * <p>
   * Used by the producer to wait for the consumer to catch up.
   *
   * @throws InterruptedException
   */
  private void waitIfBufferFull() throws InterruptedException {
    long startTime = System.currentTimeMillis();
    synchronized (LOCK) {
      while (bufferFull.test(partitionNumber)) {
        // wait for producer and consumer to synchronize
        LOCK.wait(500);
        if (System.currentTimeMillis() - startTime > TEST_MAX_WAIT_TIME_MILLIS) {
          Assert.fail("Unexpected time out waiting for buffer to get consumed");
        }
      }
    }
  }

  /**
   * Consumer notifies if it was able to consume something
   * <p>
   * Note: the lock is simply to avoid exception as the consequence of a missing signal
   * is just an additional 500ms delay..
   *
   * @param allowOverflow signifies whether consumer should waste some time..
   */
  private void notifyAndYield(boolean allowOverflow) {
    synchronized (LOCK){
      LOCK.notify();
    }
    if (allowOverflow) {
      // slow the consumer thread down even further..
      yieldLoop();
    }
  }

  /**
   * Busy loop on CPU with some yields. Useful to inject some delays/
   */
  private void yieldLoop() {
    float f = 1.0f;
    Thread.yield();
    for (int i = 0; i < 10000; i++) {
      f = f * i * 2.0f;
    }
    Thread.yield();
  }
}
