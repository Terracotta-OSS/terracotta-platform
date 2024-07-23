/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.diagnostic.client.connection;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;

/**
 * @author Mathieu Carbou
 */
public interface MultiDiagnosticServiceProvider {

  Duration getConnectionTimeout();

  /**
   * Concurrently fetch the diagnostic service of all the nodes.
   * These nodes are expected to be online.
   * If a node is unreachable, the method will fail.
   * <p>
   * The returned {@link DiagnosticServices} will only have online nodes and no offline nodes.
   *
   * @param <K> a node identifier
   * @throws DiagnosticServiceProviderException If one of the node is unreachable,
   *                                            or if all the nodes cannot be reached within a specific duration (timeout)
   */
  default <K> DiagnosticServices<K> fetchOnlineDiagnosticServices(Map<K, InetSocketAddress> expectedOnlineNodes) throws DiagnosticServiceProviderException {
    return fetchOnlineDiagnosticServices(expectedOnlineNodes, getConnectionTimeout());
  }

  /**
   * Concurrently fetch the diagnostic service of all the nodes.
   * These nodes are expected to be online.
   * If a node is unreachable, the method will fail.
   * <p>
   * The returned {@link DiagnosticServices} will only have online nodes and no offline nodes.
   *
   * @param <K> a node identifier
   * @throws DiagnosticServiceProviderException If one of the node is unreachable,
   *                                            or if all the nodes cannot be reached within a specific duration (timeout)
   */
  <K> DiagnosticServices<K> fetchOnlineDiagnosticServices(Map<K, InetSocketAddress> expectedOnlineNodes, Duration timeout) throws DiagnosticServiceProviderException;

  /**
   * Concurrently fetch the diagnostic service of all the provided nodes.
   * The method will not fail and record failures in {@link DiagnosticServices#getFailedEndpoints()}.
   * <p>
   * The returned {@link DiagnosticServices} will have a list of online nodes and a list of failed nodes.
   *
   * @param <K> a node identifier
   */
  default <K> DiagnosticServices<K> fetchDiagnosticServices(Map<K, InetSocketAddress> addresses) {
    return fetchDiagnosticServices(addresses, getConnectionTimeout());
  }

  /**
   * Same as {@link #fetchDiagnosticServices(Map)} but allows to override the connection timeout.
   * If timeout is null, we will use the default short hard-coded timeout of 5 seconds in core.
   */
  <K> DiagnosticServices<K> fetchDiagnosticServices(Map<K, InetSocketAddress> addresses, Duration connectionTimeout);

  /**
   * Concurrently fetch any single online diagnostic service of all the provided nodes.
   * This method expects at least one node to be online.
   * If all nodes have failed, an exception is thrown
   * <p>
   * If successful, the returned {@link DiagnosticServices} will have at least one online node.
   *
   * @param <K> a node identifier
   */
  default <K> DiagnosticServices<K> fetchAnyOnlineDiagnosticService(Map<K, InetSocketAddress> addresses) throws DiagnosticServiceProviderException {
    return fetchAnyOnlineDiagnosticService(addresses, getConnectionTimeout());
  }

  /**
   * Same as above but allows to override the connection timeout.
   * If timeout is null, we will use a specific mode to ask TC client to connect once with its short hard-coded
   * timeout of 5 seconds.
   */
  <K> DiagnosticServices<K> fetchAnyOnlineDiagnosticService(Map<K, InetSocketAddress> addresses, Duration connectionTimeout) throws DiagnosticServiceProviderException;
}
