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
package org.terracotta.lease.connection;

import com.tc.util.TCServiceLoader;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionPropertyNames;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Properties;

/**
 * Factory to get LeasedConnections
 */

public class LeasedConnectionFactory {
  private static final String DEFAULT_CONNECTION_TYPE = "terracotta";

  /**
   * Establish a connection based on the given uri. The method will attempt to look for the first suitable implementation
   * of a {@link LeasedConnectionService} based on whether or not it handles the given URI.
   * {@link #connect(Iterable, Properties)} is preferable to this method, as the former does not involve extra parsing and
   * enables you to provide multiple IPv6 addresses to connect to.
   *
   * @param uri URI to connect to
   * @param properties any configurations to be applied (implementation specific)
   * @return an established connection
   * @throws ConnectionException if there is an error while attempting to connect
   * @see #connect(Iterable, Properties)
   */
  public static LeasedConnection connect(URI uri, Properties properties) throws ConnectionException {
    return getLeasedConnection(uri, properties);
  }

  /**
   * Establish a connection to the provided servers. The method will attempt to look for the first suitable implementation
   * of a {@link LeasedConnectionService} based on whether or not it handles the given type.
   * This method is preferable to {@link #connect(URI, Properties)}, as it does not involve extra parsing and enables
   * you to provide multiple IPv6 addresses to connect to.
   *
   * @param servers servers to connect to
   * @param properties any configurations to be applied (implementation specific), including a connection type
   * @return an established connection
   * @throws ConnectionException if there is an error while attempting to connect
   * @see #connect(URI, Properties)
   */
  public static LeasedConnection connect(Iterable<InetSocketAddress> servers, Properties properties) throws ConnectionException {
    return getLeasedConnection(servers, properties);
  }

  private static LeasedConnection getLeasedConnection(URI uri, Properties properties) throws ConnectionException {
    LeasedConnectionService leasedConnectionService = getServices().stream()
        .filter(connectionService -> connectionService.handlesURI(uri))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException("Unknown URI " + uri));
    return leasedConnectionService.connect(uri, properties);
  }

  private static LeasedConnection getLeasedConnection(Iterable<InetSocketAddress> servers, Properties properties) throws ConnectionException {
    String connectionType = properties.getProperty(ConnectionPropertyNames.CONNECTION_TYPE, DEFAULT_CONNECTION_TYPE);
    LeasedConnectionService leasedConnectionService = getServices().stream()
        .filter(connectionService -> connectionService.handlesConnectionType(connectionType))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException("Unknown connection type " + connectionType));
    return leasedConnectionService.connect(servers, properties);
  }

  private static Collection<? extends LeasedConnectionService> getServices() {
    return TCServiceLoader.loadServices(LeasedConnectionService.class);
  }
}
