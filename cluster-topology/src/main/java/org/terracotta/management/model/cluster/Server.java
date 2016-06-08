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

import org.terracotta.management.model.context.Context;

import java.io.Serializable;
import java.time.Clock;
import java.util.Map;

/**
 * @author Mathieu Carbou
 */
public final class Server extends AbstractNodeWithManageable<Stripe, Server> implements Serializable {

  private static final long serialVersionUID = 1;

  public static final String KEY = "serverId";
  public static final String NAME_KEY = "serverName";

  private final String serverName; // matches xml config
  private String hostName; // matches xml config
  private String hostAddress; // matches xml config
  private String bindAddress; // matches xml config
  private int bindPort; // matches xml config
  private int groupPort; // matches xml config
  private ServerState state = ServerState.UNKNOWN; // taken at runtime from servers
  private String version; // taken at runtime from servers
  private String buildId; // taken at runtime from servers
  private long startTime; // taken at runtime from servers
  private long upTimeSec; // taken at runtime from servers
  private long activateTime; // taken at runtime from servers

  private Server(String serverId, String serverName) {
    super(serverId);
    this.serverName = serverName;
  }

  public boolean isActive() {
    return state == ServerState.ACTIVE;
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

  public ServerState getState() {
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

  public Server setState(ServerState state) {
    this.state = state;
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
    if (activateTime != server.activateTime) return false;
    if (startTime != server.startTime) return false;
    if (hostName != null ? !hostName.equals(server.hostName) : server.hostName != null) return false;
    if (hostAddress != null ? !hostAddress.equals(server.hostAddress) : server.hostAddress != null) return false;
    if (bindAddress != null ? !bindAddress.equals(server.bindAddress) : server.bindAddress != null) return false;
    if (version != null ? !version.equals(server.version) : server.version != null) return false;
    return buildId != null ? buildId.equals(server.buildId) : server.buildId == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (hostAddress != null ? hostAddress.hashCode() : 0);
    result = 31 * result + (bindAddress != null ? bindAddress.hashCode() : 0);
    result = 31 * result + bindPort;
    result = 31 * result + groupPort;
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (buildId != null ? buildId.hashCode() : 0);
    result = 31 * result + (int) (activateTime ^ (activateTime >>> 32));
    result = 31 * result + (int) (startTime ^ (startTime >>> 32));
    return result;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = super.toMap();
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

}
