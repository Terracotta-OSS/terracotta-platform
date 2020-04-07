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
 * Utility class to convert hostports to their InetSocketAddress representations.
 */
public class InetSocketAddressConvertor {
  private static final String INVALID_HOST_OR_IP_MESSAGE = "Server must be an RFC 1123 compliant hostname or a valid IP address";

  /**
   * Takes a string of the form {@code host:port}, or {@code host}, checks the parsed host for validity, and returns an
   * {@code InetSocketAddress}. Uses a default of {@code 9410} if a port is not found.
   *
   * @param server a {@code String} representing the server address
   * @return an {@code InetSocketAddress} from the input server
   */
  public static InetSocketAddress getInetSocketAddress(String server) {
    return getInetSocketAddress(server, 9410);
  }

  /**
   * Takes a string of the form {@code host:port}, or {@code host}, checks the parsed host for validity, and returns an
   * {@code InetSocketAddress}.
   *
   * @param hostPort    a {@code String} representing the server address, followed by its port, separated by :
   * @param defaultPort the default port to be used if a port is not found
   * @return an {@code InetSocketAddress} from the input server
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

  /**
   * Takes a string array where each string is of the form {@code host:port}, or {@code host}, checks each parsed host
   * for validity, and returns a list of {@code InetSocketAddress}es. Uses a default of {@code 0} if a port is not found.
   *
   * @param servers an array of {@code String}s representing the server addresses
   * @return a {@code List} of {@code InetSocketAddress}es from the input servers
   */
  public static List<InetSocketAddress> getInetSocketAddresses(String[] servers) {
    return getInetSocketAddresses(servers, 0);
  }

  /**
   * Takes a string array where each string is of the form {@code host:port}, or {@code host}, checks each parsed host
   * for validity, and returns a list of {@code InetSocketAddress}es.
   *
   * @param servers     an array of {@code String}s representing the server addresses
   * @param defaultPort the default port to be used if a port is not found
   * @return a {@code List} of {@code InetSocketAddress}es from the input servers
   */
  public static List<InetSocketAddress> getInetSocketAddresses(String[] servers, int defaultPort) {
    List<InetSocketAddress> serversList = new ArrayList<>();
    for (String server : servers) {
      serversList.add(getInetSocketAddress(server, defaultPort));
    }
    return serversList;
  }

  public static String toHostPort(InetSocketAddress address) {
    if (isValidIPv6(address.getHostName(), false)) {
      return "[" + address.getHostName() + "]" + ":" + address.getPort();
    } else {
      return address.getHostName() + ":" + address.getPort();
    }
  }
}
