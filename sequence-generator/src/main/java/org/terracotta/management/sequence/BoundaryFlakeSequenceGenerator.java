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
 * <ul>
 * <li>http://www.slideshare.net/davegardnerisme/unique-id-generation-in-distributed-systems</li>
 * <li>https://github.com/mumrah/flake-java</li>
 * <li>https://github.com/boundary/flake</li>
 * <li>https://github.com/rholder/fauxflake</li>
 * <li>https://github.com/hibernate/hibernate-orm/blob/master/hibernate-testing/src/main/java/org/hibernate/testing/cache/Timestamper.java</li>
 * </ul>
 * For this implementation we will use:
 * <pre>
 * SEQUENCE = TIMESTAMP + NODE_ID + SEQUENCE_ID
 * NODE_ID = MAC + PID
 * SEQUENCE_ID = CLASSLOADER_ID + INSTANCE_ID + SEQUENCE_MS
 * </pre>
 * Where:
 * <ul>
 * <li>TIMESTAMP: 46 bits - enough for next 500 years</li>
 * <li>MAC: 48 bits - full mac address</li>
 * <li>PID: 16 bits - latest bits of the PID / VMID</li>
 * <li>CLASSLOADER_ID: 32 bits - classloader hashcode</li>
 * <li>INSTANCE_ID: 14 bits - number of instances per classloader</li>
 * <li>SEQUENCE_MS: 18 bits - seq id within the same millisecond</li>
 * </ul>
 * <p>
 * This leads to 3 longs (192 bits).
 * <p>
 * This generator will be able to generate a maximum of about 262,144 unique sequence numbers / millisecond / instance / classloader / JVM / machine.
 *
 * @author Mathieu Carbou
 */
public final class BoundaryFlakeSequenceGenerator implements SequenceGenerator {

  private static final int PID_BITLENGTH = 16;
  private static final long PID_BITMASK = (1L << PID_BITLENGTH) - 1;

  private static final int SEQ_BITLENGTH = 18;
  private static final long SEQ_BITMASK = (1L << SEQ_BITLENGTH) - 1;

  private static final int INSTANCE_BITLENGTH = 18;
  private static final int INSTANCE_BITMASK = (1 << INSTANCE_BITLENGTH) - 1;

  private static final IntCyclicRangeCounter INSTANCE_ID = new IntCyclicRangeCounter(0, INSTANCE_BITMASK);

  private final TimeSource timeSource;
  private final long nodeId;
  private final long instanceId;
  private final AtomicLong timeAndSeq = new AtomicLong(); // 44 bits TS + 20 bits sequence

  public BoundaryFlakeSequenceGenerator() {
    this(TimeSource.SYSTEM);
  }

  public BoundaryFlakeSequenceGenerator(TimeSource timeSource) {
    long clId = getClass().getClassLoader().hashCode();
    byte[] mac = readMAC();
    long macId = 0;
    for (int i = 0; i < 6; i++) {
      macId = (macId << 8) | (mac[i] & 0XFF);
    }

    this.timeSource = timeSource;
    this.nodeId = (macId << PID_BITLENGTH) | (readPID() & PID_BITMASK);
    this.instanceId = ((clId << INSTANCE_BITLENGTH) | (INSTANCE_ID.getAndIncrement() & INSTANCE_BITMASK)) << SEQ_BITLENGTH;
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
