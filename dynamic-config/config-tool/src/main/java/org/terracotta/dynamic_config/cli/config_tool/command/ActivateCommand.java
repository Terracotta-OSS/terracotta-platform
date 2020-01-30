/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.System.lineSeparator;

@Parameters(commandNames = "activate", commandDescription = "Activate a cluster")
@Usage("activate ( -s <hostname[:port]> -n <cluster-name> | -f <config-file> [-n <cluster-name>] ) [-l <license-file>] [-rwt]")
public class ActivateCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-f"}, description = "Configuration file", converter = PathConverter.class)
  private Path configPropertiesFile;

  @Parameter(names = {"-n"}, description = "Cluster name")
  private String clusterName;

  @Parameter(names = {"-l"}, description = "License file", converter = PathConverter.class)
  private Path licenseFile;

  @Parameter(names = {"-rwt", "--restart-wait-time"}, description = "Maximum time to wait for the nodes to restart. Default: 60s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> restartWaitTime = Measure.of(60, TimeUnit.SECONDS);

  private Cluster cluster;
  private Collection<InetSocketAddress> runtimePeers;

  @Override
  public void validate() {
    if (node == null && configPropertiesFile == null) {
      throw new IllegalArgumentException("One of node or config properties file must be specified");
    }

    if (node != null && configPropertiesFile != null) {
      throw new IllegalArgumentException("Either node or config properties file should be specified, not both");
    }

    if (node != null && clusterName == null) {
      throw new IllegalArgumentException("Cluster name should be provided when node is specified");
    }

    if (node != null) {
      validateAddress(node);
    }

    if (licenseFile != null && !Files.exists(licenseFile)) {
      throw new ParameterException("License file not found: " + licenseFile);
    }

    cluster = loadCluster();
    runtimePeers = node == null ? cluster.getNodeAddresses() : findRuntimePeers(node);

    // check if we want to override the cluster name
    if (clusterName != null) {
      cluster.setName(clusterName);
    }

    if (cluster.getName() == null) {
      throw new IllegalArgumentException("Cluster name is missing");
    }

    // validate the topology
    new ClusterValidator(cluster).validate();

    // verify the activated state of the nodes
    if (areAllNodesActivated(runtimePeers)) {
      throw new IllegalStateException("Cluster is already activated");
    }
  }

  @Override
  public final void run() {
    logger.info("Activating cluster: {} formed with nodes: {}", cluster.getName(), toString(runtimePeers));

    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(runtimePeers)) {
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(service -> service.prepareActivation(cluster, read(licenseFile)));
      if (licenseFile == null) {
        logger.info("No license installed");
      } else {
        logger.info("License installation successful");
      }
    }

    runNomadChange(new ArrayList<>(runtimePeers), new ClusterActivationNomadChange(cluster));
    logger.debug("Configuration repositories have been created for all nodes");

    logger.info("Restarting nodes: {}", toString(runtimePeers));
    restartNodes(runtimePeers, Duration.ofMillis(restartWaitTime.getQuantity(TimeUnit.MILLISECONDS)));
    logger.info("All nodes came back up");

    logger.info("Command successful!" + lineSeparator());
  }

  /*<-- Test methods --> */
  Cluster getCluster() {
    return cluster;
  }

  ActivateCommand setNode(InetSocketAddress node) {
    this.node = node;
    return this;
  }

  ActivateCommand setConfigPropertiesFile(Path configPropertiesFile) {
    this.configPropertiesFile = configPropertiesFile;
    return this;
  }

  ActivateCommand setClusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  ActivateCommand setLicenseFile(Path licenseFile) {
    this.licenseFile = licenseFile;
    return this;
  }

  private Cluster loadCluster() {
    Cluster cluster;
    if (node != null) {
      cluster = getUpcomingCluster(node);
      logger.debug("Cluster topology validation successful");
    } else {
      ClusterFactory clusterCreator = new ClusterFactory();
      cluster = clusterCreator.create(configPropertiesFile);
      logger.debug("Config property file parsed and cluster topology validation successful");
    }
    return cluster;
  }

  private static String read(Path path) {
    if (path == null) {
      return null;
    }
    try {
      return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
