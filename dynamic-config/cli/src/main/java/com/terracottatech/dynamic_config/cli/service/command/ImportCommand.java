/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.ClusterFactory;
import com.terracottatech.dynamic_config.model.ClusterValidator;
import com.terracottatech.struct.tuple.Tuple2;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;

import static java.lang.System.lineSeparator;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "import", commandDescription = "Import a cluster configuration")
@Usage("import -f <config-file>")
public class ImportCommand extends RemoteCommand {

  @Parameter(names = {"-f"}, description = "Configuration file", required = true, converter = PathConverter.class)
  private Path configPropertiesFile;

  private Cluster cluster;
  private Collection<InetSocketAddress> runtimePeers;

  @Override
  public void validate() {
    cluster = loadCluster();
    runtimePeers = cluster.getNodeAddresses();

    // validate the topology
    new ClusterValidator(cluster).validate();

    // verify the activated state of the nodes
    boolean isClusterActive = areAllNodesActivated(runtimePeers);
    if (isClusterActive) {
      throw new IllegalStateException("Cluster is already activated");
    }
  }

  @Override
  public final void run() {
    logger.info("Importing cluster configuration from: {}", configPropertiesFile);

    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(runtimePeers)) {
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(service -> service.setUpcomingCluster(cluster));
    }

    logger.info("Command successful!" + lineSeparator());
  }

  private Cluster loadCluster() {
    ClusterFactory clusterCreator = new ClusterFactory();
    Cluster cluster = clusterCreator.create(configPropertiesFile);
    logger.debug("Config property file parsed and cluster topology validation successful");
    return cluster;
  }
}
