/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.change;

import org.terracotta.nomad.client.Consistency;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;

public class ChangeAllResultsReceiverAdapter<T> implements AllResultsReceiver<T> {
  private final ChangeResultReceiver<T> changeResultReceiver;

  public ChangeAllResultsReceiverAdapter(ChangeResultReceiver<T> changeResultReceiver) {
    this.changeResultReceiver = changeResultReceiver;
  }

  @Override
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    changeResultReceiver.startDiscovery(servers);
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    changeResultReceiver.discovered(server, discovery);
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    changeResultReceiver.discoverFail(server, reason);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    changeResultReceiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
  }

  @Override
  public void endDiscovery() {
    changeResultReceiver.endDiscovery();
  }

  @Override
  public void startSecondDiscovery() {
    changeResultReceiver.startSecondDiscovery();
  }

  @Override
  public void discoverRepeated(InetSocketAddress server) {
    changeResultReceiver.discoverRepeated(server);
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    changeResultReceiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endSecondDiscovery() {
    changeResultReceiver.endSecondDiscovery();
  }

  @Override
  public void discoverAlreadyPrepared(InetSocketAddress server, UUID changeUuid, String creationHost, String creationUser) {
    changeResultReceiver.discoverAlreadyPrepared(server, changeUuid, creationHost, creationUser);
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    changeResultReceiver.startPrepare(newChangeUuid);
  }

  @Override
  public void prepared(InetSocketAddress server) {
    changeResultReceiver.prepared(server);
  }

  @Override
  public void prepareFail(InetSocketAddress server, String reason) {
    changeResultReceiver.prepareFail(server, reason);
  }

  @Override
  public void prepareOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    changeResultReceiver.prepareOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void prepareChangeUnacceptable(InetSocketAddress server, String rejectionReason) {
    changeResultReceiver.prepareChangeUnacceptable(server, rejectionReason);
  }

  @Override
  public void endPrepare() {
    changeResultReceiver.endPrepare();
  }

  @Override
  public void startTakeover() {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void takeover(InetSocketAddress server) {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void takeoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void takeoverFail(InetSocketAddress server, String reason) {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void endTakeover() {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void startCommit() {
    changeResultReceiver.startCommit();
  }

  @Override
  public void committed(InetSocketAddress server) {
    changeResultReceiver.committed(server);
  }

  @Override
  public void commitFail(InetSocketAddress server, String reason) {
    changeResultReceiver.commitFail(server, reason);
  }

  @Override
  public void commitOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    changeResultReceiver.commitOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endCommit() {
    changeResultReceiver.endCommit();
  }

  @Override
  public void startRollback() {
    changeResultReceiver.startRollback();
  }

  @Override
  public void rolledBack(InetSocketAddress server) {
    changeResultReceiver.rolledBack(server);
  }

  @Override
  public void rollbackFail(InetSocketAddress server, String reason) {
    changeResultReceiver.rollbackFail(server, reason);
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    changeResultReceiver.rollbackOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endRollback() {
    changeResultReceiver.endRollback();
  }

  @Override
  public void done(Consistency consistency) {
    changeResultReceiver.done(consistency);
  }

  @Override
  public void cannotDecideOverCommitOrRollback() {
    changeResultReceiver.cannotDecideOverCommitOrRollback();
  }
}
