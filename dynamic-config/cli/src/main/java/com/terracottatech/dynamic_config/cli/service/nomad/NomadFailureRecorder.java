/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.nomad;

import com.terracottatech.nomad.client.change.ChangeResultReceiver;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NomadFailureRecorder implements ChangeResultReceiver {

  private volatile Set<String> failures = ConcurrentHashMap.newKeySet();

  @Override
  public void discoverFail(String server) {
    failures.add("Discover failed on server: " + server);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Set<String> committedServers, Set<String> rolledBackServers) {
    failures.add("Inconsistent cluster: servers " + committedServers + " committed change " + changeUuid + " but servers " + rolledBackServers + " have rolled back");
  }

  @Override
  public void discoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    failures.add("Other discover in progress on server: " + server);
  }

  @Override
  public void prepareFail(String server) {
    failures.add("Prepare failed for server: " + server);
  }

  @Override
  public void prepareOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    failures.add("Other prepare in progress for server: " + server);
  }

  @Override
  public void prepareChangeUnacceptable(String server, String rejectionReason) {
    failures.add("Prepare rejected for server " + server + ". Reason: " + rejectionReason);
  }

  @Override
  public void commitFail(String server) {
    failures.add("Commit failed for server: " + server);
  }

  @Override
  public void commitOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    failures.add("Other commit in progress for server: " + server);
  }

  @Override
  public void rollbackFail(String server) {
    failures.add("Rollback failed for server: " + server);
  }

  @Override
  public void rollbackOtherClient(String server, String lastMutationHost, String lastMutationUser) {
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
