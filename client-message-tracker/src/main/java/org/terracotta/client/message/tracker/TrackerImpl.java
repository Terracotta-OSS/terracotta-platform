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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.terracotta.entity.StateDumpCollector;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;

class TrackerImpl<R> implements Tracker<R> {

  private final Predicate<Object> trackerPolicy;
  private final SortedMap<Long, R> trackedValues;

  /**
   * Constructor taking a predicate to define the tracking policy. If the predicate returns true, the source will
   * be tracked.
   *
   * @param trackerPolicy defines if a source is tracked or not
   */
  @SuppressWarnings("unchecked")
  TrackerImpl(Predicate<?> trackerPolicy) {
    this.trackerPolicy = (Predicate<Object>) trackerPolicy;
    this.trackedValues = new TreeMap<>();
  }

  @Override
  public void track(long id, Object source, R value) {
    if (id > 0 && trackerPolicy.test(source)) {
      placeTrackedValue(id, value);
    }
  }
  
  private synchronized void placeTrackedValue(long id, R value) {
    trackedValues.put(id, value);
  }

  @Override
  public synchronized R getTrackedValue(long id) {
    return trackedValues.get(id);
  }

  @Override
  public synchronized void reconcile(long id) {
    trackedValues.headMap(id).clear();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public synchronized Map<Long, R> getTrackedValues() {
    return Collections.unmodifiableMap(new HashMap(trackedValues));
  }

  @Override
  public synchronized void loadOnSync(Map<Long, R> trackedValues) {
    this.trackedValues.putAll(trackedValues);
  }

  @Override
  public synchronized void addStateTo(StateDumpCollector stateDumper) {
    stateDumper.addState("TrackedResponses", new ArrayList<>(trackedValues.keySet()));
  }
}
