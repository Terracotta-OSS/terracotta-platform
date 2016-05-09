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
package org.terracotta.management.entity.server;

import org.terracotta.management.entity.ManagementAgent;
import org.terracotta.management.entity.ManagementAgentConfig;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.voltron.proxy.ClientId;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.terracotta.management.entity.server.Utils.array;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_ROOT_NAME;

/**
 * Consumes:
 * <ul>
 * <li>{@code platform/clients/<id> PlatformConnectedClient}</li>
 * <li>{@code platform/fetched/<id> PlatformClientFetchedEntity}</li>
 * <li>{@code platform/entities/<id> PlatformEntity}</li>
 * </ul>
 * Produces:
 * <ul>
 * <li>{@code platform/clients/<id>/tags Collections<String>}</li>
 * <li>{@code platform/clients/<id>/management/<id>/contextContainer ContextContainer}</li>
 * <li>{@code platform/clients/<id>/management/<id>/capabilities Collections<Capability>}</li>
 * </ul>
 *
 * @author Mathieu Carbou
 */
class ManagementAgentImpl implements ManagementAgent {

  private final ManagementAgentConfig config;
  private final IMonitoringProducer producer;
  private final IMonitoringConsumer consumer;

  ManagementAgentImpl(ManagementAgentConfig config, IMonitoringConsumer consumer, IMonitoringProducer producer) {
    this.config = config;
    this.producer = producer;
    this.consumer = consumer;
  }

  @Override
  public Future<ClientIdentifier> getClientIdentifier(@ClientId Object clientDescriptor) {
    Map.Entry<String, PlatformConnectedClient> platformConnectedClient = getPlatformConnectedClient(clientDescriptor);
    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient.getValue());
    return CompletableFuture.completedFuture(clientIdentifier);
  }

  @Override
  public Future<Void> exposeManagementMetadata(@ClientId Object clientDescriptor, ContextContainer contextContainer, Capability... capabilities) {
    // get the connection info
    Map.Entry<String, PlatformConnectedClient> entry = getPlatformConnectedClient(clientDescriptor);

    String key = contextContainer.getName() + ":" + contextContainer.getValue();
    String[] clientPath = array(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, entry.getKey());
    String[] managementRoot = array(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, entry.getKey(), "management");
    String[] managementPath = array(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, entry.getKey(), "management", key);

    // ensure the root is there
    if (!consumer.getValueForNode(managementRoot, Object.class).isPresent()) {
      producer.addNode(clientPath, "management", null);
    }

    producer.addNode(managementRoot, key, null);
    producer.addNode(managementPath, "contextContainer", contextContainer);
    producer.addNode(managementPath, "capabilities", capabilities);

    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> exposeTags(@ClientId Object clientDescriptor, String... tags) {
    // get the connection info
    Map.Entry<String, PlatformConnectedClient> entry = getPlatformConnectedClient(clientDescriptor);
    producer.addNode(array(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, entry.getKey()), "tags", tags == null ? new String[0] : tags);
    return CompletableFuture.completedFuture(null);
  }

  // return the PlatformConnectedClient object representing the connection used by this clientDescriptor
  private Map.Entry<String, PlatformConnectedClient> getPlatformConnectedClient(Object clientDescriptor) {
    return getPlatformClientFetchedEntity(clientDescriptor)
        .flatMap(entry -> consumer.getValueForNode(CLIENTS_PATH, entry.getValue().clientIdentifier, PlatformConnectedClient.class)
            .map(platformConnectedClient -> new AbstractMap.SimpleEntry<>(entry.getValue().clientIdentifier, platformConnectedClient)))
        .orElseThrow(() -> new IllegalStateException("Unable to find fetch matching client descriptor " + clientDescriptor));
  }

  // return the PlatformClientFetchedEntity object linked to this clientDescriptor
  private Optional<? extends Map.Entry<String, PlatformClientFetchedEntity>> getPlatformClientFetchedEntity(Object clientDescriptor) {
    return consumer.getChildValuesForNode(FETCHED_PATH)
        .flatMap(children -> children.entrySet()
            .stream()
            .filter(entry -> entry.getValue() instanceof PlatformClientFetchedEntity)
            .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), (PlatformClientFetchedEntity) entry.getValue()))
            .filter(entry -> entry.getValue().clientDescriptor.equals(clientDescriptor))
            .findFirst());
  }

  private static ClientIdentifier toClientIdentifier(PlatformConnectedClient connection) {
    return ClientIdentifier.create(
        connection.clientPID,
        connection.remoteAddress.getHostAddress(),
        connection.name == null || connection.name.isEmpty() ? "UNKNOWN" : connection.name,
        connection.uuid);
  }

}
