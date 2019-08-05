/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.recovery;

import com.terracottatech.nomad.client.BaseNomadDecider;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.ChangeRequestState;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.terracottatech.nomad.server.ChangeRequestState.ROLLED_BACK;

public class RecoveryProcessDecider<T> extends BaseNomadDecider<T> {
  private final Set<UUID> latestChangeUuids = ConcurrentHashMap.newKeySet();
  private volatile boolean rolledBack;

  @Override
  public boolean shouldDoCommit() {
    return latestChangeUuids.size() == 1 && !rolledBack;
  }

  @Override
  public void discovered(String server, DiscoverResponse<T> discovery) {
    super.discovered(server, discovery);

    UUID latestChangeUuid = getLatestChangeUuid(discovery);
    latestChangeUuids.add(latestChangeUuid);

    ChangeRequestState changeState = getLatestChangeState(discovery);
    if (changeState == ROLLED_BACK) {
      rolledBack = true;
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
}
