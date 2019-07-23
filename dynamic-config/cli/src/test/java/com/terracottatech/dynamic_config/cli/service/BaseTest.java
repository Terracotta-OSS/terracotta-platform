/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.service.connect.DynamicConfigNodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.service.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.service.nomad.NomadClientFactory;
import com.terracottatech.dynamic_config.cli.service.nomad.NomadManager;
import com.terracottatech.dynamic_config.cli.service.restart.RestartService;
import com.terracottatech.dynamic_config.diagnostic.LicensingService;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.nomad.NomadEnvironment;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.utilities.cache.Cache;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class BaseTest {

  protected NodeAddressDiscovery nodeAddressDiscovery;
  protected DiagnosticServiceProvider diagnosticServiceProvider;
  protected MultiDiagnosticServiceConnectionFactory connectionFactory;
  protected NomadManager nomadManager;
  protected RestartService restartService;
  protected ConcurrencySizing concurrencySizing = new ConcurrencySizing();
  protected long timeoutMillis = 2_000;

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

  private final Cache<InetSocketAddress, NomadServer> nomadServers = Cache.<InetSocketAddress, NomadServer>create()
      .withLoader(addr -> {
        NomadServer nomadServer = mock(NomadServer.class, addr.toString());
        DiagnosticService diagnosticService = diagnosticServices.get(addr);
        when(diagnosticService.getProxy(NomadServer.class)).thenReturn(nomadServer);
        return nomadServer;
      })
      .build();

  private final Cache<InetSocketAddress, LicensingService> licensingServices = Cache.<InetSocketAddress, LicensingService>create()
      .withLoader(addr -> {
        LicensingService licensingService = mock(LicensingService.class, addr.toString());
        DiagnosticService diagnosticService = diagnosticServices.get(addr);
        when(diagnosticService.getProxy(LicensingService.class)).thenReturn(licensingService);
        return licensingService;
      })
      .build();

  @Before
  public void setUp() throws Exception {
    diagnosticServiceProvider = new DiagnosticServiceProvider(getClass().getSimpleName(), 5, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, null) {
      @Override
      public DiagnosticService fetchDiagnosticService(InetSocketAddress address, long connectTimeout, TimeUnit connectTimeUnit) {
        return diagnosticServices.get(address);
      }
    };
    nodeAddressDiscovery = new DynamicConfigNodeAddressDiscovery(diagnosticServiceProvider);
    connectionFactory = new MultiDiagnosticServiceConnectionFactory(diagnosticServiceProvider, timeoutMillis, MILLISECONDS, new ConcurrencySizing());
    nomadManager = new NomadManager(new NomadClientFactory(connectionFactory, concurrencySizing, new NomadEnvironment(), timeoutMillis), false);
    restartService = new RestartService(diagnosticServiceProvider, concurrencySizing, timeoutMillis);
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

  protected NomadServer nomadServerMock(String host, int port) {
    return nomadServers.get(InetSocketAddress.createUnresolved(host, port));
  }

  protected LicensingService licensingServiceMock(String host, int port) {
    return licensingServices.get(InetSocketAddress.createUnresolved(host, port));
  }

}
