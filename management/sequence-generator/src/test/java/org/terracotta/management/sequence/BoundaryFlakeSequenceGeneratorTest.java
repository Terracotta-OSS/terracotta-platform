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
package org.terracotta.management.sequence;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.terracotta.management.sequence.Defaults.readMacAddress;
import static org.terracotta.management.sequence.Defaults.readPID;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class BoundaryFlakeSequenceGeneratorTest {

  @Test
  public void test_generation() {
    final long now = System.currentTimeMillis(); // fix the TS for testing
    BoundaryFlakeSequenceGenerator generator = new BoundaryFlakeSequenceGenerator(new TimeSource.Fixed(now), NodeIdSource.MAC_PID);

    long instanceId = generator.getInstanceId();
    long nodeId = generator.getNodeId();

    byte[] mac = readMacAddress();
    mac[0] = (byte) (mac[0] & Byte.MAX_VALUE); // to make the node id positive
    assertEquals(6, mac.length);

    long pid = readPID();
    long seq = 0;

    Sequence sequence = generator.next();
    System.out.println(sequence);

    assertEquals(now, sequence.getTimestamp());
    assertEquals(nodeId, sequence.getNodeId());
    assertEquals(instanceId | seq, sequence.getSequenceId());

    ByteBuffer buffer = ByteBuffer.allocate(24);
    buffer.putLong(now);
    buffer.put(mac);
    buffer.put((byte) ((pid >>> 8) & 0XFF));
    buffer.put((byte) (pid & 0XFF));
    buffer.putLong(instanceId | seq);

    assertEquals(DatatypeConverter.printHexBinary(buffer.array()), sequence.toHexString());
    assertEquals(sequence, BoundaryFlakeSequence.fromHexString(sequence.toHexString()));
  }

  @Test
  public void test_sys_prop() {
    BoundaryFlakeSequenceGenerator generator = new BoundaryFlakeSequenceGenerator(TimeSource.BEST, NodeIdSource.BEST);
    assertEquals(5, generator.getNodeId());
    assertEquals(7, generator.getTimeSource().getTimestamp());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void test_generation_in_different_isolated_classloaders() throws Exception {
    URL[] cp = new URL[]{
        new File("target/classes").toURI().toURL(),
        new File("target/test-classes").toURI().toURL(),
    };
    URLClassLoader cl1 = new URLClassLoader(cp, ClassLoader.getSystemClassLoader().getParent());
    URLClassLoader cl2 = new URLClassLoader(cp, ClassLoader.getSystemClassLoader().getParent());

    Callable<String> runner1 = (Callable<String>) cl1.loadClass("org.terracotta.management.sequence.Runner").newInstance();
    Callable<String> runner2 = (Callable<String>) cl2.loadClass("org.terracotta.management.sequence.Runner").newInstance();

    String seq1 = runner1.call();
    String seq2 = runner2.call();

    System.out.println(seq1);
    System.out.println(seq2);

    assertNotEquals(seq1, seq2);
  }

  @Test
  @Ignore
  public void test_no_collisions() throws InterruptedException {
    final BoundaryFlakeSequenceGenerator generator = new BoundaryFlakeSequenceGenerator(TimeSource.SYSTEM, NodeIdSource.MAC_PID);
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
