/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.status;

import org.terracotta.nomad.client.Consistency;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;

/**
 * @author Mathieu Carbou
 */
public class DiscoveryAllResultsReceiverAdapter<T> implements AllResultsReceiver<T> {
  private final DiscoverResultsReceiver<T> receiver;

  public DiscoveryAllResultsReceiverAdapter(DiscoverResultsReceiver<T> receiver) {
    this.receiver = receiver;
  }

  @Override
  public void done(Consistency consistency) {
    receiver.done(consistency);
  }

  @Override
  public void cannotDecideOverCommitOrRollback() {
    receiver.cannotDecideOverCommitOrRollback();
  }

  @Override
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    receiver.startDiscovery(servers);
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    receiver.discovered(server, discovery);
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    receiver.discoverFail(server, reason);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    receiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
  }

  @Override
  public void endDiscovery() {
    receiver.endDiscovery();
  }

  @Override
  public void startSecondDiscovery() {
    receiver.startSecondDiscovery();
  }

  @Override
  public void discoverRepeated(InetSocketAddress server) {
    receiver.discoverRepeated(server);
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    receiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endSecondDiscovery() {
    receiver.endSecondDiscovery();
  }

  @Override
  public void discoverAlreadyPrepared(InetSocketAddress server, UUID changeUuid, String creationHost, String creationUser) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepared(InetSocketAddress server) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareFail(InetSocketAddress server, String reason) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareChangeUnacceptable(InetSocketAddress server, String rejectionReason) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void endPrepare() {
    throw new AssertionError("This should not be called during the recovery process");
  }
}
