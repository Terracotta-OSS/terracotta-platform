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
package org.terracotta.nomad.client.recovery;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.NomadMessageSender;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.NomadServerMode;

import java.time.Clock;
import java.util.List;

public class RecoveryMessageSender<T> extends NomadMessageSender<T> {
  public RecoveryMessageSender(List<NomadEndpoint<T>> servers, String host, String user, Clock clock) {
    super(servers, host, user, clock);
  }

  @Override
  public void discovered(HostPort server, DiscoverResponse<T> discovery) {
    super.discovered(server, discovery);

    if (discovery.getMode() == NomadServerMode.PREPARED) {
      registerPreparedServer(server);

      // getLatestChange() cannot return null if the server is PREPARED
      changeUuid = discovery.getLatestChange().getChangeUuid(); // we won't do anything with changeUuid if it doesn't match across servers
    }
  }
}
