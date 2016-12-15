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
package org.terracotta.management.entity.management.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.entity.management.ManagementAgent;
import org.terracotta.management.entity.management.ReconnectData;
import org.terracotta.voltron.proxy.server.ActiveProxiedServerEntity;

/**
 * @author Mathieu Carbou
 */
class ActiveManagementAgentServerEntity extends ActiveProxiedServerEntity<ManagementAgent, Void, ReconnectData, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveManagementAgentServerEntity.class);

  private final ActiveManagementAgent managementAgent;

  ActiveManagementAgentServerEntity(ActiveManagementAgent managementAgent) {
    super(managementAgent, null);
    this.managementAgent = managementAgent;
  }

  @Override
  protected void onReconnect(ClientDescriptor clientDescriptor, ReconnectData reconnectData) {
    if (reconnectData != null) {
      LOGGER.trace("onReconnect({})", clientDescriptor);
      managementAgent.exposeTags(clientDescriptor, reconnectData.tags);
      managementAgent.exposeManagementMetadata(clientDescriptor, reconnectData.contextContainer, reconnectData.capabilities);
      managementAgent.pushNotification(clientDescriptor, reconnectData.contextualNotification);
    }
  }
}
