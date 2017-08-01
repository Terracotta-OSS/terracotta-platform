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
package org.terracotta.management.model.cluster;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public final class Connection extends AbstractNode<Client> {

  private static final long serialVersionUID = 3;

  public static final String KEY = "connectionId";

  // There is no validation done on the content of this field, except when using #fetchedServerEntityStream and #getFetchedServerEntityCount.
  // So at other places where this map is used, you could see wrong values (i.e. it is possible to add a entity if that is not in the topology)
  private final Map<String, Long> serverEntityIds = new TreeMap<>();
  private final Endpoint clientEndpoint;
  private final String stripeId;
  private final String serverId;
  private final String logicalConnectionUid;

  // physical connection's client id
  private Connection(String id, String logicalConnectionUid, Server server, Endpoint clientEndpoint) {
    super(id);
    this.logicalConnectionUid = Objects.requireNonNull(logicalConnectionUid);
    this.clientEndpoint = Objects.requireNonNull(clientEndpoint);
    this.serverId = server.getId();
    this.stripeId = server.getStripe().getId();
  }

  public String getLogicalConnectionUid() {
    return logicalConnectionUid;
  }

  public String getServerId() {
    return serverId;
  }

  public String getStripeId() {
    return stripeId;
  }

  public Endpoint getClientEndpoint() {
    return clientEndpoint;
  }

  public Client getClient() {
    return getParent();
  }

  public Optional<Server> getServer() {
    try {
      return getClient().getCluster().getStripe(stripeId).flatMap(stripe -> stripe.getServer(serverId));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public Stream<ServerEntity> fetchedServerEntityStream() {
    return getServer()
        .map(server -> serverEntityIds.keySet()
            .stream()
            .map(server::getServerEntity)
            .filter(Optional::isPresent)
            .map(Optional::get))
        .orElse(Stream.empty());
  }

  public int getFetchedServerEntityCount() {
    return getServer()
        .map(server -> serverEntityIds.keySet()
            .stream()
            .map(server::getServerEntity)
            .filter(Optional::isPresent)
            .count())
        .orElse(0L).intValue();
  }

  @Override
  public void remove() {
    Client parent = getParent();
    if (parent != null) {
      parent.removeConnection(getId());
    }
  }

  @Override
  String getContextKey() {
    return KEY;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    Connection that = (Connection) o;

    if (!serverEntityIds.equals(that.serverEntityIds)) return false;
    if (!clientEndpoint.equals(that.clientEndpoint)) return false;
    if (!stripeId.equals(that.stripeId)) return false;
    if (!serverId.equals(that.serverId)) return false;
    return logicalConnectionUid.equals(that.logicalConnectionUid);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + serverEntityIds.hashCode();
    result = 31 * result + clientEndpoint.hashCode();
    result = 31 * result + stripeId.hashCode();
    result = 31 * result + serverId.hashCode();
    result = 31 * result + logicalConnectionUid.hashCode();
    return result;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = super.toMap();
    map.put("logicalConnectionUid", this.logicalConnectionUid);
    map.put("clientEndpoint", clientEndpoint.toMap());
    map.put("stripeId", this.stripeId);
    map.put("serverId", this.serverId);
    map.put("serverEntityIds", this.serverEntityIds);
    return map;
  }

  public boolean unfetchServerEntity(String name, String type) {
    return unfetchServerEntity(ServerEntityIdentifier.create(name, type));
  }

  public boolean unfetchServerEntity(ServerEntityIdentifier serverEntityIdentifier) {
    String id = serverEntityIdentifier.getId();
    Long count = serverEntityIds.get(id);
    if (count == null) {
      return false;
    }
    if (count <= 0) {
      serverEntityIds.remove(id);
      return false;
    }
    if (count <= 1) {
      serverEntityIds.remove(id);
      return true;
    }
    serverEntityIds.put(id, count - 1L);
    return true;
  }

  public boolean fetchServerEntity(String name, String type) {
    return fetchServerEntity(ServerEntityIdentifier.create(name, type));
  }

  public boolean fetchServerEntity(ServerEntityIdentifier serverEntityIdentifier) {
    String id = serverEntityIdentifier.getId();
    Long count = serverEntityIds.get(id);
    serverEntityIds.put(id, count == null || count <= 0 ? 1L : count + 1);
    return true;
  }

  public boolean hasFetchedServerEntity(String name, String type) {
    return hasFetchedServerEntity(ServerEntityIdentifier.create(name, type));
  }

  public boolean hasFetchedServerEntity(ServerEntityIdentifier serverEntityIdentifier) {
    return fetchedServerEntityStream().anyMatch(serverEntity -> serverEntity.is(serverEntityIdentifier));
  }

  public boolean isConnectedTo(Server server) {
    return server.getId().equals(serverId) && server.getStripe().getId().equals(stripeId);
  }

  public boolean isConnectedTo(Endpoint clientEndpoint) {
    return this.clientEndpoint.equals(clientEndpoint);
  }

  public boolean isConnected() {
    return getServer().isPresent();
  }

  public static Connection create(String logicalConnectionUid, Server server, Endpoint clientEndpoint) {
    Objects.requireNonNull(logicalConnectionUid);
    Objects.requireNonNull(server);
    Objects.requireNonNull(clientEndpoint);
    return new Connection(
        key(logicalConnectionUid, server, clientEndpoint),
        logicalConnectionUid,
        server,
        clientEndpoint);
  }

  public static String key(String logicalConnectionUid, Server server, Endpoint clientEndpoint) {
    return logicalConnectionUid + ":" + server.getStripe().getName() + ":" + server.getServerName() + ":" + clientEndpoint.getAddress() + ":" + clientEndpoint.getPort();
  }

}
