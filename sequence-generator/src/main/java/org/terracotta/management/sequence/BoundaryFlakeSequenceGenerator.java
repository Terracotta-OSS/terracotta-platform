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

import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sort of Boundary Flake, inspired by (and related docs):
 * - http://www.slideshare.net/davegardnerisme/unique-id-generation-in-distributed-systems
 * - https://github.com/mumrah/flake-java
 * - https://github.com/boundary/flake
 * - https://github.com/rholder/fauxflake
 * - https://github.com/hibernate/hibernate-orm/blob/master/hibernate-testing/src/main/java/org/hibernate/testing/cache/Timestamper.java
 * <p>
 * For this implementation we will use:
 * - 44 bits: timestamp - positive long - 44 bits is enough for next 500 years
 * - 64 bits: machine id - long - composed of full mac address (48 bits) with latest bits of the PID / VMID (16 bits)
 * - 32 bits: instance ID  - positive int - used to identify the generator instance
 * - 20 bits: sequence  - positive int - (incremented at each call for the same millisecond)
 * <p>
 * timestamp + sequence are put together in the same long (64 bits) for CAS performance
 * <p>
 * This leads to 3 longs (192 bits): timestamp + machine id + instance id with sequence
 *
 * @author Mathieu Carbou
 */
public final class BoundaryFlakeSequenceGenerator implements SequenceGenerator {

  private static final int SEQ_BITLENGTH = 20;
  private static final long SEQ_BITMASK = (1L << SEQ_BITLENGTH) - 1;
  private static final LongCyclicRangeCounter INSTANCE_ID = new LongCyclicRangeCounter(0, Integer.MAX_VALUE);

  private final TimeSource timeSource;
  private final long nodeId;
  private final long instanceId;
  private final AtomicLong timeAndSeq = new AtomicLong(); // 44 bits relative TS + 20 bits seq

  public BoundaryFlakeSequenceGenerator() {
    this(TimeSource.SYSTEM);
  }

  public BoundaryFlakeSequenceGenerator(TimeSource timeSource) {
    this.timeSource = timeSource;
    this.instanceId = INSTANCE_ID.getAndIncrement() << 32;
    byte[] mac = readMAC();
    long id = 0;
    for (int i = 0; i < 6; i++) {
      id = (id << 8) | (mac[i] & 0XFF);
    }
    nodeId = (id << 16) | (readPID() & 0xFFFF);
  }

  @Override
  public TimeSource getTimeSource() {
    return timeSource;
  }

  @Override
  public Sequence next() {
    while (true) {
      long min = timeSource.getTimestamp() << SEQ_BITLENGTH;
      long max = min + SEQ_BITMASK;
      for (long current = timeAndSeq.get(), update = Math.max(min, current + 1);
           update < max;
           current = timeAndSeq.get(), update = Math.max(min, current + 1)) {
        if (timeAndSeq.compareAndSet(current, update)) {
          return new BoundaryFlakeSequence(
              update >>> SEQ_BITLENGTH,
              nodeId,
              instanceId | (update & SEQ_BITMASK));
        }
      }
    }
  }

  long getInstanceId() {
    return instanceId;
  }

  long getNodeId() {
    return nodeId;
  }

  static byte[] readMAC() {
    try {
      byte[] mac;
      for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        if (!networkInterface.isLoopback() && (mac = networkInterface.getHardwareAddress()) != null) {
          return mac;
        }
      }
      throw new IllegalStateException("Unable to read MAC address");
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static long readPID() {
    String name = ManagementFactory.getRuntimeMXBean().getName();
    long pid = 0;
    for (int i = 0; i < name.length() && Character.isDigit(name.charAt(i)); i++) {
      pid = pid * 10 + Character.getNumericValue(name.charAt(i));
    }
    return pid;
  }

}
