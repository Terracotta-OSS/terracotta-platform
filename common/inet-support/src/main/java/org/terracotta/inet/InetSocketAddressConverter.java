/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

/**
 * Utility class to convert hostports to their InetSocktAddress and HostPort representations.
 */
public class InetSocketAddressConverter {


  /**
   * Takes a string of the form {@code host:port}, or {@code host}, checks the parsed host for validity, and returns an
   * {@code InetSocketAddress}.
   *
   * @param hostPort    a {@code String} representing the server address, followed by its port, separated by :
   * @param defaultPort the default port to be used if a port is not found
   * @return a {@code InetSocketAddress} from the input server
   */
  public static InetSocketAddress parseInetSocketAddress(String hostPort, int defaultPort) {
    return HostPort.parse(hostPort, defaultPort).createInetSocketAddress();
  }

  public static InetSocketAddress parseInetSocketAddress(String hostPort) {
    return HostPort.parse(hostPort).createInetSocketAddress();
  }

}
