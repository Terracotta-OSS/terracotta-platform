/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.TcNode;
import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.TcStripe;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.PathResolver;
import com.terracottatech.utilities.TimeUnit;
import org.terracotta.config.BindPort;
import org.terracotta.config.ObjectFactory;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;

import java.util.LinkedHashMap;
import java.util.Map;

class StripeConfiguration {
  private static final ObjectFactory FACTORY = new ObjectFactory();

  private final Map<String, ServerConfiguration> stripeConfiguration;
  private final PathResolver pathResolver;

  StripeConfiguration(Stripe stripe, PathResolver pathResolver) {
    this.pathResolver = pathResolver;
    this.stripeConfiguration = createStripeConfig(stripe);
  }

  ServerConfiguration get(String serverName) {
    return stripeConfiguration.get(serverName);
  }

  private Map<String, ServerConfiguration> createStripeConfig(Stripe stripe) {
    // please keep an ordering
    Map<String, ServerConfiguration> stripeConfiguration = new LinkedHashMap<>();
    Servers servers = createServers(stripe);
    for (Node node : stripe.getNodes()) {
      if (stripeConfiguration.containsKey(node.getNodeName())) {
        throw new IllegalStateException("Duplicate node name: " + node.getNodeName() + " found in stripe: " + stripe);
      } else {
        stripeConfiguration.put(node.getNodeName(), new ServerConfiguration(node, servers, pathResolver));
      }
    }
    return stripeConfiguration;
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
    server.setLogs(node.getNodeLogDir() == null ? null : pathResolver.resolve(node.getNodeLogDir()).toString());
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

  TcStripe
  getClusterConfigStripe(com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.ObjectFactory factory) {
    TcStripe stripe = factory.createTcStripe();

    for (ServerConfiguration serverConfiguration : stripeConfiguration.values()) {
      TcNode node = serverConfiguration.getClusterConfigNode(factory);
      stripe.getNodes().add(node);
    }

    return stripe;
  }
}
