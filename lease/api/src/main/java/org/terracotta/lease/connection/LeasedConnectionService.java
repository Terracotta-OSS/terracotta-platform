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

import org.terracotta.connection.ConnectionException;

import java.net.URI;
import java.util.Properties;

/**
 * Service for establishing connections. The expectation is that these steps will be followed:
 *
 * 1. handlesURI() will be called with a candidate URI to see if this service handles it
 * 2. if handlesURI() returned true, connect() will be called passing in the URI again and user specified properties.
 *
 * If any error occurs during the connection process, a ConnectionException should be thrown.
 *
 */
public interface LeasedConnectionService {

  /**
   * Check if the given URI can be handled by this LeasedConnectionService.
   *
   * @param uri uri to check
   * @return true if supported
   */
  boolean handlesURI(URI uri);

  /**
   * Establish a LeasedConnection to the given URI and with the specified properties. handlesURI() must be called on the URI
   * prior to connect().
   *
   * @param uri uri to connect to
   * @param properties user specified implementation specific properties
   * @return established connection
   * @throws ConnectionException on connection failure
   */
  LeasedConnection connect(URI uri, Properties properties) throws ConnectionException;
}
