/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Properties;

/**
 * Service for establishing connections. The expectation is that either of the two ways will be used:
 * <ul>
 * <li> {@link #handlesURI(URI)} will be called with a candidate URI to see if this service handles it. If {@code handlesURI}
 * returns true, {@code connect(URI, Properties)} will be called passing in the URI again along with the user-specified properties. OR,</li>
 * <li> {@link #handlesConnectionType(String)} will be called with a candidate connection type to see if this service handles it.
 * If {@code handlesConnectionType} returns true, {@code connect(Iterable, Properties)} will be called passing in the servers
 * along with the user-specified properties.</li>
 * </ul>
 * <p>
 * If any error occurs during the connection process, a {@link ConnectionException} will be thrown.
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
   * Check if the connection with the given type is handled by this ConnectionService.
   *
   * @param connectionType connectionType to check
   * @return true if supported
   */
  boolean handlesConnectionType(String connectionType);

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

  /**
   * Establish a LeasedConnection to the given servers using the specified properties. handlesConnectionType() must be
   * called with the type prior to connect().
   *
   * @param servers servers to connect to
   * @param properties user specified implementation specific properties
   * @return established connection
   * @throws ConnectionException on connection failure
   */
  LeasedConnection connect(Iterable<InetSocketAddress> servers, Properties properties) throws ConnectionException;
}
