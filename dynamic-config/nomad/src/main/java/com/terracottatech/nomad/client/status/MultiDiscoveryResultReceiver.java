/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.status;

import com.terracottatech.nomad.client.results.DiscoverResultsReceiver;
import com.terracottatech.nomad.messages.DiscoverResponse;

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
  public void discoverFail(InetSocketAddress server, String reason) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discoverFail(server, reason);
    }
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    for (DiscoverResultsReceiver<T> receiver : receivers) {
      receiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
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
