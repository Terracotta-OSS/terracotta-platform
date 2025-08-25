/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.client.message.tracker;

import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.StateDumpCollector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Comparator.comparingLong;

public class OOOMessageHandlerImpl<M extends EntityMessage, R extends EntityResponse> implements OOOMessageHandler<M, R> {

  private final ClientTrackerImpl<M, R> clientMessageTracker;
  private final Predicate<M> trackerPolicy;
  private final DestroyCallback callback;

  AtomicLong trackid = new AtomicLong();

  private final AtomicBoolean checkDuplicates = new AtomicBoolean(true);

  public OOOMessageHandlerImpl(Predicate<M> trackerPolicy, DestroyCallback callback) {
    this.trackerPolicy = trackerPolicy;
    this.clientMessageTracker = new ClientTrackerImpl<>();
    this.callback = callback;
  }

  @Override
  public R invoke(InvokeContext context, M message, BiFunction<InvokeContext, M, R> invokeFunction) throws EntityUserException {
    if (trackerPolicy.test(message) && context.isValidClientInformation()) {
      ClientSourceId clientId = context.getClientSource();
      Tracker<M, R> messageTracker = clientMessageTracker.getTracker(clientId);
      messageTracker.reconcile(context.getOldestTransactionId());

      if (checkDuplicates.get()) {
        // if the message was reconciled, this means the application of the message
        // is invalid and the supplier cannot be invoked.  Additionally, the answer is
        // not known so return null
        if (messageTracker.wasReconciled(context.getCurrentTransactionId())) {
          return null;
        }
        R response = messageTracker.getTrackedValue(context.getCurrentTransactionId());

        if (response != null) {
          return response;
        }
      }

      R response = invokeFunction.apply(context, message);
      messageTracker.track(trackid.incrementAndGet(), context.getCurrentTransactionId(), message, response);
      return response;
    } else {
      return invokeFunction.apply(context, message);
    }
  }

  @Override
  public void closeDuplicatesWindow() {
    checkDuplicates.set(false);
  }

  @Override
  public R lookupResponse(ClientSourceId src, long txn) {
    return (checkDuplicates.get())  ? clientMessageTracker.getTracker(src).getTrackedValue(txn) : null;
  }

  @Override
  public void untrackClient(ClientSourceId clientSourceId) {
    clientMessageTracker.untrackClient(clientSourceId);
  }

  @Override
  public Stream<ClientSourceId> getTrackedClients() {
    return clientMessageTracker.getTrackedClients().stream();
  }

  @Override
  public Stream<RecordedMessage<M, R>> getRecordedMessages() {
    return clientMessageTracker.getTrackedValues().sorted(comparingLong(SequencedRecordedMessage::getSequenceId)).map(SequencedRecordedMessage::convert);
  }

  @Override
  public void loadRecordedMessages(Stream<RecordedMessage<M, R>> recorded) {
    recorded.forEach(rm-> clientMessageTracker
            .getTracker(rm.getClientSourceId())
            .track(trackid.incrementAndGet(), rm.getTransactionId(), rm.getRequest(), rm.getResponse()));
  }

  @Override
  public void destroy() {
    this.callback.destroy();
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
      clientMessageTracker.addStateTo(stateDumper.subStateDumpCollector("clientMessageTracker"));
  }
}
