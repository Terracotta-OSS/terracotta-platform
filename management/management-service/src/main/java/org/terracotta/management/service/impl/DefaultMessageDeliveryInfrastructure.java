/**
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
package org.terracotta.management.service.impl;

import org.terracotta.management.service.buffer.PartitionedRingBuffer;
import org.terracotta.management.service.buffer.impl.MultiPartitionLockFreeRingBuffer;
import org.terracotta.voltron.management.MessageDeliveryInfrastructureService;
import org.terracotta.voltron.management.consumer.MessageConsumer;
import org.terracotta.voltron.management.consumer.MessageConsumerListener;
import org.terracotta.voltron.management.producer.MessageProducer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.terracotta.management.service.impl.Constants.BUFFER_CACHE_SIZE;
import static org.terracotta.management.service.impl.Constants.MAX_PARALLEL_PRODUCERS;

/**
 * Default implementation of the {@link MessageDeliveryInfrastructureService}.
 *
 * @author RKAV
 */
public class DefaultMessageDeliveryInfrastructure implements MessageDeliveryInfrastructureService {
  private final Map<Class<?>, PerMessageTypeInfrastructure<?>> messageTypeInfraRegistry;
  private final Map<Class<?>, Collection<MessageConsumerListener<?>>> pendingListeners;

  public DefaultMessageDeliveryInfrastructure() {
    messageTypeInfraRegistry = new HashMap<>();
    pendingListeners = new HashMap<>();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <M> MessageProducer<M> createMessageProducer(Class<M> messageType) {
    PerMessageTypeInfrastructure<M> currentEntry;
    Collection<MessageConsumerListener<?>> listenersForType = Collections.emptyList();

    // must hold lock to avoid management system requesting consumer interface
    // almost at the same time the managed entity is setting up the producer and
    // delivery infrastructure. However, not an expensive synch as this is only
    // during entity startup on the active
    synchronized (this) {
      PerMessageTypeInfrastructure<?> registeredInfraForType = messageTypeInfraRegistry.get(messageType);
      if (registeredInfraForType == null) {
        currentEntry = new PerMessageTypeInfrastructure<>();
        messageTypeInfraRegistry.put(messageType, currentEntry);
      } else {
        currentEntry = (PerMessageTypeInfrastructure<M>)registeredInfraForType;
      }
      Collection<MessageConsumerListener<?>> tmpListeners = this.pendingListeners.remove(messageType);
      if (tmpListeners != null) {
        listenersForType = tmpListeners;
      }
    }

    // now invoke the listeners and return the producer
    listenersForType.forEach((lsnr) -> ((MessageConsumerListener<M>)lsnr).onCreate(currentEntry.messageConsumer));
    return new DefaultMessageProducer(currentEntry.messageCache, currentEntry.allocateProducer());
  }

  @Override
  public <M> void registerMessageConsumerListener(Class<M> messageType,
                                                  MessageConsumerListener<M> messageConsumerListener) {
    PerMessageTypeInfrastructure<M> existingEntry = null;

    // not an expensive synch as this is typically only during entity startup on the active
    synchronized (this) {
      @SuppressWarnings("unchecked")
      PerMessageTypeInfrastructure<M> currentEntry = (PerMessageTypeInfrastructure<M>) messageTypeInfraRegistry.get(messageType);
      if (currentEntry == null) {
        Collection<MessageConsumerListener<?>> listenersForType = pendingListeners.get(messageType);
        if (listenersForType == null) {
          listenersForType = new ArrayList<>();
          pendingListeners.put(messageType, listenersForType);
        }
        listenersForType.add(messageConsumerListener);
      } else {
        existingEntry = currentEntry;
      }
    }

    // if producer is already registered callback immediately...
    // always call the listener outside lock..
    if (existingEntry != null) {
      messageConsumerListener.onCreate(existingEntry.messageConsumer);
    }
  }

  /**
   * Message delivery infrastructure for each message type
   *
   * @param <T> Type of message
   */
  private static final class PerMessageTypeInfrastructure<T> {
    private final PartitionedRingBuffer<T> messageCache;
    private final MessageConsumer<T> messageConsumer;
    private final AtomicInteger producerAllocationCount;

    private PerMessageTypeInfrastructure() {
      PartitionedRingBuffer<T> rb = new MultiPartitionLockFreeRingBuffer<>(
          MAX_PARALLEL_PRODUCERS,
          BUFFER_CACHE_SIZE);
      this.messageConsumer = new DefaultMessageConsumer<>(rb);
      this.messageCache = rb;
      this.producerAllocationCount = new AtomicInteger(0);
    }

    private int allocateProducer() {
      int partitionNo = producerAllocationCount.getAndIncrement();
      if (partitionNo >= MAX_PARALLEL_PRODUCERS) {
        throw new IllegalStateException("Too many producers");
      }
      return partitionNo;
    }
  }
}
