/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.connection.ConcurrencySizing;
import org.terracotta.diagnostic.client.connection.ConcurrentDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.config_tool.nomad.NomadClientFactory;
import org.terracotta.dynamic_config.cli.config_tool.nomad.NomadManager;
import org.terracotta.dynamic_config.cli.config_tool.restart.RestartService;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.server.NomadServer;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class BaseTest {

  protected DiagnosticServiceProvider diagnosticServiceProvider;
  protected MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  protected NomadManager<NodeContext> nomadManager;
  protected RestartService restartService;
  protected ConcurrencySizing concurrencySizing = new ConcurrencySizing();

  private final Cache<InetSocketAddress, TopologyService> topologyServices = new Cache<>(addr -> mock(TopologyService.class, addr.toString()));

  private final Cache<InetSocketAddress, DynamicConfigService> dynamicConfigServices = new Cache<>(addr -> mock(DynamicConfigService.class, addr.toString()));

  @SuppressWarnings({"unchecked", "rawtypes"})
  private final Cache<InetSocketAddress, NomadServer<NodeContext>> nomadServers = new Cache<>(addr -> mock(NomadServer.class, addr.toString()));

  private final Cache<InetSocketAddress, DiagnosticService> diagnosticServices = new Cache<>(addr -> {
    final DiagnosticService diagnosticService = mock(DiagnosticService.class, addr.toString());
    lenient().when(diagnosticService.getProxy(TopologyService.class)).thenAnswer(invocation -> topologyServices.get(addr));
    lenient().when(diagnosticService.getProxy(DynamicConfigService.class)).thenAnswer(invocation -> dynamicConfigServices.get(addr));
    lenient().when(diagnosticService.getProxy(NomadServer.class)).thenAnswer(invocation -> nomadServers.get(addr));
    return diagnosticService;
  });

  @Before
  public void setUp() throws Exception {
    Duration timeout = Duration.ofSeconds(2);
    diagnosticServiceProvider = new DiagnosticServiceProvider(getClass().getSimpleName(), timeout, timeout, null) {
      @Override
      public DiagnosticService fetchDiagnosticService(InetSocketAddress address, Duration timeout) {
        return diagnosticServices.get(address);
      }
    };
    multiDiagnosticServiceProvider = new ConcurrentDiagnosticServiceProvider(diagnosticServiceProvider, timeout, new ConcurrencySizing());
    nomadManager = new NomadManager<>(new NomadClientFactory<>(multiDiagnosticServiceProvider, new NomadEnvironment()));
    restartService = new RestartService(diagnosticServiceProvider, concurrencySizing);
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

  protected DynamicConfigService dynamicConfigServiceMock(InetSocketAddress address) {
    return dynamicConfigServices.get(address);
  }

  protected DynamicConfigService dynamicConfigServiceMock(String host, int port) {
    return dynamicConfigServiceMock(InetSocketAddress.createUnresolved(host, port));
  }

  protected NomadServer<NodeContext> nomadServerMock(String host, int port) {
    return nomadServers.get(InetSocketAddress.createUnresolved(host, port));
  }

  /**
   * @author Mathieu Carbou
   */
  private static class Cache<K, V> {
    private final Function<K, V> loader;
    private final Map<K, V> cache = new ConcurrentHashMap<>();

    Cache(Function<K, V> loader) {
      this.loader = loader;
    }

    public V get(K key) {
      return cache.computeIfAbsent(key, loader);
    }
  }
}