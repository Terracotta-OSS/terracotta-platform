/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.TimeUnit;
import org.terracotta.config.BindPort;
import org.terracotta.config.ObjectFactory;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

class StripeConfiguration {
  private static final ObjectFactory FACTORY = new ObjectFactory();

  private final Map<String, ServerConfiguration> stripeConfiguration;
  private final Supplier<Path> baseDir;

  StripeConfiguration(Stripe stripe, Supplier<Path> baseDir) {
    this.baseDir = baseDir;
    Map<String, ServerConfiguration> stripeConfiguration = new HashMap<>();
    Servers servers = createServers(stripe);
    for (Node node : stripe.getNodes()) {
      stripeConfiguration.put(node.getNodeName(), new ServerConfiguration(node, servers, baseDir));
    }

    this.stripeConfiguration = stripeConfiguration;
  }

  ServerConfiguration get(String serverName) {
    return stripeConfiguration.get(serverName);
  }

  private Servers createServers(Stripe stripe) {
    Servers servers = FACTORY.createServers();

    int reconnectWindow = -1;
    for (Node node : stripe.getNodes()) {
      servers.getServer().add(createServer(node));
      if (reconnectWindow == -1) {
        Measure<TimeUnit> clientReconnectWindow = node.getClientReconnectWindow();
        reconnectWindow = (int) clientReconnectWindow.getUnit().toSeconds(clientReconnectWindow.getQuantity());
      }
    }

    servers.setClientReconnectWindow(reconnectWindow);

    return servers;
  }

  private Server createServer(Node node) {
    Server server = FACTORY.createServer();

    server.setName(node.getNodeName());
    server.setHost(node.getNodeHostname());
    server.setLogs(baseDir.get().resolve(node.getNodeLogDir()).toString());
    server.setBind(node.getNodeBindAddress());

    BindPort tsaPort = FACTORY.createBindPort();
    tsaPort.setBind(node.getNodeBindAddress());
    tsaPort.setValue(node.getNodePort());
    server.setTsaPort(tsaPort);

    BindPort tsaGroupPort = FACTORY.createBindPort();
    tsaGroupPort.setBind(node.getNodeGroupBindAddress());
    tsaGroupPort.setValue(node.getNodeGroupPort());
    server.setTsaGroupPort(tsaGroupPort);

    return server;
  }

  com.terracottatech.topology.config.xmlobjects.Stripe
  getClusterConfigStripe(com.terracottatech.topology.config.xmlobjects.ObjectFactory factory) {
    com.terracottatech.topology.config.xmlobjects.Stripe stripe = factory.createStripe();

    for (ServerConfiguration serverConfiguration : stripeConfiguration.values()) {
      com.terracottatech.topology.config.xmlobjects.Node node = serverConfiguration.getClusterConfigNode(factory);
      stripe.getNodes().add(node);
    }

    return stripe;
  }
}
