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

import java.util.Collection;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;

import org.terracotta.client.message.tracker.RecordedMessage;

public class MessageTrackerSyncMessage implements EntityMessage {

  private final Collection<RecordedMessage<EntityMessage, EntityResponse>> trackedResponses;

  public MessageTrackerSyncMessage(Collection<RecordedMessage<EntityMessage, EntityResponse>> trackedResponses) {
    this.trackedResponses = trackedResponses;
  }

  public Collection<RecordedMessage<EntityMessage, EntityResponse>> getTrackedMessages() {
    return trackedResponses;
  }
}
