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
package org.terracotta.management.tms.entity.server;

import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Endpoint;
import org.terracotta.monitoring.PlatformConnectedClient;

/**
 * @author Mathieu Carbou
 */
class Utils {

  static <T> T[] array(T... o) {
    return o;
  }

  static Endpoint toClientEndpoint(PlatformConnectedClient connection) {
    return Endpoint.create(connection.remoteAddress.getHostAddress(), connection.remotePort);
  }

  static ClientIdentifier toClientIdentifier(PlatformConnectedClient connection) {
    return ClientIdentifier.create(
        connection.clientPID,
        connection.remoteAddress.getHostAddress(),
        connection.name == null || connection.name.isEmpty() ? "UNKNOWN" : connection.name,
        connection.uuid);
  }

  static Client toClient(PlatformConnectedClient connection) {
    ClientIdentifier clientIdentifier = toClientIdentifier(connection);
    return Client.create(clientIdentifier)
        .setHostName(connection.remoteAddress.getHostName());
  }

}
