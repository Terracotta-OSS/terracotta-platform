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

import static org.terracotta.inet.HostAndIpValidator.isValidHost;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv4;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv6;

/**
 * Utility class to convert hostports to their InetSocktAddress and HostPort representations.
 */
public class InetSocketAddressConverter {
  private static final String INVALID_HOST_OR_IP_MESSAGE = "Server must be an RFC 1123 compliant hostname or a valid IP address";

  /**
   * Takes a string of the form {@code host:port}, or {@code host}, checks the parsed host for validity, and returns an
   * {@code HostPort}. Uses a default of {@code 9410} if a port is not found.
   *
   * @param server a {@code String} representing the server address
   * @return a {@code HostPort} from the input server
   */
  public static HostPort getHostPort(String server) {
    return getHostPort(server, 9410);
  }

  /**
   * Takes a string of the form {@code host:port}, or {@code host}, checks the parsed host for validity, and returns an
   * {@code InetSocketAddress}.
   *
   * @param hostPort    a {@code String} representing the server address, followed by its port, separated by :
   * @param defaultPort the default port to be used if a port is not found
   * @return a {@code InetSocketAddress} from the input server
   */
  public static InetSocketAddress getInetSocketAddress(String hostPort, int defaultPort) {
    int lastColon = hostPort.lastIndexOf(":");
    if (lastColon == -1) {
      if (!isValidIPv4(hostPort) && !isValidHost(hostPort)) {
        throw new IllegalArgumentException(INVALID_HOST_OR_IP_MESSAGE);
      }
      return InetSocketAddress.createUnresolved(hostPort, defaultPort);
    } else if (isValidIPv6(hostPort)) {
      return InetSocketAddress.createUnresolved(hostPort, defaultPort);
    } else {
      int port = Integer.parseInt(hostPort.substring(lastColon + 1));
      String hostOrIp = hostPort.substring(0, lastColon);
      if (!isValidIPv4(hostOrIp) && !isValidHost(hostOrIp) && !isValidIPv6(hostOrIp, true)) {
        throw new IllegalArgumentException(INVALID_HOST_OR_IP_MESSAGE);
      }
      return InetSocketAddress.createUnresolved(hostOrIp, port);
    }
  }

  public static InetSocketAddress getInetSocketAddress(String server) {
    return getInetSocketAddress(server, 9410);
  }

  public static HostPort getHostPort(String hostPort, int defaultPort) {
    return HostPort.create(getInetSocketAddress(hostPort, defaultPort));
  }

  /**
   * Takes a string array where each string is of the form {@code host:port}, or {@code host}, checks each parsed host
   * for validity, and returns a list of {@code HostPort}s. Uses a default of {@code 0} if a port is not found.
   *
   * @param servers an array of {@code String}s representing the server addresses
   * @return a {@code List} of {@code HostPort}s from the input servers
   */
  public static List<HostPort> getHostPorts(String[] servers) {
    return getHostPorts(servers, 0);
  }

  /**
   * Takes a string array where each string is of the form {@code host:port}, or {@code host}, checks each parsed host
   * for validity, and returns a list of {@code HostPort}s.
   *
   * @param servers     an array of {@code String}s representing the server addresses
   * @param defaultPort the default port to be used if a port is not found
   * @return a {@code List} of {@code HostPort}s from the input servers
   */
  public static List<HostPort> getHostPorts(String[] servers, int defaultPort) {
    List<HostPort> serversList = new ArrayList<>();
    for (String server : servers) {
      serversList.add(getHostPort(server, defaultPort));
    }
    return serversList;
  }
}
