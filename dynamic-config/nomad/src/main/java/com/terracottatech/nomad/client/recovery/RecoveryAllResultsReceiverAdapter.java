/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.recovery;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.client.results.AllResultsReceiver;
import com.terracottatech.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;

public class RecoveryAllResultsReceiverAdapter<T> implements AllResultsReceiver<T> {
  private final RecoveryResultReceiver<T> recoveryResultReceiver;

  public RecoveryAllResultsReceiverAdapter(RecoveryResultReceiver<T> recoveryResultReceiver) {
    this.recoveryResultReceiver = recoveryResultReceiver;
  }

  @Override
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    recoveryResultReceiver.startDiscovery(servers);
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    recoveryResultReceiver.discovered(server, discovery);
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    recoveryResultReceiver.discoverFail(server, reason);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    recoveryResultReceiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
  }

  @Override
  public void endDiscovery() {
    recoveryResultReceiver.endDiscovery();
  }

  @Override
  public void startSecondDiscovery() {
    recoveryResultReceiver.startSecondDiscovery();
  }

  @Override
  public void discoverRepeated(InetSocketAddress server) {
    recoveryResultReceiver.discoverRepeated(server);
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    recoveryResultReceiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endSecondDiscovery() {
    recoveryResultReceiver.endSecondDiscovery();
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

  @Override
  public void startTakeover() {
    recoveryResultReceiver.startTakeover();
  }

  @Override
  public void takeover(InetSocketAddress server) {
    recoveryResultReceiver.takeover(server);
  }

  @Override
  public void takeoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    recoveryResultReceiver.takeoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void takeoverFail(InetSocketAddress server, String reason) {
    recoveryResultReceiver.takeoverFail(server, reason);
  }

  @Override
  public void endTakeover() {
    recoveryResultReceiver.endTakeover();
  }

  @Override
  public void startCommit() {
    recoveryResultReceiver.startCommit();
  }

  @Override
  public void committed(InetSocketAddress server) {
    recoveryResultReceiver.committed(server);
  }

  @Override
  public void commitFail(InetSocketAddress server, String reason) {
    recoveryResultReceiver.commitFail(server, reason);
  }

  @Override
  public void commitOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    recoveryResultReceiver.commitOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endCommit() {
    recoveryResultReceiver.endCommit();
  }

  @Override
  public void startRollback() {
    recoveryResultReceiver.startRollback();
  }

  @Override
  public void rolledBack(InetSocketAddress server) {
    recoveryResultReceiver.rolledBack(server);
  }

  @Override
  public void rollbackFail(InetSocketAddress server, String reason) {
    recoveryResultReceiver.rollbackFail(server, reason);
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    recoveryResultReceiver.rollbackOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endRollback() {
    recoveryResultReceiver.endRollback();
  }

  @Override
  public void done(Consistency consistency) {
    recoveryResultReceiver.done(consistency);
  }
}
