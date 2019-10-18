/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.utilities.Json;
import com.terracottatech.utilities.MemoryUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.nio.file.Paths;
import java.util.List;

import static java.net.InetSocketAddress.createUnresolved;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou
 */
public class AttachCommandTest extends TopologyCommandTest<AttachCommand> {

  Node node0 = new Node()
      .setNodeHostname("localhost")
      .setNodePort(9410)
      .setNodeName("node0")
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setDataDir("cache", Paths.get("/data/cache1"));

  Node node1 = new Node()
      .setNodeHostname("localhost")
      .setNodePort(9411)
      .setNodeName("node1")
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setOffheapResource("bar", 1, MemoryUnit.GB)
      .setDataDir("cache", Paths.get("/data/cache2"));

  Node node2 = new Node()
      .setNodeHostname("localhost")
      .setNodePort(9412)
      .setNodeName("node2")
      .setOffheapResource("foo", 2, MemoryUnit.GB)
      .setDataDir("cache", Paths.get("/data/cache2"));

  Cluster cluster = new Cluster(new Stripe(node0));

  @Captor ArgumentCaptor<Cluster> newCluster;

  @Override
  protected AttachCommand newTopologyCommand() {
    return new AttachCommand();
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    when(topologyServiceMock("localhost", 9410).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, node0));
    when(topologyServiceMock("localhost", 9411).getUpcomingNodeContext()).thenReturn(new NodeContext(node1));
    when(topologyServiceMock("localhost", 9412).getUpcomingNodeContext()).thenReturn(new NodeContext(node2));

    when(topologyServiceMock("localhost", 9410).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, node0));
    when(topologyServiceMock("localhost", 9411).getRuntimeNodeContext()).thenReturn(new NodeContext(node1));
  }

  @Test
  public void test_attach_nodes_to_stripe() {
    DynamicConfigService mock10 = dynamicConfigServiceMock("localhost", 9410);
    DynamicConfigService mock11 = dynamicConfigServiceMock("localhost", 9411);
    DynamicConfigService mock12 = dynamicConfigServiceMock("localhost", 9412);

    newCommand()
        .setType(TopologyCommand.Type.NODE)
        .setDestination("localhost", 9410)
        .setSources(createUnresolved("localhost", 9411), createUnresolved("localhost", 9412))
        .run();

    // capture the new topology set calls
    verify(mock10).setUpcomingCluster(newCluster.capture());
    verify(mock11).setUpcomingCluster(newCluster.capture());
    verify(mock12).setUpcomingCluster(newCluster.capture());

    List<Cluster> allValues = newCluster.getAllValues();
    assertThat(allValues, hasSize(3));
    assertThat(allValues.get(0), is(equalTo(allValues.get(1))));
    assertThat(allValues.get(0), is(equalTo(allValues.get(2))));
    assertThat(allValues.get(0), is(equalTo(Json.parse(getClass().getResource("/cluster1.json"), Cluster.class))));

    Cluster cluster = allValues.get(0);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(3));
  }

  @Test
  public void test_attach_stripe() {
    DynamicConfigService mock10 = dynamicConfigServiceMock("localhost", 9410);
    DynamicConfigService mock11 = dynamicConfigServiceMock("localhost", 9411);
    DynamicConfigService mock12 = dynamicConfigServiceMock("localhost", 9412);

    newCommand()
        .setType(TopologyCommand.Type.STRIPE)
        .setDestination("localhost", 9410)
        .setSources(createUnresolved("localhost", 9411), createUnresolved("localhost", 9412))
        .run();

    // capture the new topology set calls
    verify(mock10).setUpcomingCluster(newCluster.capture());
    verify(mock11).setUpcomingCluster(newCluster.capture());
    verify(mock12).setUpcomingCluster(newCluster.capture());

    List<Cluster> allValues = newCluster.getAllValues();
    assertThat(allValues, hasSize(3));
    assertThat(allValues.get(0), is(equalTo(allValues.get(1))));
    assertThat(allValues.get(0), is(equalTo(allValues.get(2))));
    assertThat(allValues.get(0), is(equalTo(Json.parse(getClass().getResource("/cluster2.json"), Cluster.class))));

    Cluster cluster = allValues.get(0);
    assertThat(cluster.getStripes(), hasSize(2));
    assertThat(cluster.getStripes().get(0).getNodes(), hasSize(1));
    assertThat(cluster.getStripes().get(1).getNodes(), hasSize(2));
    assertThat(cluster.getNodeAddresses(), hasSize(3));
  }
}