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
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

  private final Map<InetSocketAddress, DiagnosticService> diagnosticServices = new HashMap<>();
  private final Map<InetSocketAddress, TopologyService> topologyServices = new HashMap<>();
  private final Map<InetSocketAddress, NomadServer> nomadServers = new HashMap<>();
  private final Map<InetSocketAddress, LicensingService> licensingServices = new HashMap<>();

  private final Consumer<InetSocketAddress> perNodeMockCreator = addr -> {
    diagnosticServices.putIfAbsent(addr, mock(DiagnosticService.class, addr.toString()));
    if (topologyServices.putIfAbsent(addr, mock(TopologyService.class, addr.toString())) == null) {
      when(diagnosticServices.get(addr).getProxy(TopologyService.class)).thenReturn(topologyServices.get(addr));
    }
    if (nomadServers.putIfAbsent(addr, mock(NomadServer.class, addr.toString())) == null) {
      when(diagnosticServices.get(addr).getProxy(NomadServer.class)).thenReturn(nomadServers.get(addr));
    }
    if (licensingServices.putIfAbsent(addr, mock(LicensingService.class, addr.toString())) == null) {
      when(diagnosticServices.get(addr).getProxy(LicensingService.class)).thenReturn(licensingServices.get(addr));
    }
  };


  @Before
  public void setUp() throws Exception {
    diagnosticServiceProvider = new DiagnosticServiceProvider(getClass().getSimpleName(), 5, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, null) {
      @Override
      public DiagnosticService fetchDiagnosticService(InetSocketAddress address, long connectTimeout, TimeUnit connectTimeUnit) {
        perNodeMockCreator.accept(address);
        return diagnosticServices.get(address);
      }
    };
    nodeAddressDiscovery = new DynamicConfigNodeAddressDiscovery(diagnosticServiceProvider);
    connectionFactory = new MultiDiagnosticServiceConnectionFactory(diagnosticServiceProvider, timeoutMillis, MILLISECONDS, new ConcurrencySizing());
    nomadManager = new NomadManager(new NomadClientFactory(connectionFactory, concurrencySizing, new NomadEnvironment(), timeoutMillis), false);
    restartService = new RestartService(diagnosticServiceProvider, concurrencySizing, timeoutMillis);
  }

  protected DiagnosticService diagnosticServiceMock(InetSocketAddress address) {
    perNodeMockCreator.accept(address);
    return diagnosticServices.get(address);
  }

  protected DiagnosticService diagnosticServiceMock(String host, int port) {
    return diagnosticServiceMock(InetSocketAddress.createUnresolved(host, port));
  }

  protected TopologyService topologyServiceMock(InetSocketAddress address) {
    perNodeMockCreator.accept(address);
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
