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
import org.terracotta.entity.StateDumpable;

import java.util.Map;

/**
 * Keeps track of an entity's messages with their ids to their corresponding responses.
 * The decision on whether or not to track an {@link EntityMessage} is taken using the {@link TrackerPolicy} of the entity
 */
public interface MessageTracker<M extends EntityMessage, R extends EntityResponse> extends StateDumpable {

  /**
   * Tracks the provided response associated with the given messageId.
   * The response is tracked if and only if the provided {@code message} is trackable,
   * which is determined using the tracker policy of the entity.
   *
   * @param messageId Incoming entity message ID
   * @param message Incoming entity message
   * @param response Outgoing entity response
   */
  void track(long messageId, M message, R response);

  /**
   * Returns the tracked response associated with the given message ID, null otherwise.
   *
   * @param messageId Tracked entity message ID
   * @return Tracked entity response
   */
  R getTrackedResponse(long messageId);

  /**
   * Clears messageId-response mappings for all ids less than the provided messageId.
   *
   * @param messageId Incoming entity message ID
   */
  void reconcile(long messageId);

  /**
   * Bulk load a set of message ids, response mappings.
   * To be used by a passive entity when the active syncs its message tracker data.
   *
   * @param trackedResponses a map of message id, response mappings
   */
  void loadOnSync(Map<Long, R> trackedResponses);
}
