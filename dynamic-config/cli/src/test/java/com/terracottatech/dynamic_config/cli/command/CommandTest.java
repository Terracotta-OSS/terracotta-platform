/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.command;

import com.beust.jcommander.JCommander;
import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.connect.DynamicConfigNodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
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
public abstract class CommandTest<C extends DynamicConfigCommand> {

  NodeAddressDiscovery nodeAddressDiscovery;
  DiagnosticServiceProvider diagnosticServiceProvider;
  MultiDiagnosticServiceConnectionFactory connectionFactory;
  JCommander jCommander;

  private final Map<InetSocketAddress, DiagnosticService> diagnosticServices = new HashMap<>();
  private final Map<InetSocketAddress, DynamicConfigService> dynamicConfigServices = new HashMap<>();
  private final Consumer<InetSocketAddress> perNodeMockCreator = addr -> {
    dynamicConfigServices.putIfAbsent(addr, mock(DynamicConfigService.class));
    diagnosticServices.putIfAbsent(addr, mock(DiagnosticService.class));
    when(diagnosticServices.get(addr).getProxy(DynamicConfigService.class)).thenReturn(dynamicConfigServices.get(addr));
  };

  @Before
  public void setUp() throws Exception {
    diagnosticServiceProvider = new DiagnosticServiceProvider(getClass().getSimpleName(), 5, TimeUnit.SECONDS, null) {
      @Override
      public DiagnosticService fetchDiagnosticService(InetSocketAddress address, long connectTimeout, TimeUnit connectTimeUnit) {
        perNodeMockCreator.accept(address);
        return diagnosticServices.get(address);
      }
    };
    nodeAddressDiscovery = new DynamicConfigNodeAddressDiscovery(diagnosticServiceProvider, 5, TimeUnit.SECONDS);
    connectionFactory = new MultiDiagnosticServiceConnectionFactory(diagnosticServiceProvider, 5, TimeUnit.SECONDS, new ConcurrencySizing());
    jCommander = mock(JCommander.class);
  }

  DiagnosticService diagnosticServiceMock(InetSocketAddress address) {
    perNodeMockCreator.accept(address);
    return diagnosticServices.get(address);
  }

  DiagnosticService diagnosticServiceMock(String host, int port) {
    return diagnosticServiceMock(InetSocketAddress.createUnresolved(host, port));
  }

  DynamicConfigService dynamicConfigServiceMock(InetSocketAddress address) {
    perNodeMockCreator.accept(address);
    return dynamicConfigServices.get(address);
  }

  DynamicConfigService dynamicConfigServiceMock(String host, int port) {
    return dynamicConfigServiceMock(InetSocketAddress.createUnresolved(host, port));
  }

  protected abstract C newCommand();
}