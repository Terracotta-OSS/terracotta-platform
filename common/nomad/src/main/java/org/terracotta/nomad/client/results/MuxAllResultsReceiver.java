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
import org.terracotta.nomad.messages.DiscoverResponse;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MuxAllResultsReceiver<T> implements AllResultsReceiver<T> {
  private final List<AllResultsReceiver<T>> receivers;

  public MuxAllResultsReceiver(List<AllResultsReceiver<T>> receivers) {
    this.receivers = receivers;
  }

  @Override
  public void setResults(AllResultsReceiver<T> results) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.setResults(results);
    }
  }

  @Override
  public void startDiscovery(Collection<HostPort> servers) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startDiscovery(servers);
    }
  }

  @Override
  public void discovered(HostPort server, DiscoverResponse<T> discovery) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discovered(server, discovery);
    }
  }

  @Override
  public void discoverFail(HostPort server, Throwable reason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discoverFail(server, reason);
    }
  }

  @Override
  public void discoverConfigInconsistent(UUID changeUuid, Collection<HostPort> committedServers, Collection<HostPort> rolledBackServers) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discoverConfigInconsistent(changeUuid, committedServers, rolledBackServers);
    }
  }

  @Override
  public void discoverConfigPartitioned(Collection<Collection<HostPort>> partitions) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discoverConfigPartitioned(partitions);
    }
  }

  @Override
  public void endDiscovery() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endDiscovery();
    }
  }

  @Override
  public void startSecondDiscovery() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startSecondDiscovery();
    }
  }

  @Override
  public void discoverRepeated(HostPort server) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discoverRepeated(server);
    }

  }

  @Override
  public void discoverOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endSecondDiscovery() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endSecondDiscovery();
    }
  }

  @Override
  public void discoverAlreadyPrepared(HostPort server, UUID changeUuid, String creationHost, String creationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discoverAlreadyPrepared(server, changeUuid, creationHost, creationUser);
    }
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startPrepare(newChangeUuid);
    }
  }

  @Override
  public void prepared(HostPort server) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.prepared(server);
    }
  }

  @Override
  public void prepareFail(HostPort server, Throwable reason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.prepareFail(server, reason);
    }
  }

  @Override
  public void prepareOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.prepareOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void prepareChangeUnacceptable(HostPort server, String rejectionReason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.prepareChangeUnacceptable(server, rejectionReason);
    }
  }

  @Override
  public void endPrepare() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endPrepare();
    }
  }

  @Override
  public void startTakeover() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startTakeover();
    }
  }

  @Override
  public void takeover(HostPort server) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.takeover(server);
    }
  }

  @Override
  public void takeoverOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.takeoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void takeoverFail(HostPort server, Throwable reason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.takeoverFail(server, reason);
    }
  }

  @Override
  public void endTakeover() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endTakeover();
    }
  }

  @Override
  public void startCommit() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startCommit();
    }
  }

  @Override
  public void committed(HostPort server) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.committed(server);
    }
  }

  @Override
  public void commitFail(HostPort server, Throwable reason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.commitFail(server, reason);
    }
  }

  @Override
  public void commitOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.commitOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endCommit() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endCommit();
    }
  }

  @Override
  public void startRollback() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startRollback();
    }
  }

  @Override
  public void rolledBack(HostPort server) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.rolledBack(server);
    }
  }

  @Override
  public void rollbackFail(HostPort server, Throwable reason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.rollbackFail(server, reason);
    }
  }

  @Override
  public void rollbackOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.rollbackOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endRollback() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endRollback();
    }
  }

  @Override
  public void done(Consistency consistency) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.done(consistency);
    }
  }

  @Override
  public void cannotDecideOverCommitOrRollback() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.cannotDecideOverCommitOrRollback();
    }
  }
}
