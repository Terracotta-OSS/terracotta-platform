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

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;

import java.net.URI;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class LeasedConnectionServiceImpl implements LeasedConnectionService {

  private static final String SCHEME = "terracotta";
  private static final Duration DEFAULT_LEASED_CONNECTION_TIMEOUT = Duration.ofSeconds(150);

  @Override
  public boolean handlesURI(URI uri) {
    return SCHEME.equals(uri.getScheme());
  }

  @Override
  public LeasedConnection connect(URI uri, Properties properties) throws ConnectionException {
    Connection connection = ConnectionFactory.connect(uri, properties);

    LeasedConnection leasedConnection = BasicLeasedConnection.create(connection, createTimeBudget(properties));

    return leasedConnection;
  }

  private static TimeBudget createTimeBudget(Properties properties) {
    String timeoutString = properties.getProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT,
            Long.toString(DEFAULT_LEASED_CONNECTION_TIMEOUT.toMillis()));
    long timeout = Long.parseLong(timeoutString);
    return new TimeBudget(timeout, TimeUnit.MILLISECONDS);
  }
}
