/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.nomad;

import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.nomad.client.NomadClient;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.client.change.NomadChange;

public class CloseableNomadClient implements AutoCloseable {
  private final NomadClient client;
  private final MultiDiagnosticServiceConnection connection;

  public CloseableNomadClient(NomadClient client, MultiDiagnosticServiceConnection connection) {
    this.client = client;
    this.connection = connection;
  }

  public void tryApplyChange(ChangeResultReceiver results, NomadChange change) {
    client.tryApplyChange(results, change);
  }

  @Override
  public void close() {
    connection.close();
  }
}
