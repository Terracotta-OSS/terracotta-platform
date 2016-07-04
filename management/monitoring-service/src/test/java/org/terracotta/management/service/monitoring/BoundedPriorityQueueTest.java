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
package org.terracotta.management.service.monitoring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.LinkedList;
import java.util.Queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class BoundedPriorityQueueTest {

  @Test
  public void test_eviction() throws InterruptedException {
    Queue<Integer> evicted = new LinkedList<>();
    BoundedEvictingPriorityQueue<Integer> queue = new BoundedEvictingPriorityQueue<>(5, Integer::compareTo, evicted::offer);

    queue.offer(5);
    queue.offer(4);
    queue.offer(1);
    queue.offer(3);
    queue.offer(2);

    assertEquals(Integer.valueOf(1), queue.peek());
    assertEquals(5, queue.size());
    assertEquals(0, evicted.size());

    queue.offer(0);

    assertEquals(Integer.valueOf(0), queue.peek());
    assertEquals(5, queue.size());
    assertEquals(1, evicted.size());
    assertEquals(Integer.valueOf(1), evicted.poll());
  }

  @Test
  public void test_nearly_unbounded() throws InterruptedException {
    BoundedPriorityQueue<Wraper> queue = new BoundedPriorityQueue<>(Integer.MAX_VALUE);

    queue.poll();

    queue.offer(new Wraper(5));
    queue.offer(new Wraper(4));
    queue.offer(new Wraper(1));
    queue.offer(new Wraper(3));
    queue.offer(new Wraper(2));

    assertEquals(1, queue.poll().n.intValue());
    assertEquals(2, queue.poll().n.intValue());
    assertEquals(3, queue.poll().n.intValue());
    assertEquals(4, queue.poll().n.intValue());
    assertEquals(5, queue.poll().n.intValue());
  }

  @Test(timeout = 3000)
  public void test_bounded() throws InterruptedException {

    BoundedPriorityQueue<Wraper> queue = new BoundedPriorityQueue<>(3, Wraper::compareTo);

    assertTrue(queue.offer(new Wraper(5)));
    assertTrue(queue.offer(new Wraper(4)));
    assertTrue(queue.offer(new Wraper(1)));

    assertFalse(queue.offer(new Wraper(3)));

    assertEquals(1, queue.poll().n.intValue());
    assertEquals(4, queue.poll().n.intValue());

    assertTrue(queue.offer(new Wraper(6)));
    assertTrue(queue.offer(new Wraper(2)));
    assertFalse(queue.offer(new Wraper(3)));

    assertEquals(2, queue.poll().n.intValue());
    assertEquals(5, queue.poll().n.intValue());
    assertEquals(6, queue.poll().n.intValue());

    assertEquals(0, queue.size());
  }

  private static class Wraper implements Comparable<Wraper> {
    final Integer n;

    public Wraper(Integer n) {
      this.n = n;
    }

    @Override
    public int compareTo(Wraper o) {
      return this.n.compareTo(o.n);
    }
  }
}
