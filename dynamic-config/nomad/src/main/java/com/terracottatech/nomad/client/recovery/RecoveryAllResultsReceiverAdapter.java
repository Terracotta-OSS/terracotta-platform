/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.recovery;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.client.results.AllResultsReceiver;
import com.terracottatech.nomad.messages.DiscoverResponse;

import java.util.Set;
import java.util.UUID;

public class RecoveryAllResultsReceiverAdapter<T> implements AllResultsReceiver<T> {
  private final RecoveryResultReceiver<T> recoveryResultReceiver;

  public RecoveryAllResultsReceiverAdapter(RecoveryResultReceiver<T> recoveryResultReceiver) {
    this.recoveryResultReceiver = recoveryResultReceiver;
  }

  @Override
  public void startDiscovery(Set<String> servers) {
    recoveryResultReceiver.startDiscovery(servers);
  }

  @Override
  public void discovered(String server, DiscoverResponse<T> discovery) {
    recoveryResultReceiver.discovered(server, discovery);
  }

  @Override
  public void discoverFail(String server) {
    recoveryResultReceiver.discoverFail(server);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Set<String> committedServers, Set<String> rolledBackServers) {
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
  public void discoverRepeated(String server) {
    recoveryResultReceiver.discoverRepeated(server);
  }

  @Override
  public void discoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    recoveryResultReceiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endSecondDiscovery() {
    recoveryResultReceiver.endSecondDiscovery();
  }

  @Override
  public void discoverAlreadyPrepared(String server, UUID changeUuid, String creationHost, String creationUser) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepared(String server) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareFail(String server) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareChangeUnacceptable(String server, String rejectionReason) {
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
  public void takeover(String server) {
    recoveryResultReceiver.takeover(server);
  }

  @Override
  public void takeoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    recoveryResultReceiver.takeoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void takeoverFail(String server) {
    recoveryResultReceiver.takeoverFail(server);
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
  public void committed(String server) {
    recoveryResultReceiver.committed(server);
  }

  @Override
  public void commitFail(String server, String reason) {
    recoveryResultReceiver.commitFail(server, reason);
  }

  @Override
  public void commitOtherClient(String server, String lastMutationHost, String lastMutationUser) {
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
  public void rolledBack(String server) {
    recoveryResultReceiver.rolledBack(server);
  }

  @Override
  public void rollbackFail(String server) {
    recoveryResultReceiver.rollbackFail(server);
  }

  @Override
  public void rollbackOtherClient(String server, String lastMutationHost, String lastMutationUser) {
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
