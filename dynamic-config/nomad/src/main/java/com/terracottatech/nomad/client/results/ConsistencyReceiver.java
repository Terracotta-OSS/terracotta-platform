/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.NomadServerMode;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * @author Mathieu Carbou
 */
public class ConsistencyReceiver<T> implements DiscoverResultsReceiver<T> {

  private final Map<InetSocketAddress, DiscoverResponse<T>> responses = new HashMap<>(0);

  private volatile boolean discoverFail;
  private volatile boolean discoveryInconsistentCluster;
  private volatile boolean discoverOtherClient;

  private volatile UUID changeUuid;
  private volatile Collection<InetSocketAddress> committedServers;
  private volatile Collection<InetSocketAddress> rolledBackServers;

  private volatile InetSocketAddress server;
  private volatile String lastMutationHost;
  private volatile String lastMutationUser;

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    responses.put(server, discovery);
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    discoverFail = true;
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    this.changeUuid = changeUuid;
    this.committedServers = committedServers;
    this.rolledBackServers = rolledBackServers;
    this.discoveryInconsistentCluster = true;
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    this.server = server;
    this.lastMutationHost = lastMutationHost;
    this.lastMutationUser = lastMutationUser;
    this.discoverOtherClient = true;
  }

  public Map<InetSocketAddress, DiscoverResponse<T>> getResponses() {
    return responses;
  }

  public boolean isDiscoverFail() {
    return discoverFail;
  }

  public boolean isDiscoveryInconsistentCluster() {
    return discoveryInconsistentCluster;
  }

  public boolean isDiscoverOtherClient() {
    return discoverOtherClient;
  }

  public UUID getChangeUuid() {
    return changeUuid;
  }

  public Collection<InetSocketAddress> getCommittedServers() {
    return committedServers;
  }

  public Collection<InetSocketAddress> getRolledBackServers() {
    return rolledBackServers;
  }

  public InetSocketAddress getServer() {
    return server;
  }

  public String getLastMutationHost() {
    return lastMutationHost;
  }

  public String getLastMutationUser() {
    return lastMutationUser;
  }

  public boolean isConsistent() {
    return responses.values().stream().map(DiscoverResponse::getMode).allMatch(Predicate.isEqual(NomadServerMode.ACCEPTING));
  }
}
