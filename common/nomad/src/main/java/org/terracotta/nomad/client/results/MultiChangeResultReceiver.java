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
import org.terracotta.nomad.client.change.ChangeResultReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * @author Mathieu Carbou
 */
public class MultiChangeResultReceiver<T> implements ChangeResultReceiver<T> {

  private final Collection<ChangeResultReceiver<T>> changeResultReceivers;

  public MultiChangeResultReceiver(Collection<ChangeResultReceiver<T>> changeResultReceivers) {
    this.changeResultReceivers = changeResultReceivers;
  }

  @Override
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startDiscovery(servers);
    }
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discovered(server, discovery);
    }
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverFail(server, reason);
    }
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
    }
  }

  @Override
  public void discoverClusterDesynchronized(Map<UUID, Collection<InetSocketAddress>> lastChangeUuids) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverClusterDesynchronized(lastChangeUuids);
    }
  }

  @Override
  public void endDiscovery() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endDiscovery();
    }
  }

  @Override
  public void startSecondDiscovery() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startSecondDiscovery();
    }
  }

  @Override
  public void discoverRepeated(InetSocketAddress server) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverRepeated(server);
    }
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endSecondDiscovery() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endSecondDiscovery();
    }
  }

  @Override
  public void discoverAlreadyPrepared(InetSocketAddress server, UUID changeUuid, String creationHost, String creationUser) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverAlreadyPrepared(server, changeUuid, creationHost, creationUser);
    }
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startPrepare(newChangeUuid);
    }
  }

  @Override
  public void prepared(InetSocketAddress server) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepared(server);
    }
  }

  @Override
  public void prepareFail(InetSocketAddress server, String reason) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepareFail(server, reason);
    }
  }

  @Override
  public void prepareOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepareOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void prepareChangeUnacceptable(InetSocketAddress server, String rejectionReason) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepareChangeUnacceptable(server, rejectionReason);
    }
  }

  @Override
  public void endPrepare() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endPrepare();
    }
  }

  @Override
  public void startCommit() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startCommit();
    }
  }

  @Override
  public void committed(InetSocketAddress server) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.committed(server);
    }
  }

  @Override
  public void commitFail(InetSocketAddress server, String reason) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.commitFail(server, reason);
    }
  }

  @Override
  public void commitOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.commitOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endCommit() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endCommit();
    }
  }

  @Override
  public void startRollback() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startRollback();
    }
  }

  @Override
  public void rolledBack(InetSocketAddress server) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.rolledBack(server);
    }
  }

  @Override
  public void rollbackFail(InetSocketAddress server, String reason) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.rollbackFail(server, reason);
    }
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.rollbackOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endRollback() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endRollback();
    }
  }

  @Override
  public void done(Consistency consistency) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.done(consistency);
    }
  }

  @Override
  public void cannotDecideOverCommitOrRollback() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.cannotDecideOverCommitOrRollback();
    }
  }
}
