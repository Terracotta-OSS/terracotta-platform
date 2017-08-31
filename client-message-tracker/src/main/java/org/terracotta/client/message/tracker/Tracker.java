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

import org.terracotta.entity.StateDumpable;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Keeps track of an entity's objects with their ids and their corresponding values.
 * The decision on whether or not to track an object is taken using the tracker policy of the implementation
 *
 * @param <R> type of the value
 */
public interface Tracker<R> extends StateDumpable {

  /**
   * A tracker policy that will track all messages
   */
  Predicate<Object> TRACK_ALL = obj -> true;

  /**
   * A tracker policy that will track no messages
   */
  Predicate<Object> TRACK_NONE = obj -> false;

  /**
   * Tracks the provided value associated with the given id.
   * The value is tracked if and only if the provided {@code object} is trackable,
   * which is determined using the tracker policy of the implementation.
   *
   * @param id Incoming entity object ID
   * @param object Incoming entity object
   * @param value Outgoing entity value
   */
  void track(long id, Object object, R value);

  /**
   * Returns the tracked value associated with the given ID, null otherwise.
   *
   * @param id Tracked entity ID
   * @return Tracked entity value
   */
  R getTrackedValue(long id);

  /**
   * Clears id-value mappings for all ids less than the provided id.
   *
   * @param id Incoming entity ID
   */
  void reconcile(long id);

  /**
   * Get all id - value mappings.

   * @return all tracked values
   */
  Map<Long, R> getTrackedValues();

  /**
   * Bulk load a set of ids, value mappings.
   * To be used by a passive entity when the active syncs its message tracker data.
   *
   * @param trackedValues a map of id, value mappings
   */
  void loadOnSync(Map<Long, R> trackedValues);
}
