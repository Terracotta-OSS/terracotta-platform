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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumpCollector;

/**
 * Keeps track of the message trackers for individual clients.
 */
public interface ClientMessageTracker extends StateDumpable {

  /**
   * Retrieve the message tracker of the client with the specified {@code clientDescriptor}
   * If a tracker does not exist already create one and return.
   *
   * @param clientDescriptor a client descriptor
   * @return the message tracker associated with the specified {@code clientDescriptor}
   */
  MessageTracker getMessageTracker(ClientDescriptor clientDescriptor);

  /**
   * Deregister a client from being tracked.
   *
   * @param clientDescriptor a client descriptor
   */
  void untrackClient(ClientDescriptor clientDescriptor);

}
