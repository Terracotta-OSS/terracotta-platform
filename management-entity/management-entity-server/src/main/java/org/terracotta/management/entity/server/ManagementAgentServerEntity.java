/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.terracotta.management.entity.server;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.entity.ManagementAgent;
import org.terracotta.management.entity.ManagementAgentConfig;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.voltron.proxy.server.ProxiedServerEntity;

import java.util.Objects;

import static org.terracotta.management.entity.server.Utils.array;
import static org.terracotta.management.entity.server.Utils.getClientIdentifier;

/**
 * @author Mathieu Carbou
 */
class ManagementAgentServerEntity extends ProxiedServerEntity<ManagementAgent> {

  private final IMonitoringConsumer consumer;
  private final IMonitoringProducer producer;

  ManagementAgentServerEntity(ManagementAgentConfig config, IMonitoringConsumer consumer, IMonitoringProducer producer) {
    super(new ManagementAgentImpl(config, consumer, producer));
    this.consumer = Objects.requireNonNull(consumer, "IMonitoringConsumer service is missing");
    this.producer = Objects.requireNonNull(producer, "IMonitoringProducer service is missing");

    // when an entity is created, we create the root: /management (null)
    if (!consumer.getChildNamesForNode(array("management")).isPresent()) {
      producer.addNode(new String[0], "management", null);
    }
    if (!consumer.getChildNamesForNode(array("management", "clients")).isPresent()) {
      producer.addNode(array("management"), "clients", null);
    }
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    super.connected(clientDescriptor);

    // when an entity is fetched, we create the root /management/<id> (ClientDescriptor)
    ClientIdentifier identifier = getClientIdentifier(consumer, clientDescriptor);
    producer.addNode(array("management", "clients"), identifier.getClientId(), clientDescriptor);
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    // when an entity is closed, we remove the node /management/<id> having the same ClientDescriptor
    ClientIdentifier identifier = getClientIdentifier(consumer, clientDescriptor);
    producer.removeNode(array("management", "clients"), identifier.getClientId());

    super.disconnected(clientDescriptor);
  }

  @Override
  public void destroy() {
    consumer.close();
    super.destroy();
  }

}
