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

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.management.entity.management.ManagementAgent;
import org.terracotta.management.model.message.Message;
import org.terracotta.voltron.proxy.server.ProxiedServerEntity;

/**
 * @author Mathieu Carbou
 */
class ManagementAgentServerEntity extends ProxiedServerEntity<ManagementAgent> {
  ManagementAgentServerEntity(ManagementAgentImpl managementAgent, ClientCommunicator communicator) {
    super(managementAgent, communicator, Message.class);
  }
}
