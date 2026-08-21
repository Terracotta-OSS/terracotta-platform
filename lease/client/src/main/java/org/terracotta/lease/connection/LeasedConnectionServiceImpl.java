/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.lease.SystemTimeSource;
import org.terracotta.lease.TimeSource;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Properties;

public class LeasedConnectionServiceImpl implements LeasedConnectionService {

  private static final String SCHEME = "terracotta";
  private static final String DEFAULT_LEASED_CONNECTION_TIMEOUT = "150000";

  private TimeSource timeSource = new SystemTimeSource();

  public void setTimeSource(TimeSource timeSource) {
    this.timeSource = timeSource;
  }

  @Override
  public boolean handlesURI(URI uri) {
    return SCHEME.equals(uri.getScheme());
  }

  @Override
  public boolean handlesConnectionType(String connectionType) {
    return SCHEME.equals(connectionType);
  }

  @Override
  public LeasedConnection connect(URI uri, Properties properties) throws ConnectionException {
    Connection connection = ConnectionFactory.connect(uri, properties);
    return createLeasedConnection(properties, connection);
  }

  @Override
  public LeasedConnection connect(Iterable<InetSocketAddress> servers, Properties properties) throws ConnectionException {
    Connection connection = ConnectionFactory.connect(servers, properties);
    return createLeasedConnection(properties, connection);
  }

  private LeasedConnection createLeasedConnection(Properties properties, Connection connection) throws ConnectionException {
    long timeoutMillis = Long.parseLong(properties.getProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, DEFAULT_LEASED_CONNECTION_TIMEOUT));
    return BasicLeasedConnection.create(connection, timeoutMillis, timeSource);
  }
}
