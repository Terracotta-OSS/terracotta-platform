/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import com.terracottatech.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;

public interface DiscoverResultsReceiver<T> extends WrapUpResultsReceiver {
  default void startDiscovery(Collection<InetSocketAddress> endpoints) {}

  default void discovered(InetSocketAddress endpoint, DiscoverResponse<T> discovery) {}

  default void discoverFail(InetSocketAddress endpoint, String reason) {}

  default void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {}

  default void endDiscovery() {}

  default void startSecondDiscovery() {}

  default void discoverRepeated(InetSocketAddress server) {}

  default void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {}

  default void endSecondDiscovery() {}
}
