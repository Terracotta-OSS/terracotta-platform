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

  void serverEntityReconfigured(PlatformServer sender, PlatformEntity entity);

  void serverEntityDestroyed(PlatformServer sender, PlatformEntity entity);

  void serverStateChanged(PlatformServer sender, ServerState state);

  void clientConnected(PlatformServer currentActive, PlatformConnectedClient client);

  void clientDisconnected(PlatformServer currentActive, PlatformConnectedClient client);

  void clientFetch(PlatformConnectedClient client, PlatformEntity entity, ClientDescriptor clientDescriptor);

  void clientUnfetch(PlatformConnectedClient client, PlatformEntity entity, ClientDescriptor clientDescriptor);

}
