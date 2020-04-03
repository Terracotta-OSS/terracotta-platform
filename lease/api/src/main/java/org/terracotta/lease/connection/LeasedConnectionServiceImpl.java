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

import org.terracotta.common.struct.TimeBudget;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class LeasedConnectionServiceImpl implements LeasedConnectionService {

  private static final String SCHEME = "terracotta";
  private static final String DEFAULT_LEASED_CONNECTION_TIMEOUT = "150000";

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
    TimeBudget timeBudget = createTimeBudget(properties);
    return BasicLeasedConnection.create(connection, timeBudget);
  }

  private static TimeBudget createTimeBudget(Properties properties) {
    String timeoutString = properties.getProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT,
            DEFAULT_LEASED_CONNECTION_TIMEOUT);
    long timeout = Long.parseLong(timeoutString);
    return new TimeBudget(timeout, TimeUnit.MILLISECONDS);
  }
}
