/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.nomad;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import com.terracottatech.nomad.NomadEnvironment;
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

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
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
  private MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;

  @Mock
  private NomadEnvironment environment;

  @Mock
  private DiagnosticServices diagnosticServices;

  @Mock
  private DiagnosticService diagnostics1;

  @Mock
  private DiagnosticService diagnostics2;

  @Mock
  private DiagnosticService diagnostics3;

  @Mock
  private DiagnosticService diagnostics4;

  @Mock
  private ChangeResultReceiver<String> results;

  @Mock
  private NomadServer<String> nomadServer;

  @Captor
  private ArgumentCaptor<Set<String>> serverNamesCaptor;

  private Collection<InetSocketAddress> hostPortList;

  @Before
  public void before() {
    InetSocketAddress server1 = InetSocketAddress.createUnresolved("host1", 1234);
    InetSocketAddress server2 = InetSocketAddress.createUnresolved("host1", 1235);
    InetSocketAddress server3 = InetSocketAddress.createUnresolved("host2", 1234);
    InetSocketAddress server4 = InetSocketAddress.createUnresolved("host2", 1235);

    hostPortList = Arrays.asList(server1, server2, server3, server4);
    when(multiDiagnosticServiceProvider.fetchDiagnosticServices(hostPortList)).thenReturn(diagnosticServices);
    when(environment.getHost()).thenReturn("host");
    when(environment.getUser()).thenReturn("user");

    when(diagnosticServices.getEndpoints()).thenReturn(hostPortList);
    when(diagnosticServices.getDiagnosticService(server1)).thenReturn(Optional.of(diagnostics1));
    when(diagnosticServices.getDiagnosticService(server2)).thenReturn(Optional.of(diagnostics2));
    when(diagnosticServices.getDiagnosticService(server3)).thenReturn(Optional.of(diagnostics3));
    when(diagnosticServices.getDiagnosticService(server4)).thenReturn(Optional.of(diagnostics4));
    Stream.of(diagnostics1, diagnostics2, diagnostics3, diagnostics4)
        .forEach(diagnostics -> when(diagnostics.getProxy(NomadServer.class)).thenReturn(nomadServer));
  }

  @Test
  public void createClient() throws NomadException {
    NomadClientFactory<String> factory = new NomadClientFactory<>(multiDiagnosticServiceProvider, new ConcurrencySizing(), environment, Duration.ofSeconds(2));
    CloseableNomadClient<String> client = factory.createClient(hostPortList);
    client.tryApplyChange(results, new SimpleNomadChange("change", "summary"));

    verify(results).startDiscovery(serverNamesCaptor.capture());
    assertThat(serverNamesCaptor.getValue(), containsInAnyOrder("host1:1234", "host2:1234", "host1:1235", "host2:1235"));

    Stream.of(diagnostics1, diagnostics2, diagnostics3, diagnostics4)
        .forEach(diagnostics -> verify(diagnostics).getProxy(NomadServer.class));
    verify(nomadServer, times(4)).discover();
  }

  @Test
  public void close() {
    NomadClientFactory<String> factory = new NomadClientFactory<>(multiDiagnosticServiceProvider, new ConcurrencySizing(), environment, Duration.ofSeconds(2));
    CloseableNomadClient<String> client = factory.createClient(hostPortList);

    verify(diagnosticServices, never()).close();
    client.close();
    verify(diagnosticServices).close();
  }
}
