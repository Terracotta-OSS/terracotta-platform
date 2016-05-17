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

import javax.xml.bind.DatatypeConverter;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * @author Mathieu Carbou
 */
public final class BoundaryFlakeSequence implements Sequence, Serializable {

  private final long timestamp;
  private final long nodeId;
  private final long sequence;

  BoundaryFlakeSequence(long timestamp, long nodeId, long sequence) {
    this.timestamp = timestamp;
    this.nodeId = nodeId;
    this.sequence = sequence;
  }

  @Override
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public long getNodeId() {
    return nodeId;
  }

  @Override
  public long getSequenceId() {
    return sequence;
  }

  @Override
  public String toHexString() {
    ByteBuffer buffer = ByteBuffer.allocate(24);
    buffer.putLong(timestamp);
    buffer.putLong(nodeId);
    buffer.putLong(sequence);
    return DatatypeConverter.printHexBinary(buffer.array());
  }

  @Override
  public String toString() {
    return toHexString();
  }

  @Override
  public int compareTo(Sequence o) {
    long d = timestamp - o.getTimestamp();
    if (d == 0) {
      d = nodeId - o.getNodeId();
      if (d == 0) {
        d = sequence - o.getSequenceId();
      }
    }
    return (int) d;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BoundaryFlakeSequence that = (BoundaryFlakeSequence) o;
    return timestamp == that.timestamp && nodeId == that.nodeId && sequence == that.sequence;
  }

  @Override
  public int hashCode() {
    int result = (int) (timestamp ^ (timestamp >>> 32));
    result = 31 * result + (int) (nodeId ^ (nodeId >>> 32));
    result = 31 * result + (int) (sequence ^ (sequence >>> 32));
    return result;
  }

  public static BoundaryFlakeSequence fromHexString(String hex) {
    ByteBuffer buffer = ByteBuffer.wrap(DatatypeConverter.parseHexBinary(hex));
    long timestamp = buffer.getLong();
    long nodeId = buffer.getLong();
    long sequence = buffer.getLong();
    return new BoundaryFlakeSequence(timestamp, nodeId, sequence);
  }

}
