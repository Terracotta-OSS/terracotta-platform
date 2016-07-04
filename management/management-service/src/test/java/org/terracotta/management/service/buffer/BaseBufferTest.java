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
package org.terracotta.management.service.buffer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.collection.IsArrayWithSize.emptyArray;
import static org.junit.Assert.assertThat;
import static org.terracotta.management.service.TestConstants.BUFFER_SIZE;

/**
 * Base class for testing all kinds of ring buffer implementations with different types.
 *
 * @author RKAV
 */
public abstract class BaseBufferTest<E> {
  protected PartitionedRingBuffer<E> bufferUnderTest;
  protected Random randGenerator;

  @Before
  public void setup() {
    bufferUnderTest = getBufferUnderTest(BUFFER_SIZE);
    randGenerator = new Random(System.currentTimeMillis());
  }

  @Test
  public void testEmpty() {
    Assert.assertEquals(0, bufferUnderTest.size(-1));
    Assert.assertEquals(getNumPartitions() * BUFFER_SIZE, bufferUnderTest.capacity(-1));
    assertThat(bufferUnderTest.toArray(getArrayType()), emptyArray());
  }

  @Test
  public void testOneInsert() {
    bufferUnderTest.insert(0, getOneItem());
    Assert.assertEquals(1, bufferUnderTest.size(-1));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testOneInsertOneRemove() {
    E item = getOneItem();
    bufferUnderTest.insert(0, item);
    Assert.assertEquals(1, bufferUnderTest.size(0));
    Assert.assertFalse(bufferUnderTest.hasOverSpillThresholdReached());
    assertThat(bufferUnderTest.toArray(getArrayType()), arrayContaining(item));
    Assert.assertEquals(0, bufferUnderTest.size(-1));
  }

  @Test
  public void testMultiInsertMultiRemove() {
    for (int i = 0; i < 10000; i++) {
      bufferUnderTest.insert(0, getOneItem());
      if (((i + 1) % 10) == 0) {
        Assert.assertFalse(bufferUnderTest.hasOverSpillThresholdReached());
        E[] currentCachedItems = bufferUnderTest.toArray(getArrayType());
        Assert.assertEquals(10, currentCachedItems.length);
      }
    }
    Assert.assertEquals(0, bufferUnderTest.size(-1));
    assertThat(bufferUnderTest.toArray(getArrayType()), emptyArray());
  }

  @Test
  public void testWithWrapping() {
    for (int i = 0; i < 4 * BUFFER_SIZE + 2; i++) {
      bufferUnderTest.insert(0, getOneItem());
      if (((i + 1) % (randGenerator.nextInt(3) + 1)) != 0) {
        E[] currentCachedItems = bufferUnderTest.toArray(getArrayType());
        assertThat(currentCachedItems.length, greaterThan(0));
      }
    }
    int size = bufferUnderTest.size(-1);
    // now things have wrapped around several times
    // empty the current contents
    E[] currentCachedItems = bufferUnderTest.toArray(getArrayType());
    Assert.assertEquals(size, currentCachedItems.length);
    Assert.assertEquals(0, bufferUnderTest.size(-1));

    // now verify a normal test
    testMultiInsertMultiRemove();
  }

  @Test
  public void testWithOverflow() {
    for (int i = 0; i < 4 * BUFFER_SIZE + 2; i++) {
      bufferUnderTest.insert(0, getOneItem());
      if (((i + 1) % (randGenerator.nextInt(3) + 1)) != 0) {
        E[] currentCachedItems = bufferUnderTest.toArray(getArrayType());
        assertThat(currentCachedItems.length, greaterThan(0));
      }
    }
    int size = bufferUnderTest.size(-1);
    // now things have wrapped around several times
    // empty the current contents
    E[] currentCachedItems = bufferUnderTest.toArray(getArrayType());
    Assert.assertEquals(size, currentCachedItems.length);
    Assert.assertEquals(0, bufferUnderTest.size(-1));

    // now verify a normal test
    testMultiInsertMultiRemove();
  }

  @Test
  public void testSingleProducerSingleConsumerRemoveAll() {
    assertNProducerSingleConsumer((ai) -> {
      Collection<E> itemCollection = bufferUnderTest.removeAll();
      if (itemCollection.size() > 0) {
        ai.addAndGet(itemCollection.size());
        return true;
      }
      return false;
    }, false, Objects::equals, 1);
  }

  @Test
  public void testSingleProducerSingleConsumerToArray() {
    assertNProducerSingleConsumer((ai) -> {
      E[] itemCollection = bufferUnderTest.toArray(getArrayType());
      if (itemCollection.length > 0) {
        ai.addAndGet(itemCollection.length);
        return true;
      }
      return false;
    }, false, Objects::equals, 1);
  }

  @Test
  public void testSingleProducerSingleConsumerWithOverflow() {
    assertNProducerSingleConsumer(
        (ai) -> {
          E[] itemCollection = bufferUnderTest.toArray(getArrayType());
          if (itemCollection.length > 0) {
            ai.addAndGet(itemCollection.length);
            return true;
          }
          return false;
        },
        true,
        (i, r) -> r <= i,
        1);
  }

  protected abstract PartitionedRingBuffer<E> getBufferUnderTest(int size);
  protected abstract E getOneItem();
  protected abstract Class<E[]> getArrayType();

  protected int getNumPartitions() {
    return 1;
  }

  protected void assertNProducerSingleConsumer(final Predicate<AtomicInteger> actualConsumer,
                                                  boolean allowOverflow,
                                                  final BiPredicate<Integer, Integer> assertionPredicate,
                                                  int numProducers) {
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(1 + numProducers);
    AtomicInteger numInserted = new AtomicInteger(0);
    AtomicInteger numRemoved = new AtomicInteger(0);

    for (int i = 0; i < numProducers; i++) {
      // producer thread
      new Thread(new ProducerOrConsumerSimulator(
          doneLatch,
          startLatch,
          (x) -> {
            bufferUnderTest.insert(x, getOneItem());
            numInserted.getAndIncrement();
            return true;
          },
          (x) -> bufferUnderTest.size(x) >= BUFFER_SIZE - 1,
          allowOverflow, i)).start();
    }

    // consumer thread
    new Thread(new ProducerOrConsumerSimulator(
        doneLatch,
        startLatch,
        (i) -> actualConsumer.test(numRemoved),
        null,
        allowOverflow, -1)).start();

    startLatch.countDown();
    try {
      doneLatch.await();
    } catch (InterruptedException ignored) {
    }
    Collection<E> itemCollection = bufferUnderTest.removeAll();
    numRemoved.addAndGet(itemCollection.size());
    Assert.assertTrue(assertionPredicate.test(numInserted.get(), numRemoved.get()));
    Assert.assertEquals(0, bufferUnderTest.size(-1));
  }
}
