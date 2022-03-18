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
package org.terracotta.diagnostic.client;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.ConnectionService;
import org.terracotta.connection.Diagnostics;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.diagnostic.common.DiagnosticCodec;
import org.terracotta.diagnostic.common.JsonDiagnosticCodec;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.json.ObjectMapperFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticServiceFactory {

  private static final String SECURITY_ROOT_DIRECTORY = "security.root.directory";

  public static DiagnosticService fetch(InetSocketAddress nodeAddress,
                                        String connectionName,
                                        Duration connectTimeout,
                                        Duration diagnosticInvokeTimeout,
                                        String securityRootDirectory,
                                        ObjectMapperFactory objectMapperFactory) throws ConnectionException {
    Properties properties = buildProperties(connectionName, connectTimeout, securityRootDirectory);
    Connection connection = ConnectionFactory.connect(Collections.singletonList(nodeAddress), properties);
    try {
      return fetch(connection, diagnosticInvokeTimeout, objectMapperFactory);
    } catch (EntityException e) {
      try {
        connection.close();
      } catch (IOException ignored) {
      }
      // we decide to consider any entity exception as connection exception because the diagnostic entity should always be there and
      // the caller anyway has no way to properly react to these catch exception. Only ConnectionException is relevant.
      throw new ConnectionException(e);
    }
  }

  public static DiagnosticService fetch(ConnectionService connectionService,
                                        InetSocketAddress nodeAddress,
                                        String connectionName,
                                        Duration connectionTimeout,
                                        Duration diagnosticInvokeTimeout,
                                        String securityRootDirectory,
                                        ObjectMapperFactory objectMapperFactory) throws ConnectionException {
    Properties properties = buildProperties(connectionName, connectionTimeout, securityRootDirectory);
    Connection connection = connectionService.connect(Collections.singletonList(nodeAddress), properties);
    try {
      return fetch(connection, diagnosticInvokeTimeout, objectMapperFactory);
    } catch (EntityException e) {
      try {
        connection.close();
      } catch (IOException ignored) {
      }
      // we decide to consider any entity exception as connection exception because the diagnostic entity should always be there and
      // the caller anyway has no way to properly react to these catch exception. Only ConnectionException is relevant.
      throw new ConnectionException(e);
    }

  }

  public static DiagnosticService getDiagnosticService(Connection connection, Diagnostics delegate, ObjectMapperFactory objectMapperFactory) {
    // We could default to the JavaDiagnosticCodec also, or a runnel codec if we want to get rid of Json and only do serialization.
    // The codec needs to be the same on client-side and server-side of course.
    return getDiagnosticService(connection, delegate, new JsonDiagnosticCodec(objectMapperFactory));
  }

  public static DiagnosticService getDiagnosticService(Connection connection, Diagnostics delegate, DiagnosticCodec<?> codec) {
    return new DiagnosticServiceImpl(connection, delegate, codec);
  }

  private static DiagnosticService fetch(Connection connection, Duration diagnosticInvokeTimeout, ObjectMapperFactory objectMapperFactory)
      throws EntityNotProvidedException, EntityVersionMismatchException, EntityNotFoundException {
    EntityRef<Diagnostics, Object, Properties> ref = connection.getEntityRef(Diagnostics.class, 1, "root");
    Properties properties = new Properties();
    properties.setProperty("request.timeout", String.valueOf(diagnosticInvokeTimeout.toMillis()));
    Diagnostics delegate = ref.fetchEntity(properties);
    return getDiagnosticService(connection, delegate, objectMapperFactory);
  }

  private static Properties buildProperties(String connectionName, Duration connectionTimeout, String securityRootDirectory) {
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, String.valueOf(connectionTimeout.toMillis()));
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, connectionName);
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TYPE, "diagnostic");
    if (securityRootDirectory != null) {
      properties.setProperty(SECURITY_ROOT_DIRECTORY, securityRootDirectory);
    }
    return properties;
  }

}
