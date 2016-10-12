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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Connection;
import org.terracotta.management.model.cluster.Endpoint;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.cluster.ServerEntityIdentifier;
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.sequence.Sequence;
import org.terracotta.management.sequence.SequenceGenerator;
import org.terracotta.management.service.monitoring.platform.IStripeMonitoring;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.terracotta.management.service.monitoring.DefaultStripeMonitoring.Notification.CLIENT_CONNECTED;
import static org.terracotta.management.service.monitoring.DefaultStripeMonitoring.Notification.CLIENT_DISCONNECTED;
import static org.terracotta.management.service.monitoring.DefaultStripeMonitoring.Notification.SERVER_ENTITY_CREATED;
import static org.terracotta.management.service.monitoring.DefaultStripeMonitoring.Notification.SERVER_ENTITY_DESTROYED;
import static org.terracotta.management.service.monitoring.DefaultStripeMonitoring.Notification.SERVER_ENTITY_FETCHED;
import static org.terracotta.management.service.monitoring.DefaultStripeMonitoring.Notification.SERVER_ENTITY_UNFETCHED;
import static org.terracotta.management.service.monitoring.DefaultStripeMonitoring.Notification.SERVER_JOINED;
import static org.terracotta.management.service.monitoring.DefaultStripeMonitoring.Notification.SERVER_LEFT;
import static org.terracotta.management.service.monitoring.DefaultStripeMonitoring.Notification.SERVER_STATE_CHANGED;

/**
 * @author Mathieu Carbou
 */
class DefaultStripeMonitoring implements IStripeMonitoring {

  private static final Logger LOGGER = Logger.getLogger(DefaultStripeMonitoring.class.getName());

  private final SequenceGenerator sequenceGenerator;
  private final Cluster cluster;
  private final Stripe stripe;

  private final Map<String, Map<Long, ServerEntityIdentifier>> entities = new HashMap<>();
  private final Map<Long, DefaultMonitoringService> monitoringServices = new HashMap<>();

  private volatile Server currentActive;

  DefaultStripeMonitoring(SequenceGenerator sequenceGenerator) {
    this.sequenceGenerator = sequenceGenerator;
    this.cluster = Cluster.create().addStripe(stripe = Stripe.create("SINGLE"));
  }

  @Override
  public synchronized void serverDidBecomeActive(PlatformServer self) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "serverDidBecomeActive(" + self + ")");
    }
    serverDidJoinStripe(self);

    currentActive = stripe.getServerByName(self.getServerName()).get();
    currentActive.setState(Server.State.ACTIVE);
    currentActive.setActivateTime(System.currentTimeMillis());
  }

  @Override
  public synchronized void serverDidJoinStripe(PlatformServer platformServer) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "serverDidJoinStripe(" + platformServer + "):\n" + cluster);
    }

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

    entities.putIfAbsent(server.getServerName(), new HashMap<>());

    fireNotification(SERVER_JOINED, server.getContext());
  }

  @Override
  public synchronized void serverDidLeaveStripe(PlatformServer platformServer) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "serverDidLeaveStripe(" + platformServer + "):\n" + cluster);
    }

    stripe.getServerByName(platformServer.getServerName())
        .ifPresent(server -> {

          Context context = server.getContext();
          server.remove();

          entities.remove(server.getServerName());

          fireNotification(SERVER_LEFT, context);
        });
  }

  @Override
  public synchronized void serverEntityCreated(PlatformServer sender, PlatformEntity platformEntity) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "serverEntityCreated(" + sender + ", " + platformEntity + "):\n" + cluster);
    }

    if (platformEntity.isActive && !sender.getServerName().equals(currentActive.getServerName())) {
      throw new IllegalStateException("Server " + sender + " is not current active server but it created an active entity " + platformEntity);
    }

    if (!platformEntity.isActive && sender.getServerName().equals(currentActive.getServerName())) {
      throw new IllegalStateException("Server " + sender + " is the current active server but it created a passive entity " + platformEntity);
    }

    // do not use .ifPresent() because we want to fail if the server is not there!
    Server server = stripe.getServerByName(sender.getServerName())
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing server: " + sender.getServerName()));
    ServerEntity entity = ServerEntity.create(platformEntity.name, platformEntity.typeName);

    server.addServerEntity(entity);

    entities.get(server.getServerName()).put(platformEntity.consumerID, entity.getServerEntityIdentifier());

    fireNotification(SERVER_ENTITY_CREATED, entity.getContext());
  }

  @Override
  public synchronized void serverEntityDestroyed(PlatformServer sender, PlatformEntity platformEntity) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "serverEntityDestroyed(" + sender + ", " + platformEntity + "):\n" + cluster);
    }

    if (platformEntity.isActive && !sender.getServerName().equals(currentActive.getServerName())) {
      throw new IllegalStateException("Server " + sender + " is not current active server but it destroyed an active entity " + platformEntity);
    }

    if (!platformEntity.isActive && sender.getServerName().equals(currentActive.getServerName())) {
      throw new IllegalStateException("Server " + sender + " is the current active server but it destroyed a passive entity " + platformEntity);
    }

    stripe.getServerByName(sender.getServerName())
        .ifPresent(server -> {
          server.getServerEntity(platformEntity.name, platformEntity.typeName)
              .ifPresent(entity -> {

                closeMonitoringService(platformEntity.consumerID);

                Context context = entity.getContext();
                entity.remove();

                entities.get(server.getServerName()).remove(platformEntity.consumerID);

                fireNotification(SERVER_ENTITY_DESTROYED, context);
              });
        });
  }

  @Override
  public synchronized void clientConnected(PlatformConnectedClient platformConnectedClient) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "clientConnected(" + platformConnectedClient + "):\n" + cluster);
    }

    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
    Endpoint endpoint = Endpoint.create(platformConnectedClient.remoteAddress.getHostAddress(), platformConnectedClient.remotePort);
    // do not use .ifPresent() because we want to fail if the server is not there!

    Client client = Client.create(clientIdentifier)
        .setHostName(platformConnectedClient.remoteAddress.getHostName());
    cluster.addClient(client);

    client.addConnection(Connection.create(clientIdentifier.getConnectionUid(), currentActive, endpoint));

    fireNotification(CLIENT_CONNECTED, client.getContext());
  }

  @Override
  public synchronized void clientDisconnected(PlatformConnectedClient platformConnectedClient) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "clientDisconnected(" + platformConnectedClient + "):\n" + cluster);
    }

    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
    cluster.getClient(clientIdentifier)
        .ifPresent(client -> {

          Context context = client.getContext();
          client.remove();

          fireNotification(CLIENT_DISCONNECTED, context);
        });
  }

  @Override
  public synchronized void clientFetch(PlatformConnectedClient platformConnectedClient, PlatformEntity platformEntity, ClientDescriptor clientDescriptor) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "clientFetch(" + platformConnectedClient + ", " + platformEntity + "):\n" + cluster);
    }

    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
    Endpoint endpoint = Endpoint.create(platformConnectedClient.remoteAddress.getHostAddress(), platformConnectedClient.remotePort);

    // do not use .ifPresent() because we want to fail if the server is not there!
    Client client = cluster.getClient(clientIdentifier)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing client: " + clientIdentifier));
    Connection connection = client.getConnection(currentActive, endpoint)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing connection between server " + currentActive + " and client " + clientIdentifier));
    ServerEntity entity = currentActive.getServerEntity(platformEntity.name, platformEntity.typeName)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing entity: name=" + platformEntity.name + ", type=" + platformEntity.typeName));

    if (!connection.fetchServerEntity(platformEntity.name, platformEntity.typeName)) {
      throw new IllegalStateException();
    }

    DefaultMonitoringService monitoringService = monitoringServices.get(platformEntity.consumerID);
    if (monitoringService != null) {
      monitoringService.addFetch(clientDescriptor, clientIdentifier);
    }

    fireNotification(SERVER_ENTITY_FETCHED, entity.getContext(), client.getContext());
  }

  @Override
  public synchronized void clientUnfetch(PlatformConnectedClient platformConnectedClient, PlatformEntity platformEntity, ClientDescriptor clientDescriptor) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "clientUnfetch(" + platformConnectedClient + ", " + platformEntity + "):\n" + cluster);
    }

    ClientIdentifier clientIdentifier = toClientIdentifier(platformConnectedClient);
    Endpoint endpoint = Endpoint.create(platformConnectedClient.remoteAddress.getHostAddress(), platformConnectedClient.remotePort);

    // do not use .ifPresent() because we want to fail if the server is not there!
    ServerEntity entity = currentActive.getServerEntity(platformEntity.name, platformEntity.typeName)
        .<IllegalStateException>orElseThrow(() -> newIllegalTopologyState("Missing entity: name=" + platformEntity.name + ", type=" + platformEntity.typeName));

    cluster.getClient(clientIdentifier)
        .ifPresent(client -> client.getConnection(currentActive, endpoint)
            .ifPresent(connection -> {

              DefaultMonitoringService monitoringService = monitoringServices.get(platformEntity.consumerID);
              if (monitoringService != null) {
                monitoringService.removeFetch(clientDescriptor, clientIdentifier);
              }

              if (connection.unfetchServerEntity(platformEntity.name, platformEntity.typeName)) {
                fireNotification(SERVER_ENTITY_UNFETCHED, entity.getContext(), client.getContext());
              }

            }));
  }

  @Override
  public synchronized void serverStateChanged(PlatformServer sender, ServerState serverState) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "serverStateChanged(" + sender + ", " + serverState + "):\n" + cluster);
    }

    stripe.getServerByName(sender.getServerName())
        .ifPresent(server -> {

          server.setState(Server.State.parse(serverState.getState()));
          server.setActivateTime(serverState.getActivate());

          Map<String, String> attrs = new HashMap<>();
          attrs.put("state", serverState.getState());
          attrs.put("activateTime", serverState.getActivate() > 0 ? String.valueOf(serverState.getActivate()) : "0");

          fireNotification(SERVER_STATE_CHANGED, server.getContext(), attrs);
        });
  }

  synchronized MonitoringService getOrCreateMonitoringService(long consumerID, MonitoringServiceConfiguration config) {

    return monitoringServices.computeIfAbsent(consumerID, id -> new DefaultMonitoringService(
        this,
        consumerID,
        config.getClientCommunicator().orElse(null)));
  }

  synchronized void clear() {
    while (!monitoringServices.isEmpty()) {
      closeMonitoringService(monitoringServices.keySet().iterator().next());
    }
  }

  synchronized <V> V applyCluster(Function<Cluster, V> fn) {
    // cannot do a simple getCluster() method because Cluster object might be mutated, and it must be mutated within a synchronized method
    return fn.apply(cluster);
  }

  synchronized void consumeCluster(Consumer<Cluster> consumer) {
    // cannot do a simple getCluster() method because Cluster object might be mutated, and it must be mutated within a synchronized method
    consumer.accept(cluster);
  }

  ServerEntityIdentifier getServerEntityIdentifier(long consumerId) {
    Objects.requireNonNull(currentActive);
    ServerEntityIdentifier serverEntityIdentifier = entities.get(currentActive.getServerName()).get(consumerId);
    if (serverEntityIdentifier == null) {
      throw newIllegalTopologyState("Missing consumer ID: " + consumerId + " on server " + currentActive.getServerName());
    }
    return serverEntityIdentifier;
  }

  synchronized IllegalStateException newIllegalTopologyState(String message) {
    return new IllegalStateException("Illegal monitoring topology state: " + message + "\n"
        + "- currentActive: " + currentActive
        + "\n- cluster:\n"
        + cluster
        + "\n- monitoring services:"
        + monitoringServices.values().stream().map(DefaultMonitoringService::toString).reduce((s, s2) -> s + "\n   * " + s2).orElse(" 0"));
  }

  void fireNotification(ContextualNotification notification) {
    DefaultMessage message = new DefaultMessage(sequenceGenerator.next(), "NOTIFICATION", notification);
    monitoringServices.values().forEach(defaultMonitoringService -> defaultMonitoringService.push(message));
  }

  void fireStatistics(ContextualStatistics[] statistics) {
    DefaultMessage message = new DefaultMessage(sequenceGenerator.next(), "STATISTICS", statistics);
    monitoringServices.values().forEach(defaultMonitoringService -> defaultMonitoringService.push(message));
  }

  Sequence nextSequence() {
    return sequenceGenerator.next();
  }

  private void closeMonitoringService(long consumerId) {
    DefaultMonitoringService monitoringService = monitoringServices.remove(consumerId);
    monitoringService.close();
  }

  private void fireNotification(Notification notification, Context context) {
    fireNotification(notification, context, Collections.emptyMap());
  }

  private void fireNotification(Notification notification, Context context, Map<String, String> attrs) {
    fireNotification(new ContextualNotification(context, notification.name(), attrs));
  }

  private static ClientIdentifier toClientIdentifier(PlatformConnectedClient connection) {
    return ClientIdentifier.create(
        connection.clientPID,
        connection.remoteAddress.getHostAddress(),
        connection.name == null || connection.name.isEmpty() ? "UNKNOWN" : connection.name,
        connection.uuid);
  }

  enum Notification {
    SERVER_ENTITY_CREATED,
    SERVER_ENTITY_DESTROYED,

    SERVER_ENTITY_FETCHED,
    SERVER_ENTITY_UNFETCHED,

    CLIENT_CONNECTED,
    CLIENT_DISCONNECTED,

    SERVER_JOINED,
    SERVER_LEFT,
    SERVER_STATE_CHANGED,
  }

}
