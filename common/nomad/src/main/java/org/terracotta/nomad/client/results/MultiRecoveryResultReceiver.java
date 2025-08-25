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
package org.terracotta.nomad.client.results;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.Consistency;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.util.Collection;
import java.util.UUID;

/**
 * @author Mathieu Carbou
 */
public class MultiRecoveryResultReceiver<T> implements RecoveryResultReceiver<T> {

  private final Collection<RecoveryResultReceiver<T>> recoveryResultReceivers;

  public MultiRecoveryResultReceiver(Collection<RecoveryResultReceiver<T>> recoveryResultReceivers) {
    this.recoveryResultReceivers = recoveryResultReceivers;
  }

  @Override
  public void startTakeover() {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.startTakeover();
    }
  }

  @Override
  public void takeover(HostPort server) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.takeover(server);
    }
  }

  @Override
  public void takeoverOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.takeoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void takeoverFail(HostPort server, Throwable reason) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.takeoverFail(server, reason);
    }
  }

  @Override
  public void endTakeover() {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.endTakeover();
    }
  }

  @Override
  public void startDiscovery(Collection<HostPort> servers) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.startDiscovery(servers);
    }
  }

  @Override
  public void discovered(HostPort server, DiscoverResponse<T> discovery) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.discovered(server, discovery);
    }
  }

  @Override
  public void discoverFail(HostPort server, Throwable reason) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.discoverFail(server, reason);
    }
  }

  @Override
  public void discoverConfigInconsistent(UUID changeUuid, Collection<HostPort> committedServers, Collection<HostPort> rolledBackServers) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.discoverConfigInconsistent(changeUuid, committedServers, rolledBackServers);
    }
  }

  @Override
  public void discoverConfigPartitioned(Collection<Collection<HostPort>> partitions) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.discoverConfigPartitioned(partitions);
    }
  }

  @Override
  public void endDiscovery() {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.endDiscovery();
    }
  }

  @Override
  public void startSecondDiscovery() {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.startSecondDiscovery();
    }
  }

  @Override
  public void discoverRepeated(HostPort server) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.discoverRepeated(server);
    }
  }

  @Override
  public void discoverOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endSecondDiscovery() {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.endSecondDiscovery();
    }
  }

  @Override
  public void startCommit() {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.startCommit();
    }
  }

  @Override
  public void committed(HostPort server) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.committed(server);
    }
  }

  @Override
  public void commitFail(HostPort server, Throwable reason) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.commitFail(server, reason);
    }
  }

  @Override
  public void commitOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.commitOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endCommit() {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.endCommit();
    }
  }

  @Override
  public void startRollback() {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.startRollback();
    }
  }

  @Override
  public void rolledBack(HostPort server) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.rolledBack(server);
    }
  }

  @Override
  public void rollbackFail(HostPort server, Throwable reason) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.rollbackFail(server, reason);
    }
  }

  @Override
  public void rollbackOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.rollbackOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endRollback() {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.endRollback();
    }
  }

  @Override
  public void done(Consistency consistency) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.done(consistency);
    }
  }

  @Override
  public void cannotDecideOverCommitOrRollback() {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.cannotDecideOverCommitOrRollback();
    }
  }
}
