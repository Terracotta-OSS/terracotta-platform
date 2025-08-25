/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Connection;
import org.terracotta.management.model.cluster.Endpoint;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.cluster.ServerEntityIdentifier;
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.terracotta.management.service.monitoring.Notification.CLIENT_CONNECTED;
import static org.terracotta.management.service.monitoring.Notification.CLIENT_DISCONNECTED;
import static org.terracotta.management.service.monitoring.Notification.SERVER_ENTITY_CREATED;
import static org.terracotta.management.service.monitoring.Notification.SERVER_ENTITY_DESTROYED;
import static org.terracotta.management.service.monitoring.Notification.SERVER_ENTITY_FETCHED;
import static org.terracotta.management.service.monitoring.Notification.SERVER_ENTITY_RECONFIGURED;
import static org.terracotta.management.service.monitoring.Notification.SERVER_ENTITY_UNFETCHED;
import static org.terracotta.management.service.monitoring.Notification.SERVER_JOINED;
import static org.terracotta.management.service.monitoring.Notification.SERVER_LEFT;
import static org.terracotta.management.service.monitoring.Notification.SERVER_STATE_CHANGED;

/**
 * @author Mathieu Carbou
 */
class TopologyService implements PlatformListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(TopologyService.class);

  private final Cluster cluster;
  private final Stripe stripe;
  // map of topology client created per fetch (client descriptor), per entity, on the active server
  private final ConcurrentMap<Long, ConcurrentMap<ClientDescriptor, ExecutionChain<Client>>> entityFetches = new ConcurrentHashMap<>();
  // map of entities created per consumer id on each server
  private final ConcurrentMap<String, ConcurrentMap<Long, ExecutionChain<ServerEntity>>> serverEntities = new ConcurrentHashMap<>();
  private final FiringService firingService;
  private final PlatformConfiguration platformConfiguration;
  private final List<TopologyEventListener> topologyEventListeners = new CopyOnWriteArrayList<>();

  private volatile Server currentActive;

  TopologyService(FiringService firingService, PlatformConfiguration platformConfiguration) {
    this.firingService = Objects.requireNonNull(firingService);
    this.platformConfiguration = platformConfiguration;
    this.cluster = new Cluster();
    org.terracotta.dynamic_config.api.service.TopologyService dcTopologyService =
      platformConfiguration.getExtendedConfiguration(org.terracotta.dynamic_config.api.service.TopologyService.class)
        .iterator().next();
    this.cluster.addStripe(stripe = Stripe.create(dcTopologyService.getRuntimeNodeContext().getStripe().getName()));
  }

  // ================================================
  // PLATFORM CALLBACKS: only called on active server
  // ================================================

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public synchronized void serverDidBecomeActive(PlatformServer self) {
    LOGGER.trace("[0] serverDidBecomeActive({})", self.getServerName());

    long now = System.currentTimeMillis();
    Server server = Server.create(self.getServerName())
        .setBindAddress(self.getBindAddress())
        .setBindPort(self.getBindPort())
        .setBuildId(self.getBuild())
        .setGroupPort(self.getGroupPort())
        .setHostName(self.getHostName())
        .setStartTime(self.getStartTime())
        .setActivateTime(now)
        .setHostAddress(self.getHostAddress())
        .setVersion(self.getVersion())
        .setState(Server.State.ACTIVE)
        .computeUpTime();

    if (stripe.addServer(server)) {
      currentActive = stripe.getServerByName(self.getServerName()).get();

      topologyEventListeners.forEach(listener -> listener.onBecomeActive(platformConfiguration.getServerName()));

      Map<String, String> attrs = new HashMap<>();
      attrs.put("startTime", String.valueOf(server.getStartTime()));
      attrs.put("activateTime", String.valueOf(server.getActivateTime()));
      attrs.put("version", server.getVersion());
      attrs.put("buildId", server.getBuildId());
      attrs.put("state", server.getState().toString());

      firingService.fireNotification(new ContextualNotification(server.getContext(), SERVER_JOINED.name(), attrs));

      serverStateChanged(self, new ServerState("ACTIVE", now, now));
    }
  }

  @Override
  public synchronized void serverDidJoinStripe(PlatformServer platformServer) {
    LOGGER.trace("[0] serverDidJoinStripe({})", platformServer.getServerName());

    Server server = Server.create(platformServer.getServerName())
        .setBindAddress(platformServer.getBindAddress())
        .setBindPort(platformServer.getBindPort())
        .setBuildId(platformServer.getBuild())
        .setGroupPort(platformServer.getGroupPort())
        .setHostName(platformServer.getHostName())
        .setStartTime(platformServer.getStartTime())
        .setHostAddress(platformServer.getHostAddress())
        .setVersion(platformServer.getVersion())
        .setState(Server.State.STARTING)
        .computeUpTime();

    if (stripe.addServer(server)) {
      Map<String, String> attrs = new HashMap<>();
      attrs.put("startTime", String.valueOf(server.getStartTime()));
      attrs.put("activateTime", String.valueOf(server.getActivateTime()));
      attrs.put("version", server.getVersion());
      attrs.put("buildId", server.getBuildId());
      attrs.put("state", server.getState().toString());

      firingService.fireNotification(new ContextualNotification(server.getContext(), SERVER_JOINED.name(), attrs));
    }
  }

  @Override
  public synchronized void serverDidLeaveStripe(PlatformServer platformServer) {
    LOGGER.trace("[0] serverDidLeaveStripe({})", platformServer.getServerName());

    stripe.getServerByName(platformServer.getServerName()).ifPresent(server -> {
      Context context = server.getContext();
      server.remove();

      server.setState(Server.State.UNREACHABLE);
      serverEntities.remove(platformServer.getServerName());

      Map<String, String> attrs = new HashMap<>();
      attrs.put("startTime", "0");
      attrs.put("activateTime", "0");
      attrs.put("version", null);
      attrs.put("buildId", null);
      attrs.put("state", server.getState().toString());

      firingService.fireNotification(new ContextualNotification(context, SERVER_LEFT.name(), attrs));
    });
  }

  @Override
  public synchronized void serverEntityCreated(PlatformServer sender, PlatformEntity platformEntity) {
    LOGGER.trace("[0] serverEntityCreated({}, {})", sender.getServerName(), platformEntity);

    if (platformEntity.isActive && !sender.getServerName().equals(getActiveServer().getServerName())) {
      Utils.warnOrAssert(LOGGER, "[0] serverEntityCreated({}, {}): Server is not current active server but it created an active entity", sender.getServerName(), platformEntity);
      return;
    }

    if (!platformEntity.isActive && sender.getServerName().equals(getActiveServer().getServerName())) {
      Utils.warnOrAssert(LOGGER, "[0] serverEntityCreated({}, {}): Server is the current active server but it created a passive entity", sender.getServerName(), platformEntity);
      return;
    }

    if (isInterestingEntity(platformEntity)) {
      stripe.getServerByName(sender.getServerName()).ifPresent(server -> {
        ServerEntityIdentifier identifier = ServerEntityIdentifier.create(platformEntity.name, platformEntity.typeName);
        ServerEntity entity = ServerEntity.create(identifier).setConsumerId(platformEntity.consumerID);

        if (server.addServerEntity(entity)) {
          firingService.fireNotification(new ContextualNotification(entity.getContext(), SERVER_ENTITY_CREATED.name()));

          whenServerEntity(platformEntity.consumerID, sender.getServerName()).complete(entity);

          if (sender.getServerName().equals(getServerName())) {
            topologyEventListeners.forEach(listener -> listener.onEntityCreated(platformEntity.consumerID));
          }
        }
      });
    }
  }

  @Override
  public void serverEntityReconfigured(PlatformServer sender, PlatformEntity platformEntity) {
    if (isInterestingEntity(platformEntity)) {
      LOGGER.trace("[0] serverEntityReconfigured({}, {})", sender.getServerName(), platformEntity);

      stripe.getServerByName(sender.getServerName()).ifPresent(server -> {
        ServerEntityIdentifier identifier = ServerEntityIdentifier.create(platformEntity.name, platformEntity.typeName);
        server.getServerEntity(identifier)
          .ifPresent(entity -> firingService.fireNotification(new ContextualNotification(entity.getContext(), SERVER_ENTITY_RECONFIGURED.name())));
      });
    }
  }

  @Override
  public synchronized void serverEntityDestroyed(PlatformServer sender, PlatformEntity platformEntity) {
    LOGGER.trace("[0] serverEntityDestroyed({}, {})", sender.getServerName(), platformEntity);

    if (platformEntity.isActive && !sender.getServerName().equals(getActiveServer().getServerName())) {
      Utils.warnOrAssert(LOGGER, "[0] serverEntityDestroyed({}, {}): Server is not current active server but it destroyed an active entity", sender.getServerName(), platformEntity);
      return;
    }

    if (!platformEntity.isActive && sender.getServerName().equals(getActiveServer().getServerName())) {
      Utils.warnOrAssert(LOGGER, "[0] serverEntityDestroyed({}, {}): Server is the current active server but it destroyed a passive entity", sender.getServerName(), platformEntity);
      return;
    }

    if (isInterestingEntity(platformEntity)) {
      stripe.getServerByName(sender.getServerName())
        .flatMap(server -> server.getServerEntity(platformEntity.name, platformEntity.typeName))
        .ifPresent(entity -> {
          Context context = entity.getContext();
          entity.remove();

          serverEntities.get(sender.getServerName()).remove(platformEntity.consumerID);

          if (isCurrentServerActive() && sender.getServerName().equals(currentActive.getServerName())) {
            entityFetches.remove(platformEntity.consumerID);
          }

          if (sender.getServerName().equals(getServerName())) {
            topologyEventListeners.forEach(listener -> listener.onEntityDestroyed(platformEntity.consumerID));
          }

          firingService.fireNotification(new ContextualNotification(context, SERVER_ENTITY_DESTROYED.name()));
        });
    }
  }

  @Override
  public synchronized void clientConnected(PlatformServer currentActive, PlatformConnectedClient platformConnectedClient) {
    LOGGER.trace("[0] clientConnected({})", platformConnectedClient);

    if (isInterestingClient(platformConnectedClient)) {
      stripe.getServerByName(currentActive.getServerName())
        .ifPresent(server -> {
            ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
            Endpoint endpoint = Endpoint.create(platformConnectedClient.remoteAddress.getHostAddress(), platformConnectedClient.remotePort);
            Client client = Client.create(clientIdentifier).setHostName(platformConnectedClient.remoteAddress.getHostName());

            cluster.addClient(client);

            if (client.addConnection(Connection.create(clientIdentifier.getConnectionUid(), getActiveServer(), endpoint))) {
                firingService.fireNotification(new ContextualNotification(server.getContext(), CLIENT_CONNECTED.name(), client.getContext()));
            }
        });
    }
  }

  @Override
  public synchronized void clientAddProperty(PlatformConnectedClient platformConnectedClient, String key, String value) {
    LOGGER.trace("[0] client property added ({}, key:{}, value:{})", platformConnectedClient, key, value);

    if (isInterestingClient(platformConnectedClient)) {
      stripe.getServerByName(currentActive.getServerName())
        .ifPresent(server -> {
            ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
            cluster.getClient(clientIdentifier)
              .ifPresent(client -> {
                client.addProperty(key, value);
                  firingService.fireNotification(new ContextualNotification(client.getContext(), Notification.CLIENT_PROPERTY_ADDED.name(), Collections.singletonMap(key, value)));
              });
        });
    }
  }

  @Override
  public synchronized void clientDisconnected(PlatformServer currentActive, PlatformConnectedClient platformConnectedClient) {
    LOGGER.trace("[0] clientDisconnected({})", platformConnectedClient);

    if (isInterestingClient(platformConnectedClient)) {
      stripe.getServerByName(currentActive.getServerName())
        .ifPresent(server -> {
            ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
            cluster.getClient(clientIdentifier)
              .ifPresent(client -> {
                Context clientContext = client.getContext();
                client.remove();
                  firingService.fireNotification(new ContextualNotification(server.getContext(), CLIENT_DISCONNECTED.name(), clientContext));
              });
        });
    }
  }

  @Override
  public synchronized void clientFetch(PlatformConnectedClient platformConnectedClient, PlatformEntity platformEntity, ClientDescriptor clientDescriptor) {
    LOGGER.trace("[0] clientFetch({}, {})", platformConnectedClient, platformEntity);

    if (isInterestingClient(platformConnectedClient) && isInterestingEntity(platformEntity)) {
      Server currentActive = getActiveServer();
      ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
      Endpoint endpoint = Endpoint.create(platformConnectedClient.remoteAddress.getHostAddress(), platformConnectedClient.remotePort);

      cluster.getClient(clientIdentifier).ifPresent(client -> client.getConnection(currentActive, endpoint)
        .ifPresent(connection -> currentActive.getServerEntity(platformEntity.name, platformEntity.typeName)
          .ifPresent(entity -> {
            connection.fetchServerEntity(platformEntity.name, platformEntity.typeName);
            firingService.fireNotification(new ContextualNotification(entity.getContext(), SERVER_ENTITY_FETCHED.name(), client.getContext()));
            whenFetchClient(platformEntity.consumerID, clientDescriptor).complete(client);
            topologyEventListeners.forEach(listener -> listener.onFetch(platformEntity.consumerID, clientDescriptor));
          })));
    }
  }

  @Override
  public synchronized void clientUnfetch(PlatformConnectedClient platformConnectedClient, PlatformEntity platformEntity, ClientDescriptor clientDescriptor) {
    LOGGER.trace("[0] clientUnfetch({}, {})", platformConnectedClient, platformEntity);

    if (isInterestingClient(platformConnectedClient) && isInterestingEntity(platformEntity)) {
      Server currentActive = getActiveServer();
      ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
      Endpoint endpoint = Endpoint.create(platformConnectedClient.remoteAddress.getHostAddress(), platformConnectedClient.remotePort);

      currentActive.getServerEntity(platformEntity.name, platformEntity.typeName)
        .ifPresent(entity -> cluster.getClient(clientIdentifier)
          .ifPresent(client -> client.getConnection(currentActive, endpoint)
            .ifPresent(connection -> {
              entityFetches.get(platformEntity.consumerID).remove(clientDescriptor);
              if (connection.unfetchServerEntity(platformEntity.name, platformEntity.typeName)) {
                firingService.fireNotification(new ContextualNotification(entity.getContext(), SERVER_ENTITY_UNFETCHED.name(), client.getContext()));
              }
              topologyEventListeners.forEach(listener -> listener.onUnfetch(platformEntity.consumerID, clientDescriptor));
            })));
    }
  }

  @Override
  public synchronized void serverStateChanged(PlatformServer sender, ServerState serverState) {
    stripe.getServerByName(sender.getServerName()).ifPresent(server -> {
      Server.State oldState = server.getState();
      Server.State newState = Server.State.parse(serverState.getState());

      LOGGER.trace("[0] serverStateChanged({}, newState={}, oldState={})", sender.getServerName(), newState, server.getState());

      server.setState(Server.State.parse(serverState.getState()));

      boolean available = newState != Server.State.UNREACHABLE && newState != Server.State.UNKNOWN;
      long startTime = available ? server.getStartTime() : 0;
      long activateTime = available ? server.getActivateTime() : 0;

      server.setStartTime(startTime).setActivateTime(activateTime);

      Map<String, String> attrs = new HashMap<>();
      attrs.put("oldState", oldState.name());
      attrs.put("state", serverState.getState());
      attrs.put("startTime", String.valueOf(startTime));
      attrs.put("activateTime", String.valueOf(activateTime));
      if (available) {
        attrs.put("version", server.getVersion());
        attrs.put("buildId", server.getBuildId());
      } else {
        server.setBuildId(null).setVersion(null);
      }

      firingService.fireNotification(new ContextualNotification(server.getContext(), SERVER_STATE_CHANGED.name(), attrs));
    });
  }

  // ======================================================================
  // ENTITIES and CLIENTS topology completion by adding management metadata
  // ======================================================================

  /**
   * Records stats that needs to be sent in future (or now) when the client fetch info will have arrived
   */
  void willPushClientStatistics(long consumerId, ClientDescriptor from, ContextualStatistics... statistics) {
    whenFetchClient(consumerId, from).executeOrDiscard(client -> {
      Context context = client.getContext();
      for (ContextualStatistics statistic : statistics) {
        statistic.setContext(statistic.getContext().with(context));
      }
      LOGGER.trace("[{}] willPushClientStatistics({}, {})", consumerId, from, statistics.length);
      firingService.fireStatistics(statistics);
    });
  }

  /**
   * Records notification that needs to be sent in future (or now) when the client fetch info will have arrived
   */
  void willPushClientNotification(long consumerId, ClientDescriptor from, ContextualNotification notification) {
    whenFetchClient(consumerId, from).executeOrDelay(client -> {
      Context context = client.getContext();
      notification.setContext(notification.getContext().with(context));
      LOGGER.trace("[{}] willPushClientNotification({}, {})", consumerId, from, notification);
      firingService.fireNotification(notification);
    });
  }

  /**
   * Records registry that needs to be sent in future (or now) when the client will have arrived
   */
  void willSetClientManagementRegistry(long consumerId, ClientDescriptor clientDescriptor, ManagementRegistry newRegistry) {
    LOGGER.trace("[{}] willSetClientManagementRegistry({}, {})", consumerId, clientDescriptor, newRegistry);

    whenFetchClient(consumerId, clientDescriptor).executeOrDelay("client-registry", client -> {
      if (!newRegistry.equals(client.getManagementRegistry().orElse(null))) {
        client.setManagementRegistry(newRegistry);
        firingService.fireNotification(new ContextualNotification(client.getContext(), Notification.CLIENT_REGISTRY_AVAILABLE.name()));
      }
    });
  }

  /**
   * Records tags that needs to be sent in future (or now) when the client info will have arrived
   */
  void willSetClientTags(long consumerId, ClientDescriptor clientDescriptor, String[] tags) {
    LOGGER.trace("[{}] willSetClientTags({}, {})", consumerId, clientDescriptor, Arrays.toString(tags));

    whenFetchClient(consumerId, clientDescriptor).executeOrDelay("client-tags", client -> {
      Set<String> currtags = new HashSet<>(client.getTags());
      Set<String> newTags = new HashSet<>(Arrays.asList(tags));
      if (!currtags.equals(newTags)) {
        client.setTags(tags);
        firingService.fireNotification(new ContextualNotification(client.getContext(), Notification.CLIENT_TAGS_UPDATED.name()));
      }
    });
  }

  /**
   * Records stats that needs to be sent in future (or now) when the entity will have arrived
   */
  void willPushEntityStatistics(long consumerId, String serverName, ContextualStatistics... statistics) {
    // Stats are collected by the NMS entity collector so the consumer id calling willPushEntityStatistics
    // will be the consumer id of the NMS entity. Thus, we have to retrieve the server entity thanks to the
    // context that is hold in the stat results
    Stream.of(statistics)
        .collect(Collectors.groupingBy(o -> Long.parseLong(o.getContext().getOrDefault(ServerEntity.CONSUMER_ID, String.valueOf(consumerId)))))
        .forEach((cid, cid_stats) -> whenServerEntity(cid, serverName).executeOrDiscard(serverEntity -> {
          Context context = serverEntity.getContext();
          for (ContextualStatistics statistic : cid_stats) {
            statistic.setContext(statistic.getContext().with(context));
          }
          LOGGER.trace("[{}] willPushEntityStatistics({}, {})", cid, serverName, cid_stats.size());
          firingService.fireStatistics(cid_stats.toArray(new ContextualStatistics[0]));
        }));
  }

  /**
   * Records notification that needs to be sent in future (or now) when the entity will have arrived
   */
  void willPushEntityNotification(long consumerId, String serverName, ContextualNotification notification) {
    // notifications contains a context, but if this context contains an origin consumer id, do not override it
    long cid = Long.parseLong(notification.getContext().getOrDefault(ServerEntity.CONSUMER_ID, String.valueOf(consumerId)));
    whenServerEntity(cid, serverName).executeOrDelay(serverEntity -> {
      Context context = serverEntity.getContext();
      notification.setContext(notification.getContext().with(context));
      LOGGER.trace("[{}] willPushEntityNotification({}, {})", cid, serverName, notification);
      firingService.fireNotification(notification);
    });
  }

  /**
   * Records registry that needs to be sent in future (or now) when the entity will have arrived
   */
  void willSetEntityManagementRegistry(long consumerId, String serverName, ManagementRegistry newRegistry) {
    List<String> names = newRegistry.getCapabilities().stream().map(Capability::getName).collect(Collectors.toList());
    LOGGER.trace("[{}] willSetEntityManagementRegistry({}, {})", consumerId, serverName, names);

    whenServerEntity(consumerId, serverName).executeOrDelay("entity-registry", serverEntity -> {
      if (!newRegistry.equals(serverEntity.getManagementRegistry().orElse(null))) {
        serverEntity.setManagementRegistry(newRegistry);
        firingService.fireNotification(new ContextualNotification(serverEntity.getContext(), Notification.ENTITY_REGISTRY_AVAILABLE.name()));
      }
    });
  }

  // ==============
  // QUERY TOPOLOGY
  // ==============

  CompletableFuture<ClientIdentifier> getClientIdentifier(long consumerId, ClientDescriptor clientDescriptor) {
    CompletableFuture<ClientIdentifier> clientIdentifier = new CompletableFuture<>();
    whenFetchClient(consumerId, clientDescriptor).executeOrDelay(client -> clientIdentifier.complete(client.getClientIdentifier()));
    return clientIdentifier;
  }

  synchronized Optional<Context> getManageableEntityContext(String serverName, long consumerId) {
    return stripe.getServerByName(serverName)
        .flatMap(server -> server.getServerEntity(consumerId))
        .filter(ServerEntity::isManageable)
        .map(ServerEntity::getContext);
  }

  synchronized Optional<Context> getManageableEntityContext(String serverName, String entityName, String entityType) {
    return stripe.getServerByName(serverName)
        .flatMap(server -> server.getServerEntity(entityName, entityType))
        .filter(ServerEntity::isManageable)
        .map(ServerEntity::getContext);
  }

  synchronized Optional<Context> getManageableClientContext(ClientIdentifier clientIdentifier) {
    return cluster.getClient(clientIdentifier)
        .filter(Client::isManageable)
        .map(Client::getContext);
  }

  synchronized Cluster getClusterCopy() {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(cluster);
        oos.flush();
      }
      try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
        return (Cluster) ois.readObject();
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  void addTopologyEventListener(TopologyEventListener topologyEventListener) {
    topologyEventListeners.add(Objects.requireNonNull(topologyEventListener));
  }

  void removeTopologyEventListener(TopologyEventListener topologyEventListener) {
    if (topologyEventListener != null) {
      topologyEventListeners.remove(topologyEventListener);
    }
  }

  String getServerName() {
    return platformConfiguration.getServerName();
  }

  boolean isServerActive(String serverName) {
    return currentActive != null && currentActive.getServerName().equals(serverName);
  }

  Server getActiveServer() {
    return Objects.requireNonNull(currentActive);
  }

  public boolean isCurrentServerActive() {
    return isServerActive(getServerName());
  }

  private ExecutionChain<Client> whenFetchClient(long consumerId, ClientDescriptor clientDescriptor) {
    ConcurrentMap<ClientDescriptor, ExecutionChain<Client>> fetches = entityFetches.computeIfAbsent(consumerId, cid -> new ConcurrentHashMap<>());
    return fetches.computeIfAbsent(clientDescriptor, key -> new ExecutionChain<>());
  }

  private ExecutionChain<ServerEntity> whenServerEntity(long consumerId, String serverName) {
    ConcurrentMap<Long, ExecutionChain<ServerEntity>> entities = serverEntities.computeIfAbsent(serverName, name -> new ConcurrentHashMap<>());
    return entities.computeIfAbsent(consumerId, key -> new ExecutionChain<>());
  }

  private static ClientIdentifier toClientIdentifier(PlatformConnectedClient connection) {
    return ClientIdentifier.create(
        connection.clientPID,
        connection.remoteAddress.getHostAddress(),
        connection.name == null || connection.name.isEmpty() ? "UNKNOWN" : connection.name,
        connection.uuid);
  }

  private final static Set<String> CLIENT_NAME_BLACKLIST = new HashSet<>(Arrays.asList("", "CONFIG-TOOL", "CLUSTER-TOOL"));

  private static boolean isInterestingClient(PlatformConnectedClient platformConnectedClient) {
    return !CLIENT_NAME_BLACKLIST.contains(platformConnectedClient.name);
  }

  private final static Set<String> ENTITY_TYPE_BLACKLIST = new HashSet<>(Arrays.asList("org.terracotta.lease.LeaseAcquirer",
    "org.terracotta.dynamic_config.entity.topology.client.DynamicTopologyEntity",
    "com.terracottatech.br.entity.BackupRestoreEntity",
    "com.terracottatech.scale.ctrl.ScalerEntity",
    "com.terracottatech.multi_stripe.scaling_entity.client.ScalingEntity",
    "org.terracotta.catalog.SystemCatalog",
    "com.terracottatech.shutdown.entity.ShutdownEntity",
    "com.terracottatech.licensing.client.LicenseEntity",
    "org.terracotta.nomad.entity.client.NomadEntity"));

  private static boolean isInterestingEntity(PlatformEntity platformEntity) {
    return !ENTITY_TYPE_BLACKLIST.contains(platformEntity.typeName);
  }
}
