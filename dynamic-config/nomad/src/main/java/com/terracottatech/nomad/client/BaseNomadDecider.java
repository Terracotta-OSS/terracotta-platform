/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.client.results.AllResultsReceiver;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.NomadServerMode;

import java.util.Set;
import java.util.UUID;

import static com.terracottatech.nomad.client.Consistency.CONSISTENT;
import static com.terracottatech.nomad.client.Consistency.MAY_NEED_RECOVERY;
import static com.terracottatech.nomad.client.Consistency.UNKNOWN_BUT_NO_CHANGE;
import static com.terracottatech.nomad.client.Consistency.UNRECOVERABLY_INCONSISTENT;
import static com.terracottatech.nomad.server.NomadServerMode.PREPARED;

public abstract class BaseNomadDecider implements NomadDecider, AllResultsReceiver {
  private volatile boolean discoverFail;
  private volatile boolean discoveryInconsistentCluster;
  private volatile boolean preparedServer;
  private volatile boolean prepareFail;
  private volatile boolean takeoverFail;
  private volatile boolean commitRollbackFail;

  @Override
  public boolean isDiscoverSuccessful() {
    return !discoverFail;
  }

  @Override
  public boolean isWholeClusterAccepting() {
    return !preparedServer;
  }

  @Override
  public boolean isPrepareSuccessful() {
    return !prepareFail;
  }

  @Override
  public boolean isTakeoverSuccessful() {
    return !takeoverFail;
  }

  @Override
  public Consistency getConsistency() {
    if (discoveryInconsistentCluster) {
      return UNRECOVERABLY_INCONSISTENT;
    }

    if (!isDiscoverSuccessful()) {
      return UNKNOWN_BUT_NO_CHANGE;
    }

    if (!isTakeoverSuccessful()) {
      return MAY_NEED_RECOVERY;
    }

    if (commitRollbackFail) {
      return MAY_NEED_RECOVERY;
    }

    return CONSISTENT;
  }

  @Override
  public void discovered(String server, DiscoverResponse discovery) {
    NomadServerMode mode = discovery.getMode();
    if (mode == PREPARED) {
      preparedServer = true;
    }
  }

  @Override
  public void discoverFail(String server) {
    discoverFail = true;
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Set<String> committedServers, Set<String> rolledBackServers) {
    discoverFail = true;
    discoveryInconsistentCluster = true;
  }

  @Override
  public void discoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    discoverFail = true;
  }

  @Override
  public void prepareFail(String server) {
    prepareFail = true;
  }

  @Override
  public void prepareOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    prepareFail = true;
  }

  @Override
  public void prepareChangeUnacceptable(String server, String rejectionReason) {
    prepareFail = true;
  }

  @Override
  public void takeoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    takeoverFail = true;
  }

  @Override
  public void takeoverFail(String server) {
    takeoverFail = true;
  }

  @Override
  public void commitFail(String server) {
    commitRollbackFail = true;
  }

  @Override
  public void commitOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    commitRollbackFail = true;
  }

  @Override
  public void rollbackFail(String server) {
    commitRollbackFail = true;
  }

  @Override
  public void rollbackOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    commitRollbackFail = true;
  }
}
