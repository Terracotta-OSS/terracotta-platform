/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.nomad;

import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.nomad.client.NomadClient;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.client.recovery.RecoveryResultReceiver;
import com.terracottatech.nomad.client.results.DiscoverResultsReceiver;
import com.terracottatech.nomad.server.ChangeRequestState;

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
