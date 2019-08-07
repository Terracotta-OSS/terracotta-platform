/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.connect;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.utilities.Tuple2;

import java.net.InetSocketAddress;
import java.util.Collection;

import static com.terracottatech.utilities.Tuple2.tuple2;
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
  public Tuple2<InetSocketAddress, Collection<InetSocketAddress>> discover(InetSocketAddress aNode) throws NodeAddressDiscoveryException {
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(aNode)) {
      TopologyService topologyService = requireNonNull(diagnosticService.getProxy(TopologyService.class));
      InetSocketAddress thisNodeAddress = requireNonNull(topologyService.getThisNodeAddress());
      Cluster cluster = requireNonNull(topologyService.getCluster());
      return tuple2(thisNodeAddress, cluster.getNodeAddresses());
    } catch (Exception e) {
      throw new NodeAddressDiscoveryException(e);
    }
  }
}
