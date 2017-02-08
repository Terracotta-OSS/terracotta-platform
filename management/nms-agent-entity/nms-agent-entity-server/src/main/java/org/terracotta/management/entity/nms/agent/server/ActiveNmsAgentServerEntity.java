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
package org.terracotta.management.entity.nms.agent.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.entity.nms.agent.NmsAgent;
import org.terracotta.management.entity.nms.agent.ReconnectData;
import org.terracotta.voltron.proxy.server.ActiveProxiedServerEntity;

/**
 * @author Mathieu Carbou
 */
class ActiveNmsAgentServerEntity extends ActiveProxiedServerEntity<NmsAgent, Void, ReconnectData, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveNmsAgentServerEntity.class);

  private final ActiveNmsAgent activeNmsAgent;

  ActiveNmsAgentServerEntity(ActiveNmsAgent activeNmsAgent) {
    super(activeNmsAgent, null);
    this.activeNmsAgent = activeNmsAgent;
  }

  @Override
  protected void onReconnect(ClientDescriptor clientDescriptor, ReconnectData reconnectData) {
    if (reconnectData != null) {
      LOGGER.trace("onReconnect({})", clientDescriptor);
      activeNmsAgent.exposeTags(clientDescriptor, reconnectData.tags);
      activeNmsAgent.exposeManagementMetadata(clientDescriptor, reconnectData.contextContainer, reconnectData.capabilities);
      activeNmsAgent.pushNotification(clientDescriptor, reconnectData.contextualNotification);
    }
  }
}
