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
public class DefaultDiagnosticServiceProvider implements DiagnosticServiceProvider {

  private final String connectionName;
  private final Duration diagnosticInvokeTimeout;
  private final Duration connectTimeout;
  private final String securityRootDirectory;
  private final ObjectMapperFactory objectMapperFactory;

  public DefaultDiagnosticServiceProvider(String connectionName, Duration connectTimeout, Duration diagnosticInvokeTimeout, String securityRootDirectory, ObjectMapperFactory objectMapperFactory) {
    this.connectionName = requireNonNull(connectionName);
    this.diagnosticInvokeTimeout = diagnosticInvokeTimeout;
    this.connectTimeout = connectTimeout;
    this.securityRootDirectory = securityRootDirectory;
    this.objectMapperFactory = objectMapperFactory;
  }

  @Override
  public DiagnosticService fetchDiagnosticService(InetSocketAddress address) throws DiagnosticServiceProviderException {
    return fetchDiagnosticService(address, connectTimeout);
  }

  @Override
  public DiagnosticService fetchDiagnosticService(InetSocketAddress address, Duration connectTimeout) throws DiagnosticServiceProviderException {
    try {
      return DiagnosticServiceFactory.fetch(address, connectionName, connectTimeout, diagnosticInvokeTimeout, securityRootDirectory, objectMapperFactory);
    } catch (ConnectionException e) {
      throw new DiagnosticServiceProviderException(e);
    }
  }
}
