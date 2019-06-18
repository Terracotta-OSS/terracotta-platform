/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.command;


import com.beust.jcommander.Parameter;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyChangeCommand extends AbstractCommand {

  private final NodeAddressDiscovery nodeAddressDiscovery;
  private final MultiDiagnosticServiceConnectionFactory connectionFactory;

  protected TopologyChangeCommand(NodeAddressDiscovery nodeAddressDiscovery, MultiDiagnosticServiceConnectionFactory connectionFactory) {
    this.nodeAddressDiscovery = nodeAddressDiscovery;
    this.connectionFactory = connectionFactory;
  }

  public enum Type {NODE, STRIPE}

  @Parameter(required = true, names = {"-t"}, converter = TypeConverter.class)
  private Type type = Type.NODE;

  @Parameter(required = true, names = {"-d"}, converter = InetSocketAddressConverter.class)
  private InetSocketAddress destination;

  @Parameter(required = true, names = {"-s"}, variableArity = true, converter = InetSocketAddressConverter.class)
  private List<InetSocketAddress> sources = Collections.emptyList();

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public InetSocketAddress getDestination() {
    return destination;
  }

  public void setDestination(InetSocketAddress destination) {
    this.destination = destination;
  }

  public List<InetSocketAddress> getSources() {
    return sources;
  }

  public void setSources(List<InetSocketAddress> sources) {
    this.sources = sources;
  }

  @Override
  public void validate() {
    if (sources.contains(destination)) {
      throw new IllegalArgumentException("The destination endpoint must not be listed in the source endpoints.");
    }
  }

  @Override
  public final void run() {
    // get all the nodes to update (which includes source node plus all the nodes on the destination cluster)
    Collection<InetSocketAddress> addresses = Stream.concat(
        nodeAddressDiscovery.discover(destination).stream(),
        sources.stream()).collect(toSet());

    // create a multi-connection based on all teh cluster addresses plus the one to add
    try (MultiDiagnosticServiceConnection connections = connectionFactory.createConnection(addresses)) {

      // get the target node / cluster
      Target dest = connections.getDiagnosticService(destination)
          .map(ds -> ds.getProxy(DynamicConfigService.class))
          .map(dcs -> new Target(dcs.getThisNode(), dcs.getTopology()))
          .get();

      // get all the source node info
      Collection<Node> src = sources.stream()
          .map(addr -> connections.getDiagnosticService(addr)
              .map(ds -> ds.getProxy(DynamicConfigService.class))
              .map(DynamicConfigService::getThisNode)
              .get())
          .collect(toList());

      // build an updated topology
      Cluster result = updateTopology(dest, src);

      // push the updated topology to all the addresses
      addresses.stream()
          .map(endpoint -> connections.getDiagnosticService(endpoint).get())
          .map(ds -> ds.getProxy(DynamicConfigService.class))
          .forEach(dcs -> dcs.setTopology(result));
    }
  }

  protected abstract Cluster updateTopology(Target destination, Collection<Node> sources);

  static class Target {

    // the node information from DynamicConfigService
    private final Node node;

    // the cluster topology this node has
    private final Cluster cluster;

    Target(Node node, Cluster cluster) {
      this.node = node;
      this.cluster = cluster;
      if (!cluster.containsNode(node.getNodeAddress())) {
        throw new IllegalArgumentException("Node " + node.getNodeAddress() + " not found in cluster " + cluster);
      }
    }

    Node getNode() {
      return node;
    }

    Cluster getCluster() {
      return cluster;
    }
  }
}
