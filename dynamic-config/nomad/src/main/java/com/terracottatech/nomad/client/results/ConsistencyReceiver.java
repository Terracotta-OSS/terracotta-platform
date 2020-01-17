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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * @author Mathieu Carbou
 */
public class ConsistencyReceiver<T> implements DiscoverResultsReceiver<T> {

  private final Map<InetSocketAddress, DiscoverResponse<T>> responses = new HashMap<>(0);

  private volatile String discoverFailure;
  private volatile boolean discoveredInconsistentCluster;
  private volatile boolean discoveredOtherClient;

  private volatile UUID inconsistentChangeUuid;
  private volatile Collection<InetSocketAddress> committedServers;
  private volatile Collection<InetSocketAddress> rolledBackServers;

  private volatile InetSocketAddress serverProcessingOtherClient;
  private volatile String otherClientHost;
  private volatile String otherClientUser;

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    responses.put(server, discovery);
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    discoverFailure = reason;
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    this.inconsistentChangeUuid = changeUuid;
    this.committedServers = committedServers;
    this.rolledBackServers = rolledBackServers;
    this.discoveredInconsistentCluster = true;
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    this.serverProcessingOtherClient = server;
    this.otherClientHost = lastMutationHost;
    this.otherClientUser = lastMutationUser;
    this.discoveredOtherClient = true;
  }

  public Optional<DiscoverResponse<T>> getDiscoveryResponse(InetSocketAddress node) {
    return Optional.ofNullable(responses.get(node));
  }

  public String getDiscoverFailure() {
    return discoverFailure;
  }

  public boolean areAllAccepting() {
    return responses.values().stream().map(DiscoverResponse::getMode).allMatch(Predicate.isEqual(NomadServerMode.ACCEPTING));
  }

  // discoverClusterInconsistent

  public boolean hasDiscoveredInconsistentCluster() {
    return discoveredInconsistentCluster;
  }

  public UUID getInconsistentChangeUuid() {
    return inconsistentChangeUuid;
  }

  public Collection<InetSocketAddress> getCommittedServers() {
    return committedServers;
  }

  public Collection<InetSocketAddress> getRolledBackServers() {
    return rolledBackServers;
  }

  // discoverOtherClient

  public boolean hasDiscoveredOtherClient() {
    return discoveredOtherClient;
  }

  public InetSocketAddress getServerProcessingOtherClient() {
    return serverProcessingOtherClient;
  }

  public String getOtherClientHost() {
    return otherClientHost;
  }

  public String getOtherClientUser() {
    return otherClientUser;
  }
}
