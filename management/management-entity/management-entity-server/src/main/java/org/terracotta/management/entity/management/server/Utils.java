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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.entity.management.ManagementAgentConfig;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_PATH;

/**
 * @author Mathieu Carbou
 */
class Utils {

  static <T> T[] array(T... o) {
    return o;
  }

  static Optional<ClientIdentifier> getClientIdentifier(IMonitoringConsumer consumer, Object clientDescriptor) {
    return getPlatformConnectedClient(consumer, clientDescriptor).map(entry -> toClientIdentifier(entry.getValue()));
  }

  // return the PlatformConnectedClient object representing the connection used by this clientDescriptor
  private static Optional<Map.Entry<String, PlatformConnectedClient>> getPlatformConnectedClient(IMonitoringConsumer consumer, Object clientDescriptor) {
    return getPlatformClientFetchedEntity(consumer, clientDescriptor)
        .flatMap(entry -> consumer.getValueForNode(CLIENTS_PATH, entry.getValue().clientIdentifier, PlatformConnectedClient.class)
            .map(platformConnectedClient -> new AbstractMap.SimpleEntry<>(entry.getValue().clientIdentifier, platformConnectedClient)));
  }

  // return the PlatformClientFetchedEntity object linked to this clientDescriptor
  private static Optional<? extends Map.Entry<String, PlatformClientFetchedEntity>> getPlatformClientFetchedEntity(IMonitoringConsumer consumer, Object clientDescriptor) {
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

  static Optional<ClientDescriptor> getClientDescriptor(IMonitoringConsumer consumer, ClientIdentifier clientIdentifier) {
    return consumer.getChildValuesForNode(CLIENTS_PATH)
        // walk on all platform clients having same client identifier
        .flatMap(children -> children.entrySet()
            .stream()
            .filter(entry -> entry.getValue() instanceof PlatformConnectedClient)
            .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), (PlatformConnectedClient) entry.getValue()))
            .filter(entry -> toClientIdentifier(entry.getValue()).equals(clientIdentifier))
            .map(Map.Entry::getKey)
            .findFirst())
        // then find all fetches for this client
        .flatMap(voltronConnectionId -> consumer.getChildValuesForNode(FETCHED_PATH)
            .flatMap(children -> children.entrySet()
                .stream()
                .filter(entry -> entry.getValue() instanceof PlatformClientFetchedEntity)
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), (PlatformClientFetchedEntity) entry.getValue()))
                .filter(entry -> entry.getValue().clientIdentifier.equals(voltronConnectionId))
                .map(Map.Entry::getValue)
                // only interested in the fetch for the management entity
                .filter(platformClientFetchedEntity -> consumer.getValueForNode(ENTITIES_PATH, platformClientFetchedEntity.entityIdentifier, PlatformEntity.class)
                    .filter(platformEntity -> platformEntity.typeName.equals(ManagementAgentConfig.ENTITY_TYPE))
                    .isPresent())
                // return the client descriptor for the client that has fetched the management entity
                .map(platformClientFetchedEntity -> platformClientFetchedEntity.clientDescriptor)
                .findFirst()));
  }

  static Collection<ClientIdentifier> getManageableClients(IMonitoringConsumer consumer) {
    return consumer.getChildNamesForNode("management", "clients")
        .orElse(Collections.emptyList())
        .stream()
        .map(ClientIdentifier::valueOf)
        .collect(Collectors.toSet());
  }

  static boolean isManageableClient(IMonitoringConsumer consumer, ClientIdentifier to) {
    return consumer.getChildNamesForNode("management", "clients", to.getClientId()).isPresent();
  }

}
