/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.cli.service.nomad.NomadManager;
import com.terracottatech.dynamic_config.cli.service.restart.RestartProgress;
import com.terracottatech.dynamic_config.cli.service.restart.RestartService;
import com.terracottatech.dynamic_config.diagnostic.LicensingService;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.parsing.ConfigFileParser;
import com.terracottatech.dynamic_config.model.validation.ClusterValidator;
import com.terracottatech.dynamic_config.model.validation.LicenseValidator;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.utilities.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.terracottatech.utilities.Assertion.assertNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Parameters(commandNames = "activate", commandDescription = "Activate the cluster")
@Usage("activate <-s HOST[:PORT] | -f CONFIG-PROPERTIES-FILE> -n CLUSTER-NAME -l LICENSE-FILE")
public class ActivateCommand extends Command {
  private static final Logger LOGGER = LoggerFactory.getLogger(ActivateCommand.class);

  @Parameter(names = {"-s"}, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-f"}, description = "Config properties file", converter = PathConverter.class)
  private Path configPropertiesFile;

  @Parameter(names = {"-n"}, description = "Cluster name")
  private String clusterName;

  @Parameter(required = true, names = {"-l"}, description = "Path to license file", converter = PathConverter.class)
  private Path licenseFile;

  @Resource
  public MultiDiagnosticServiceConnectionFactory connectionFactory;

  @Resource
  public NomadManager nomadManager;

  @Resource
  public RestartService restartService;

  private MultiDiagnosticServiceConnection connection;
  private Cluster cluster;

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

    if (configPropertiesFile != null) {
      // By default the cluster name is the file name of the property file.
      // But the user can override it by specifying --cluster-name
      clusterName = ConfigFileParser.getClusterName(configPropertiesFile.toFile(), clusterName);
    }

    assertNonNull(clusterName, "clusterName must not be null");
    LOGGER.debug("Command validation successful");
  }

  @Override
  public final void run() {

    try {
      cluster = loadCluster();
      connection = connectionFactory.createConnection(cluster.getNodeAddresses());

      ensureNoActiveNode();

      cluster.setName(clusterName);
      LOGGER.debug("Setting cluster name successful");

      prepareClusterForActivation();
      LOGGER.debug("Setting nomad writable successful");

      runNomadChange();
      LOGGER.debug("Nomad change run successful");

      installLicense();
      LOGGER.info("License installation successful");

      restartNodes();

    } finally {
      if (connection != null) {
        connection.close();
        connection = null;
      }
    }

    LOGGER.info("Command successful!\n");
  }

  private Cluster loadCluster() {
    if (node != null) {
      Cluster cluster = getInMemoryTopology(node);
      ClusterValidator.validate(cluster);
      LOGGER.debug("Cluster topology validation successful");
      return cluster;
    } else {
      Cluster cluster = ConfigFileParser.parse(configPropertiesFile.toFile(), clusterName);
      LOGGER.debug("Config property file parsing and cluster topology validation successful");
      return cluster;
    }
  }

  private void restartNodes() {
    try {
      RestartProgress progress = restartService.restart(cluster);
      Map<InetSocketAddress, Tuple2<String, Throwable>> failures = progress.await();
      if (failures.isEmpty()) {
        LOGGER.info("All cluster nodes: {} came back up", cluster.getNodeAddresses());
      } else {
        throw new IllegalStateException("Some cluster nodes have failed to restart:\n - "
            + failures.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue().t1).collect(joining("\n - ")));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Restart has been interrupted");
    }
  }

  private void ensureNoActiveNode() {
    LOGGER.debug("Contacting all cluster nodes: {} to check for first run of activation command", cluster.getNodeAddresses());
    List<InetSocketAddress> activated = getTopologyServiceStream()
        .filter(t -> t.t2.isActivated())
        .map(Tuple2::getT1)
        .collect(toList());
    if (!activated.isEmpty()) {
      throw new RuntimeException("Some nodes are already activated: " + activated);
    }
  }

  private Cluster getInMemoryTopology(InetSocketAddress node) {
    try (MultiDiagnosticServiceConnection connection = connectionFactory.createConnection(node)) {
      return connection.getDiagnosticService(node).get().getProxy(TopologyService.class).getTopology();
    }
  }

  private void prepareClusterForActivation() {
    LOGGER.debug("Contacting all cluster nodes: {} to prepare for activation", cluster.getNodeAddresses());
    getTopologyServiceStream().map(Tuple2::getT2).forEach(ts -> ts.prepareActivation(cluster));
  }

  private void runNomadChange() {
    nomadManager.runChange(cluster.getNodeAddresses(), new ClusterActivationNomadChange(cluster));
  }

  private Stream<Tuple2<InetSocketAddress, TopologyService>> getTopologyServiceStream() {
    return connection.map((address, diagnosticService) -> diagnosticService.getProxy(TopologyService.class));
  }

  private void installLicense() {
    LicenseValidator.validateLicense(cluster, licenseFile.toString());
    LOGGER.debug("License validation successful");

    String validatedLicense;
    try {
      validatedLicense = Files.readAllLines(licenseFile).stream().collect(Collectors.joining(System.lineSeparator()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    LOGGER.debug("Contacting all cluster nodes: {} to install license", cluster.getNodeAddresses());
    cluster.getNodeAddresses()
        .stream()
        .map(endpoint -> connection.getDiagnosticService(endpoint).get())
        .map(ds -> ds.getProxy(LicensingService.class))
        .forEach(dcs -> dcs.installLicense(validatedLicense));
  }

}
