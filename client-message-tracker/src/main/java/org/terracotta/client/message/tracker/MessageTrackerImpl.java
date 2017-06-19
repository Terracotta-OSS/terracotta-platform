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

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.StateDumpCollector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.LongStream;

public class MessageTrackerImpl implements MessageTracker {

  private final TrackerPolicy trackerPolicy;
  private final ConcurrentMap<Long, EntityResponse> trackedResponses;
  private volatile long lastReconciledMessageId = -1;

  public MessageTrackerImpl(TrackerPolicy trackerPolicy) {
    this.trackerPolicy = trackerPolicy;
    this.trackedResponses = new ConcurrentHashMap<>();
  }

  @Override
  public void track(long messageId, EntityMessage message, EntityResponse response) {
    if (trackerPolicy.trackable(message) && messageId > 0) {
      trackedResponses.put(messageId, response);
    }
  }

  @Override
  public EntityResponse getTrackedResponse(long messageId) {
    return trackedResponses.get(messageId);
  }

  @Override
  public void reconcile(long messageId) {
    if (messageId > lastReconciledMessageId) {
      LongStream.rangeClosed(lastReconciledMessageId + 1, messageId).forEach(id -> trackedResponses.remove(id));
      lastReconciledMessageId = messageId;
    }
  }

  @Override
  public void loadOnSync(Map<Long, EntityResponse> trackedResponses) {
    trackedResponses.putAll(trackedResponses);
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    stateDumper.addState("TrackedResponses", trackedResponses.keySet().toString());
  }
}
