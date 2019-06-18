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
import com.terracottatech.utilities.Tuple2;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

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

  public TopologyChangeCommand setType(Type type) {
    this.type = type;
    return this;
  }

  public InetSocketAddress getDestination() {
    return destination;
  }

  public TopologyChangeCommand setDestination(InetSocketAddress destination) {
    this.destination = destination;
    return this;
  }

  public TopologyChangeCommand setDestination(String host, int port) {
    return setDestination(InetSocketAddress.createUnresolved(host, port));
  }

  public List<InetSocketAddress> getSources() {
    return sources;
  }

  public TopologyChangeCommand setSources(List<InetSocketAddress> sources) {
    this.sources = sources;
    return this;
  }

  public TopologyChangeCommand setSources(InetSocketAddress... sources) {
    setSources(Arrays.asList(sources));
    return this;
  }

  public TopologyChangeCommand setSource(InetSocketAddress source) {
    return setSources(source);
  }

  public TopologyChangeCommand setSource(String host, int port) {
    return setSource(InetSocketAddress.createUnresolved(host, port));
  }

  @Override
  public final void validate() {
    if (sources.isEmpty()) {
      throw new IllegalArgumentException("Missing source nodes.");
    }
    if (destination == null) {
      throw new IllegalArgumentException("Missing destination node.");
    }
    if (type == null) {
      throw new IllegalArgumentException("Missing type.");
    }
    if (sources.contains(destination)) {
      throw new IllegalArgumentException("The destination endpoint must not be listed in the source endpoints.");
    }
  }

  @Override
  public final void run() {
    // get all the nodes to update (which includes source node plus all the nodes on the destination cluster)
    Tuple2<InetSocketAddress, Collection<InetSocketAddress>> discovered = nodeAddressDiscovery.discover(destination);

    // replaces the destination address used by the user by the real configured one
    InetSocketAddress destination = discovered.t1;

    // create a list of addresses to connect to
    Collection<InetSocketAddress> addresses = concat(discovered.t2.stream(), sources.stream()).collect(toSet());

    // create a multi-connection based on all the cluster addresses plus the one to add
    try (MultiDiagnosticServiceConnection connections = connectionFactory.createConnection(addresses)) {

      // get the target node / cluster
      Target dest = connections.getDiagnosticService(destination)
          .map(ds -> ds.getProxy(DynamicConfigService.class))
          .map(dcs -> new Target(destination, dcs.getTopology()))
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

    // the node address from DynamicConfigService
    private final InetSocketAddress nodeAddress;

    // the cluster topology this node has
    private final Cluster cluster;

    Target(InetSocketAddress nodeAddress, Cluster cluster) {
      this.nodeAddress = nodeAddress;
      this.cluster = cluster;
      if (!cluster.containsNode(nodeAddress)) {
        throw new IllegalArgumentException("Node " + nodeAddress + " not found in cluster " + cluster);
      }
    }

    InetSocketAddress getConfiguredNodeAddress() {
      return nodeAddress;
    }

    Cluster getCluster() {
      return cluster;
    }
  }
}
