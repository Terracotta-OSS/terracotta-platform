/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;


import com.beust.jcommander.Parameter;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.TypeConverter;
import com.terracottatech.dynamic_config.cli.service.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.utilities.Json;
import com.terracottatech.utilities.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyChangeCommand extends Command {

  public enum Type {

    NODE,
    STRIPE;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  @Resource public NodeAddressDiscovery nodeAddressDiscovery;
  @Resource public MultiDiagnosticServiceConnectionFactory connectionFactory;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Parameter(names = {"-t"}, description = "Type of attachment or detachment (default: node)", converter = TypeConverter.class)
  private Type type = Type.NODE;

  @Parameter(required = true, names = {"-d"}, description = "Node of the destination stripe or cluster", converter = InetSocketAddressConverter.class)
  private InetSocketAddress destination;

  @Parameter(required = true, names = {"-s"}, description = "Selected nodes", variableArity = true, converter = InetSocketAddressConverter.class)
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
    logger.debug("Validating parameter commands...");
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
    logger.debug("Command validation successful.");
  }

  @Override
  public final void run() {
    logger.debug("Discovering destination cluster nodes to update by using node {}...", destination);

    // get all the nodes to update (which includes source node plus all the nodes on the destination cluster)
    Tuple2<InetSocketAddress, Collection<InetSocketAddress>> discovered = nodeAddressDiscovery.discover(destination);
    if (logger.isDebugEnabled()) {
      logger.debug("Discovered nodes {} through {}.", nodeAddresses(discovered.t2), discovered.t1);
    }

    // create a list of addresses to connect to
    Collection<InetSocketAddress> addresses = concat(discovered.t2.stream(), sources.stream()).collect(toSet());
    if (logger.isDebugEnabled()) {
      logger.debug("Connecting to nodes: {}...", nodeAddresses(addresses));
    }

    Cluster result;

    // create a multi-connection based on all the cluster addresses
    try (MultiDiagnosticServiceConnection connections = connectionFactory.createConnection(addresses)) {

      // get the target node / cluster
      Target dest = connections.getDiagnosticService(discovered.t1)
          .map(ds -> ds.getProxy(DynamicConfigService.class))
          .map(dcs -> new Target(discovered.t1, dcs.getTopology()))
          .orElseThrow(() -> new IllegalStateException("Diagnostic service not found for " + destination));

      // get all the source node info
      Collection<Node> src = sources.stream()
          .map(addr -> connections.getDiagnosticService(addr)
              .map(ds -> ds.getProxy(DynamicConfigService.class))
              .map(DynamicConfigService::getThisNode)
              .orElseThrow(() -> new IllegalStateException("Diagnostic service not found for " + addr)))
          .collect(toList());

      // build an updated topology
      result = updateTopology(dest, src);

      if (logger.isDebugEnabled()) {
        logger.debug("Updated topology:\n{}.", Json.toPrettyJson(result));
      }
    }

    // apply the changes only to the nodes remaining on the cluster
    try (MultiDiagnosticServiceConnection connections = connectionFactory.createConnection(result.getNodeAddresses())) {
      logger.info("Pushing the updated topology to all the cluster nodes: {}.", nodeAddresses(result.getNodeAddresses()));

      // push the updated topology to all the addresses
      result.getNodeAddresses().stream()
          .map(endpoint -> connections.getDiagnosticService(endpoint).get())
          .map(ds -> ds.getProxy(DynamicConfigService.class))
          .forEach(dcs -> dcs.setTopology(result));
    }
    logger.info("Command successful!\n");
  }

  protected abstract Cluster updateTopology(Target destination, Collection<Node> sources);

  private String nodeAddresses(Collection<InetSocketAddress> addresses) {
    return addresses.stream().map(InetSocketAddress::toString).collect(Collectors.joining(", "));
  }

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
