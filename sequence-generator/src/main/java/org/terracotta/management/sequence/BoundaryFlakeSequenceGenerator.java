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

import java.util.concurrent.atomic.AtomicLong;

import static org.terracotta.management.sequence.Defaults.INSTANCE_BITLENGTH;
import static org.terracotta.management.sequence.Defaults.INSTANCE_BITMASK;
import static org.terracotta.management.sequence.Defaults.SEQ_BITLENGTH;
import static org.terracotta.management.sequence.Defaults.SEQ_BITMASK;

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
 * <li>TIMESTAMP: 46 bits - enough for next >200 years</li>
 * <li>MAC: 48 bits - full mac address</li>
 * <li>PID: 16 bits - latest bits of the PID / VMID</li>
 * <li>CLASSLOADER_ID: 32 bits - classloader hashcode</li>
 * <li>INSTANCE_ID: 14 bits - number of instances per classloader</li>
 * <li>SEQUENCE_MS: 18 bits - seq id within the same millisecond</li>
 * </ul>
 * <p>
 * This leads to 3 longs (192 bits).
 * <p>
 * This generator will generate a maximum of about 262,144 sequence numbers / millisecond / instance / classloader / JVM / machine,
 * with high probability to be unique if node id is unique.
 *
 * @author Mathieu Carbou
 */
public final class BoundaryFlakeSequenceGenerator implements SequenceGenerator {

  private static final IntCyclicRangeCounter INSTANCE_ID = new IntCyclicRangeCounter(0, INSTANCE_BITMASK);

  private final TimeSource timeSource;
  private final long nodeId;
  private final long instanceId;
  private final AtomicLong timeAndSeq = new AtomicLong(); // 44 bits TS + 20 bits sequence

  public BoundaryFlakeSequenceGenerator() {
    this(TimeSource.BEST, NodeIdSource.BEST);
  }

  public BoundaryFlakeSequenceGenerator(TimeSource timeSource, NodeIdSource nodeIdSource) {
    long clId = getClass().getClassLoader().hashCode();
    this.timeSource = timeSource;
    this.nodeId = nodeIdSource.getNodeId();
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

}
