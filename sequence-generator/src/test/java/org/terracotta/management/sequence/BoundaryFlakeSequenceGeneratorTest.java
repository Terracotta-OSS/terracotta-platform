/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.terracotta.management.sequence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Long.toHexString;
import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class BoundaryFlakeSequenceGeneratorTest {

  @Test
  public void test_generation() {
    final long now = System.currentTimeMillis(); // fix the TS for testing
    BoundaryFlakeSequenceGenerator generator = new BoundaryFlakeSequenceGenerator(new TimeSource() {
      @Override
      public long getTimestamp() {
        return now;
      }
    });

    long instanceId = generator.getInstanceId();
    long nodeId = generator.getNodeId();
    byte[] mac = BoundaryFlakeSequenceGenerator.readMAC();
    long pid = BoundaryFlakeSequenceGenerator.readPID();
    long seq = 0;

    Sequence sequence = generator.next();
    System.out.println(sequence);

    assertEquals(now, sequence.getTimestamp());
    assertEquals(nodeId, sequence.getNodeId());
    assertEquals(instanceId | seq, sequence.getSequenceId());

    StringBuilder expectedHex = new StringBuilder();
    expectedHex.append(pad(16, '0', toHexString(now)));
    for (byte b : mac) {
      expectedHex.append(pad(2, '0', toHexString(b & 0xff)));
    }
    expectedHex.append(pad(4, '0', toHexString(pid & 0xffff)));
    expectedHex.append(pad(16, '0', toHexString(instanceId | seq)));
    assertEquals(expectedHex.toString(), sequence.toHexString());
    assertEquals(sequence, BoundaryFlakeSequence.fromHexString(sequence.toHexString()));
  }

  private static String pad(int length, char character, String string) {
    char[] padding = new char[length - string.length()];
    Arrays.fill(padding, character);
    return new String(padding) + string;
  }

  @Test
  public void test_no_collisions() throws InterruptedException {
    final BoundaryFlakeSequenceGenerator generator = new BoundaryFlakeSequenceGenerator(TimeSource.SYSTEM);
    final Collection<Sequence> bag = new ConcurrentSkipListSet<Sequence>();
    // single thread generation
    {
      int counts = 0;
      for (int i = 0; i < 5; i++) {
        counts += gen(generator, bag, 1000);
      }
      System.out.println("Number of generated sequence within 10 sec: " + counts);
      assertEquals(counts, bag.size());
    }
    bag.clear();
    {
      final CyclicBarrier barrier = new CyclicBarrier(Runtime.getRuntime().availableProcessors());
      final AtomicInteger counts = new AtomicInteger();
      Thread[] threads = new Thread[barrier.getParties()];
      for (int i = 0; i < barrier.getParties(); i++) {
        final int tid = i;
        threads[i] = new Thread("thread-" + tid) {
          @Override
          public void run() {
            try {
              barrier.await();
            } catch (Exception ignored) {
              return;
            }
            for (int i = 0; i < 5; i++) {
              counts.addAndGet(gen(generator, bag, 1000));
            }
          }
        };
        threads[i].start();
      }
      for (Thread thread : threads) {
        thread.join();
      }
      System.out.println("Number of generated sequence within 10 sec for " + threads.length + " threads: " + counts.get());
      assertEquals(counts.get(), bag.size());
    }
  }

  private static int gen(SequenceGenerator generator, Collection<Sequence> bag, int durationMs) {
    int n = 0;
    long time = generator.getTimeSource().getTimestamp();
    Sequence seq;
    while ((seq = generator.next()).getTimestamp() - time < durationMs) {
      n++;
      bag.add(seq);
    }
    return n;
  }

}
