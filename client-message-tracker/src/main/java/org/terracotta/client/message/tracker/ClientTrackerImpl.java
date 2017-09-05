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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

public class ClientTrackerImpl<K, R> implements ClientTracker<K, R> {

  private final Predicate<?> trackerPolicy;
  private final ConcurrentMap<K, Tracker<R>> objectTrackers = new ConcurrentHashMap<>();

  public ClientTrackerImpl(Predicate<?> trackerPolicy) {
    this.trackerPolicy = trackerPolicy;
  }

  @Override
  public Tracker<R> getTracker(K clientId) {
    return objectTrackers.computeIfAbsent(clientId, d -> new TrackerImpl<>(trackerPolicy));
  }

  @Override
  public void untrackClient(K clientId) {
    objectTrackers.remove(clientId);
  }

  @Override
  public Set<K> getTrackedClients() {
    return objectTrackers.keySet();
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    for (Map.Entry<K, Tracker<R>> entry : objectTrackers.entrySet()) {
      entry.getValue().addStateTo(stateDumper.subStateDumpCollector(entry.getKey().toString()));
    }
  }
}
