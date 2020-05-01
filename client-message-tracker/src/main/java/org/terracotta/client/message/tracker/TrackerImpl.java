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
import java.util.Collection;
import java.util.Map;
import org.terracotta.entity.StateDumpCollector;

import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

class TrackerImpl<M, R> implements Tracker<M, R> {

  private final Predicate<Object> trackerPolicy;
  private final SortedMap<Long, RequestResponse<M, R>> trackedValues;

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
  public void track(long track, long id, M source, R value) {
    if (id > 0 && trackerPolicy.test(source)) {
      placeTrackedValue(track, id, source, value);
    }
  }
  
  private synchronized void placeTrackedValue(long insert, long id, M req, R value) {
    trackedValues.put(id, new RequestResponse<>(insert, id, req, value));
  }

  @Override
  public synchronized R getTrackedValue(long id) {
    return Optional.ofNullable(trackedValues.get(id)).map(RequestResponse::getResponse).orElse(null);
  }

  @Override
  public synchronized R getTrackedValue(M id) {
    return trackedValues.values().stream()
      .filter(rr->rr.getRequest().equals(id))
      .findAny().map(rr->rr.getResponse()).get();
  }
  
  @Override
  public synchronized M getTrackedRequest(long id) {
    return Optional.ofNullable(trackedValues.get(id)).map(RequestResponse::getRequest).orElse(null);
  }

  @Override
  public synchronized void reconcile(long id) {
    trackedValues.headMap(id).clear();
  }

  synchronized Collection<RequestResponse<M, R>> getTrackedValues() {
    return trackedValues.values();
  }
  /**
   * only used in a deprecated call {@link OOOMessageHandler#loadTrackedResponsesForSegment(int, org.terracotta.entity.ClientSourceId, java.util.Map) }
   */
  synchronized void loadOnSync(Map<Long, R> mappedResponses) {
    for (Map.Entry<Long, R> e : mappedResponses.entrySet()) {
      this.trackedValues.put(e.getKey(), new RequestResponse<>(-1, e.getKey(), null, e.getValue()));
    }
  }

  @Override
  public synchronized void loadOnSync(Stream<RecordedMessage<M, R>> trackedValues) {
    trackedValues.forEach(e->{
      M request = e.getRequest();
      R response = e.getResponse();
      this.trackedValues.put(e.getTransactionId(), new RequestResponse<>(e.getSequenceId(), e.getTransactionId(), request, response));
    });
  }

  @Override
  public synchronized void addStateTo(StateDumpCollector stateDumper) {
    stateDumper.addState("TrackedResponses", new ArrayList<>(trackedValues.keySet()));
  }

  static class RequestResponse<M, R> {
    private final long insert;
    private final long transaction;
    private final M request;
    private final R response;

    RequestResponse(long insert, long transaction, M request, R response) {
      this.insert = insert;
      this.transaction = transaction;
      this.request = request;
      this.response = response;
    }

    public long getSequenceId() {
      return insert;
    }

    public long getTransactionId() {
      return transaction;
    }

    public M getRequest() {
      return request;
    }

    public R getResponse() {
      return response;
    }
  }
}
