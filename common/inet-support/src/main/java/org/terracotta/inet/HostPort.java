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
package org.terracotta.inet;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;
import static org.terracotta.inet.HostAndIpValidator.isValidHost;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv4;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv6;

/**
 * Representing the connection endpoint of a running TC server defined with a server host name and a port number.
 * Conversions to/from {@code InetSocketAddress} are provided to satisfy external libraries.
 *
 * @see InetSocketAddress
 */
public class HostPort {
  private static final String INVALID_HOST_OR_IP_MESSAGE = "Server must be an RFC 1123 compliant hostname or a valid IP address";

  private final String host;
  private final int port;

  private HostPort(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  /**
   * @return {@code <host>:<port>} representation
   */
  public String toString() {
    if (isValidIPv6(host, false)) {
      return "[" + host + "]" + ":" + port;
    } else {
      return host + ":" + port;
    }
  }

  /**
   * Creates a new, unresolved {@code InetSocketAddress} instance based on this {@code HostPort}'s
   * host name and port number.
   *
   * @return a new {@code InetSocketAddress} instance
   * @see InetSocketAddress
   */
  public InetSocketAddress createInetSocketAddress() {
    return InetSocketAddress.createUnresolved(host, port);
  }

  /**
   * Creates a new {@code HostPort} instance based on the host string and port number
   * of the supplied {@code InetSocketAddress}.
   *
   * @param address the InetSocketAddress of a TC server
   * @return a new {@code HostPort} instance
   * @see InetSocketAddress
   */
  public static HostPort create(InetSocketAddress address) {
    requireNonNull(address);
    return create(address.getHostString(), address.getPort());
  }

  /**
   * Creates a new {@code HostPort} instance based on the supplied host string and port number
   *
   * @param host host name of TC server
   * @param port port number of TC server
   * @return a new {@code HostPort} instance
   */
  public static HostPort create(String host, int port) {
    requireNonNull(host);
    if (port < 0 || port > 0xFFFF) {
      throw new IllegalArgumentException("port out of range");
    }
    // remove first and/or last character if equal to '[' or ']' respectively
    if (host.startsWith("[")) {
      host = host.substring(1);
    }
    if (host.endsWith("]")) {
      host = host.substring(0, max(0, host.length() - 1));
    }
    if (host.isEmpty()) {
      throw new IllegalArgumentException("host name can not be blank");
    }
    return new HostPort(host, port);
  }

  /**
   * Takes a string array where each string is of the form {@code host:port}, or {@code host}, checks each parsed host
   * for validity, and returns a list of {@code HostPort}s.
   *
   * @param hostPorts   an array of {@code String}s representing the server addresses
   * @param defaultPort the default port to be used if a port is not found
   * @return a {@code List} of {@code HostPort}s from the input servers
   */
  public static List<HostPort> parse(String[] hostPorts, int defaultPort) {
    List<HostPort> serversList = new ArrayList<>();
    for (String hostPort : hostPorts) {
      serversList.add(parse(hostPort, defaultPort));
    }
    return serversList;
  }

  public static List<HostPort> parse(String... hostPorts) {
    List<HostPort> serversList = new ArrayList<>();
    for (String hostPort : hostPorts) {
      serversList.add(parse(hostPort));
    }
    return serversList;
  }

  /**
   * Takes a string of the form {@code host:port}, or {@code host}, checks the parsed host for validity, and returns an
   * {@code HostPort}. Uses a default of {@code 9410} if a port is not found.
   *
   * @param hostPort a {@code String} representing the server address
   * @return a {@code HostPort} from the input server
   */
  public static HostPort parse(String hostPort, int defaultPort) {
    int lastColon = hostPort.lastIndexOf(":");
    if (lastColon == -1) {
      if (!isValidIPv4(hostPort) && !isValidHost(hostPort)) {
        throw new IllegalArgumentException(INVALID_HOST_OR_IP_MESSAGE);
      }
      return HostPort.create(hostPort, defaultPort);
    } else if (isValidIPv6(hostPort)) {
      return HostPort.create(hostPort, defaultPort);
    } else {
      return parse(hostPort);
    }
  }

  public static HostPort parse(String hostPort) {
    int lastColon = hostPort.lastIndexOf(":");
    if (lastColon == -1 || isValidIPv6(hostPort)) {
      throw new IllegalArgumentException("Missing port: " + hostPort);
    }
    String hostOrIp = hostPort.substring(0, lastColon);
    if (!isValidIPv4(hostOrIp) && !isValidHost(hostOrIp) && !isValidIPv6(hostOrIp, true)) {
      throw new IllegalArgumentException(INVALID_HOST_OR_IP_MESSAGE);
    }
    return HostPort.create(hostOrIp, Integer.parseInt(hostPort.substring(lastColon + 1)));
  }

  public boolean isWildcard() {
    return host.equals("0.0.0.0") || host.equals("::");
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof HostPort)) {
      return false;
    }
    HostPort that = (HostPort) o;
    boolean sameHost;
    if (host == null) {
      sameHost = that.host == null;
    } else {
      sameHost = host.equalsIgnoreCase(that.host);
    }
    return sameHost && (port == that.port);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(host.toLowerCase(), port);
  }
}
