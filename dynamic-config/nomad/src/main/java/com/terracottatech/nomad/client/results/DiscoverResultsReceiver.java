/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import com.terracottatech.nomad.messages.DiscoverResponse;

import java.util.Set;
import java.util.UUID;

public interface DiscoverResultsReceiver<T> {
  default void startDiscovery(Set<String> servers) {}

  default void discovered(String server, DiscoverResponse<T> discovery) {};

  default void discoverFail(String server, String reason) {}

  default void discoverClusterInconsistent(UUID changeUuid, Set<String> committedServers, Set<String> rolledBackServers) {}

  default void endDiscovery() {}

  default void startSecondDiscovery() {}

  default void discoverRepeated(String server) {}

  default void discoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {}

  default void endSecondDiscovery() {}
}
