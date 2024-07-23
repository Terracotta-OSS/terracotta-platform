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
package org.terracotta.nomad.client.change;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.BaseNomadDecider;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.NomadServerMode;

import java.util.UUID;

import static org.terracotta.nomad.server.NomadServerMode.PREPARED;

public class ChangeProcessDecider<T> extends BaseNomadDecider<T> {
  private volatile AllResultsReceiver<T> results;

  @Override
  public void setResults(AllResultsReceiver<T> results) {
    this.results = results;
  }

  @Override
  public boolean isDiscoverSuccessful() {
    return super.isDiscoverSuccessful() && super.isWholeClusterAccepting();
  }

  @Override
  public boolean shouldDoCommit() {
    return isPrepareSuccessful();
  }

  @Override
  public void discovered(HostPort server, DiscoverResponse<T> discovery) {
    super.discovered(server, discovery);

    NomadServerMode mode = discovery.getMode();

    if (mode == PREPARED) {
      // latestChange cannot be null if the server is PREPARED
      ChangeDetails<T> latestChange = discovery.getLatestChange();
      UUID changeUuid = latestChange.getChangeUuid();
      String creationHost = latestChange.getCreationHost();
      String creationUser = latestChange.getCreationUser();
      results.discoverAlreadyPrepared(server, changeUuid, creationHost, creationUser);
    }
  }
}
