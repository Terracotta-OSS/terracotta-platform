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

import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;

import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_PATH;

/**
 * @author Mathieu Carbou
 */
class Utils {

  static <T> T[] array(T... o) {
    return o;
  }

  static ClientIdentifier getClientIdentifier(IMonitoringConsumer consumer, Object clientDescriptor) {
    return toClientIdentifier(getPlatformConnectedClient(consumer, clientDescriptor).getValue());
  }

  // return the PlatformConnectedClient object representing the connection used by this clientDescriptor
  private static Map.Entry<String, PlatformConnectedClient> getPlatformConnectedClient(IMonitoringConsumer consumer, Object clientDescriptor) {
    return getPlatformClientFetchedEntity(consumer, clientDescriptor)
        .flatMap(entry -> consumer.getValueForNode(CLIENTS_PATH, entry.getValue().clientIdentifier, PlatformConnectedClient.class)
            .map(platformConnectedClient -> new AbstractMap.SimpleEntry<>(entry.getValue().clientIdentifier, platformConnectedClient)))
        .orElseThrow(() -> new IllegalStateException("Unable to find fetch information matching client descriptor " + clientDescriptor));
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

}
