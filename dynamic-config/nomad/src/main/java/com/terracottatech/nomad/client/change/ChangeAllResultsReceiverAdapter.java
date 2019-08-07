/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.change;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.client.results.AllResultsReceiver;
import com.terracottatech.nomad.messages.DiscoverResponse;

import java.util.Set;
import java.util.UUID;

public class ChangeAllResultsReceiverAdapter<T> implements AllResultsReceiver<T> {
  private final ChangeResultReceiver<T> changeResultReceiver;

  public ChangeAllResultsReceiverAdapter(ChangeResultReceiver<T> changeResultReceiver) {
    this.changeResultReceiver = changeResultReceiver;
  }

  @Override
  public void startDiscovery(Set<String> servers) {
    changeResultReceiver.startDiscovery(servers);
  }

  @Override
  public void discovered(String server, DiscoverResponse<T> discovery) {
    changeResultReceiver.discovered(server, discovery);
  }

  @Override
  public void discoverFail(String server) {
    changeResultReceiver.discoverFail(server);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Set<String> committedServers, Set<String> rolledBackServers) {
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
  public void discoverRepeated(String server) {
    changeResultReceiver.discoverRepeated(server);
  }

  @Override
  public void discoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    changeResultReceiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endSecondDiscovery() {
    changeResultReceiver.endSecondDiscovery();
  }

  @Override
  public void discoverAlreadyPrepared(String server, UUID changeUuid, String creationHost, String creationUser) {
    changeResultReceiver.discoverAlreadyPrepared(server, changeUuid, creationHost, creationUser);
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    changeResultReceiver.startPrepare(newChangeUuid);
  }

  @Override
  public void prepared(String server) {
    changeResultReceiver.prepared(server);
  }

  @Override
  public void prepareFail(String server) {
    changeResultReceiver.prepareFail(server);
  }

  @Override
  public void prepareOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    changeResultReceiver.prepareOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void prepareChangeUnacceptable(String server, String rejectionReason) {
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
  public void takeover(String server) {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void takeoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void takeoverFail(String server) {
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
  public void committed(String server) {
    changeResultReceiver.committed(server);
  }

  @Override
  public void commitFail(String server, String reason) {
    changeResultReceiver.commitFail(server, reason);
  }

  @Override
  public void commitOtherClient(String server, String lastMutationHost, String lastMutationUser) {
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
  public void rolledBack(String server) {
    changeResultReceiver.rolledBack(server);
  }

  @Override
  public void rollbackFail(String server) {
    changeResultReceiver.rollbackFail(server);
  }

  @Override
  public void rollbackOtherClient(String server, String lastMutationHost, String lastMutationUser) {
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
}
