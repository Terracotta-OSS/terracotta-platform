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
package org.terracotta.management.service.monitoring;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

/**
 * Interface used only by the platform to callback on platform events
 * <p>
 * This is only ever called by the consumerID 0 instance.
 */
interface PlatformListener {

  void serverDidBecomeActive(PlatformServer self);

  void serverDidJoinStripe(PlatformServer server);

  void serverDidLeaveStripe(PlatformServer server);

  void serverEntityCreated(PlatformServer sender, PlatformEntity entity);

  void serverEntityDestroyed(PlatformServer sender, PlatformEntity entity);

  /**
   * Called when the passive takes over an active.
   * 1. Passive becomes active
   * 2. Voltron will "replay" the passive monitoring states (entities)
   * 3. serverEntityFailover() will be called with these passive entities originating from the new active server
   * 4. Voltron will then create a new monitoring tree
   * 5. New active entities are created
   * 6. serverEntityCreated() will be called with the new active entities
   * <p>
   * So this method enables to see the ongoing transitions of entities that were on a passive
   */
  void serverEntityFailover(PlatformServer sender, PlatformEntity entity);

  void serverStateChanged(PlatformServer sender, ServerState state);

  void clientConnected(PlatformConnectedClient client);

  void clientDisconnected(PlatformConnectedClient client);

  void clientFetch(PlatformConnectedClient client, PlatformEntity entity, ClientDescriptor clientDescriptor);

  void clientUnfetch(PlatformConnectedClient client, PlatformEntity entity, ClientDescriptor clientDescriptor);

}
