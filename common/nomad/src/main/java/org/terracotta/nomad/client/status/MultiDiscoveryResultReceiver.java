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
package org.terracotta.nomad.client.status;

import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
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
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.startDiscovery(servers);
    }
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discovered(server, discovery);
    }
  }

  @Override
  public void discoverFail(InetSocketAddress server, Throwable reason) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discoverFail(server, reason);
    }
  }

  @Override
  public void discoverClusterInconsistent(Collection<UUID> changeUuids, Collection<InetSocketAddress> servers) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discoverClusterInconsistent(changeUuids, servers);
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
  public void discoverRepeated(InetSocketAddress server) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discoverRepeated(server);
    }
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
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
