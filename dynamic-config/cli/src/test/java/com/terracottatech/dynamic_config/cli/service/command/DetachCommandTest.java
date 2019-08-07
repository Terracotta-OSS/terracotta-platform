/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
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

  Node node0 = new Node()
      .setNodeHostname("localhost")
      .setNodePort(9410)
      .setNodeName("node0")
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setDataDir("cache", Paths.get("/data/cache0"))
      .setNodeRepositoryDir(Paths.get("/config/node0"));

  Node node1 = new Node()
      .setNodeHostname("localhost")
      .setNodePort(9411)
      .setNodeName("node1")
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setDataDir("cache", Paths.get("/data/cache1"))
      .setNodeRepositoryDir(Paths.get("/config/node1"));

  Node node2 = new Node()
      .setNodeHostname("localhost")
      .setNodePort(9412)
      .setNodeName("node2")
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setDataDir("cache", Paths.get("/data/cache2"))
      .setNodeRepositoryDir(Paths.get("/config/node2"));

  Cluster cluster = new Cluster(new Stripe(node0), new Stripe(node1, node2));

  @Captor ArgumentCaptor<Cluster> newCluster;

  @Override
  protected DetachCommand newTopologyCommand() {
    return new DetachCommand();
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    // mock discover call
    when(topologyServiceMock("127.0.0.1", 9410).getThisNodeAddress()).thenReturn(createUnresolved("localhost", 9410));
    when(topologyServiceMock("127.0.0.1", 9410).getCluster()).thenReturn(cluster);

    // mock destination node information retrieval
    when(topologyServiceMock(node0.getNodeAddress()).getCluster()).thenReturn(cluster);

    // mock source node information retrieval
    when(topologyServiceMock("127.0.0.1", 9411).getThisNode()).thenReturn(node1);
    when(topologyServiceMock("127.0.0.1", 9412).getThisNode()).thenReturn(node2);
  }

  @Test
  public void test_detach_nodes_from_stripe() {
    newCommand()
        .setType(TopologyChangeCommand.Type.NODE)
        .setDestination("127.0.0.1", 9410)
        .setSources(createUnresolved("127.0.0.1", 9411), createUnresolved("127.0.0.1", 9412))
        .run();

    // capture the new topology set calls
    verify(topologyServiceMock("localhost", 9410)).setCluster(newCluster.capture());

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
        .setType(TopologyChangeCommand.Type.STRIPE)
        .setDestination("127.0.0.1", 9410)
        .setSources(createUnresolved("127.0.0.1", 9411))
        .run();

    // capture the new topology set calls
    verify(topologyServiceMock("localhost", 9410)).setCluster(newCluster.capture());

    List<Cluster> allValues = newCluster.getAllValues();
    assertThat(allValues, hasSize(1));
    assertThat(allValues.get(0), is(equalTo(Json.parse(getClass().getResource("/cluster3.json"), Cluster.class))));

    Cluster cluster = allValues.get(0);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), contains(createUnresolved("localhost", 9410)));
  }
}