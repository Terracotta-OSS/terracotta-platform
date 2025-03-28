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
package org.terracotta.nomad.client.status;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.util.Collection;
import java.util.UUID;

/**
 * @author Mathieu Carbou
 */
public class MultiDiscoveryResultReceiver<T> implements DiscoverResultsReceiver<T> {

  private final Collection<DiscoverResultsReceiver<T>> receivers;

  public MultiDiscoveryResultReceiver(Collection<DiscoverResultsReceiver<T>> receivers) {
    this.receivers = receivers;
  }

  @Override
  public void startDiscovery(Collection<HostPort> servers) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.startDiscovery(servers);
    }
  }

  @Override
  public void discovered(HostPort server, DiscoverResponse<T> discovery) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discovered(server, discovery);
    }
  }

  @Override
  public void discoverFail(HostPort server, Throwable reason) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discoverFail(server, reason);
    }
  }

  @Override
  public void discoverConfigInconsistent(UUID changeUuid, Collection<HostPort> committedServers, Collection<HostPort> rolledBackServers) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discoverConfigInconsistent(changeUuid, committedServers, rolledBackServers);
    }
  }

  @Override
  public void discoverConfigPartitioned(Collection<Collection<HostPort>> partitions) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discoverConfigPartitioned(partitions);
    }
  }

  @Override
  public void endDiscovery() {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.endDiscovery();
    }
  }

  @Override
  public void startSecondDiscovery() {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.startSecondDiscovery();
    }
  }

  @Override
  public void discoverRepeated(HostPort server) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discoverRepeated(server);
    }
  }

  @Override
  public void discoverOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endSecondDiscovery() {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.endSecondDiscovery();
    }
  }
}
