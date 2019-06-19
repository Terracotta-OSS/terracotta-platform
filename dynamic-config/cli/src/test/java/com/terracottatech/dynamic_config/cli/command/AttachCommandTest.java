/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.command;

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
      .setDataDir("cache", Paths.get("/data/cache1"))
      .setNodeConfigDir(Paths.get("/config/node0"));

  Node node1 = new Node()
      .setNodeHostname("localhost")
      .setNodePort(9411)
      .setNodeName("node1")
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setOffheapResource("bar", 1, MemoryUnit.GB)
      .setDataDir("cache", Paths.get("/data/cache2"))
      .setNodeConfigDir(Paths.get("/config/node1"));

  Node node2 = new Node()
      .setNodeHostname("localhost")
      .setNodePort(9412)
      .setNodeName("node2")
      .setOffheapResource("foo", 2, MemoryUnit.GB)
      .setDataDir("cache", Paths.get("/data/cache2"))
      .setNodeConfigDir(Paths.get("/config/node2"));

  Cluster cluster = new Cluster(new Stripe(node0));

  @Captor ArgumentCaptor<Cluster> newCluster;

  @Override
  protected AttachCommand newCommand() {
    return new AttachCommand();
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    // mock discover call
    when(dynamicConfigServiceMock("127.0.0.1", 9410).getThisNodeAddress()).thenReturn(createUnresolved("localhost", 9410));
    when(dynamicConfigServiceMock("127.0.0.1", 9410).getTopology()).thenReturn(cluster);

    // mock destination node information retrieval
    when(dynamicConfigServiceMock(node0.getNodeAddress()).getTopology()).thenReturn(cluster);

    // mock source node information retrieval
    when(dynamicConfigServiceMock("127.0.0.1", 9411).getThisNode()).thenReturn(node1);
    when(dynamicConfigServiceMock("127.0.0.1", 9412).getThisNode()).thenReturn(node2);
  }

  @Test
  public void test_attach_nodes_to_stripe() {
    newCommand()
        .setType(TopologyChangeCommand.Type.NODE)
        .setDestination("127.0.0.1", 9410)
        .setSources(createUnresolved("127.0.0.1", 9411), createUnresolved("127.0.0.1", 9412))
        .process(jCommander, nodeAddressDiscovery, connectionFactory);

    // capture the new topology set calls
    verify(dynamicConfigServiceMock("localhost", 9410)).setTopology(newCluster.capture());
    verify(dynamicConfigServiceMock("127.0.0.1", 9411)).setTopology(newCluster.capture());
    verify(dynamicConfigServiceMock("127.0.0.1", 9412)).setTopology(newCluster.capture());

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
    newCommand()
        .setType(TopologyChangeCommand.Type.STRIPE)
        .setDestination("127.0.0.1", 9410)
        .setSources(createUnresolved("127.0.0.1", 9411), createUnresolved("127.0.0.1", 9412))
        .process(jCommander, nodeAddressDiscovery, connectionFactory);

    // capture the new topology set calls
    verify(dynamicConfigServiceMock("localhost", 9410)).setTopology(newCluster.capture());
    verify(dynamicConfigServiceMock("127.0.0.1", 9411)).setTopology(newCluster.capture());
    verify(dynamicConfigServiceMock("127.0.0.1", 9412)).setTopology(newCluster.capture());

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