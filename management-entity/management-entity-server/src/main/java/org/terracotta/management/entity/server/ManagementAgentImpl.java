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
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Manageable;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.voltron.proxy.ClientId;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.terracotta.management.entity.server.Utils.array;
import static org.terracotta.management.entity.server.Utils.toClientIdentifier;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_ROOT_NAME;
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
 * <li>{@code platform/fetched/<id>/contextContainer ContextContainer}</li>
 * <li>{@code platform/fetched/<id>/capabilities Collections<Capability>}</li>
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
  public Future<Collection<Context>> getEntityContexts(@ClientId Object clientDescriptor) {
    Map.Entry<String, PlatformConnectedClient> platformConnectedClient = getPlatformConnectedClient(clientDescriptor);
    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient.getValue());
    Context parent = Context.create(Client.KEY, clientIdentifier.getClientId());

    return CompletableFuture.completedFuture(getPlatformClientFetchedEntities(platformConnectedClient.getKey())
        .map(entry -> {

          Optional<PlatformEntity> platformEntity = consumer.getValueForNode(ENTITIES_PATH, entry.getValue().entityIdentifier, PlatformEntity.class);
          if (!platformEntity.isPresent()) {
            return Context.empty();
          }

          // create a manageable just to build a context object easily. we could have just set the keys by our own instead.
          Manageable manageable = Manageable.create(platformEntity.get().name, platformEntity.get().typeName);

          consumer.getValueForNode(array(PLATFORM_ROOT_NAME, FETCHED_ROOT_NAME, entry.getKey(), "contextContainer"), ContextContainer.class)
              .ifPresent(manageable::setContextContainer);

          return parent.with(manageable.getContext());
        })
        .distinct()
        .collect(Collectors.toList()));
  }

  @Override
  public Future<Void> expose(Context entityContext, ContextContainer contextContainer, Collection<Capability> capabilities, Collection<String> tags, @ClientId Object clientDescriptor) {
    if (!entityContext.contains(Manageable.NAME_KEY) || !entityContext.contains(Manageable.TYPE_KEY)) {
      throw new IllegalArgumentException("Bad entity context: " + entityContext);
    }

    // get the connection info
    Map.Entry<String, PlatformConnectedClient> platformConnectedClient = getPlatformConnectedClient(clientDescriptor);

    // iterate over all fetched entities iver this connection to find the "fetch" representing this context
    // once found, we pout in the tree the management metadata, under platform/fetched/<id>/contextContainer and platform/fetched/<id>/capabilities
    getPlatformClientFetchedEntities(platformConnectedClient.getKey())
        .forEach(entry -> consumer.getValueForNode(ENTITIES_PATH, entry.getValue().entityIdentifier, PlatformEntity.class)
            .filter(platformEntity -> platformEntity.name.equals(entityContext.get(Manageable.NAME_KEY)) && platformEntity.typeName.equals(entityContext.get(Manageable.TYPE_KEY)))
            .ifPresent(platformEntity -> {
              producer.addNode(array(PLATFORM_ROOT_NAME, FETCHED_ROOT_NAME, entry.getKey()), "contextContainer", contextContainer);
              producer.addNode(array(PLATFORM_ROOT_NAME, FETCHED_ROOT_NAME, entry.getKey()), "capabilities", capabilities);
              producer.addNode(array(PLATFORM_ROOT_NAME, FETCHED_ROOT_NAME, entry.getKey()), "tags", tags == null ? Collections.emptyList() : tags);
            }));

    return CompletableFuture.completedFuture(null);
  }

  // returns all the fetched entities over a connection
  private Stream<? extends Map.Entry<String, PlatformClientFetchedEntity>> getPlatformClientFetchedEntities(String clientIdentifier) {
    return consumer.getChildValuesForNode(FETCHED_PATH)
        .map(children -> children.entrySet()
            .stream()
            .filter(entry -> entry.getValue() instanceof PlatformClientFetchedEntity)
            .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), (PlatformClientFetchedEntity) entry.getValue()))
            .filter(entry -> entry.getValue().clientIdentifier.equals(clientIdentifier)))
        .orElseThrow(() -> new IllegalStateException("No entities have been fetched"));
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

}
