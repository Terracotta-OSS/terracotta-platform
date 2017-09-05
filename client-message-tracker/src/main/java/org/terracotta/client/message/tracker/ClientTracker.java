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

import java.util.Set;

/**
 * Keeps track of the trackers for individual clients.
 */
public interface ClientTracker<K, R> extends StateDumpable {

  /**
   * Retrieve the tracker of the client with the specified {@code clientId}
   * If a tracker does not exist already create one and return it.
   *
   * @param clientId a client id
   * @return the tracker associated with the specified {@code clientId}
   */
  Tracker<R> getTracker(K clientId);

  /**
   * Deregister a client from being tracked.
   *
   * @param clientId a client id
   */
  void untrackClient(K clientId);

  /**
   * Return ids of all the clients tracked by this tracker.
   *
   * @return set of tracked client sources
   */
  Set<K> getTrackedClients();

}
