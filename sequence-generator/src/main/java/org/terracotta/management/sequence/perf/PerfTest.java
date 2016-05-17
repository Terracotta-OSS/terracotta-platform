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

package org.terracotta.management.sequence.perf;

import org.terracotta.management.sequence.BoundaryFlakeSequenceGenerator;
import org.terracotta.management.sequence.TimeSource;

import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;

/**
 * TIP: run with: -ea -server -Xmx512M -Xms512M -verbose:gc
 *
 * @author Mathieu Carbou
 */
public class PerfTest {

  private static BoundaryFlakeSequenceGenerator generator = new BoundaryFlakeSequenceGenerator(TimeSource.SYSTEM);

  public static void main(String[] args) throws InterruptedException {
    {
      int[] counts = new int[10];
      for (int i = 0; i < counts.length; i++) {
        counts[i] = genMs(1000);
      }
      System.out.println("Number of generated sequence per ms: " + toStat(counts) + "\n" + Arrays.toString(counts));
    }
    {
      final CyclicBarrier barrier = new CyclicBarrier(Runtime.getRuntime().availableProcessors());
      final int[][] counts = new int[barrier.getParties()][];
      Thread[] threads = new Thread[barrier.getParties()];
      for (int i = 0; i < barrier.getParties(); i++) {
        final int tid = i;
        counts[i] = new int[10];
        threads[i] = new Thread("thread-" + tid) {
          @Override
          public void run() {
            try {
              barrier.await();
            } catch (Exception ignored) {
              return;
            }
            for (int i = 0; i < counts[tid].length; i++) {
              counts[tid][i] = genMs(1000);
            }
          }
        };
        threads[i].start();
      }
      for (Thread thread : threads) {
        thread.join();
      }
      for (int i = 0; i < counts.length; i++) {
        System.out.println("{thread-" + i + "}\nNumber of generated sequence per ms: " + toStat(counts[i]) + "\n" + Arrays.toString(counts[i]));
      }
    }
  }

  private static Stat toStat(int[] counts) {
    Arrays.sort(counts);
    int mean = 0;
    for (long count : counts) {
      mean += count;
    }
    return new Stat(counts[0], counts[counts.length - 1], mean / counts.length);
  }

  private static int genMs(int durationMs) {
    int n = 0;
    long time = System.currentTimeMillis();
    while (generator.next().getTimestamp() - time < durationMs) {
      n++;
    }
    return n / durationMs;
  }

  static class Stat {
    final int mean;
    final int min;
    final int max;

    Stat(int min, int max, int mean) {
      this.min = min;
      this.max = max;
      this.mean = mean;
    }

    @Override
    public String toString() {
      return "min=" + mean + ", max=" + max + ", mean=" + mean;
    }
  }
}
