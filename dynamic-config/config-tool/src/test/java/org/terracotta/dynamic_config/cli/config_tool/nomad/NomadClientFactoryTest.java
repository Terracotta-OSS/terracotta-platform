/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.nomad;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.server.NomadServer;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NomadClientFactoryTest {

  @Mock MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  @Mock DiagnosticService diagnostics1;
  @Mock DiagnosticService diagnostics2;
  @Mock DiagnosticService diagnostics3;
  @Mock DiagnosticService diagnostics4;
  @Mock NomadServer<String> nomadServer;

  private final InetSocketAddress server1 = InetSocketAddress.createUnresolved("host1", 1234);
  private final InetSocketAddress server2 = InetSocketAddress.createUnresolved("host1", 1235);
  private final InetSocketAddress server3 = InetSocketAddress.createUnresolved("host2", 1234);
  private final InetSocketAddress server4 = InetSocketAddress.createUnresolved("host2", 1235);
  private final List<InetSocketAddress> servers = Arrays.asList(server1, server2, server3, server4);
  private final NomadEnvironment environment = new NomadEnvironment() {
    @Override
    public String getHost() {
      return "host";
    }

    @Override
    public String getUser() {
      return "user";
    }
  };

  private DiagnosticServices diagnosticServices;

  @Before
  public void before() {
    diagnosticServices = new DiagnosticServices(new HashMap<InetSocketAddress, DiagnosticService>() {
      private static final long serialVersionUID = 1L;

      {
        put(server1, diagnostics1);
        put(server2, diagnostics2);
        put(server3, diagnostics3);
        put(server4, diagnostics4);
      }
    }, Collections.emptyMap());
    when(multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(servers)).thenReturn(diagnosticServices);
    servers.forEach(addr -> when(diagnosticServices.getDiagnosticService(addr).get().getProxy(NomadServer.class)).thenReturn(nomadServer));
  }

  @Test
  public void createClient() {
    NomadClientFactory<String> factory = new NomadClientFactory<>(multiDiagnosticServiceProvider, environment);
    factory.createClient(servers);

    Stream.of(diagnostics1, diagnostics2, diagnostics3, diagnostics4).forEach(diagnostics -> verify(diagnostics).getProxy(NomadServer.class));
  }

  @Test
  public void close() {
    NomadClientFactory<String> factory = new NomadClientFactory<>(multiDiagnosticServiceProvider, environment);
    CloseableNomadClient<String> client = factory.createClient(servers);

    Stream.of(diagnostics1, diagnostics2, diagnostics3, diagnostics4).forEach(diagnostics -> verify(diagnostics, never()).close());
    client.close();
    Stream.of(diagnostics1, diagnostics2, diagnostics3, diagnostics4).forEach(diagnostics -> verify(diagnostics).close());
  }
}
