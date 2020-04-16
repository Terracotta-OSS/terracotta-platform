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
package org.terracotta.diagnostic.client.connection;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public interface MultiDiagnosticServiceProvider {
  /**
   * Concurrently fetch the diagnostic service of all the nodes.
   * These nodes are expected to be online.
   * If a node is unreachable, the method will fail.
   * <p>
   * The returned {@link DiagnosticServices} will only have online nodes and no offline nodes.
   *
   * @throws DiagnosticServiceProviderException If one of the node is unreachable,
   *                                            or if all the nodes cannot be reached within a specific duration (timeout)
   */
  default DiagnosticServices fetchOnlineDiagnosticServices(Collection<InetSocketAddress> expectedOnlineNodes) throws DiagnosticServiceProviderException {
    DiagnosticServices diagnosticServices = fetchDiagnosticServices(expectedOnlineNodes);
    Collection<InetSocketAddress> offlineEndpoints = diagnosticServices.getOfflineEndpoints();
    if (!offlineEndpoints.isEmpty()) {
      DiagnosticServiceProviderException exception = new DiagnosticServiceProviderException("Diagnostic connection to: " + offlineEndpoints + " failed");
      // add all errors
      offlineEndpoints.stream()
          .map(address -> diagnosticServices.getError(address).orElse(null))
          .filter(Objects::nonNull)
          .forEach(exception::addSuppressed);
      try {
        diagnosticServices.close();
      } catch (RuntimeException e) {
        exception.addSuppressed(e);
      }
      throw exception;
    }
    return diagnosticServices;
  }

  /**
   * Concurrently fetch the diagnostic service of all the provides nodes.
   * The method will not fail, except if some connection timeout is reached.
   * If some nodes are offline, they will be reported in {@link DiagnosticServices#getOfflineEndpoints()}.
   * <p>
   * The returned {@link DiagnosticServices} will have a list of online nodes and a list of offline nodes.
   */
  DiagnosticServices fetchDiagnosticServices(Collection<InetSocketAddress> addresses);
}
