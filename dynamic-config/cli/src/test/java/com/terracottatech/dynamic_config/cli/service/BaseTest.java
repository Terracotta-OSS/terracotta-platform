/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.ConcurrentDiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import com.terracottatech.dynamic_config.cli.service.connect.DynamicConfigNodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.service.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.service.nomad.NomadClientFactory;
import com.terracottatech.dynamic_config.cli.service.nomad.NomadManager;
import com.terracottatech.dynamic_config.cli.service.restart.RestartService;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.nomad.NomadEnvironment;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.utilities.cache.Cache;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class BaseTest {

  protected NodeAddressDiscovery nodeAddressDiscovery;
  protected DiagnosticServiceProvider diagnosticServiceProvider;
  protected MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  protected NomadManager<NodeContext> nomadManager;
  protected RestartService restartService;
  protected ConcurrencySizing concurrencySizing = new ConcurrencySizing();

  private final Cache<InetSocketAddress, DiagnosticService> diagnosticServices = Cache.<InetSocketAddress, DiagnosticService>create()
      .withLoader(addr -> mock(DiagnosticService.class, addr.toString()))
      .build();

  private final Cache<InetSocketAddress, TopologyService> topologyServices = Cache.<InetSocketAddress, TopologyService>create()
      .withLoader(addr -> {
        TopologyService topologyService = mock(TopologyService.class, addr.toString());
        DiagnosticService diagnosticService = diagnosticServices.get(addr);
        when(diagnosticService.getProxy(TopologyService.class)).thenReturn(topologyService);
        return topologyService;
      })
      .build();

  private final Cache<InetSocketAddress, NomadServer<NodeContext>> nomadServers = Cache.<InetSocketAddress, NomadServer<NodeContext>>create()
      .withLoader(addr -> {
        @SuppressWarnings("unchecked") NomadServer<NodeContext> nomadServer = mock(NomadServer.class, addr.toString());
        DiagnosticService diagnosticService = diagnosticServices.get(addr);
        when(diagnosticService.getProxy(NomadServer.class)).thenReturn(nomadServer);
        return nomadServer;
      })
      .build();

  @Before
  public void setUp() throws Exception {
    Duration timeout = Duration.ofSeconds(2);
    diagnosticServiceProvider = new DiagnosticServiceProvider(getClass().getSimpleName(), timeout, timeout, null) {
      @Override
      public DiagnosticService fetchDiagnosticService(InetSocketAddress address, Duration timeout) {
        return diagnosticServices.get(address);
      }
    };
    nodeAddressDiscovery = new DynamicConfigNodeAddressDiscovery(diagnosticServiceProvider);
    multiDiagnosticServiceProvider = new ConcurrentDiagnosticServiceProvider(diagnosticServiceProvider, timeout, new ConcurrencySizing());
    nomadManager = new NomadManager<>(new NomadClientFactory<>(multiDiagnosticServiceProvider, concurrencySizing, new NomadEnvironment(), timeout), false);
    restartService = new RestartService(diagnosticServiceProvider, concurrencySizing, timeout);
  }

  protected DiagnosticService diagnosticServiceMock(String host, int port) {
    return diagnosticServices.get(InetSocketAddress.createUnresolved(host, port));
  }

  protected TopologyService topologyServiceMock(InetSocketAddress address) {
    return topologyServices.get(address);
  }

  protected TopologyService topologyServiceMock(String host, int port) {
    return topologyServiceMock(InetSocketAddress.createUnresolved(host, port));
  }

  protected NomadServer<NodeContext> nomadServerMock(String host, int port) {
    return nomadServers.get(InetSocketAddress.createUnresolved(host, port));
  }
}
