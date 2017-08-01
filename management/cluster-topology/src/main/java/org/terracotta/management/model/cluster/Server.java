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

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public final class Server extends AbstractNode<Stripe> {

  private static final long serialVersionUID = 2;

  public static final String KEY = "serverId";
  public static final String NAME_KEY = "serverName";

  private final Map<String, ServerEntity> serverEntities = new TreeMap<>();
  private final String serverName; // matches xml config

  private String hostName; // matches xml config
  private String hostAddress; // matches xml config
  private String bindAddress; // matches xml config
  private int bindPort; // matches xml config
  private int groupPort; // matches xml config
  private State state = State.UNKNOWN; // taken at runtime from servers
  private String version; // taken at runtime from servers
  private String buildId; // taken at runtime from servers
  private long startTime; // taken at runtime from servers
  private long upTimeSec; // taken at runtime from servers
  private long activateTime; // taken at runtime from servers

  private Server(String serverId, String serverName) {
    super(serverId);
    this.serverName = Objects.requireNonNull(serverName);
  }

  public boolean isActive() {
    return state == State.ACTIVE;
  }

  public String getServerName() {
    return serverName;
  }

  public Stripe getStripe() {
    return getParent();
  }

  @Override
  String getContextKey() {
    return KEY;
  }

  @Override
  public Context getContext() {
    return super.getContext().with(NAME_KEY, getServerName());
  }

  public String getBuildId() {
    return buildId;
  }

  public Server setBuildId(String buildId) {
    this.buildId = buildId;
    return this;
  }

  public State getState() {
    return state;
  }

  public String getBindAddress() {
    return bindAddress;
  }

  public int getBindPort() {
    return bindPort;
  }

  public String getHostName() {
    return hostName;
  }

  public Server setBindAddress(String bindAddress) {
    this.bindAddress = bindAddress;
    return this;
  }

  public Server setBindPort(int bindPort) {
    this.bindPort = bindPort;
    return this;
  }

  public int getGroupPort() {
    return groupPort;
  }

  public Server setGroupPort(int groupPort) {
    this.groupPort = groupPort;
    return this;
  }

  public Server setHostName(String hostName) {
    this.hostName = hostName;
    return this;
  }

  public String getHostAddress() {
    return hostAddress;
  }

  public Server setHostAddress(String hostAddress) {
    this.hostAddress = hostAddress;
    return this;
  }

  public long getStartTime() {
    return startTime;
  }

  public Server setStartTime(long startTime) {
    this.startTime = startTime;
    return this;
  }

  public long getUpTimeSec() {
    return upTimeSec;
  }

  public Server setUpTimeSec(long upTimeSec) {
    this.upTimeSec = upTimeSec;
    return this;
  }

  public Server computeUpTime() {
    return computeUpTime(Clock.systemUTC());
  }

  public Server computeUpTime(Clock clock) {
    if (startTime > 0) {
      upTimeSec = (clock.millis() - startTime) / 1000;
    } else {
      upTimeSec = 0;
    }
    return this;
  }

  public Server setState(State state) {
    this.state = Objects.requireNonNull(state);
    return this;
  }

  public String getVersion() {
    return version;
  }

  public Server setVersion(String version) {
    this.version = version;
    return this;
  }

  public long getActivateTime() {
    return activateTime;
  }

  public Server setActivateTime(long activateTime) {
    this.activateTime = activateTime > 0 ? activateTime : 0;
    return this;
  }

  public final Map<String, ServerEntity> getServerEntities() {
    return serverEntities;
  }

  public final int getServerEntityCount() {
    return serverEntities.size();
  }

  public final Stream<ServerEntity> serverEntityStream() {
    return serverEntities.values().stream();
  }

  public final Server addServerEntity(ServerEntity serverEntity) {
    // ServerEntityId are unique per their ID but also per their combination of (type + name)
    for (ServerEntity m : serverEntities.values()) {
      if (m.is(serverEntity.getType(), serverEntity.getName())) {
        throw new IllegalArgumentException("Duplicate serverEntity: type=" + serverEntity.getType() + ", name=" + serverEntity.getName());
      }
    }
    if (serverEntities.putIfAbsent(serverEntity.getId(), serverEntity) != null) {
      throw new IllegalArgumentException("Duplicate serverEntity: " + serverEntity.getId());
    }
    serverEntity.setParent(this);
    return this;
  }

  public final Optional<ServerEntity> getServerEntity(Context context) {
    String id = context.get(ServerEntity.KEY);
    if (id != null) {
      return getServerEntity(id);
    }
    String type = context.get(ServerEntity.TYPE_KEY);
    String name = context.get(ServerEntity.NAME_KEY);
    if (type != null && name != null) {
      return getServerEntity(name, type);
    }
    String consumerId = context.get(ServerEntity.CONSUMER_ID);
    if (consumerId != null) {
      long cid = Long.parseLong(consumerId);
      return serverEntityStream().filter(serverEntity -> serverEntity.getConsumerId() == cid).findFirst();
    }
    return Optional.empty();
  }

  public final Optional<ServerEntity> getServerEntity(ServerEntityIdentifier serverEntityIdentifier) {
    return getServerEntity(serverEntityIdentifier.getId());
  }

  public final Optional<ServerEntity> getServerEntity(long consumerId) {
    return serverEntityStream().filter(serverEntity -> serverEntity.getConsumerId() == consumerId).findFirst();
  }

  public final Optional<ServerEntity> getServerEntity(String id) {
    return id == null ? Optional.empty() : Optional.ofNullable(serverEntities.get(id));
  }

  public final Optional<ServerEntity> getServerEntity(String name, String type) {
    return serverEntityStream().filter(serverEntity -> serverEntity.is(name, type)).findFirst();
  }

  public final boolean hasServerEntity(String name, String type) {
    return getServerEntity(name, type).isPresent();
  }

  public final Optional<ServerEntity> removeServerEntity(String id) {
    Optional<ServerEntity> serverEntity = getServerEntity(id);
    serverEntity.ifPresent(m -> {
      if (serverEntities.remove(id, m)) {
        m.detach();
      }
    });
    return serverEntity;
  }

  public final Stream<ServerEntity> serverEntityStream(String type) {
    return serverEntityStream().filter(serverEntity -> serverEntity.isType(type));
  }

  @Override
  public void remove() {
    Stripe parent = getParent();
    if (parent != null) {
      parent.removeServer(getId());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    Server server = (Server) o;

    if (bindPort != server.bindPort) return false;
    if (groupPort != server.groupPort) return false;
    if (startTime != server.startTime) return false;
    if (activateTime != server.activateTime) return false;
    if (!serverEntities.equals(server.serverEntities)) return false;
    if (!serverName.equals(server.serverName)) return false;
    if (hostName != null ? !hostName.equals(server.hostName) : server.hostName != null) return false;
    if (hostAddress != null ? !hostAddress.equals(server.hostAddress) : server.hostAddress != null) return false;
    if (bindAddress != null ? !bindAddress.equals(server.bindAddress) : server.bindAddress != null) return false;
    if (state != server.state) return false;
    if (version != null ? !version.equals(server.version) : server.version != null) return false;
    return buildId != null ? buildId.equals(server.buildId) : server.buildId == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + serverEntities.hashCode();
    result = 31 * result + serverName.hashCode();
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (hostAddress != null ? hostAddress.hashCode() : 0);
    result = 31 * result + (bindAddress != null ? bindAddress.hashCode() : 0);
    result = 31 * result + bindPort;
    result = 31 * result + groupPort;
    result = 31 * result + (state != null ? state.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (buildId != null ? buildId.hashCode() : 0);
    result = 31 * result + (int) (startTime ^ (startTime >>> 32));
    result = 31 * result + (int) (activateTime ^ (activateTime >>> 32));
    return result;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = super.toMap();
    map.put("serverEntities", serverEntityStream().sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).map(ServerEntity::toMap).collect(Collectors.toList()));
    map.put("serverName", this.getServerName());
    map.put("hostName", this.hostName);
    map.put("hostAddress", this.hostAddress);
    map.put("bindAddress", this.bindAddress);
    map.put("bindPort", this.bindPort);
    map.put("groupPort", this.groupPort);
    map.put("state", this.state.name());
    map.put("version", this.version);
    map.put("buildId", this.buildId);
    map.put("startTime", this.startTime);
    map.put("upTimeSec", this.upTimeSec);
    map.put("activateTime", this.activateTime);
    return map;
  }

  public static Server create(String serverName) {
    return new Server(serverName, serverName);
  }

  /**
   * Possibles transitions:
   * <pre>
   * STARTING ➡ UNINITIALIZED ➡ SYNCHRONIZING ➡ PASSIVE ➡ ACTIVE
   *    ⬇                                         ⬆        ⬆
   *     ----------------------------------------------------
   * </pre>
   */
  public enum State {

    /**
     * When a server is not reachable, this will be the status used
     */
    UNREACHABLE,

    /**
     * Server is bootstrapping
     */
    STARTING("STARTING", "START_STATE", "START-STATE"),

    /**
     * A fresh server
     */
    UNINITIALIZED("UNINITIALIZED", "PASSIVE-UNINITIALIZED"),

    /**
     * Passive server is synchronizing with active server
     */
    SYNCHRONIZING("SYNCHRONIZING", "PASSIVE-SYNCING"),

    /**
     * Passive server is up and ready to replicate
     */
    PASSIVE("PASSIVE", "PASSIVE-STANDBY"),

    /**
     * Active server is ready to receive clients
     */
    ACTIVE("ACTIVE", "ACTIVE-COORDINATOR"),

    /**
     * Status returned when parsing failed
     */
    UNKNOWN;

    private final String[] mappings;

    State(String... mappings) {
      this.mappings = mappings == null ? new String[0] : mappings;
    }

    public static State parse(String value) {
      if (value == null) {
        return UNKNOWN;
      }
      for (State serverState : State.values()) {
        for (String mapping : serverState.mappings) {
          if (mapping.equalsIgnoreCase(value)) {
            return serverState;
          }
        }
      }
      return UNKNOWN;
    }

  }
}
