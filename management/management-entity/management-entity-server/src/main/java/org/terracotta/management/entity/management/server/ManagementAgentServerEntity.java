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
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.entity.management.ManagementAgent;
import org.terracotta.management.entity.management.ManagementEvent;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.management.service.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.voltron.proxy.server.ProxiedServerEntity;

import java.util.Objects;

import static org.terracotta.management.entity.management.server.Utils.array;

/**
 * @author Mathieu Carbou
 */
class ManagementAgentServerEntity extends ProxiedServerEntity<ManagementAgent> {

  private final IMonitoringConsumer consumer;
  private final IMonitoringProducer producer;
  private final ManagementAgentImpl managementAgent;

  ManagementAgentServerEntity(ManagementAgentImpl managementAgent, IMonitoringConsumer consumer, IMonitoringProducer producer, ClientCommunicator communicator) {
    super(managementAgent, communicator, ManagementEvent.class);
    this.managementAgent = Objects.requireNonNull(managementAgent);
    this.consumer = Objects.requireNonNull(consumer, "IMonitoringConsumer service is missing");
    this.producer = Objects.requireNonNull(producer, "IMonitoringProducer service is missing");
    producer.addNode(new String[0], "management", null);
    producer.addNode(array("management"), "clients", null);
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    super.connected(clientDescriptor);
    PlatformConnectedClient platformConnectedClient = consumer.getPlatformConnectedClient(clientDescriptor)
        .orElseThrow(() -> new IllegalStateException("Invalid monitoring tree: cannot get " + PlatformConnectedClient.class.getSimpleName() + " from descriptor " + clientDescriptor));
    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
    producer.addNode(array("management", "clients"), clientIdentifier.getClientId(), null);
    producer.addNode(array("management", "clients", clientIdentifier.getClientId()), "registry", null);
    managementAgent.connected(clientDescriptor, clientIdentifier);
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    PlatformConnectedClient platformConnectedClient = consumer.getPlatformConnectedClient(clientDescriptor)
        .orElseThrow(() -> new IllegalStateException("Invalid monitoring tree: cannot get " + PlatformConnectedClient.class.getSimpleName() + " from descriptor " + clientDescriptor));
    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
    managementAgent.disconnected(clientDescriptor);
    producer.removeNode(array("management", "clients"), clientIdentifier.getClientId());
    super.disconnected(clientDescriptor);
  }

  @Override
  public void destroy() {
    consumer.close();
    producer.removeNode(new String[0], "management");
    super.destroy();
  }

  private static ClientIdentifier toClientIdentifier(PlatformConnectedClient connection) {
    return ClientIdentifier.create(
        connection.clientPID,
        connection.remoteAddress.getHostAddress(),
        connection.name == null || connection.name.isEmpty() ? "UNKNOWN" : connection.name,
        connection.uuid);
  }

}
