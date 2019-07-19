/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.client.change.ChangeProcess;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.client.recovery.RecoveryProcess;
import com.terracottatech.nomad.client.recovery.RecoveryResultReceiver;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Consumer;

public class NomadClient {
  private final Collection<NamedNomadServer> servers;
  private final String host;
  private final String user;
  private volatile int concurrency;
  private volatile Duration timeout = Duration.ofSeconds(10);

  /**
   * @param servers the set of servers to run the Nomad protocol across
   * @param host the name of the local machine
   * @param user the name of the user the current process is running as
   */
  public NomadClient(Collection<NamedNomadServer> servers, String host, String user) {
    if (servers.isEmpty()) {
      throw new IllegalArgumentException("There must be at least one server");
    }

    this.servers = servers;
    this.host = host;
    this.user = user;
    this.concurrency = servers.size();
  }

  public void setConcurrency(int concurrency) {
    this.concurrency = concurrency;
  }

  public void setTimeoutMillis(long timeout) {
    this.timeout = Duration.ofMillis(timeout);
  }

  public void tryApplyChange(ChangeResultReceiver results, NomadChange change) {
    withAsyncCaller(asyncCaller -> {
      ChangeProcess changeProcess = new ChangeProcess(servers, host, user, asyncCaller);
      changeProcess.applyChange(results, change);
    });
  }

  public void tryRecovery(RecoveryResultReceiver results) {
    withAsyncCaller(asyncCaller -> {
      RecoveryProcess recoveryProcess = new RecoveryProcess(servers, host, user, asyncCaller);
      recoveryProcess.recover(results);
    });
  }

  private void withAsyncCaller(Consumer<AsyncCaller> action) {
    try (AsyncCaller asyncCaller = new AsyncCaller(concurrency, timeout)) {
      action.accept(asyncCaller);
    }
  }
}
