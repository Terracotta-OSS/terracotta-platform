/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client;

import org.terracotta.nomad.client.change.ChangeProcess;
import org.terracotta.nomad.client.change.ChangeResultReceiver;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.client.recovery.RecoveryProcess;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.client.status.DiscoveryProcess;
import org.terracotta.nomad.server.ChangeRequestState;

import java.time.Clock;
import java.util.List;

public class NomadClient<T> implements AutoCloseable {
  private final List<NomadEndpoint<T>> servers;
  private final String host;
  private final String user;
  private final Clock clock;

  /**
   * @param servers the set of servers to run the Nomad protocol across
   * @param host    the name of the local machine
   * @param user    the name of the user the current process is running as
   */
  public NomadClient(List<NomadEndpoint<T>> servers, String host, String user, Clock clock) {
    this.clock = clock;
    if (servers.isEmpty()) {
      throw new IllegalArgumentException("There must be at least one server");
    }

    this.servers = servers;
    this.host = host;
    this.user = user;
  }

  public void tryApplyChange(ChangeResultReceiver<T> results, NomadChange change) {
    ChangeProcess<T> changeProcess = new ChangeProcess<>(servers, host, user, clock);
    changeProcess.applyChange(results, change);
  }

  public void tryRecovery(RecoveryResultReceiver<T> results, int expectedNodeCount, ChangeRequestState forcedState) {
    RecoveryProcess<T> recoveryProcess = new RecoveryProcess<>(servers, host, user, clock);
    recoveryProcess.recover(results, expectedNodeCount, forcedState);
  }

  public void tryDiscovery(DiscoverResultsReceiver<T> results) {
    DiscoveryProcess<T> discoveryProcess = new DiscoveryProcess<>(servers, host, user, clock);
    discoveryProcess.discover(results);
  }

  @Override
  public void close() {
    RuntimeException error = null;
    for (NomadEndpoint<T> server : servers) {
      try {
        server.close();
      } catch (RuntimeException e) {
        if (error == null) {
          error = e;
        } else {
          error.addSuppressed(e);
        }
      }
    }
    if (error != null) {
      throw error;
    }
  }
}
