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
package org.terracotta.nomad.client;

import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.ChangeRequestState;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterConsistencyChecker<T> implements AllResultsReceiver<T> {
  private final Map<UUID, Collection<InetSocketAddress>> commits = new ConcurrentHashMap<>();
  private final Map<UUID, Collection<InetSocketAddress>> rollbacks = new ConcurrentHashMap<>();

  public void checkClusterConsistency(DiscoverResultsReceiver<T> results) {
    HashSet<UUID> inconsistentUUIDs = new HashSet<>(commits.keySet());
    inconsistentUUIDs.retainAll(rollbacks.keySet());

    for (UUID uuid : inconsistentUUIDs) {
      results.discoverClusterInconsistent(uuid, commits.get(uuid), rollbacks.get(uuid));
    }
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    ChangeDetails<T> latestChange = discovery.getLatestChange();

    if (latestChange != null) {
      UUID latestChangeUuid = latestChange.getChangeUuid();
      ChangeRequestState changeState = latestChange.getState();
      switch (changeState) {
        case COMMITTED:
          addChangeState(commits, latestChangeUuid, server);
          break;
        case ROLLED_BACK:
          addChangeState(rollbacks, latestChangeUuid, server);
          break;
        case PREPARED:
          // Do nothing
          break;
        default:
          throw new AssertionError("Unknown ChangeRequestState: " + changeState);
      }
    }
  }

  private void addChangeState(Map<UUID, Collection<InetSocketAddress>> map, UUID latestChangeUuid, InetSocketAddress server) {
    map.computeIfAbsent(latestChangeUuid, k -> new HashSet<>()).add(server);
  }
}
