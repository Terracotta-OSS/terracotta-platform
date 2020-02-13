/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
   *
   * @throws DiagnosticServiceProviderException If one of the node is unreachable,
   *                                            or if all the nodes cannot be reached within a specific duration (timeout)
   */
  DiagnosticServices fetchDiagnosticServices(Collection<InetSocketAddress> addresses);
}
