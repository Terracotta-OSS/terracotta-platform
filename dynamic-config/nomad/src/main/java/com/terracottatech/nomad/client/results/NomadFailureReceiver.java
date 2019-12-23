/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.client.recovery.RecoveryResultReceiver;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.System.lineSeparator;

/**
 * @author Mathieu Carbou
 */
public class NomadFailureReceiver<T> implements ChangeResultReceiver<T>, RecoveryResultReceiver<T> {

  private volatile List<String> failures = new CopyOnWriteArrayList<>();

  @Override
  public void takeoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    failures.add("Takeover by other client on server: " + server + ". Host: " + lastMutationHost + ", User: " + lastMutationUser);
  }

  @Override
  public void takeoverFail(InetSocketAddress server, String reason) {
    failures.add("Takeover failed on server: " + server + ". Reason: " + reason);
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    failures.add("Discover failed on server: " + server + ". Reason: " + reason);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    failures.add("Inconsistent cluster: servers " + committedServers + " committed change " + changeUuid + " but servers " + rolledBackServers + " have rolled back");
  }

  @Override
  public void discoverAlreadyPrepared(InetSocketAddress server, UUID changeUUID, String creationHost, String creationUser) {
    failures.add("Another change (with UUID " + changeUUID + " is already underway on " + server + ". It was started by " + creationUser + " on " + creationHost);
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
    failures.add("Commit failed for server " + server + ". Reason: " + reason);
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
      StringBuilder msg = new StringBuilder("Two-Phase commit failed with " + failures.size() + " messages(s):" + lineSeparator() + lineSeparator());
      for (int i = 0; i < failures.size(); i++) {
        if (msg.charAt(msg.length() - 1) != '\n') {
          msg.append(lineSeparator());
        }
        msg.append("(").append(i + 1).append(") ").append(failures.get(i));
      }
      throw new IllegalStateException(msg.toString());
    }
  }

  @Override
  public void done(Consistency consistency) {
    switch (consistency) {
      case MAY_NEED_RECOVERY:
      case UNKNOWN_BUT_NO_CHANGE:
        failures.add("Please run the check command to diagnose the configuration state.");
        break;
      case UNRECOVERABLY_INCONSISTENT:
        failures.add("Please run the check command to diagnose the configuration state and please seek support. The cluster is inconsistent and cannot be trivially recovered.");
        break;
      default:
        // do nothing
    }
  }
}
