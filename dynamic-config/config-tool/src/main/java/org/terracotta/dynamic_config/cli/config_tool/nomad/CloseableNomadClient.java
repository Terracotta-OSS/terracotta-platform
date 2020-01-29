/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.nomad;

import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.change.ChangeResultReceiver;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.server.ChangeRequestState;

public class CloseableNomadClient<T> implements AutoCloseable {
  private final NomadClient<T> client;
  private final DiagnosticServices diagnosticServices;

  public CloseableNomadClient(NomadClient<T> client, DiagnosticServices diagnosticServices) {
    this.client = client;
    this.diagnosticServices = diagnosticServices;
  }

  public void tryApplyChange(ChangeResultReceiver<T> results, NomadChange change) {
    client.tryApplyChange(results, change);
  }

  public void tryRecovery(RecoveryResultReceiver<T> results, int expectedNodeCount, ChangeRequestState forcedState) {
    client.tryRecovery(results, expectedNodeCount, forcedState);
  }

  public void tryDiscovery(DiscoverResultsReceiver<T> results) {
    client.tryDiscovery(results);
  }

  @Override
  public void close() {
    diagnosticServices.close();
  }
}
