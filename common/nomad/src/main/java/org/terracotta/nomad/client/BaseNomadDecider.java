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
package org.terracotta.nomad.client;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.NomadServerMode;

import java.util.Collection;
import java.util.UUID;

import static org.terracotta.nomad.client.Consistency.CONSISTENT;
import static org.terracotta.nomad.client.Consistency.MAY_NEED_RECOVERY;
import static org.terracotta.nomad.client.Consistency.UNKNOWN_BUT_NO_CHANGE;
import static org.terracotta.nomad.client.Consistency.UNRECOVERABLY_INCONSISTENT;
import static org.terracotta.nomad.client.Consistency.UNRECOVERABLY_PARTITIONNED;
import static org.terracotta.nomad.server.NomadServerMode.PREPARED;

public abstract class BaseNomadDecider<T> implements NomadDecider<T>, AllResultsReceiver<T> {
  private volatile boolean discoverFail;
  private volatile boolean discoveredConfigInconsistent;
  private volatile boolean discoveredConfigPartitioned;
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
    if (discoveredConfigInconsistent) {
      return UNRECOVERABLY_INCONSISTENT;
    }

    if (discoveredConfigPartitioned) {
      return UNRECOVERABLY_PARTITIONNED;
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
  public void discovered(HostPort server, DiscoverResponse<T> discovery) {
    NomadServerMode mode = discovery.getMode();
    if (mode == PREPARED) {
      preparedServer = true;
    }
  }

  @Override
  public void discoverFail(HostPort server, Throwable reason) {
    discoverFail = true;
  }

  @Override
  public void discoverConfigInconsistent(UUID changeUuid, Collection<HostPort> committedServers, Collection<HostPort> rolledBackServers) {
    discoverFail = true;
    discoveredConfigInconsistent = true;
  }

  @Override
  public void discoverConfigPartitioned(Collection<Collection<HostPort>> partitions) {
    discoverFail = true;
    discoveredConfigPartitioned = true;
  }

  @Override
  public void discoverOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    discoverFail = true;
  }

  @Override
  public void prepareFail(HostPort server, Throwable reason) {
    prepareFail = true;
  }

  @Override
  public void prepareOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    prepareFail = true;
  }

  @Override
  public void prepareChangeUnacceptable(HostPort server, String rejectionReason) {
    prepareFail = true;
  }

  @Override
  public void takeoverOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    takeoverFail = true;
  }

  @Override
  public void takeoverFail(HostPort server, Throwable reason) {
    takeoverFail = true;
  }

  @Override
  public void commitFail(HostPort server, Throwable reason) {
    commitRollbackFail = true;
  }

  @Override
  public void commitOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    commitRollbackFail = true;
  }

  @Override
  public void rollbackFail(HostPort server, Throwable reason) {
    commitRollbackFail = true;
  }

  @Override
  public void rollbackOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    commitRollbackFail = true;
  }
}
