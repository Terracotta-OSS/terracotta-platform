/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity Management Service.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.management.service.impl;

import org.terracotta.management.service.buffer.PartitionedRingBuffer;
import org.terracotta.voltron.management.consumer.MessageListener;
import org.terracotta.voltron.management.consumer.MessageConsumer;
import org.terracotta.voltron.management.consumer.PreviousMessageAckPendingException;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.terracotta.management.service.impl.Constants.*;
import static org.terracotta.management.service.impl.Constants.COLLECTION_INTERVAL;
import static org.terracotta.management.service.impl.Constants.DEFAULT_TIME_UNIT;

/**
 * The management service consumer implementation.
 * <p>
 * TODO: As of now only the shell for message pushing is created mainly to
 * test statistics pushing.
 *
 * @author RKAV
 */
public class DefaultMessageConsumer<M> implements MessageConsumer<M> {
  private final PartitionedRingBuffer<M> messageCache;
  private final ScheduledExecutorService messageCollectorScheduler;
  private volatile MessageCollector messageCollector;

  public DefaultMessageConsumer(PartitionedRingBuffer<M> messageCache) {
    this.messageCache = messageCache;
    // This thread pool MUST have one and only one thread..it is a design assumption
    this.messageCollectorScheduler = Executors.newScheduledThreadPool(1);
    this.messageCollector = null;
  }

  @Override
  public void setupPeriodicManagementMessageCollector(MessageListener<M> messageCallback) {
    if (messageCollectorScheduler.isShutdown()) {
      throw new IllegalStateException("Scheduler is shutdown");
    }
    if (messageCollector == null) {
      messageCollector = new MessageCollector<>(messageCallback, messageCache);
      messageCollectorScheduler.scheduleAtFixedRate(messageCollector, COLLECTION_INTERVAL, COLLECTION_INTERVAL, DEFAULT_TIME_UNIT);
    }
    // TODO: allow for changing the listener etc..
  }

  public void shutdown() {
    messageCollectorScheduler.shutdown();
  }

  /**
   * The periodic message collector task..
   */
  private static final class MessageCollector<M> implements Runnable {
    private final MessageListener<M> messageCallback;
    private final PartitionedRingBuffer<M> messageCache;

    // thread local area where pending messages are temporarily stored in case
    // of failures in posting messages to the consumer.
    // This has to be instance specific thread local as different collectors may have different
    // pending stores...
    private final ThreadLocal<Collection<M>> pendingCollectionStore;

    private MessageCollector(MessageListener<M> messageCallback, PartitionedRingBuffer<M> messageCache) {
      this.messageCallback = messageCallback;
      this.messageCache = messageCache;
      this.pendingCollectionStore = new ThreadLocal<>();
    }

    @Override
    public void run() {
      boolean success = false;
      boolean lastChance = false;

      Collection<M> messageCollection;
      Collection<M> pendingCollection = pendingCollectionStore.get();
      if (pendingCollection != null) {
        messageCollection = pendingCollection;
        if (messageCache.hasOverSpillThresholdReached()) {
          lastChance = true;
        } else if (messageCollection.size() < OVERSPILL_SIZE) {
          messageCollection.addAll(messageCache.removeAll());
        }
        pendingCollectionStore.set(null);
      } else {
        messageCollection = messageCache.removeAll();
      }
      try {
        if (messageCollection.size() > 0) {
          try {
            messageCallback.postMessages(messageCollection);
            success = true;
          } catch (PreviousMessageAckPendingException ignored) {
            // previous ack not arrived yet..Suppress this exception
            // so that the task is scheduled again
            // TODO: we may have to suppress other exceptions as well.
          }
        } else {
          success = true;
        }
      } finally {
        if (!success) {
          if (!lastChance) {
            pendingCollectionStore.set(messageCollection);
          }
        }
      }
    }
  }
}
