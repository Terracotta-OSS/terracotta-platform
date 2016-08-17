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

import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterator.SUBSIZED;

/**
 * @author Mathieu Carbou
 */
abstract class MonitoringConsumer implements IMonitoringConsumer {

  private final ReadWriteBuffer<Mutation> mutations;
  private final long callerConsumerID;

  MonitoringConsumer(long callerConsumerID, MonitoringConsumerConfiguration consumerConfiguration) {
    this.callerConsumerID = callerConsumerID;
    this.mutations = consumerConfiguration.isRecordingMutations() ? new RingBuffer<>(consumerConfiguration.getMaximumUnreadMutations()) : null;
  }

  @Override
  public final Stream<Mutation> readMutations() {
    if (mutations == null) {
      return Stream.empty();
    } else {
      return StreamSupport.stream(new Spliterators.AbstractSpliterator<Mutation>(mutations.size(), NONNULL | ORDERED | SIZED | SUBSIZED | DISTINCT | IMMUTABLE) {
        @Override
        public boolean tryAdvance(Consumer<? super Mutation> action) {
          Mutation mutation = mutations.read();
          if (mutation == null) {
            return false;
          }
          action.accept(mutation);
          return true;
        }
      }, false);
    }
  }

  final void record(TreeMutation mutation) {
    if (mutations != null) {
      mutations.put(mutation);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MonitoringConsumer consumer = (MonitoringConsumer) o;
    return callerConsumerID == consumer.callerConsumerID;
  }

  @Override
  public int hashCode() {
    return (int) (callerConsumerID ^ (callerConsumerID >>> 32));
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("MonitoringConsumer{");
    sb.append("callerConsumerID=").append(callerConsumerID);
    sb.append('}');
    return sb.toString();
  }

}
