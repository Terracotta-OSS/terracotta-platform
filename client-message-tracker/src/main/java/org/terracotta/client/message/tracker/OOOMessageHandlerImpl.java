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
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import static org.terracotta.client.message.tracker.Tracker.TRACK_ALL;

public class OOOMessageHandlerImpl<M extends EntityMessage, R extends EntityResponse> implements OOOMessageHandler<M, R> {

  private final List<ClientTracker<ClientSourceId, R>> clientMessageTrackers;
  private final Predicate<M> trackerPolicy;
  private final ToIntFunction<M> segmentationStrategy;

  private final ClientTracker<ClientSourceId, R> sharedMessageTracker;

  public OOOMessageHandlerImpl(Predicate<M> trackerPolicy, int segments, ToIntFunction<M> segmentationStrategy) {
    this.trackerPolicy = trackerPolicy;
    this.segmentationStrategy = segmentationStrategy;
    this.clientMessageTrackers = new ArrayList<>(segments);
    for (int i = 0; i < segments; i++) {
      //Passing the TRACK_ALL tracker policy here to avoid the redundant trackability test in Tracker as the real policy is used in the invoke
      clientMessageTrackers.add(new ClientTrackerImpl(TRACK_ALL));
    }
    sharedMessageTracker = new ClientTrackerImpl(TRACK_ALL);
  }

  @Override
  public R invoke(InvokeContext context, M message, BiFunction<InvokeContext, M, R> invokeFunction) throws EntityUserException {
    if (trackerPolicy.test(message) && context.isValidClientInformation()) {
      ClientSourceId clientId = context.getClientSource();
      int index = segmentationStrategy.applyAsInt(message);
      Tracker<R> messageTracker = clientMessageTrackers.get(index).getTracker(clientId);
      messageTracker.reconcile(context.getOldestTransactionId());
      R response = messageTracker.getTrackedValue(context.getCurrentTransactionId());

      if (response == null && sharedMessageTracker.getTrackedClients().contains(clientId)) {
        Tracker<R> sharedTracker = sharedMessageTracker.getTracker(clientId);
        sharedTracker.reconcile(context.getOldestTransactionId());
        response = sharedTracker.getTrackedValue(context.getCurrentTransactionId());
      }

      if (response != null) {
        return response;
      }

      response = invokeFunction.apply(context, message);
      messageTracker.track(context.getCurrentTransactionId(), message, response);
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

  @Override
  public Map<Long, R> getTrackedResponsesForSegment(int index, ClientSourceId clientSourceId) {
    return this.clientMessageTrackers.get(index).getTracker(clientSourceId).getTrackedValues();
  }

  @Override
  public void loadTrackedResponsesForSegment(int index, ClientSourceId clientSourceId, Map<Long, R> trackedResponses) {
    this.clientMessageTrackers.get(index).getTracker(clientSourceId).loadOnSync(trackedResponses);
  }

  @Deprecated
  @Override
  public void loadOnSync(ClientSourceId clientSourceId, Map<Long, R> trackedResponses) {
    this.sharedMessageTracker.getTracker(clientSourceId).loadOnSync(trackedResponses);
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    for (int i = 0; i < clientMessageTrackers.size(); i++) {
      clientMessageTrackers.get(i).addStateTo(stateDumper.subStateDumpCollector("segment-" + i));
    }

    sharedMessageTracker.addStateTo(stateDumper.subStateDumpCollector("shared"));
  }
}
