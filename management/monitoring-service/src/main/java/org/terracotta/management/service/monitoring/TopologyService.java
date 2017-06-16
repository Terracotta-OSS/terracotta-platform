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
package org.terracotta.management.service.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PlatformConfiguration;
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
    this.cluster = Cluster.create().addStripe(stripe = Stripe.create("SINGLE"));
  }

  // ================================================
  // PLATFORM CALLBACKS: only called on active server
  // ================================================

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public synchronized void serverDidBecomeActive(PlatformServer self) {
    LOGGER.trace("[0] serverDidBecomeActive({})", self.getServerName());

    Server server = Server.create(self.getServerName())
        .setBindAddress(self.getBindAddress())
        .setBindPort(self.getBindPort())
        .setBuildId(self.getBuild())
        .setGroupPort(self.getGroupPort())
        .setHostName(self.getHostName())
        .setStartTime(self.getStartTime())
        .setHostAddress(self.getHostAddress())
        .setVersion(self.getVersion())
        .computeUpTime();

    stripe.addServer(server);

    currentActive = stripe.getServerByName(self.getServerName()).get();

    topologyEventListeners.forEach(TopologyEventListener::onBecomeActive);

    firingService.fireNotification(new ContextualNotification(server.getContext(), SERVER_JOINED.name()));

    // we assume server.getStartTime() == activate time, but this will be fixed after into serverStateChanged() call by platform
    serverStateChanged(self, new ServerState("ACTIVE", server.getStartTime(), server.getStartTime()));
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
        .computeUpTime();

    stripe.addServer(server);

    firingService.fireNotification(new ContextualNotification(server.getContext(), SERVER_JOINED.name()));
  }

  @Override
  public synchronized void serverDidLeaveStripe(PlatformServer platformServer) {
    LOGGER.trace("[0] serverDidLeaveStripe({})", platformServer.getServerName());

    Server server = stripe.getServerByName(platformServer.getServerName())
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing server: " + platformServer.getServerName()));

    Context context = server.getContext();
    server.remove();

    serverEntities.remove(platformServer.getServerName());

    firingService.fireNotification(new ContextualNotification(context, SERVER_LEFT.name()));
  }

  @Override
  public synchronized void serverEntityCreated(PlatformServer sender, PlatformEntity platformEntity) {
    LOGGER.trace("[0] serverEntityCreated({}, {})", sender.getServerName(), platformEntity);

    if (platformEntity.isActive && !sender.getServerName().equals(getActiveServer().getServerName())) {
      throw newIllegalTopologyState("Server " + sender.getServerName() + " is not current active server but it created an active entity " + platformEntity);
    }

    if (!platformEntity.isActive && sender.getServerName().equals(getActiveServer().getServerName())) {
      throw newIllegalTopologyState("Server " + sender.getServerName() + " is the current active server but it created a passive entity " + platformEntity);
    }

    Server server = stripe.getServerByName(sender.getServerName())
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing server: " + sender.getServerName()));
    ServerEntityIdentifier identifier = ServerEntityIdentifier.create(platformEntity.name, platformEntity.typeName);
    ServerEntity entity = ServerEntity.create(identifier)
        .setConsumerId(platformEntity.consumerID);

    server.addServerEntity(entity);

    firingService.fireNotification(new ContextualNotification(entity.getContext(), SERVER_ENTITY_CREATED.name()));

    whenServerEntity(platformEntity.consumerID, sender.getServerName()).complete(entity);

    if (sender.getServerName().equals(platformConfiguration.getServerName())) {
      topologyEventListeners.forEach(listener -> listener.onEntityCreated(platformEntity.consumerID));
    }
  }

  @Override
  public void serverEntityReconfigured(PlatformServer sender, PlatformEntity platformEntity) {
    LOGGER.trace("[0] serverEntityReconfigured({}, {})", sender.getServerName(), platformEntity);

    Server server = stripe.getServerByName(sender.getServerName())
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing server: " + sender.getServerName()));
    ServerEntityIdentifier identifier = ServerEntityIdentifier.create(platformEntity.name, platformEntity.typeName);

    ServerEntity entity = server.getServerEntity(identifier)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing server entity " + identifier + " on server " + sender.getServerName()));

    firingService.fireNotification(new ContextualNotification(entity.getContext(), SERVER_ENTITY_RECONFIGURED.name()));
  }

  @Override
  public synchronized void serverEntityDestroyed(PlatformServer sender, PlatformEntity platformEntity) {
    LOGGER.trace("[0] serverEntityDestroyed({}, {})", sender.getServerName(), platformEntity);

    if (platformEntity.isActive && !sender.getServerName().equals(getActiveServer().getServerName())) {
      throw newIllegalTopologyState("Server " + sender.getServerName() + " is not current active server but it destroyed an active entity " + platformEntity);
    }

    if (!platformEntity.isActive && sender.getServerName().equals(getActiveServer().getServerName())) {
      throw newIllegalTopologyState("Server " + sender.getServerName() + " is the current active server but it destroyed a passive entity " + platformEntity);
    }

    Server server = stripe.getServerByName(sender.getServerName())
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing server: " + sender.getServerName()));

    ServerEntity entity = server.getServerEntity(platformEntity.name, platformEntity.typeName)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing entity: " + platformEntity + " on server " + sender.getServerName()));

    Context context = entity.getContext();
    entity.remove();

    serverEntities.get(sender.getServerName()).remove(platformEntity.consumerID);

    if (isCurrentServerActive() && sender.getServerName().equals(currentActive.getServerName())) {
      entityFetches.remove(platformEntity.consumerID);
    }

    if (sender.getServerName().equals(platformConfiguration.getServerName())) {
      topologyEventListeners.forEach(listener -> listener.onEntityDestroyed(platformEntity.consumerID));
    }

    firingService.fireNotification(new ContextualNotification(context, SERVER_ENTITY_DESTROYED.name()));
  }

  @Override
  public synchronized void clientConnected(PlatformServer currentActive, PlatformConnectedClient platformConnectedClient) {
    LOGGER.trace("[0] clientConnected({})", platformConnectedClient);

    Server server = stripe.getServerByName(currentActive.getServerName())
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing active server: " + currentActive.getServerName()));
    
    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
    Endpoint endpoint = Endpoint.create(platformConnectedClient.remoteAddress.getHostAddress(), platformConnectedClient.remotePort);

    Client client = Client.create(clientIdentifier)
        .setHostName(platformConnectedClient.remoteAddress.getHostName());
    cluster.addClient(client);

    client.addConnection(Connection.create(clientIdentifier.getConnectionUid(), getActiveServer(), endpoint));

    firingService.fireNotification(new ContextualNotification(server.getContext(), CLIENT_CONNECTED.name(), client.getContext()));
  }

  @Override
  public synchronized void clientDisconnected(PlatformServer currentActive, PlatformConnectedClient platformConnectedClient) {
    LOGGER.trace("[0] clientDisconnected({})", platformConnectedClient);

    Server server = stripe.getServerByName(currentActive.getServerName())
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing active server: " + currentActive.getServerName()));
    
    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
    Client client = cluster.getClient(clientIdentifier)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing client: " + clientIdentifier));
    Context clientContext = client.getContext();

    client.remove();

    firingService.fireNotification(new ContextualNotification(server.getContext(), CLIENT_DISCONNECTED.name(), clientContext));
  }

  @Override
  public synchronized void clientFetch(PlatformConnectedClient platformConnectedClient, PlatformEntity platformEntity, ClientDescriptor clientDescriptor) {
    LOGGER.trace("[0] clientFetch({}, {})", platformConnectedClient, platformEntity);

    Server currentActive = getActiveServer();
    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
    Endpoint endpoint = Endpoint.create(platformConnectedClient.remoteAddress.getHostAddress(), platformConnectedClient.remotePort);

    Client client = cluster.getClient(clientIdentifier)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing client: " + clientIdentifier));

    Connection connection = client.getConnection(currentActive, endpoint)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing connection between server " + currentActive + " and client " + clientIdentifier));
    ServerEntity entity = currentActive.getServerEntity(platformEntity.name, platformEntity.typeName)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing entity: name=" + platformEntity.name + ", type=" + platformEntity.typeName));

    connection.fetchServerEntity(platformEntity.name, platformEntity.typeName);

    firingService.fireNotification(new ContextualNotification(entity.getContext(), SERVER_ENTITY_FETCHED.name(), client.getContext()));

    whenFetchClient(platformEntity.consumerID, clientDescriptor).complete(client);

    topologyEventListeners.forEach(listener -> listener.onFetch(platformEntity.consumerID, clientDescriptor));
  }

  @Override
  public synchronized void clientUnfetch(PlatformConnectedClient platformConnectedClient, PlatformEntity platformEntity, ClientDescriptor clientDescriptor) {
    LOGGER.trace("[0] clientUnfetch({}, {})", platformConnectedClient, platformEntity);

    Server currentActive = getActiveServer();
    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
    Endpoint endpoint = Endpoint.create(platformConnectedClient.remoteAddress.getHostAddress(), platformConnectedClient.remotePort);

    ServerEntity entity = currentActive.getServerEntity(platformEntity.name, platformEntity.typeName)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing entity: name=" + platformEntity.name + ", type=" + platformEntity.typeName));

    Client client = cluster.getClient(clientIdentifier)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing client: " + clientIdentifier));
    Connection connection = client.getConnection(currentActive, endpoint)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing connection: " + endpoint + " to server " + currentActive.getServerName() + " from client " + clientIdentifier));

    entityFetches.get(platformEntity.consumerID).remove(clientDescriptor);

    if (connection.unfetchServerEntity(platformEntity.name, platformEntity.typeName)) {
      firingService.fireNotification(new ContextualNotification(entity.getContext(), SERVER_ENTITY_UNFETCHED.name(), client.getContext()));
    }

    topologyEventListeners.forEach(listener -> listener.onUnfetch(platformEntity.consumerID, clientDescriptor));
  }

  @Override
  public synchronized void serverStateChanged(PlatformServer sender, ServerState serverState) {
    LOGGER.trace("[0] serverStateChanged({}, {})", sender.getServerName(), serverState.getState());

    Server server = stripe.getServerByName(sender.getServerName())
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing server: " + sender.getServerName()));

    Server.State oldState = server.getState();

    if (oldState == Server.State.ACTIVE && currentActive != null && currentActive.getServerName().equals(server.getServerName())) {
      // in case of a failover, the server state changed is replayed. So the server is active but will become passive and will become active again
      // we filter this out
      return;
    }

    server.setState(Server.State.parse(serverState.getState()));
    server.setActivateTime(serverState.getActivate());

    if (oldState != server.getState()) {
      // avoid sending another event to report the same state as before, to avoid duplicates

      Map<String, String> attrs = new HashMap<>();
      attrs.put("state", serverState.getState());
      attrs.put("activateTime", serverState.getActivate() > 0 ? String.valueOf(serverState.getActivate()) : "0");

      firingService.fireNotification(new ContextualNotification(server.getContext(), SERVER_STATE_CHANGED.name(), attrs));
    }
  }

  // ======================================================================
  // ENTITIES and CLIENTS topology completion by adding management metadata
  // ======================================================================

  /**
   * Records stats that needs to be sent in future (or now) when the client fetch info will have arrived
   */
  void willPushClientStatistics(long consumerId, ClientDescriptor from, ContextualStatistics... statistics) {
    whenFetchClient(consumerId, from).execute(client -> {
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
    whenFetchClient(consumerId, from).execute(client -> {
      Context context = client.getContext();
      notification.setContext(notification.getContext().with(context));
      LOGGER.trace("[{}] willPushClientNotification({}, {})", consumerId, from, notification);
      firingService.fireNotification(notification);
    });
  }

  /**
   * Records registry that needs to be sent in future (or now) when the client will have arrived
   */
  CompletableFuture<Context> willSetClientManagementRegistry(long consumerId, ClientDescriptor clientDescriptor, ManagementRegistry newRegistry) {
    CompletableFuture<Context> futureContext = new CompletableFuture<>();
    whenFetchClient(consumerId, clientDescriptor).execute(client -> {
      boolean hadRegistry = client.getManagementRegistry().isPresent();
      LOGGER.trace("[{}] willSetClientManagementRegistry({}, {})", consumerId, clientDescriptor, newRegistry);
      client.setManagementRegistry(newRegistry);
      if (!hadRegistry) {
        firingService.fireNotification(new ContextualNotification(client.getContext(), Notification.CLIENT_REGISTRY_AVAILABLE.name()));
      }
      futureContext.complete(client.getContext());
    });
    return futureContext;
  }

  /**
   * Records tags that needs to be sent in future (or now) when the client info will have arrived
   */
  void willSetClientTags(long consumerId, ClientDescriptor clientDescriptor, String[] tags) {
    whenFetchClient(consumerId, clientDescriptor).execute(client -> {
      Set<String> currtags = new HashSet<>(client.getTags());
      Set<String> newTags = new HashSet<>(Arrays.asList(tags));
      if (!currtags.equals(newTags)) {
        LOGGER.trace("[{}] willSetClientTags({}, {})", consumerId, clientDescriptor, Arrays.toString(tags));
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
    // will be the consumer id of the NMS entity. Thus we have to retrieve the server entity thanks to the
    // context that is hold in the stat results
    Stream.of(statistics)
        .collect(Collectors.groupingBy(o -> Long.parseLong(o.getContext().getOrDefault(ServerEntity.CONSUMER_ID, String.valueOf(consumerId)))))
        .forEach((cid, cid_stats) -> whenServerEntity(cid, serverName).execute(serverEntity -> {
          Context context = serverEntity.getContext();
          for (ContextualStatistics statistic : cid_stats) {
            statistic.setContext(statistic.getContext().with(context));
          }
          LOGGER.trace("[{}] willPushEntityStatistics({}, {})", cid, serverName, cid_stats.size());
          firingService.fireStatistics(cid_stats.toArray(new ContextualStatistics[cid_stats.size()]));
        }));
  }

  /**
   * Records notification that needs to be sent in future (or now) when the entity will have arrived
   */
  void willPushEntityNotification(long consumerId, String serverName, ContextualNotification notification) {
    // notifications contains a context, but if this context contains an origin consumer id, do not override it
    long cid = Long.parseLong(notification.getContext().getOrDefault(ServerEntity.CONSUMER_ID, String.valueOf(consumerId)));
    whenServerEntity(cid, serverName).execute(serverEntity -> {
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
    whenServerEntity(consumerId, serverName).execute(serverEntity -> {
      boolean hadRegistry = serverEntity.getManagementRegistry().isPresent();
      LOGGER.trace("[{}] willSetEntityManagementRegistry({}, {})", consumerId, serverName, newRegistry.getCapabilities());
      serverEntity.setManagementRegistry(newRegistry);
      if (!hadRegistry) {
        firingService.fireNotification(new ContextualNotification(serverEntity.getContext(), Notification.ENTITY_REGISTRY_AVAILABLE.name()));
      }
    });
  }

  // ==============
  // QUERY TOPOLOGY
  // ==============

  CompletableFuture<ClientIdentifier> getClientIdentifier(long consumerId, ClientDescriptor clientDescriptor) {
    CompletableFuture<ClientIdentifier> clientIdentifier = new CompletableFuture<>();
    whenFetchClient(consumerId, clientDescriptor).execute(client -> clientIdentifier.complete(client.getClientIdentifier()));
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

  private boolean isCurrentServerActive() {
    return currentActive != null && currentActive.getServerName().equals(platformConfiguration.getServerName());
  }

  private Server getActiveServer() {
    if (currentActive == null) {
      throw newIllegalTopologyState("No active server defined!");
    }
    return currentActive;
  }

  private ExecutionChain<Client> whenFetchClient(long consumerId, ClientDescriptor clientDescriptor) {
    ConcurrentMap<ClientDescriptor, ExecutionChain<Client>> fetches = entityFetches.computeIfAbsent(consumerId, cid -> new ConcurrentHashMap<>());
    return fetches.computeIfAbsent(clientDescriptor, key -> new ExecutionChain<>());
  }

  private ExecutionChain<ServerEntity> whenServerEntity(long consumerId, String serverName) {
    ConcurrentMap<Long, ExecutionChain<ServerEntity>> entities = serverEntities.computeIfAbsent(serverName, name -> new ConcurrentHashMap<>());
    return entities.computeIfAbsent(consumerId, key -> new ExecutionChain<>());
  }

  private IllegalStateException newIllegalTopologyState(String message) {
    return new IllegalStateException("Illegal monitoring topology state: " + message + "\n- currentActive: " + currentActive + "\n- cluster:" + cluster);
  }

  private static ClientIdentifier toClientIdentifier(PlatformConnectedClient connection) {
    return ClientIdentifier.create(
        connection.clientPID,
        connection.remoteAddress.getHostAddress(),
        connection.name == null || connection.name.isEmpty() ? "UNKNOWN" : connection.name,
        connection.uuid);
  }

}
