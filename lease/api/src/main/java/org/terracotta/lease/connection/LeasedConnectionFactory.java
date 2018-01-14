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

import com.tc.util.ManagedServiceLoader;
import org.terracotta.connection.ConnectionException;

import java.net.URI;
import java.util.Collection;
import java.util.Properties;

/**
 * Factory to get LeasedConnections
 */

public class LeasedConnectionFactory {

  /**
   * Establish a connection based on the given uri. The method will attempt to look for the first suitable implementation
   * of a {@link LeasedConnectionService} based on whether or not it handles the given URI.
   *
   * @param uri URI to connect to
   * @param properties any configurations to be applied (implementation specific)
   * @return an established connection
   * @throws ConnectionException if there is an error while attempting to connect
   */
  public static LeasedConnection connect(URI uri, Properties properties) throws ConnectionException {

    Collection<LeasedConnectionService> serviceLoaders = ManagedServiceLoader.loadServices(LeasedConnectionService.class,
            LeasedConnectionService.class.getClassLoader());
    for (LeasedConnectionService connectionService : serviceLoaders) {
      if (connectionService.handlesURI(uri)) {
        return connectionService.connect(uri, properties);
      }
    }
    throw new IllegalArgumentException("Unknown URI " + uri);
  }

}
