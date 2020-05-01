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

import org.terracotta.entity.StateDumpCollector;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.terracotta.entity.ClientSourceId;

class ClientTrackerImpl<M, R> implements ClientTracker<M, R> {

  private final Predicate<?> trackerPolicy;
  private final ConcurrentMap<ClientSourceId, TrackerImpl<M, R>> objectTrackers = new ConcurrentHashMap<>();

  public ClientTrackerImpl(Predicate<?> trackerPolicy) {
    this.trackerPolicy = trackerPolicy;
  }

  Stream<RecordedMessage<M, R>> getTrackedValues() {
    Stream<RecordedMessage<M, R>> base = Stream.empty();
    for (Entry<ClientSourceId, TrackerImpl<M, R>> t : objectTrackers.entrySet()) {
      base = Stream.concat(base, t.getValue().getTrackedValues().stream().map(e->convert(t.getKey(), e)));
    }
    return base;
  }

  static <M, R> RecordedMessage<M, R> convert(ClientSourceId cid, TrackerImpl.RequestResponse<M, R> rr) {
    return new RecordedMessage<M, R>() {
      @Override
      public long getSequenceId() {
        return rr.getSequenceId();
      }

      @Override
      public ClientSourceId getClientSourceId() {
        return cid;
      }

      @Override
      public long getTransactionId() {
        return rr.getTransactionId();
      }

      @Override
      public M getRequest() {
        return rr.getRequest();
      }

      @Override
      public R getResponse() {
        return rr.getResponse();
      }
    };
  }

  TrackerImpl<M, R> getTracker(ClientSourceId clientId) {
    return objectTrackers.computeIfAbsent(clientId, d -> new TrackerImpl<>(trackerPolicy));
  }

  @Override
  public void untrackClient(ClientSourceId clientId) {
    objectTrackers.remove(clientId);
  }

  @Override
  public Set<ClientSourceId> getTrackedClients() {
    return objectTrackers.keySet();
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    for (Map.Entry<ClientSourceId, TrackerImpl<M, R>> entry : objectTrackers.entrySet()) {
      entry.getValue().addStateTo(stateDumper.subStateDumpCollector(entry.getKey().toString()));
    }
  }
}
