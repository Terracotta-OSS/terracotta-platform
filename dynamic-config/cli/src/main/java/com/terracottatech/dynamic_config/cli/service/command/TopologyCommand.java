/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;


import com.beust.jcommander.Parameter;
import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.TypeConverter;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.utilities.Json;
import com.terracottatech.utilities.Tuple2;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyCommand extends RemoteCommand {

  public enum Type {

    NODE,
    STRIPE;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  @Parameter(names = {"-t"}, description = "Type of attachment or detachment (default: node)", converter = TypeConverter.class)
  private Type type = Type.NODE;

  @Parameter(required = true, names = {"-d"}, description = "Node of the destination stripe or cluster", converter = InetSocketAddressConverter.class)
  private InetSocketAddress destination;

  @Parameter(required = true, names = {"-s"}, description = "Selected nodes", variableArity = true, converter = InetSocketAddressConverter.class)
  private List<InetSocketAddress> sources = Collections.emptyList();

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
    verifyAddress(destination);
    sources.forEach(this::verifyAddress);
  }

  @Override
  public final void run() {
    // get all the nodes to update (which includes source node plus all the nodes on the destination cluster)
    Collection<InetSocketAddress> discovered = getPeers(destination);

    // create a list of addresses to connect to
    Collection<InetSocketAddress> addresses = concat(sources.stream(), discovered.stream()).collect(toCollection(LinkedHashSet::new));
    logger.debug("Connecting to nodes to apply topology changes: {}", toString(addresses));

    // create a multi-connection based on all the cluster addresses
    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchDiagnosticServices(addresses)) {

      // get the target node / cluster
      NodeContext dest = diagnosticServices.getDiagnosticService(destination)
          .map(ds -> ds.getProxy(TopologyService.class))
          .map(TopologyService::getThisNodeContext)
          .orElseThrow(() -> new IllegalStateException("Diagnostic service not found for " + destination));

      // get all the source node info
      List<Node> src = sources.stream()
          .map(addr -> diagnosticServices.getDiagnosticService(addr)
              .map(ds -> ds.getProxy(TopologyService.class))
              .map(TopologyService::getThisNode)
              .orElseThrow(() -> new IllegalStateException("Diagnostic service not found for " + addr)))
          .collect(toList());

      // build an updated topology
      Cluster result = updateTopology(dest, src);

      if (logger.isDebugEnabled()) {
        logger.debug("Updated topology:\n{}.", Json.toPrettyJson(result));
      }

      // push the updated topology to all the addresses
      // If a node has been removed, then it will make itself alone on its own cluster and will have no more links to the previous nodes
      // This is done in the TopologyService#setCluster() method
      logger.info("Pushing the updated topology to all the nodes: {}.", toString(addresses));
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(ts -> ts.setCluster(result));
    }

    logger.info("Command successful!\n");
  }

  /*<-- Test methods --> */
  Type getType() {
    return type;
  }

  TopologyCommand setType(Type type) {
    this.type = type;
    return this;
  }

  InetSocketAddress getDestination() {
    return destination;
  }

  TopologyCommand setDestination(InetSocketAddress destination) {
    this.destination = destination;
    return this;
  }

  TopologyCommand setDestination(String host, int port) {
    return setDestination(InetSocketAddress.createUnresolved(host, port));
  }

  List<InetSocketAddress> getSources() {
    return sources;
  }

  TopologyCommand setSources(List<InetSocketAddress> sources) {
    this.sources = sources;
    return this;
  }

  TopologyCommand setSources(InetSocketAddress... sources) {
    setSources(Arrays.asList(sources));
    return this;
  }

  protected abstract Cluster updateTopology(NodeContext destination, List<Node> sources);
}
