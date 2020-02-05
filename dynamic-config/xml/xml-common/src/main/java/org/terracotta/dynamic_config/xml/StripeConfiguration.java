/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml;

import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.config.BindPort;
import org.terracotta.config.ObjectFactory;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcNode;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcStripe;

import java.util.LinkedHashMap;
import java.util.Map;

public class StripeConfiguration {
  private static final ObjectFactory FACTORY = new ObjectFactory();

  private final Map<String, ServerConfiguration> stripeConfiguration;
  private final PathResolver pathResolver;

  public StripeConfiguration(Stripe stripe, PathResolver pathResolver) {
    this.pathResolver = pathResolver;
    this.stripeConfiguration = createStripeConfig(stripe);
  }

  public ServerConfiguration get(String serverName) {
    return stripeConfiguration.get(serverName);
  }

  protected ServerConfiguration newServer(Servers servers, Node node, PathResolver pathResolver) {
    return new ServerConfiguration(node, servers, pathResolver);
  }

  private Map<String, ServerConfiguration> createStripeConfig(Stripe stripe) {
    // please keep an ordering
    Map<String, ServerConfiguration> stripeConfiguration = new LinkedHashMap<>();
    Servers servers = createServers(stripe);
    for (Node node : stripe.getNodes()) {
      if (stripeConfiguration.containsKey(node.getNodeName())) {
        throw new IllegalStateException("Duplicate node name: " + node.getNodeName() + " found in stripe: " + stripe);
      } else {
        stripeConfiguration.put(node.getNodeName(), newServer(servers, node, pathResolver));
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
  getClusterConfigStripe(org.terracotta.dynamic_config.xml.topology.config.xmlobjects.ObjectFactory factory) {
    TcStripe stripe = factory.createTcStripe();

    for (ServerConfiguration serverConfiguration : stripeConfiguration.values()) {
      TcNode node = serverConfiguration.getClusterConfigNode(factory);
      stripe.getNodes().add(node);
    }

    return stripe;
  }
}