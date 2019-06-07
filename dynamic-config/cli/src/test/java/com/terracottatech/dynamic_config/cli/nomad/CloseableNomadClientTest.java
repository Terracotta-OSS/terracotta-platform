/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.nomad;

import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.nomad.client.NomadClient;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.client.change.SimpleNomadChange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CloseableNomadClientTest {
  @Mock
  private NomadClient nomadClient;

  @Mock
  private MultiDiagnosticServiceConnection connection;

  @Mock
  private ChangeResultReceiver results;

  @Test
  public void delegatesAndCloses() {
    try (CloseableNomadClient client = new CloseableNomadClient(nomadClient, connection)) {
      client.tryApplyChange(results, new SimpleNomadChange("change", "summary"));
    }

    verify(nomadClient).tryApplyChange(results, new SimpleNomadChange("change", "summary"));
    verify(connection).close();
  }
}
