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
package org.terracotta.client.message.tracker;

import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.StateDumpCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.terracotta.client.message.tracker.Tracker.TRACK_ALL;

public class OOOMessageHandlerImpl<M extends EntityMessage, R extends EntityResponse> implements OOOMessageHandler<M, R> {

  private final List<ClientTrackerImpl<M, R>> clientMessageTrackers;
  private final Predicate<M> trackerPolicy;
  private final ToIntFunction<M> segmentationStrategy;
  private final DestroyCallback callback;

  private final ClientTrackerImpl<M, R> sharedMessageTracker;

  AtomicLong trackid = new AtomicLong();

  public OOOMessageHandlerImpl(Predicate<M> trackerPolicy, int segments, ToIntFunction<M> segmentationStrategy, DestroyCallback callback) {
    this.trackerPolicy = trackerPolicy;
    this.segmentationStrategy = segmentationStrategy;
    this.clientMessageTrackers = new ArrayList<>(segments);
    for (int i = 0; i < segments; i++) {
      //Passing the TRACK_ALL tracker policy here to avoid the redundant trackability test in Tracker as the real policy is used in the invoke
      clientMessageTrackers.add(new ClientTrackerImpl<>(TRACK_ALL));
    }
    sharedMessageTracker = new ClientTrackerImpl<>(TRACK_ALL);
    this.callback = callback;
  }

  @Override
  public R invoke(InvokeContext context, M message, BiFunction<InvokeContext, M, R> invokeFunction) throws EntityUserException {
    if (trackerPolicy.test(message) && context.isValidClientInformation()) {
      ClientSourceId clientId = context.getClientSource();
      int index = segmentationStrategy.applyAsInt(message);
      Tracker<M, R> messageTracker = clientMessageTrackers.get(index).getTracker(clientId);
      messageTracker.reconcile(context.getOldestTransactionId());
      R response = messageTracker.getTrackedValue(context.getCurrentTransactionId());
//      Object request = messageTracker.getTrackedRequest(index);

      if (response == null && sharedMessageTracker.getTrackedClients().contains(clientId)) {
        Tracker<M, R> sharedTracker = sharedMessageTracker.getTracker(clientId);
        sharedTracker.reconcile(context.getOldestTransactionId());
        response = sharedTracker.getTrackedValue(context.getCurrentTransactionId());
//        request = sharedTracker.getTrackedRequest(index);
      }

      if (response != null) {
        return response;
      }

      response = invokeFunction.apply(context, message);
      messageTracker.track(trackid.incrementAndGet(), context.getCurrentTransactionId(), message, response);
      return response;
    } else {
      return invokeFunction.apply(context, message);
    }
  }

  @Override
  public void untrackClient(ClientSourceId clientSourceId) {
    clientMessageTrackers.stream().forEach(tracker -> tracker.untrackClient(clientSourceId));
    sharedMessageTracker.untrackClient(clientSourceId);
  }

  @Override
  public Stream<ClientSourceId> getTrackedClients() {
    return clientMessageTrackers.stream()
        .flatMap(tracker -> tracker.getTrackedClients().stream())
        .distinct();
  }

  public R getTrackedResponseForMessage(ClientSourceId source, M message) {
    int index = segmentationStrategy.applyAsInt(message);
    return this.clientMessageTrackers.get(index).getTracker(source).getTrackedValue(message);
  }

  @Override
  public Stream<RecordedMessage<M, R>> getRecordedMessages() {
    Stream<RecordedMessage<M, R>> base = Stream.empty();
    for (ClientTrackerImpl<M, R> ct : clientMessageTrackers) {
      base = Stream.concat(base, ct.getTrackedValues());
    }
    return base.sorted((r1,r2)->(int)(r1.getSequenceId() - r2.getSequenceId()));
  }

  @Override
  public void loadRecordedMessages(Stream<RecordedMessage<M, R>> recorded) {
    recorded.forEach(rm->{
      clientMessageTrackers.get(segmentationStrategy.applyAsInt(rm.getRequest()))
              .getTracker(rm.getClientSourceId())
              .track(rm.getSequenceId(), rm.getTransactionId(), rm.getRequest(), rm.getResponse());
    });
  }
  
  public R getTrackedResponse(ClientSourceId source, int index, long id) {
    return this.clientMessageTrackers.get(index).getTracker(source).getTrackedValue(id);
  }
  /**
   * for compatability with old tracker usage
   */
  @Override @Deprecated
  public Map<Long, R> getTrackedResponsesForSegment(int index, ClientSourceId clientSourceId) {
    return this.clientMessageTrackers.get(index)
            .getTracker(clientSourceId).getTrackedValues()
            .stream()
            .collect(Collectors.toMap(rr->rr.getTransactionId(), rr->rr.getResponse()));
  }

  @Override @Deprecated
  public void loadTrackedResponsesForSegment(int index, ClientSourceId clientSourceId,  Map<Long, R> trackedResponses) {
    this.clientMessageTrackers.get(index).getTracker(clientSourceId).loadOnSync(trackedResponses);
  }

  @Override @Deprecated
  public void loadOnSync(ClientSourceId clientSourceId, Map<Long, R> trackedResponses) {
    this.sharedMessageTracker.getTracker(clientSourceId).loadOnSync(trackedResponses);
  }
  
  @Override
  public void destroy() {
    this.callback.destroy();
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    for (int i = 0; i < clientMessageTrackers.size(); i++) {
      clientMessageTrackers.get(i).addStateTo(stateDumper.subStateDumpCollector("segment-" + i));
    }

    sharedMessageTracker.addStateTo(stateDumper.subStateDumpCollector("shared"));
  }
}
