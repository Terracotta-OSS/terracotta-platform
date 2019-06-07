/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.nomad;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.diagnostic.client.connection.NodeInfo;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.client.change.SimpleNomadChange;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NomadClientFactoryTest {
  @Mock
  private MultiDiagnosticServiceConnectionFactory connectionFactory;

  @Mock
  private NomadEnvironment environment;

  @Mock
  private MultiDiagnosticServiceConnection connection;

  @Mock
  private DiagnosticService diagnostics1;

  @Mock
  private DiagnosticService diagnostics2;

  @Mock
  private DiagnosticService diagnostics3;

  @Mock
  private DiagnosticService diagnostics4;

  @Mock
  private ChangeResultReceiver results;

  @Mock
  private NomadServer nomadServer;

  @Captor
  private ArgumentCaptor<Set<String>> serverNamesCaptor;

  private List<String> hostPortList;

  @Before
  public void before() {
    hostPortList = Arrays.asList("host1:1234", "host2:1234");
    when(connectionFactory.createConnection(hostPortList)).thenReturn(connection);
    when(environment.getHost()).thenReturn("host");
    when(environment.getUser()).thenReturn("user");
    NodeInfo server1 = new NodeInfo("server1", "host1", 1234);
    NodeInfo server2 = new NodeInfo("server2", "host1", 1235);
    NodeInfo server3 = new NodeInfo("server3", "host2", 1234);
    NodeInfo server4 = new NodeInfo("server4", "host1", 1235);
    when(connection.getServers()).thenReturn(new HashSet<>(Arrays.asList(server1, server2, server3, server4)));
    when(connection.getDiagnosticService(server1)).thenReturn(diagnostics1);
    when(connection.getDiagnosticService(server2)).thenReturn(diagnostics2);
    when(connection.getDiagnosticService(server3)).thenReturn(diagnostics3);
    when(connection.getDiagnosticService(server4)).thenReturn(diagnostics4);
    Stream.of(diagnostics1, diagnostics2, diagnostics3, diagnostics4)
        .forEach(diagnostics -> when(diagnostics.getProxy(NomadServer.class)).thenReturn(nomadServer));
  }

  @Test
  public void createClient() throws NomadException {
    NomadClientFactory factory = new NomadClientFactory(connectionFactory, new ConcurrencySizing(), environment, 2_000);
    CloseableNomadClient client = factory.createClient(hostPortList);
    client.tryApplyChange(results, new SimpleNomadChange("change", "summary"));

    verify(results).startDiscovery(serverNamesCaptor.capture());
    assertThat(serverNamesCaptor.getValue(), containsInAnyOrder("server1", "server2", "server3", "server4"));

    Stream.of(diagnostics1, diagnostics2, diagnostics3, diagnostics4)
        .forEach(diagnostics -> verify(diagnostics).getProxy(NomadServer.class));
    verify(nomadServer, times(4)).discover();
  }

  @Test
  public void close() {
    NomadClientFactory factory = new NomadClientFactory(connectionFactory, new ConcurrencySizing(), environment, 2_000);
    CloseableNomadClient client = factory.createClient(hostPortList);

    verify(connection, never()).close();
    client.close();
    verify(connection).close();
  }
}
