/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.utilities.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;
import org.terracotta.config.TcConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class StripeConfigurationTest {

  Supplier<Path> basedir = () -> Paths.get("");

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testCreation() throws IOException {
    List<Node> nodeList = new ArrayList<>();
    Node node1 = new Node();
    Path logPath = temporaryFolder.newFolder().toPath();
    node1.setNodeName("server-1");
    node1.setNodeHostname("localhost");
    node1.setNodeLogDir(logPath);
    node1.setClientReconnectWindow(100, TimeUnit.SECONDS);
    node1.setNodeBindAddress("127.0.0.1");
    node1.setNodeGroupBindAddress("127.0.1.1");
    node1.setNodePort(94101);
    node1.setNodeGroupPort(94102);
    nodeList.add(node1);

    Node node2 = new Node();
    node2.setNodeName("server-2");
    node2.setNodeHostname("localhost");
    node2.setNodeLogDir(logPath);
    node2.setClientReconnectWindow(100, TimeUnit.SECONDS);
    node2.setNodeBindAddress("127.0.0.1");
    node2.setNodeGroupBindAddress("127.0.1.1");
    node2.setNodePort(94101);
    node2.setNodeGroupPort(94102);
    nodeList.add(node2);

    Stripe stripe = new Stripe(nodeList);

    StripeConfiguration stripeConfiguration = new StripeConfiguration(stripe, basedir);

    assertThat(stripeConfiguration.get("server-1"), notNullValue());
    assertThat(stripeConfiguration.get("server-2"), notNullValue());

    TcConfig tcConfig = stripeConfiguration.get("server-1").getTcConfig();
    Servers servers = tcConfig.getServers();
    assertThat(servers, notNullValue());
    assertThat(servers.getClientReconnectWindow(), is(100));
    assertThat(servers.getServer().size(), is(2));

    verifyServers(servers);
    verifyServer(servers.getServer().get(0), logPath.toString());
  }

  private void verifyServer(Server server, String logLocation) {
    assertThat(server, notNullValue());
    assertThat(server.getTsaPort(), notNullValue());
    assertThat(server.getTsaPort().getValue(), is(94101));
    assertThat(server.getTsaPort().getBind(), is("127.0.0.1"));

    assertThat(server.getTsaGroupPort(), notNullValue());
    assertThat(server.getTsaGroupPort().getValue(), is(94102));
    assertThat(server.getTsaGroupPort().getBind(), is("127.0.1.1"));

    assertThat(server.getBind(), is("127.0.0.1"));
    assertThat(server.getHost(), is("localhost"));
    assertThat(server.getLogs(), is(logLocation));
  }

  private static void verifyServers(Servers servers) {
    Set<String> expected = new HashSet<>(Arrays.asList("server-1", "server-2"));
    Set<String> actual = new HashSet<>();
    for (Server server : servers.getServer()) {
      actual.add(server.getName());
    }

    assertThat(actual, is(expected));
  }
}