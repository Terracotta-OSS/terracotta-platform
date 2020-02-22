/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.nomad;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.dynamic_config.cli.config_tool.SimpleNomadChange;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.change.ChangeResultReceiver;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CloseableNomadClientTest {
  @Mock
  private NomadClient<String> nomadClient;

  @Mock
  private DiagnosticServices diagnosticServices;

  @Mock
  private ChangeResultReceiver<String> results;

  @Test
  public void delegatesAndCloses() {
    try (CloseableNomadClient<String> client = new CloseableNomadClient<>(nomadClient, diagnosticServices)) {
      client.tryApplyChange(results, new SimpleNomadChange("change", "summary"));
    }

    verify(nomadClient).tryApplyChange(results, new SimpleNomadChange("change", "summary"));
    verify(diagnosticServices).close();
  }
}
