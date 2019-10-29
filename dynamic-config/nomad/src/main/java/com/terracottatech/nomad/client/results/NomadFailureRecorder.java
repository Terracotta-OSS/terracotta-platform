/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import com.terracottatech.nomad.client.change.ChangeResultReceiver;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class NomadFailureRecorder<T> implements ChangeResultReceiver<T> {

  private volatile List<String> failures = new CopyOnWriteArrayList<>();

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    failures.add("Discover failed on server: " + server + ". Reason: " + reason);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    failures.add("Inconsistent cluster: servers " + committedServers + " committed change " + changeUuid + " but servers " + rolledBackServers + " have rolled back");
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    failures.add("Other discover in progress on server: " + server);
  }

  @Override
  public void prepareFail(InetSocketAddress server, String reason) {
    failures.add("Prepare failed for server: " + server + ". Reason: " + reason);
  }

  @Override
  public void prepareOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    failures.add("Other prepare in progress for server: " + server);
  }

  @Override
  public void prepareChangeUnacceptable(InetSocketAddress server, String rejectionReason) {
    failures.add("Prepare rejected for server " + server + ". Reason: " + rejectionReason);
  }

  @Override
  public void commitFail(InetSocketAddress server, String reason) {
    failures.add("Commit failed for server " + server + ": " + reason);
  }

  @Override
  public void commitOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    failures.add("Other commit in progress for server: " + server);
  }

  @Override
  public void rollbackFail(InetSocketAddress server, String reason) {
    failures.add("Rollback failed for server: " + server + ". Reason: " + reason);
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    failures.add("Other rollback in progress for server: " + server);
  }

  public boolean isEmpty() {
    return failures.isEmpty();
  }

  public void reThrow() throws IllegalStateException {
    if (!isEmpty()) {
      throw new IllegalStateException("Two-Phase commit failed:\n - " + String.join("\n - ", new TreeSet<>(failures)));
    }
  }
}
