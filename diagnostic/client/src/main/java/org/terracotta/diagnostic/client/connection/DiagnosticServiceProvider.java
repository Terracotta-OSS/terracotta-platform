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

import org.terracotta.connection.ConnectionException;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.json.ObjectMapperFactory;

import java.net.InetSocketAddress;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticServiceProvider {

  private final String connectionName;
  private final Duration diagnosticInvokeTimeout;
  private final Duration connectTimeout;
  private final String securityRootDirectory;
  private final ObjectMapperFactory objectMapperFactory;

  public DiagnosticServiceProvider(String connectionName, Duration connectTimeout, Duration diagnosticInvokeTimeout, String securityRootDirectory, ObjectMapperFactory objectMapperFactory) {
    this.connectionName = requireNonNull(connectionName);
    this.diagnosticInvokeTimeout = requireNonNull(diagnosticInvokeTimeout);
    this.connectTimeout = requireNonNull(connectTimeout);
    this.securityRootDirectory = securityRootDirectory;
    this.objectMapperFactory = objectMapperFactory;
  }

  public DiagnosticService fetchDiagnosticService(InetSocketAddress address) throws DiagnosticServiceProviderException {
    return fetchDiagnosticService(address, connectTimeout);
  }

  public DiagnosticService fetchDiagnosticService(InetSocketAddress address, Duration connectTimeout) throws DiagnosticServiceProviderException {
    try {
      return DiagnosticServiceFactory.fetch(address, connectionName, connectTimeout, diagnosticInvokeTimeout, securityRootDirectory, objectMapperFactory);
    } catch (ConnectionException e) {
      throw new DiagnosticServiceProviderException(e);
    }
  }

  public DiagnosticService fetchUnSecuredDiagnosticService(InetSocketAddress address) throws DiagnosticServiceProviderException {
    return fetchUnSecuredDiagnosticService(address, connectTimeout);
  }

  public DiagnosticService fetchUnSecuredDiagnosticService(InetSocketAddress address, Duration connectTimeout) throws DiagnosticServiceProviderException {
    try {
      return DiagnosticServiceFactory.fetch(address, connectionName, connectTimeout, diagnosticInvokeTimeout, null, objectMapperFactory);
    } catch (ConnectionException e) {
      throw new DiagnosticServiceProviderException(e);
    }
  }

  public DiagnosticService fetchDiagnosticServiceWithFallback(InetSocketAddress address) throws DiagnosticServiceProviderException {
    return fetchDiagnosticServiceWithFallback(address, connectTimeout);
  }

  public DiagnosticService fetchDiagnosticServiceWithFallback(InetSocketAddress address, Duration connectTimeout) throws DiagnosticServiceProviderException {
    try {
      // try the default approach, could be secured or unsecured.
      return fetchDiagnosticService(address, connectTimeout);
    } catch (DiagnosticServiceProviderException ex) {
      // try using unsecured approach only if secured approach is tried before.
      if (isSecurityEnabled()) {
        return fetchUnSecuredDiagnosticService(address, connectTimeout);
      } else {
        // throw the original exception without creating new one.
        throw ex;
      }
    }
  }

  private boolean isSecurityEnabled() {
    return securityRootDirectory != null && !securityRootDirectory.isEmpty();
  }
}
