/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
