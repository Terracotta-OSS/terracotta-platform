/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.config_tool.command;

import com.terracottatech.common.struct.MemoryUnit;
import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Node;
import com.terracottatech.dynamic_config.api.model.NodeContext;
import com.terracottatech.dynamic_config.api.model.Stripe;
import com.terracottatech.json.Json;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.nio.file.Paths;
import java.util.List;

import static com.terracottatech.dynamic_config.cli.config_tool.converter.AttachmentType.NODE;
import static com.terracottatech.dynamic_config.cli.config_tool.converter.AttachmentType.STRIPE;
import static java.net.InetSocketAddress.createUnresolved;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou
 */
public class DetachCommandTest extends TopologyCommandTest<DetachCommand> {

  Node node0 = Node.newDefaultNode("node0", "localhost", 9410)
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setDataDir("cache", Paths.get("/data/cache0"));

  Node node1 = Node.newDefaultNode("node1", "localhost", 9411)
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setDataDir("cache", Paths.get("/data/cache1"));

  Node node2 = Node.newDefaultNode("node2", "localhost", 9412)
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setDataDir("cache", Paths.get("/data/cache2"));

  Cluster cluster = new Cluster(new Stripe(node0), new Stripe(node1, node2));

  @Captor
  ArgumentCaptor<Cluster> newCluster;

  @Override
  protected DetachCommand newTopologyCommand() {
    return new DetachCommand();
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    when(topologyServiceMock("localhost", 9410).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, node0.getNodeAddress()));
    when(topologyServiceMock("localhost", 9411).getUpcomingNodeContext()).thenReturn(new NodeContext(node1));
    when(topologyServiceMock("localhost", 9412).getUpcomingNodeContext()).thenReturn(new NodeContext(node2));

    when(topologyServiceMock("localhost", 9410).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, node0.getNodeAddress()));
    when(topologyServiceMock("localhost", 9411).getRuntimeNodeContext()).thenReturn(new NodeContext(node1));
  }

  @Test
  public void test_detach_nodes_from_stripe() {
    newCommand()
        .setAttachmentType(NODE)
        .setDestination("localhost", 9410)
        .setSources(createUnresolved("localhost", 9411), createUnresolved("localhost", 9412))
        .run();

    // capture the new topology set calls
    verify(dynamicConfigServiceMock("localhost", 9410)).setUpcomingCluster(newCluster.capture());

    List<Cluster> allValues = newCluster.getAllValues();
    assertThat(allValues, hasSize(1));
    assertThat(allValues.get(0), is(equalTo(Json.parse(getClass().getResource("/cluster3.json"), Cluster.class))));

    Cluster cluster = allValues.get(0);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), contains(createUnresolved("localhost", 9410)));
  }

  @Test
  public void test_detach_stripe() {
    newCommand()
        .setAttachmentType(STRIPE)
        .setDestination("localhost", 9410)
        .setSources(createUnresolved("localhost", 9411))
        .run();

    // capture the new topology set calls
    verify(dynamicConfigServiceMock("localhost", 9410)).setUpcomingCluster(newCluster.capture());

    List<Cluster> allValues = newCluster.getAllValues();
    assertThat(allValues, hasSize(1));
    assertThat(allValues.get(0), is(equalTo(Json.parse(getClass().getResource("/cluster3.json"), Cluster.class))));

    Cluster cluster = allValues.get(0);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), contains(createUnresolved("localhost", 9410)));
  }
}