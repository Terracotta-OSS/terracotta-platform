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

/**
 * A {@link TrackerPolicy} for an entity defines which one of its messages types need once and only once invocation guarantee.
 * This will be used by the {@link ClientMessageTracker} to track such messages with their response cached.
 */
public interface TrackerPolicy {

  /**
   * Indicates whether the specified message need to be tracked by {@link ClientMessageTracker} or not.
   *
   * @param message an entity message
   * @return true for trackable messages and false for non-trackable messages
   */
  boolean trackable(EntityMessage message);
}
