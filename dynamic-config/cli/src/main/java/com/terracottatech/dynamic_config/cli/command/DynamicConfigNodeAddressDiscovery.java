/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.command;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigNodeAddressDiscovery implements NodeAddressDiscovery {

  private final DiagnosticServiceProvider diagnosticServiceProvider;
  private final long connectTimeout;
  private final TimeUnit connectTimeoutUnit;

  public DynamicConfigNodeAddressDiscovery(DiagnosticServiceProvider diagnosticServiceProvider, long connectTimeout, TimeUnit connectTimeoutUnit) {
    this.diagnosticServiceProvider = diagnosticServiceProvider;
    this.connectTimeout = connectTimeout;
    this.connectTimeoutUnit = connectTimeoutUnit;
  }

  @Override
  public Collection<InetSocketAddress> discover(InetSocketAddress aNode) throws NodeAddressDiscoveryException {
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(aNode, connectTimeout, connectTimeoutUnit)) {
      return diagnosticService.getProxy(DynamicConfigService.class).getTopology().getNodeAddresses();
    } catch (Exception e) {
      throw new NodeAddressDiscoveryException(e);
    }
  }
}
