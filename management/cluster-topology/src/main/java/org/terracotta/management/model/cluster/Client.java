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

import org.terracotta.management.model.context.Context;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public final class Client extends AbstractManageableNode<Cluster> {

  private static final long serialVersionUID = 2;

  public static final String KEY = "clientId";

  // physical connections to stripes (active server)
  private final Map<String, Connection> connections = new TreeMap<>();

  private final ClientIdentifier clientIdentifier;
  private final SortedSet<String> tags = new TreeSet<>();
  private String hostName;

  private Client(ClientIdentifier clientIdentifier) {
    super(clientIdentifier.getClientId());
    this.clientIdentifier = Objects.requireNonNull(clientIdentifier);
  }

  public Collection<String> getTags() {
    return tags;
  }

  public Client addTag(String tag) {
    return addTags(tag);
  }

  public Client addTags(String... tags) {
    Collections.addAll(this.tags, tags);
    return this;
  }

  public Client setTags(String[] tags) {
    this.tags.clear();
    Collections.addAll(this.tags, tags);
    return this;
  }

  public boolean isTagged(String tag) {
    return tags.contains(tag);
  }

  public ClientIdentifier getClientIdentifier() {
    return clientIdentifier;
  }

  public String getHostAddress() {
    return clientIdentifier.getHostAddress();
  }

  public String getHostName() {
    return hostName;
  }

  public Client setHostName(String hostName) {
    this.hostName = hostName;
    return this;
  }

  public Cluster getCluster() {
    return getParent();
  }

  public long getPid() {
    return clientIdentifier.getPid();
  }

  public String getClientId() {
    return clientIdentifier.getClientId();
  }

  public String getVmId() {
    return clientIdentifier.getVmId();
  }

  public String getLogicalConnectionUid() {return clientIdentifier.getConnectionUid();}

  public String getName() {return clientIdentifier.getName();}

  public Map<String, Connection> getConnections() {
    return connections;
  }

  public int getConnectionCount() {
    return connections.size();
  }

  public Client addConnection(Connection connection) {
    for (Connection c : connections.values()) {
      if (c.getClientEndpoint().equals(connection.getClientEndpoint())) {
        throw new IllegalArgumentException("Duplicate connection: " + connection.getId() + ": same endpoint: " + c.getClientEndpoint());
      }
      if (!getLogicalConnectionUid().equals(connection.getLogicalConnectionUid())) {
        throw new IllegalArgumentException("Connection " + connection + " does not belong to client " + this);
      }
    }
    if (connections.putIfAbsent(connection.getId(), connection) != null) {
      throw new IllegalArgumentException("Duplicate connection: " + connection.getId());
    } else {
      connection.setParent(this);
    }
    return this;
  }

  public Optional<Connection> getConnection(Context context) {
    return getConnection(context.get(Connection.KEY));
  }

  public Optional<Connection> getConnection(String id) {
    return id == null ? Optional.empty() : Optional.ofNullable(connections.get(id));
  }

  public Stream<Connection> connectionStream() {
    return connections.values().stream();
  }

  public Stream<Connection> connectionStream(Server server) {
    return connectionStream().filter(c -> c.isConnectedTo(server));
  }

  public Optional<Connection> getConnection(Server server, Endpoint endpoint) {
    return connectionStream()
        .filter(c -> c.isConnectedTo(server) && c.isConnectedTo(endpoint))
        .findFirst();
  }

  public Optional<Connection> removeConnection(String id) {
    Optional<Connection> connection = getConnection(id);
    connection.ifPresent(c -> {
      if (connections.remove(id, c)) {
        c.detach();
      }
    });
    return connection;
  }

  public Stream<ServerEntity> fetchedServerEntityStream() {
    return connectionStream()
        .flatMap(Connection::fetchedServerEntityStream);
  }

  public int getFetchedServerEntityCount() {
    return connectionStream()
        .mapToInt(Connection::getFetchedServerEntityCount).sum();
  }

  @Override
  public void remove() {
    Cluster parent = getParent();
    if (parent != null) {
      parent.removeClient(getId());
    }
  }

  public boolean isConnectedTo(Server server) {
    return connectionStream(server).findFirst().isPresent();
  }

  public boolean isConnected() {
    return connectionStream().anyMatch(Connection::isConnected);
  }

  public boolean hasFetchedServerEntity(String name, String type) {
    return getFetchedServerEntity(name, type).isPresent();
  }

  public Optional<ServerEntity> getFetchedServerEntity(String name, String type) {
    return fetchedServerEntityStream().filter(serverEntity -> serverEntity.is(name, type)).findFirst();
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

    Client client = (Client) o;

    if (!connections.equals(client.connections)) return false;
    if (!clientIdentifier.equals(client.clientIdentifier)) return false;
    if (!tags.equals(client.tags)) return false;
    return hostName != null ? hostName.equals(client.hostName) : client.hostName == null;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + connections.hashCode();
    result = 31 * result + clientIdentifier.hashCode();
    result = 31 * result + tags.hashCode();
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    return result;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = super.toMap();
    map.put("pid", getPid());
    map.put("hostAddress", getHostAddress());
    map.put("name", getName());
    map.put("logicalConnectionUid", getLogicalConnectionUid());
    map.put("vmId", getVmId());
    map.put("clientId", getClientId());
    map.put("hostName", getHostName());
    map.put("tags", tags);
    map.put("connections", connectionStream().sorted(Comparator.comparing(AbstractNode::getId)).map(Connection::toMap).collect(Collectors.toList()));
    map.put("managementRegistry", getManagementRegistry().map(ManagementRegistry::toMap).orElse(null));
    return map;
  }

  public static Client create(String clientIdentifier) {
    return create(ClientIdentifier.valueOf(clientIdentifier));
  }

  public static Client create(ClientIdentifier clientIdentifier) {
    return new Client(clientIdentifier);
  }

}
