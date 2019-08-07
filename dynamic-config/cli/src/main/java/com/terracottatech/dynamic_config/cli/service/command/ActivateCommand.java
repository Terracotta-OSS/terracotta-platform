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
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.config.ConfigFileContainer;
import com.terracottatech.dynamic_config.util.PropertiesFileLoader;
import com.terracottatech.dynamic_config.model.validation.ClusterValidator;
import com.terracottatech.dynamic_config.model.validation.ConfigFileValidator;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.nomad.client.results.NomadFailureRecorder;
import com.terracottatech.utilities.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Stream;

import static com.terracottatech.utilities.Assertion.assertNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

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
  public NomadManager<String> nomadManager;

  @Resource
  public RestartService restartService;

  private MultiDiagnosticServiceConnection connection;
  private ConfigFileContainer configFileContainer;
  private Cluster cluster;

  public Cluster getCluster() {
    return cluster;
  }

  public ActivateCommand setNode(InetSocketAddress node) {
    this.node = node;
    return this;
  }

  public ActivateCommand setConfigPropertiesFile(Path configPropertiesFile) {
    this.configPropertiesFile = configPropertiesFile;
    return this;
  }

  public ActivateCommand setClusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  public ActivateCommand setLicenseFile(Path licenseFile) {
    this.licenseFile = licenseFile;
    return this;
  }

  public InetSocketAddress getNode() {
    return node;
  }

  public Path getConfigPropertiesFile() {
    return configPropertiesFile;
  }

  public String getClusterName() {
    return clusterName;
  }

  public Path getLicenseFile() {
    return licenseFile;
  }

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
      String fileName = configPropertiesFile.toFile().getName(); //Path::getFileName can return null, which trips spotBugs
      Properties properties = new PropertiesFileLoader(configPropertiesFile).loadProperties();
      ConfigFileValidator configFileValidator = new ConfigFileValidator(fileName, properties);
      configFileValidator.validate();
      configFileContainer = new ConfigFileContainer(fileName, properties, clusterName);
      clusterName = configFileContainer.getClusterName();
    }

    assertNonNull(licenseFile, "licenseFile must not be null");
    assertNonNull(clusterName, "clusterName must not be null");
    assertNonNull(connectionFactory, "connectionFactory must not be null");
    assertNonNull(nomadManager, "nomadManager must not be null");
    assertNonNull(restartService, "restartService must not be null");
    LOGGER.debug("Command validation successful");
  }

  @Override
  public final void run() {
    try {
      cluster = loadCluster();
      connection = connectionFactory.createConnection(cluster.getNodeAddresses());

      ensureNoActivatedNode();

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
    Cluster cluster;
    if (node != null) {
      cluster = getValidatedInMemoryTopology(node);
      LOGGER.debug("Cluster topology validation successful");
    } else {
      cluster = configFileContainer.createCluster();
      LOGGER.debug("Config property file parsing and cluster topology validation successful");
    }
    return cluster;
  }

  private void restartNodes() {
    try {
      RestartProgress progress = restartService.restart(cluster);
      Map<InetSocketAddress, Tuple2<String, Exception>> failures = progress.await();
      if (failures.isEmpty()) {
        LOGGER.info("All cluster nodes: {} came back up", cluster.getNodeAddresses());
      } else {
        String failedNodes = failures.entrySet()
            .stream()
            .map(e -> e.getKey() + ": " + e.getValue().t1)
            .collect(joining("\n - "));
        throw new IllegalStateException("Some cluster nodes failed to restart:\n - " + failedNodes);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Restart has been interrupted");
    }
  }

  private void ensureNoActivatedNode() {
    LOGGER.debug("Contacting all cluster nodes: {} to check for first run of activation command", cluster.getNodeAddresses());
    Collection<String> activated = getTopologyServiceStream()
        .filter(t -> t.t2.isActivated())
        .map(Tuple2::getT1)
        .map(InetSocketAddress::toString)
        .collect(toCollection(TreeSet::new));
    if (!activated.isEmpty()) {
      throw new IllegalStateException("Some nodes are already activated: " + String.join(", ", activated));
    }
  }

  private Cluster getValidatedInMemoryTopology(InetSocketAddress node) {
    try (MultiDiagnosticServiceConnection connection = connectionFactory.createConnection(node)) {
      Cluster cluster = connection.getDiagnosticService(node).get().getProxy(TopologyService.class).getCluster();
      new ClusterValidator(cluster).validate();
      return cluster;
    }
  }

  private void prepareClusterForActivation() {
    LOGGER.debug("Contacting all cluster nodes: {} to prepare for activation", cluster.getNodeAddresses());
    getTopologyServiceStream().map(Tuple2::getT2).forEach(ts -> ts.prepareActivation(cluster));
  }

  private void runNomadChange() {
    NomadFailureRecorder<String> failures = new NomadFailureRecorder<>();
    nomadManager.runChange(cluster.getNodeAddresses(), new ClusterActivationNomadChange(cluster), failures);
    failures.reThrow();
  }

  private Stream<Tuple2<InetSocketAddress, TopologyService>> getTopologyServiceStream() {
    return connection.map((address, diagnosticService) -> diagnosticService.getProxy(TopologyService.class));
  }

  private void installLicense() {
    LOGGER.debug("Reading license");
    String xml;
    try {
      xml = new String(Files.readAllBytes(licenseFile), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    LOGGER.debug("Contacting all cluster nodes: {} to install license", cluster.getNodeAddresses());
    getTopologyServiceStream()
        .map(tuple -> {
          try {
            tuple.t2.installLicense(xml);
            return null;
          } catch (RuntimeException e) {
            LOGGER.warn("License installation failed on node {}: {}", tuple.t1, e.getMessage());
            return e;
          }
        })
        .filter(Objects::nonNull)
        .reduce((result, element) -> {
          result.addSuppressed(element);
          return result;
        })
        .ifPresent(e -> {
          throw e;
        });
  }

}
