/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.connect;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicConfigNodeAddressDiscoveryTest {

  @Mock public DiagnosticService diagnosticService;
  @Mock public TopologyService topologyService;
  @Mock public Cluster cluster;

  @Test
  public void test_discover() {
    Duration timeout = Duration.ofSeconds(1);
    DynamicConfigNodeAddressDiscovery discovery = new DynamicConfigNodeAddressDiscovery(new DiagnosticServiceProvider("foo", timeout, timeout, null) {
      @Override
      public DiagnosticService fetchDiagnosticService(InetSocketAddress address, Duration timeout) {
        return diagnosticService;
      }
    });

    when(diagnosticService.getProxy(TopologyService.class)).thenReturn(topologyService);
    when(topologyService.getCluster()).thenReturn(cluster);
    when(cluster.getNodeAddresses()).thenReturn(Arrays.asList(
        InetSocketAddress.createUnresolved("localhost", 9410),
        InetSocketAddress.createUnresolved("1.2.3.4", 9411),
        InetSocketAddress.createUnresolved("1.2.3.5", 9410),
        InetSocketAddress.createUnresolved("1.2.3.5", 9411)
    ));

    InetSocketAddress anAddress = InetSocketAddress.createUnresolved("localhost", 9410);
    Collection<InetSocketAddress> nodes = discovery.discover(anAddress);

    assertThat(nodes, Matchers.hasSize(4));
    assertThat(nodes, Matchers.containsInAnyOrder(
        InetSocketAddress.createUnresolved("localhost", 9410),
        InetSocketAddress.createUnresolved("1.2.3.4", 9411),
        InetSocketAddress.createUnresolved("1.2.3.5", 9410),
        InetSocketAddress.createUnresolved("1.2.3.5", 9411)
    ));
  }
}