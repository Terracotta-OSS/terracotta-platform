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
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.NomadServerMode;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static org.terracotta.nomad.client.Consistency.CONSISTENT;
import static org.terracotta.nomad.client.Consistency.MAY_NEED_RECOVERY;
import static org.terracotta.nomad.client.Consistency.UNKNOWN_BUT_NO_CHANGE;
import static org.terracotta.nomad.client.Consistency.UNRECOVERABLY_INCONSISTENT;
import static org.terracotta.nomad.server.NomadServerMode.PREPARED;

public abstract class BaseNomadDecider<T> implements NomadDecider<T>, AllResultsReceiver<T> {
  private volatile boolean discoverFail;
  private volatile boolean discoveryInconsistentCluster;
  private volatile boolean discoveryDesynchronizedCluster;
  private volatile boolean preparedServer;
  private volatile boolean prepareFail;
  private volatile boolean takeoverFail;
  private volatile boolean commitRollbackFail;

  @Override
  public boolean isDiscoverSuccessful() {
    return !discoverFail;
  }

  @Override
  public boolean isWholeClusterAccepting() {
    return !preparedServer;
  }

  @Override
  public boolean isPrepareSuccessful() {
    return !prepareFail;
  }

  @Override
  public boolean isTakeoverSuccessful() {
    return !takeoverFail;
  }

  @Override
  public Consistency getConsistency() {
    if (discoveryInconsistentCluster) {
      return UNRECOVERABLY_INCONSISTENT;
    }

    if (discoveryDesynchronizedCluster) {
      return UNRECOVERABLY_INCONSISTENT;
    }

    if (!isDiscoverSuccessful()) {
      return UNKNOWN_BUT_NO_CHANGE;
    }

    if (!isTakeoverSuccessful()) {
      return MAY_NEED_RECOVERY;
    }

    if (commitRollbackFail) {
      return MAY_NEED_RECOVERY;
    }

    return CONSISTENT;
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    NomadServerMode mode = discovery.getMode();
    if (mode == PREPARED) {
      preparedServer = true;
    }
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    discoverFail = true;
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    discoverFail = true;
    discoveryInconsistentCluster = true;
  }

  @Override
  public void discoverClusterDesynchronized(Map<UUID, Collection<InetSocketAddress>> lastChangeUuids) {
    discoverFail = true;
    discoveryDesynchronizedCluster = true;
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    discoverFail = true;
  }

  @Override
  public void prepareFail(InetSocketAddress server, String reason) {
    prepareFail = true;
  }

  @Override
  public void prepareOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    prepareFail = true;
  }

  @Override
  public void prepareChangeUnacceptable(InetSocketAddress server, String rejectionReason) {
    prepareFail = true;
  }

  @Override
  public void takeoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    takeoverFail = true;
  }

  @Override
  public void takeoverFail(InetSocketAddress server, String reason) {
    takeoverFail = true;
  }

  @Override
  public void commitFail(InetSocketAddress server, String reason) {
    commitRollbackFail = true;
  }

  @Override
  public void commitOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    commitRollbackFail = true;
  }

  @Override
  public void rollbackFail(InetSocketAddress server, String reason) {
    commitRollbackFail = true;
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    commitRollbackFail = true;
  }
}
