/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.client.results.AllResultsReceiver;
import com.terracottatech.nomad.client.results.DiscoverResultsReceiver;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.ChangeRequestState;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterConsistencyChecker<T> implements AllResultsReceiver<T> {
  private final Map<UUID, Set<String>> commits = new ConcurrentHashMap<>();
  private final Map<UUID, Set<String>> rollbacks = new ConcurrentHashMap<>();

  public void checkClusterConsistency(DiscoverResultsReceiver<T> results) {
    HashSet<UUID> inconsistentUUIDs = new HashSet<>(commits.keySet());
    inconsistentUUIDs.retainAll(rollbacks.keySet());

    for (UUID uuid : inconsistentUUIDs) {
      results.discoverClusterInconsistent(uuid, commits.get(uuid), rollbacks.get(uuid));
    }
  }

  @Override
  public void discovered(String server, DiscoverResponse<T> discovery) {
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

  private void addChangeState(Map<UUID, Set<String>> map, UUID latestChangeUuid, String server) {
    map.computeIfAbsent(latestChangeUuid, k -> new HashSet<>()).add(server);
  }
}
