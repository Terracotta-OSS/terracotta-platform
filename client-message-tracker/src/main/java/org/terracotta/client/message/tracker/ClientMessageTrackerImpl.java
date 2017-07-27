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
import org.terracotta.entity.StateDumpCollector;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientMessageTrackerImpl implements ClientMessageTracker {

  private final TrackerPolicy trackerPolicy;
  private final ConcurrentMap<ClientSourceId, MessageTracker> messageTrackers = new ConcurrentHashMap<>();

  public ClientMessageTrackerImpl(TrackerPolicy TrackerPolicy) {
    this.trackerPolicy = TrackerPolicy;
  }

  @Override
  public MessageTracker getMessageTracker(ClientSourceId clientSourceId) {
    return messageTrackers.computeIfAbsent(clientSourceId, d -> new MessageTrackerImpl(trackerPolicy));
  }

  @Override
  public void untrackClient(ClientSourceId clientSourceId) {
    messageTrackers.remove(clientSourceId);
  }

  @Override
  public Set<ClientSourceId> getTrackedClients() {
    return messageTrackers.keySet();
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    for (Map.Entry<ClientSourceId, MessageTracker> entry : messageTrackers.entrySet()) {
      entry.getValue().addStateTo(stateDumper.subStateDumpCollector(entry.getKey().toString()));
    }
  }
}
