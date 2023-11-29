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
package org.terracotta.nomad.client.recovery;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.BaseNomadDecider;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.ChangeRequestState;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;

public class RecoveryProcessDecider<T> extends BaseNomadDecider<T> {
  private final Set<UUID> latestChangeUuids = ConcurrentHashMap.newKeySet();
  private final AtomicInteger rolledBack = new AtomicInteger();
  private final AtomicInteger committed = new AtomicInteger();
  private final AtomicInteger prepared = new AtomicInteger();
  private final int expectedTotalNodeCount;
  private final ChangeRequestState forcedState;

  public RecoveryProcessDecider(int expectedTotalNodeCount, ChangeRequestState forcedState) {
    this.expectedTotalNodeCount = expectedTotalNodeCount;
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
  public void discovered(HostPort server, DiscoverResponse<T> discovery) {
    super.discovered(server, discovery);

    UUID latestChangeUuid = getLatestChangeUuid(discovery);
    if (latestChangeUuid != null) {
      latestChangeUuids.add(latestChangeUuid);
    }

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

  public boolean partiallyCommitted() {
    return latestChangeUuids.size() == 1 // online servers all have the same change UUID at the end of the append log
        && rolledBack.get() == 0 // AND we have no server online having rolled back this change
        && prepared.get() > 0 // AND we have some servers online that are still prepared
        && (prepared.get() + committed.get() == expectedTotalNodeCount // AND we have all the nodes that are online, and they are all either prepared or committed
        || committed.get() > 0 // OR we have some nodes offline, but amongst the online nodes, some are committed, so we can commit
        || committed.get() == 0 && forcedState == COMMITTED // OR we have some nodes offline, but amongst the online ones none are committed (they are all prepared), but user says he wants to force a commit
    );
  }

  public boolean partiallyRolledBack() {
    return latestChangeUuids.size() == 1 // online servers all have the same change UUID at the end of the append log
        && committed.get() == 0 // AND we have no server online having committed this change
        && prepared.get() > 0 // AND we have some servers online that are still prepared
        && (prepared.get() + rolledBack.get() == expectedTotalNodeCount // AND we have all the nodes that are online, and they are all either prepared or rolled back
        || rolledBack.get() > 0 // OR we have some nodes offline, but amongst the online nodes, some are rolled back, so we can rollback the prepared ones
        || rolledBack.get() == 0 && forcedState == ROLLED_BACK // OR we have some nodes offline, but amongst the online ones none are rolled back (they are all prepared), but user says he wants to force a rollback
    );
  }

  public boolean partiallyPrepared() {
    return latestChangeUuids.size() > 1 && prepared.get() > 0;
  }

  private UUID getLatestChangeUuid(DiscoverResponse<T> discovery) {
    return discovery.getLatestChange() == null ? null : discovery.getLatestChange().getChangeUuid();
  }

  private ChangeRequestState getLatestChangeState(DiscoverResponse<T> discovery) {
    return discovery.getLatestChange() == null ? null : discovery.getLatestChange().getState();
  }
}
