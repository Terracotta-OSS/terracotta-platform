/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.recovery;

import com.terracottatech.nomad.client.BaseNomadDecider;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.ChangeRequestState;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.terracottatech.nomad.server.ChangeRequestState.COMMITTED;
import static com.terracottatech.nomad.server.ChangeRequestState.ROLLED_BACK;

public class RecoveryProcessDecider<T> extends BaseNomadDecider<T> {
  private final Set<UUID> latestChangeUuids = ConcurrentHashMap.newKeySet();
  private final AtomicInteger rolledBack = new AtomicInteger();
  private final AtomicInteger committed = new AtomicInteger();
  private final AtomicInteger prepared = new AtomicInteger();
  private final int expectedNodeCount;
  private final ChangeRequestState forcedState;

  public RecoveryProcessDecider(int expectedNodeCount, ChangeRequestState forcedState) {
    this.expectedNodeCount = expectedNodeCount;
    this.forcedState = forcedState; // can be null
  }

  @Override
  public boolean shouldDoCommit() {
    return partiallyCommitted();
  }

  @Override
  public boolean shouldDoRollback() {
    return partiallyRolledBack() || partiallyPrepared();
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    super.discovered(server, discovery);

    UUID latestChangeUuid = getLatestChangeUuid(discovery);
    latestChangeUuids.add(latestChangeUuid);

    ChangeRequestState changeState = getLatestChangeState(discovery);
    if (changeState != null) {
      switch (changeState) {
        case COMMITTED:
          committed.incrementAndGet();
          break;
        case ROLLED_BACK:
          rolledBack.incrementAndGet();
          break;
        case PREPARED:
          prepared.incrementAndGet();
          break;
        default:
          throw new AssertionError(changeState);
      }
    }
  }

  private UUID getLatestChangeUuid(DiscoverResponse<T> discovery) {
    ChangeDetails<T> latestChange = discovery.getLatestChange();

    if (latestChange == null) {
      return null;
    }

    return latestChange.getChangeUuid();
  }

  private ChangeRequestState getLatestChangeState(DiscoverResponse<T> discovery) {
    ChangeDetails<T> latestChange = discovery.getLatestChange();

    if (latestChange == null) {
      return null;
    }

    return latestChange.getState();
  }

  private boolean partiallyCommitted() {
    return latestChangeUuids.size() == 1 // online servers all have the same change UUID at the end of the append log
        && rolledBack.get() == 0 // AND we have no server online having rolled back this change
        && prepared.get() > 0 // AND we have some servers online that are still prepared
        && (prepared.get() + committed.get() == expectedNodeCount // AND we have all the nodes that are online and they are all either prepared or committed
        || committed.get() > 0 // OR we have some nodes offline, but amongst the online nodes, some are committed, so we can commit
        || committed.get() == 0 && forcedState == COMMITTED // OR we have some nodes offline, but amongst the online ones none are committed (they are all prepared), but user says he wants to force a commit
    );
  }

  private boolean partiallyRolledBack() {
    return latestChangeUuids.size() == 1 // online servers all have the same change UUID at the end of the append log
        && committed.get() == 0 // AND we have no server online having committed this change
        && prepared.get() > 0 // AND we have some servers online that are still prepared
        && (prepared.get() + rolledBack.get() == expectedNodeCount // AND we have all the nodes that are online and they are all either prepared or rolled back
        || rolledBack.get() > 0 // OR we have some nodes offline, but amongst the online nodes, some are rolled back, so we can rollback the prepared ones
        || rolledBack.get() == 0 && forcedState == ROLLED_BACK // OR we have some nodes offline, but amongst the online ones none are rolled back (they are all prepared), but user says he wants to force a rollback
    );
  }

  private boolean partiallyPrepared() {
    return latestChangeUuids.size() > 1 && prepared.get() > 0;
  }
}
