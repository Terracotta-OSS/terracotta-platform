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
package org.terracotta.management.model.cluster;



import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public final class Connection extends AbstractNode<Client> implements Serializable {

  private static final long serialVersionUID = 1;

  public static final String KEY = "connectionId";

  private final Collection<String> manageableIds = new ConcurrentSkipListSet<>();
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

  public Stream<Manageable> serverManageableStream() {
    return getServer()
        .map(server -> manageableIds.stream()
            .map(server::getManageable)
            .filter(Optional::isPresent)
            .map(Optional::get))
        .orElse(Stream.empty());
  }

  public int getServerManageableCount() {
    return getServer()
        .map(server -> manageableIds.stream()
            .map(server::getManageable)
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
    // we do not consider super because it would include the connection id in the hashcode, which is not "predictatable"
    // and can be different whether we opened/closed several connections in our different tests
    //if (!super.equals(o)) return false;

    Connection that = (Connection) o;

    if (!manageableIds.equals(that.manageableIds)) return false;
    if (!clientEndpoint.equals(that.clientEndpoint)) return false;
    if (stripeId != null ? !stripeId.equals(that.stripeId) : that.stripeId != null) return false;
    return serverId != null ? serverId.equals(that.serverId) : that.serverId == null;

  }

  @Override
  public int hashCode() {
    // we do not consider super because it would include the connection id in the hashcode, which is not "predictatable"
    // and can be different whether we opened/closed several connections in our different tests
    //int result = super.hashCode();
    int result = 0;
    result = 31 * result + manageableIds.hashCode();
    result = 31 * result + clientEndpoint.hashCode();
    result = 31 * result + (stripeId != null ? stripeId.hashCode() : 0);
    result = 31 * result + (serverId != null ? serverId.hashCode() : 0);
    return result;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = super.toMap();
    map.put("logicalConnectionUid", this.logicalConnectionUid);
    map.put("clientEndpoint", clientEndpoint.toMap());
    map.put("stripeId", this.stripeId);
    map.put("serverId", this.serverId);
    map.put("manageableIds", this.manageableIds);
    return map;
  }

  public void disconnectServerManageable(String name, String type) {
    manageableIds.remove(Manageable.key(name, type));
  }

  public boolean connectServerManageable(Manageable manageable) {
    if (!isConnected()) {
      throw new IllegalStateException("not connnected");
    }
    if (!(manageable.getParent() instanceof Server)) {
      throw new IllegalArgumentException(String.valueOf(manageable.getParent()));
    }
    Server server = (Server) manageable.getParent();
    if (server != getServer().get()) {
      throw new IllegalStateException("wrong server manageable");
    }
    return manageableIds.add(manageable.getId());
  }

  public boolean isConnectedToServerManageable(String name, String type) {
    return serverManageableStream().filter(manageable -> manageable.is(name, type)).findFirst().isPresent();
  }

  public boolean isConnectedToActiveServer() {
    return getServer().filter(Server::isActive).isPresent();
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
