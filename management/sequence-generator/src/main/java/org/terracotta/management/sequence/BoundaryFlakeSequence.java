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

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * @author Mathieu Carbou
 */
public final class BoundaryFlakeSequence implements Sequence, Serializable {

  private static final long serialVersionUID = 1;

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
    byte[] bytes = toBytes();
    StringBuilder r = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      r.append(Integer.toHexString((b >> 4) & 0xF));
      r.append(Integer.toHexString(b & 0xF));
    }
    return r.toString();
  }

  @Override
  public byte[] toBytes() {
    ByteBuffer buffer = ByteBuffer.allocate(24);
    buffer.putLong(timestamp);
    buffer.putLong(nodeId);
    buffer.putLong(sequence);
    return buffer.array();
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

  public static BoundaryFlakeSequence fromBytes(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    long timestamp = buffer.getLong();
    long nodeId = buffer.getLong();
    long sequence = buffer.getLong();
    return new BoundaryFlakeSequence(timestamp, nodeId, sequence);
  }

  public static BoundaryFlakeSequence fromHexString(String hex) {
    final int len = hex.length();
    byte[] bytes = new byte[len >>> 1];

    for (int i = 0; i < len; i += 2) {
      bytes[i >>> 1] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
    }
    return fromBytes(bytes);
  }
}
