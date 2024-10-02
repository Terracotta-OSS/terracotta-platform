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
package org.terracotta.nomad.entity.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.ConnectionService;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.nomad.entity.common.NomadEntityConstants;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.server.NomadException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Connect to a Nomad entity on a stripe, given the addresses of the nodes on that stripe
 *
 * @author Mathieu Carbou
 */
public class NomadEntityFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadEntityFactory.class);

  private static final String SECURITY_ROOT_DIRECTORY = "security.root.directory";

  public static <T> NomadEntity<T> fetch(Collection<InetSocketAddress> addresses,
                                         String connectionName,
                                         Duration connectTimeout,
                                         NomadEntity.Settings settings,
                                         String securityRootDirectory) throws ConnectionException {
    Properties properties = buildProperties(connectionName, connectTimeout, securityRootDirectory);
    Connection connection = ConnectionFactory.connect(addresses, properties);
    try {
      final NomadEntity<T> entity = fetch(connection, settings);
      return closeable(entity, connection);
    } catch (EntityException e) {
      try {
        connection.close();
      } catch (IOException ignored) {
      }
      throw new ConnectionException(e);
    }
  }

  public static <T> NomadEntity<T> fetch(ConnectionService connectionService,
                                         List<InetSocketAddress> addresses,
                                         String connectionName,
                                         Duration connectionTimeout,
                                         NomadEntity.Settings settings,
                                         String securityRootDirectory) throws ConnectionException {
    Properties properties = buildProperties(connectionName, connectionTimeout, securityRootDirectory);
    Connection connection = connectionService.connect(addresses, properties);
    try {
      final NomadEntity<T> entity = fetch(connection, settings);
      return closeable(entity, connection);
    } catch (EntityException e) {
      try {
        connection.close();
      } catch (IOException ignored) {
      }
      throw new ConnectionException(e);
    }

  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static <T> NomadEntity<T> fetch(Connection connection, NomadEntity.Settings settings)
      throws EntityNotProvidedException, EntityVersionMismatchException, EntityNotFoundException {
    EntityRef<NomadEntity, Object, NomadEntity.Settings> ref = connection.getEntityRef(NomadEntity.class, 1, NomadEntityConstants.ENTITY_NAME);
    return (NomadEntity<T>) ref.fetchEntity(settings);
  }

  private static Properties buildProperties(String connectionName, Duration connectionTimeout, String securityRootDirectory) {
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, String.valueOf(connectionTimeout.toMillis()));
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, connectionName);
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TYPE, "terracotta");
    if (securityRootDirectory != null) {
      properties.setProperty(SECURITY_ROOT_DIRECTORY, securityRootDirectory);
    }
    return properties;
  }

  private static <T> NomadEntity<T> closeable(NomadEntity<T> entity, Connection connection) {
    return new NomadEntity<T>() {
      @Override
      public AcceptRejectResponse send(MutativeMessage message) throws NomadException {
        return entity.send(message);
      }

      @Override
      public void close() {
        try {
          connection.close();
        } catch (IOException | RuntimeException e) {
          LOGGER.warn("Error closing entity connection: {}", e.getMessage(), e);
        }
      }
    };
  }
}
