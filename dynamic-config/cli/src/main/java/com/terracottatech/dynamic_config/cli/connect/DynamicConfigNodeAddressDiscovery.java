/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.connect;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.utilities.Tuple2;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static com.terracottatech.utilities.Tuple2.tuple2;
import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigNodeAddressDiscovery implements NodeAddressDiscovery {

  private final DiagnosticServiceProvider diagnosticServiceProvider;
  private final long connectTimeout;
  private final TimeUnit connectTimeoutUnit;

  public DynamicConfigNodeAddressDiscovery(DiagnosticServiceProvider diagnosticServiceProvider, long connectTimeout, TimeUnit connectTimeoutUnit) {
    this.diagnosticServiceProvider = requireNonNull(diagnosticServiceProvider);
    this.connectTimeout = connectTimeout;
    this.connectTimeoutUnit = requireNonNull(connectTimeoutUnit);
  }

  @Override
  public Tuple2<InetSocketAddress, Collection<InetSocketAddress>> discover(InetSocketAddress aNode) throws NodeAddressDiscoveryException {
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(aNode, connectTimeout, connectTimeoutUnit)) {
      DynamicConfigService dynamicConfigService = requireNonNull(diagnosticService.getProxy(DynamicConfigService.class));
      InetSocketAddress thisNodeAddress = requireNonNull(dynamicConfigService.getThisNodeAddress());
      Cluster cluster = requireNonNull(dynamicConfigService.getTopology());
      return tuple2(thisNodeAddress, cluster.getNodeAddresses());
    } catch (Exception e) {
      throw new NodeAddressDiscoveryException(e);
    }
  }
}
