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

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toSet;

public class ClusterConsistencyChecker<T> implements AllResultsReceiver<T> {
  private final Map<InetSocketAddress, ChangeDetails<T>> commits = new ConcurrentHashMap<>();

  public void checkClusterConsistency(DiscoverResultsReceiver<T> results) {
    Set<UUID> uuids = commits.values().stream().map(ChangeDetails::getChangeUuid).collect(toSet());
    if (uuids.size() < 2) {
      // if we discover nothing or if changes all have the exact same uuid, then cluster is ok
      return;
    }

    // if we have found several different uuid, then let's look at the result hash to see if the latest
    // change (prepared or committed) has lead to the same result
    Set<String> hashes = commits.values().stream().map(ChangeDetails::getChangeResultHash).collect(toSet());
    if (hashes.size() < 2) {
      // if we do not have different hashes, then the cluster is consistent.
      // It means different changes have lead to the same result.
      // This might be the case for a rolled back concurrent tx, and also for an automatic config upgrade at startup
      return;
    }

    // here, we have found some inconsistencies: => fire the event
    results.discoverClusterInconsistent(uuids, commits.keySet());
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    ChangeDetails<T> latestChange = discovery.getLatestChange();
    if (latestChange != null) {
      switch (latestChange.getState()) {
        case COMMITTED:
          commits.put(server, latestChange);
          break;
        case ROLLED_BACK:
          throw new AssertionError("Impossible ChangeRequestState: ROLLED_BACK. Discovery should only output the last non rolled back change");
        case PREPARED:
          // Do nothing
          break;
        default:
          throw new AssertionError("Unknown ChangeRequestState: " + latestChange.getState());
      }
    }
  }
}
