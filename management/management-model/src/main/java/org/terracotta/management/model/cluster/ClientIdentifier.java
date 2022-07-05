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

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Client identifier: {@code <PID>@<ADDRESS>:<NAME>:<UUID>}. Where:
 * <ul>
 * <li>PID is the JVM PID</li>
 * <li>ADDRESS is the JVM host address used to connect to the cluster</li>
 * <li>NAME is a user-defined string passed when opening a connection to a cluster (connection.name property) such as EHCACHE:my-cache-manager</li>
 * <li>UUID is the logical connection ID set by the platform, used to regroup several physical connections when in a cluster with several stripes</li>
 * </ul>
 *
 * @author Mathieu Carbou
 */
public final class ClientIdentifier implements Serializable {

  private static final long serialVersionUID = 1;

  private static final Logger LOGGER = Logger.getLogger(ClientIdentifier.class.getName());

  private final long pid;
  private final String name;
  private final String connectionUid;
  private final String hostAddress;

  private ClientIdentifier(long pid, String hostAddress, String name, String connectionUid) {
    this.hostAddress = Objects.requireNonNull(hostAddress);
    this.pid = pid;
    this.connectionUid = Objects.requireNonNull(connectionUid);
    this.name = Objects.requireNonNull(name);
    if (hostAddress.isEmpty()) {
      throw new IllegalArgumentException("Empty host address");
    }
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Empty name");
    }
  }

  public String getConnectionUid() {
    return connectionUid;
  }

  public String getHostAddress() {
    return hostAddress;
  }

  public long getPid() {
    return pid;
  }

  public String getName() {
    return name;
  }

  public String getVmId() {
    String ha = hostAddress;

    if (hostAddress.contains(":")) {
      ha = "[" + hostAddress + "]";
    }

    return pid + "@" + ha;
  }

  public String getClientId() {
    return getVmId() + ":" + name + ":" + connectionUid;
  }

  public String getAppId() {
    return getVmId() + ":" + name;
  }

  @Override
  public String toString() {
    return getClientId();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClientIdentifier that = (ClientIdentifier) o;
    return pid == that.pid
        && name.equals(that.name)
        && connectionUid.equals(that.connectionUid)
        && hostAddress.equals(that.hostAddress);
  }

  @Override
  public int hashCode() {
    int result = (int) (pid ^ (pid >>> 32));
    result = 31 * result + name.hashCode();
    result = 31 * result + connectionUid.hashCode();
    result = 31 * result + hostAddress.hashCode();
    return result;
  }

  public static ClientIdentifier create(long pid, String hostAddress, String name, String uuid) {
    return new ClientIdentifier(pid, hostAddress, name, uuid);
  }

  public static ClientIdentifier create(String hostname, String name, String logicalConnectionUid) {
    return new ClientIdentifier(discoverPID(), hostname, name, logicalConnectionUid);
  }

  public static ClientIdentifier valueOf(String identifier) {
    try {
      int ampIdx = identifier.indexOf('@');
      int firstColon;
      String hostAddress;
      if (identifier.charAt(ampIdx + 1) == '[') {
        int end = identifier.indexOf("]");
        hostAddress = identifier.substring(ampIdx + 2, end);
        firstColon = identifier.indexOf(':', end + 1);
      } else {
        firstColon = identifier.indexOf(':', ampIdx + 1);
        hostAddress = identifier.substring(ampIdx + 1, firstColon);
      }
      int lastColon = identifier.lastIndexOf(':');
      return new ClientIdentifier(
          Long.parseLong(identifier.substring(0, ampIdx)),
          hostAddress,
          identifier.substring(firstColon + 1, lastColon),
          identifier.substring(lastColon + 1));
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(identifier);
    }
  }

  static long discoverPID() {
    String name = ManagementFactory.getRuntimeMXBean().getName();
    long pid = 0;
    for (int i = 0; i < name.length() && Character.isDigit(name.charAt(i)); i++) {
      pid = pid * 10 + Character.getNumericValue(name.charAt(i));
    }
    return pid;
  }
}
