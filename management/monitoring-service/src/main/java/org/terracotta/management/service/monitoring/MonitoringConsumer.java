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

import java.util.LinkedList;
import java.util.Queue;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
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

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private static final Queue<Mutation> EMPTY_QUEUE = new LinkedList<>();

  private static final Logger LOGGER = Logger.getLogger(MonitoringService.class.getName());

  private final Queue<Mutation> mutationQueue;
  private final long callerConsumerID;

  MonitoringConsumer(long callerConsumerID, MonitoringConsumerConfiguration consumerConfiguration) {
    this.callerConsumerID = callerConsumerID;
    if (consumerConfiguration.isRecordingMutations()) {
      //TODO: MATHIEU - PERF: https://github.com/Terracotta-OSS/terracotta-platform/issues/109
      mutationQueue = new BoundedEvictingPriorityQueue<>(
          consumerConfiguration.getMaximumUnreadMutations(),
          mutation -> {
            if (LOGGER.isLoggable(Level.FINE)) {
              LOGGER.fine("CONSUMER " + callerConsumerID + ": discarded mutation " + mutation);
            }
          });
    } else {
      mutationQueue = EMPTY_QUEUE;
    }
  }

  @Override
  public final Stream<Mutation> readMutations() {
    return StreamSupport.stream(new Spliterators.AbstractSpliterator<Mutation>(mutationQueue.size(), NONNULL | ORDERED | SIZED | SUBSIZED | DISTINCT | IMMUTABLE) {
      @Override
      public boolean tryAdvance(Consumer<? super Mutation> action) {
        if (mutationQueue == EMPTY_QUEUE) {
          return false;
        }
        Mutation mutation = mutationQueue.poll();
        if (mutation == null) {
          return false;
        }
        action.accept(mutation);
        return true;
      }
    }, false);
  }

  void record(TreeMutation mutation) {
    if (mutationQueue != EMPTY_QUEUE) {
      mutationQueue.offer(mutation);
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
