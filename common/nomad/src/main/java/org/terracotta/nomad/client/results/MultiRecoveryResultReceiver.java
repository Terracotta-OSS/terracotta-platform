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
package org.terracotta.nomad.client.results;

import org.terracotta.nomad.client.Consistency;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
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
  public void takeover(InetSocketAddress server) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.takeover(server);
    }
  }

  @Override
  public void takeoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.takeoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void takeoverFail(InetSocketAddress server, Throwable reason) {
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
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.startDiscovery(servers);
    }
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.discovered(server, discovery);
    }
  }

  @Override
  public void discoverFail(InetSocketAddress server, Throwable reason) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.discoverFail(server, reason);
    }
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
    }
  }

  @Override
  public void discoverClusterDesynchronized(Map<UUID, Collection<InetSocketAddress>> lastChangeUuids) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.discoverClusterDesynchronized(lastChangeUuids);
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
  public void discoverRepeated(InetSocketAddress server) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.discoverRepeated(server);
    }
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
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
  public void committed(InetSocketAddress server) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.committed(server);
    }
  }

  @Override
  public void commitFail(InetSocketAddress server, Throwable reason) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.commitFail(server, reason);
    }
  }

  @Override
  public void commitOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
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
  public void rolledBack(InetSocketAddress server) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.rolledBack(server);
    }
  }

  @Override
  public void rollbackFail(InetSocketAddress server, Throwable reason) {
    for (RecoveryResultReceiver<T> recoveryResultReceiver : recoveryResultReceivers) {
      recoveryResultReceiver.rollbackFail(server, reason);
    }
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
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
