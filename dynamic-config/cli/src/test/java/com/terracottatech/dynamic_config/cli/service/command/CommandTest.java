/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.service.connect.DynamicConfigNodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.service.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class CommandTest<C extends Command> {

  protected NodeAddressDiscovery nodeAddressDiscovery;
  protected DiagnosticServiceProvider diagnosticServiceProvider;
  protected MultiDiagnosticServiceConnectionFactory connectionFactory;

  private final Map<InetSocketAddress, DiagnosticService> diagnosticServices = new HashMap<>();
  private final Map<InetSocketAddress, TopologyService> dynamicConfigServices = new HashMap<>();
  private final Consumer<InetSocketAddress> perNodeMockCreator = addr -> {
    dynamicConfigServices.putIfAbsent(addr, mock(TopologyService.class));
    diagnosticServices.putIfAbsent(addr, mock(DiagnosticService.class));
    when(diagnosticServices.get(addr).getProxy(TopologyService.class)).thenReturn(dynamicConfigServices.get(addr));
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
    connectionFactory = new MultiDiagnosticServiceConnectionFactory(diagnosticServiceProvider, 5, TimeUnit.SECONDS, new ConcurrencySizing());
  }

  DiagnosticService diagnosticServiceMock(InetSocketAddress address) {
    perNodeMockCreator.accept(address);
    return diagnosticServices.get(address);
  }

  DiagnosticService diagnosticServiceMock(String host, int port) {
    return diagnosticServiceMock(InetSocketAddress.createUnresolved(host, port));
  }

  TopologyService dynamicConfigServiceMock(InetSocketAddress address) {
    perNodeMockCreator.accept(address);
    return dynamicConfigServices.get(address);
  }

  TopologyService dynamicConfigServiceMock(String host, int port) {
    return dynamicConfigServiceMock(InetSocketAddress.createUnresolved(host, port));
  }

  protected abstract C newCommand();
}