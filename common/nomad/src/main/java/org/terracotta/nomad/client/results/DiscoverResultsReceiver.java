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

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.util.Collection;
import java.util.UUID;

public interface DiscoverResultsReceiver<T> extends WrapUpResultsReceiver {
  default void startDiscovery(Collection<HostPort> endpoints) {
  }

  default void discovered(HostPort endpoint, DiscoverResponse<T> discovery) {
  }

  default void discoverFail(HostPort endpoint, Throwable reason) {
  }

  default void discoverConfigInconsistent(UUID changeUuid, Collection<HostPort> committedServers, Collection<HostPort> rolledBackServers) {
  }

  default void discoverConfigPartitioned(Collection<Collection<HostPort>> partitions) {
  }

  default void endDiscovery() {}

  default void startSecondDiscovery() {}

  default void discoverRepeated(HostPort server) {
  }

  default void discoverOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
  }

  default void endSecondDiscovery() {}
}
