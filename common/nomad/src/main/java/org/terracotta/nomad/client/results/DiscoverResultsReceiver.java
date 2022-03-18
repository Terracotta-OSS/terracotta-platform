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

import org.terracotta.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface DiscoverResultsReceiver<T> extends WrapUpResultsReceiver {
  default void startDiscovery(Collection<InetSocketAddress> endpoints) {}

  default void discovered(InetSocketAddress endpoint, DiscoverResponse<T> discovery) {}

  default void discoverFail(InetSocketAddress endpoint, Throwable reason) {}

  default void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {}

  default void discoverClusterDesynchronized(Map<UUID, Collection<InetSocketAddress>> lastChangeUuids) {}

  default void endDiscovery() {}

  default void startSecondDiscovery() {}

  default void discoverRepeated(InetSocketAddress server) {}

  default void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {}

  default void endSecondDiscovery() {}
}
