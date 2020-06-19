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
package org.terracotta.dynamic_config.entity.topology.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.ConnectionService;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.entity.topology.common.DynamicTopologyEntityConstants;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Connect to a DynamicTopologyEntity on a stripe, given the addresses of the nodes on that stripe
 *
 * @author Mathieu Carbou
 */
public class DynamicTopologyEntityFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTopologyEntityFactory.class);

  private static final String SECURITY_ROOT_DIRECTORY = "security.root.directory";

  public static DynamicTopologyEntity fetch(Collection<InetSocketAddress> addresses,
                                            String connectionName,
                                            Duration connectTimeout,
                                            DynamicTopologyEntity.Settings settings,
                                            String securityRootDirectory) throws ConnectionException {
    Properties properties = buildProperties(connectionName, connectTimeout, securityRootDirectory);
    Connection connection = ConnectionFactory.connect(addresses, properties);
    try {
      final DynamicTopologyEntity entity = fetch(connection, settings);
      return closeable(entity, connection);
    } catch (EntityException e) {
      try {
        connection.close();
      } catch (IOException ignored) {
      }
      throw new ConnectionException(e);
    }
  }

  public static DynamicTopologyEntity fetch(ConnectionService connectionService,
                                            List<InetSocketAddress> addresses,
                                            String connectionName,
                                            Duration connectionTimeout,
                                            DynamicTopologyEntity.Settings settings,
                                            String securityRootDirectory) throws ConnectionException {
    Properties properties = buildProperties(connectionName, connectionTimeout, securityRootDirectory);
    Connection connection = connectionService.connect(addresses, properties);
    try {
      final DynamicTopologyEntity entity = fetch(connection, settings);
      return closeable(entity, connection);
    } catch (EntityException e) {
      try {
        connection.close();
      } catch (IOException ignored) {
      }
      throw new ConnectionException(e);
    }

  }

  private static DynamicTopologyEntity fetch(Connection connection, DynamicTopologyEntity.Settings settings)
      throws EntityNotProvidedException, EntityVersionMismatchException, EntityNotFoundException {
    EntityRef<DynamicTopologyEntity, Object, DynamicTopologyEntity.Settings> ref = connection.getEntityRef(DynamicTopologyEntity.class, 1, DynamicTopologyEntityConstants.ENTITY_NAME);
    return ref.fetchEntity(settings);
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

  private static DynamicTopologyEntity closeable(DynamicTopologyEntity entity, Connection connection) {
    return new DynamicTopologyEntity() {
      public void setListener(Listener listener) {
        entity.setListener(listener);
      }

      public Cluster getUpcomingCluster() throws TimeoutException, InterruptedException {
        return entity.getUpcomingCluster();
      }

      public Cluster getRuntimeCluster() throws TimeoutException, InterruptedException {
        return entity.getRuntimeCluster();
      }

      public boolean mustBeRestarted() throws TimeoutException, InterruptedException {
        return entity.mustBeRestarted();
      }

      public boolean hasIncompleteChange() throws TimeoutException, InterruptedException {
        return entity.hasIncompleteChange();
      }

      public License getLicense() throws TimeoutException, InterruptedException {
        return entity.getLicense();
      }

      @Override
      public Future<Void> releaseEntity() {
        return entity.releaseEntity();
      }

      @Override
      public void close() {
        try {
          connection.close();
        } catch (IOException | RuntimeException e) {
          LOGGER.warn("Error closing entity connection: {}", e.getMessage(), e);
        }
      }
      
      @Override
      public void asyncClose() throws IOException {
        entity.asyncClose();
      }
    };
  }
}
