/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.connect;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;

import java.net.InetSocketAddress;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigNodeAddressDiscovery implements NodeAddressDiscovery {

  private final DiagnosticServiceProvider diagnosticServiceProvider;

  public DynamicConfigNodeAddressDiscovery(DiagnosticServiceProvider diagnosticServiceProvider) {
    this.diagnosticServiceProvider = requireNonNull(diagnosticServiceProvider);
  }

  @Override
  public Collection<InetSocketAddress> discover(InetSocketAddress aNode) {
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(aNode)) {
      TopologyService topologyService = requireNonNull(diagnosticService.getProxy(TopologyService.class));
      Collection<InetSocketAddress> nodeAddresses = topologyService.getCluster().getNodeAddresses();
      if (!nodeAddresses.contains(aNode)) {
        throw new IllegalArgumentException("Node address " + aNode + " used to connect does not match any known node in cluster " + nodeAddresses);
      }
      return nodeAddresses;
    }
  }
}
