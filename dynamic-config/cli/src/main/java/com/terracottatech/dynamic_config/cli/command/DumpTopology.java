/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.command;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.utilities.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;

@Parameters(commandDescription = "Dump the cluster topology")
public class DumpTopology extends AbstractCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(DumpTopology.class);

  @Parameter(required = true, names = {"-d"}, description = "Destination node", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Override
  public String getName() {
    return "dump-topology";
  }

  @Override
  public void validate() {
    if (node == null) {
      throw new IllegalArgumentException("Missing destination node.");
    }
  }

  @Override
  public final void process(JCommander jCommander, NodeAddressDiscovery nodeAddressDiscovery, MultiDiagnosticServiceConnectionFactory connectionFactory) {
    if (processHelp(help, jCommander)) return;

    // get all the nodes to update (which includes source node plus all the nodes on the destination cluster)
    Tuple2<InetSocketAddress, Collection<InetSocketAddress>> discovered = nodeAddressDiscovery.discover(node);

    // create a multi-connection based on all the cluster addresses plus the one to add
    try (MultiDiagnosticServiceConnection connections = connectionFactory.createConnection(discovered.t2)) {
      Cluster topology = connections.getDiagnosticService(discovered.t1)
          .map(ds -> ds.getProxy(DynamicConfigService.class))
          .get()
          .getTopology();
      LOGGER.info(topology.toString());
    }
  }

  @Override
  public String usage() {
    return getName() + " -d HOST[:PORT]";
  }
}
