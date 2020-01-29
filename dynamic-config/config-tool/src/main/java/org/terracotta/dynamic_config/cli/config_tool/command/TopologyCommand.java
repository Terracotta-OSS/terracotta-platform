/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameter;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.config_tool.converter.AttachmentType;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.json.Json;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyCommand extends RemoteCommand {
  @Parameter(names = {"-t"}, description = "Determine if the sources are nodes or stripes. Default: node", converter = AttachmentType.TypeConverter.class)
  private AttachmentType attachmentType = AttachmentType.NODE;

  @Parameter(required = true, names = {"-d"}, description = "Destination stripe or cluster", converter = InetSocketAddressConverter.class)
  private InetSocketAddress destination;

  @Parameter(required = true, names = {"-s"}, description = "Source nodes or stripes", variableArity = true, converter = InetSocketAddressConverter.class)
  private List<InetSocketAddress> sources = Collections.emptyList();

  @Override
  public final void validate() {
    if (sources.isEmpty()) {
      throw new IllegalArgumentException("Missing source nodes.");
    }
    if (destination == null) {
      throw new IllegalArgumentException("Missing destination node.");
    }
    if (attachmentType == null) {
      throw new IllegalArgumentException("Missing type.");
    }
    if (sources.contains(destination)) {
      throw new IllegalArgumentException("The destination endpoint must not be listed in the source endpoints.");
    }
    validateAddress(destination);
    sources.forEach(this::validateAddress);
    // prevent any topology change if a configuration change has been made through Nomad, requiring a restart, but nodes were not restarted yet
    if (mustBeRestarted(destination)) {
      throw new IllegalStateException("Impossible to do any topology change. Cluster at address: " + destination + " is waiting to be restarted to apply some pending changes.");
    }
  }

  @Override
  public final void run() {
    // get all the nodes to update (which includes source node plus all the nodes on the destination cluster)
    Collection<InetSocketAddress> discovered = findRuntimePeers(destination);

    // create a list of addresses to connect to
    Collection<InetSocketAddress> addresses = concat(sources.stream(), discovered.stream()).collect(toCollection(LinkedHashSet::new));
    logger.debug("Connecting to nodes to apply topology changes: {}", toString(addresses));

    // create a multi-connection based on all the cluster addresses
    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(addresses)) {

      // get the target node / cluster
      NodeContext dest = diagnosticServices.getDiagnosticService(destination)
          .map(ds -> ds.getProxy(TopologyService.class))
          .map(TopologyService::getUpcomingNodeContext)
          .orElseThrow(() -> new IllegalStateException("Diagnostic service not found for " + destination));

      // get all the source node info
      List<Node> src = sources.stream()
          .map(addr -> diagnosticServices.getDiagnosticService(addr)
              .map(ds -> ds.getProxy(TopologyService.class))
              .map(TopologyService::getUpcomingNodeContext)
              .map(NodeContext::getNode)
              .orElseThrow(() -> new IllegalStateException("Diagnostic service not found for " + addr)))
          .collect(toList());

      // build an updated topology
      Cluster result = updateTopology(dest, src);

      if (logger.isDebugEnabled()) {
        logger.debug("Updated topology:{}{}.", lineSeparator(), Json.toPrettyJson(result));
      }

      // push the updated topology to all the addresses
      // If a node has been removed, then it will make itself alone on its own cluster and will have no more links to the previous nodes
      // This is done in the TopologyService#setCluster() method
      logger.info("Pushing the updated topology to all the nodes: {}.", toString(addresses));
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(ts -> ts.setUpcomingCluster(result));
    }

    logger.info("Command successful!" + lineSeparator());
  }

  /*<-- Test methods --> */
  AttachmentType getAttachmentType() {
    return attachmentType;
  }

  TopologyCommand setAttachmentType(AttachmentType attachmentType) {
    this.attachmentType = attachmentType;
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
