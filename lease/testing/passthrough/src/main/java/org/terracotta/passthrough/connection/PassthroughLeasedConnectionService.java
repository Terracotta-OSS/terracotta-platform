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

package org.terracotta.passthrough.connection;

import com.tc.classloader.OverrideService;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.lease.connection.LeasedConnection;
import org.terracotta.lease.connection.LeasedConnectionService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Properties;

@OverrideService("org.terracotta.lease.connection.LeasedConnectionServiceImpl")
public class PassthroughLeasedConnectionService implements LeasedConnectionService {
  @Override
  public boolean handlesURI(URI uri) {
    return handlesConnectionType(uri.getScheme());
  }

  @Override
  public boolean handlesConnectionType(String connectionType) {
    return connectionType.equals("passthrough") ||
           connectionType.equals("terracotta") || // for test and demo code
           connectionType.equals("mock"); // for the tests which are using mock connection service
  }

  @Override
  public LeasedConnection connect(URI uri, Properties properties) throws ConnectionException {
    return new PassthroughLeasedConnection(ConnectionFactory.connect(uri, properties));
  }

  @Override
  public LeasedConnection connect(Iterable<InetSocketAddress> servers, Properties properties) throws ConnectionException {
    return new PassthroughLeasedConnection(ConnectionFactory.connect(servers, properties));
  }

  static class PassthroughLeasedConnection implements LeasedConnection {

    private final Connection connection;

    PassthroughLeasedConnection(Connection connection) {
      this.connection = connection;
    }

    @Override
    public <T extends Entity, C, U> EntityRef<T, C, U> getEntityRef(Class<T> cls, long version, String name) throws EntityNotProvidedException {
      return connection.getEntityRef(cls, version, name);
    }

    @Override
    public void close() throws IOException {
      connection.close();
    }

    @Override
    public boolean isValid() {
      return connection.isValid();
    }
  }
}