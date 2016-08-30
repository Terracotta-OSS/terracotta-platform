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

import org.terracotta.management.sequence.Sequence;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Mathieu Carbou
 */
class DefaultPlatformNotification implements PlatformNotification {

  private static final long serialVersionUID = 1;

  private static final AtomicLong NOTIFICATION_INDEX = new AtomicLong(Long.MIN_VALUE);

  private final Serializable source;
  private final byte[] sequence;
  private final Type type;
  private final long index;

  DefaultPlatformNotification(Sequence sequence, Type type, Serializable source) {
    this.index = NOTIFICATION_INDEX.getAndIncrement();
    this.sequence = sequence.toBytes();
    this.type = type;
    this.source = source;
  }

  @Override
  public <T extends Serializable> T getSource(Class<T> type) {
    return type.cast(source);
  }

  @Override
  public byte[] getSequence() {
    return sequence;
  }

  @Override
  public long getIndex() {
    return index;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    return "DefaultPlatformNotification{" + "index=" + index +
        ", type=" + type +
        ", sequence=" + Arrays.toString(sequence) +
        ", source=" + source +
        '}';
  }
}
