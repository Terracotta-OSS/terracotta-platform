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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.LongStream;

public class TrackerImpl implements Tracker {

  private final Predicate<Object> trackerPolicy;
  private final ConcurrentMap<Long, Object> trackedValues;
  private volatile long lastReconciledId = 0;

  @SuppressWarnings("unchecked")
  public TrackerImpl(Predicate<?> trackerPolicy) {
    this.trackerPolicy = (Predicate<Object>) trackerPolicy;
    this.trackedValues = new ConcurrentHashMap<>();
  }

  long getLastReconciledId() {
    return lastReconciledId;
  }

  @Override
  public void track(long id, Object source, Object value) {
    if (id > 0 && trackerPolicy.test(source)) {
      trackedValues.put(id, value);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getTrackedValues(long id) {
    return (T) trackedValues.get(id);
  }

  @Override
  public void reconcile(long id) {
    long lastId = lastReconciledId;
    if (id > lastId) {
      LongStream.range(lastId, id).forEach(i -> trackedValues.remove(i));
      while (id > lastReconciledId) {
        lastReconciledId = id;
      }
    }
  }

  @Override
  public Map<Long, Object> getTrackedValues() {
    return trackedValues;
  }

  @Override
  public void loadOnSync(Map<Long, Object> trackedValues) {
    this.trackedValues.putAll(trackedValues);
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    stateDumper.addState("TrackedResponses", trackedValues.keySet());
  }
}
