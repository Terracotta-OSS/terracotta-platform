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
package org.terracotta.nomad.client;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.ChangeRequestState;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterConsistencyChecker<T> implements AllResultsReceiver<T> {
  private final Map<UUID, Collection<HostPort>> commits = new ConcurrentHashMap<>();
  private final Map<UUID, Collection<HostPort>> rollbacks = new ConcurrentHashMap<>();
  private final Map<String, Collection<HostPort>> lastCommittedChangeResultHashes = new ConcurrentHashMap<>();

  public void checkClusterConsistency(DiscoverResultsReceiver<T> results) {
    HashSet<UUID> inconsistentUUIDs = new HashSet<>(commits.keySet());
    inconsistentUUIDs.retainAll(rollbacks.keySet());

    // check for inconsistency first
    if (!inconsistentUUIDs.isEmpty()) {
      for (UUID uuid : inconsistentUUIDs) {
        results.discoverConfigInconsistent(uuid, commits.get(uuid), rollbacks.get(uuid));
      }
    } else {
      // At this point, excluding any change in preparation (which would fail a discovery) we
      // have all nodes having either all the same UUID in the same state or different UUID but
      // either committed or rolled back (not a mix).
      // The following code makes sure that all servers have the same configuration by
      // looking at the last committed configuration.
      if (lastCommittedChangeResultHashes.size() > 1) {
        results.discoverConfigPartitioned(lastCommittedChangeResultHashes.values());
      }
    }
  }

  @Override
  public void discovered(HostPort server, DiscoverResponse<T> discovery) {
    ChangeDetails<T> latestChange = discovery.getLatestChange();

    if (latestChange != null) {
      UUID latestChangeUuid = latestChange.getChangeUuid();
      ChangeRequestState changeState = latestChange.getState();
      switch (changeState) {
        case COMMITTED:
          lastCommittedChangeResultHashes.computeIfAbsent(latestChange.getChangeResultHash(), k -> new LinkedHashSet<>()).add(server);
          commits.computeIfAbsent(latestChangeUuid, k -> new LinkedHashSet<>()).add(server);
          break;
        case ROLLED_BACK:
          lastCommittedChangeResultHashes.computeIfAbsent(discovery.getLatestCommittedChange().getChangeResultHash(), k -> new LinkedHashSet<>()).add(server);
          rollbacks.computeIfAbsent(latestChangeUuid, k -> new LinkedHashSet<>()).add(server);
          break;
        case PREPARED:
          // Do nothing
          break;
        default:
          throw new AssertionError("Unknown ChangeRequestState: " + changeState);
      }
    }
  }
}
