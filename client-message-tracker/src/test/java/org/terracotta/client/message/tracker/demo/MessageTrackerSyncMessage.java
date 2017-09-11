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
package org.terracotta.client.message.tracker.demo;

import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;

import java.util.Map;

public class MessageTrackerSyncMessage implements EntityMessage {

  private final int segmentIndex;
  private final ClientSourceId clientSourceId;
  private final Map<Long, EntityResponse> trackedResponses;

  public MessageTrackerSyncMessage(int segmentIndex, ClientSourceId clientSourceId, Map<Long, EntityResponse> trackedResponses) {
    this.segmentIndex = segmentIndex;
    this.clientSourceId = clientSourceId;
    this.trackedResponses = trackedResponses;
  }

  public int getSegmentIndex() {
    return segmentIndex;
  }

  public ClientSourceId getClientSourceId() {
    return clientSourceId;
  }

  public Map<Long, EntityResponse> getTrackedResponses() {
    return trackedResponses;
  }
}
