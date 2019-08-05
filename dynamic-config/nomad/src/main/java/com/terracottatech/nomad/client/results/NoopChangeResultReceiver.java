/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.messages.DiscoverResponse;

import java.util.Set;
import java.util.UUID;

public class NoopChangeResultReceiver<T> implements ChangeResultReceiver<T> {
  @Override
  public void startDiscovery(Set<String> servers) {
  }

  @Override
  public void discovered(String server, DiscoverResponse<T> discovery) {
  }

  @Override
  public void discoverFail(String server) {
  }

  @Override
  public void discoverAlreadyPrepared(String server, UUID changeUUID, String creationHost, String creationUser) {
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Set<String> committedServers, Set<String> rolledBackServers) {
  }

  @Override
  public void endDiscovery() {
  }

  @Override
  public void startSecondDiscovery() {
  }

  @Override
  public void discoverRepeated(String server) {
  }

  @Override
  public void discoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
  }

  @Override
  public void endSecondDiscovery() {
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
  }

  @Override
  public void prepared(String server) {
  }

  @Override
  public void prepareFail(String server) {
  }

  @Override
  public void prepareOtherClient(String server, String lastMutationHost, String lastMutationUser) {
  }

  @Override
  public void prepareChangeUnacceptable(String server, String rejectionReason) {
  }

  @Override
  public void endPrepare() {
  }

  @Override
  public void startCommit() {
  }

  @Override
  public void committed(String server) {
  }

  @Override
  public void commitFail(String server) {
  }

  @Override
  public void commitOtherClient(String server, String lastMutationHost, String lastMutationUser) {
  }

  @Override
  public void endCommit() {
  }

  @Override
  public void startRollback() {
  }

  @Override
  public void rolledBack(String server) {
  }

  @Override
  public void rollbackFail(String server) {
  }

  @Override
  public void rollbackOtherClient(String server, String lastMutationHost, String lastMutationUser) {
  }

  @Override
  public void endRollback() {
  }

  @Override
  public void done(Consistency consistency) {
  }
}
