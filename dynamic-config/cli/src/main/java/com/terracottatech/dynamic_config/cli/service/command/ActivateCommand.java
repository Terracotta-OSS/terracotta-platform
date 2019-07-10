/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.terracottatech.diagnostic.client.DiagnosticOperationTimeoutException;
import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.CommandRepository;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.cli.service.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.service.nomad.NomadClientFactory;
import com.terracottatech.dynamic_config.cli.service.nomad.NomadManager;
import com.terracottatech.dynamic_config.diagnostic.LicensingService;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.ClusterValidator;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.dynamic_config.nomad.NomadEnvironment;
import com.terracottatech.tools.detailed.state.LogicalServerState;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Parameters(commandNames = "activate", commandDescription = "Activate the cluster")
@Usage("activate -s HOST[:PORT] -n CLUSTER-NAME -l LICENSE-FILE")
public class ActivateCommand extends Command {
  private static final Logger LOGGER = LoggerFactory.getLogger(ActivateCommand.class);

  @Parameter(required = true, names = {"-s"}, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(required = true, names = {"-n"}, description = "Cluster name")
  private String clusterName;

  @Parameter(required = true, names = {"-l"}, description = "Path to license file")
  private String licenseFile;

  @Resource
  public MultiDiagnosticServiceConnectionFactory connectionFactory;

  @Resource
  public NodeAddressDiscovery nodeAddressDiscovery;

  private MultiDiagnosticServiceConnection connection;

  @Override
  public void validate() {
  }

  @Override
  public final void run() {
    Cluster cluster = getCluster(node);
    if (ensureConsistentTopologies(cluster)) {
      throw new RuntimeException("Topology received from the provided node: " + node + " is not the same as the constituent nodes in the topology");
    }

    if (isActivated(cluster)) {
      throw new RuntimeException("Cluster is already activated");
    }

    ClusterValidator.validate(cluster);
    LOGGER.debug("Cluster topology validation successful");

    updateClusterAndStripeNames(cluster);
    LOGGER.debug("Setting stripe and cluster name successful");

    prepareClusterForActivation(cluster);
    LOGGER.debug("Setting nomad writable successful");

    runNomadChange(cluster);
    LOGGER.debug("Nomad change run successful");

    updateClusterTopology(cluster);
    LOGGER.debug("Cluster topology update successful");

    installLicense(cluster);
    LOGGER.info("License installation successful");

    restartNodes(cluster);
    ensureSuccessfulRestart(cluster);
    LOGGER.info("All cluster nodes: {} came back up as Actives or Passives", cluster.getNodeAddresses());

    closeConnection();
    LOGGER.info("Command successful!\n");
  }

  private void ensureSuccessfulRestart(Cluster cluster) {
    LOGGER.debug("Contacting all cluster nodes: {} for status post restart", cluster.getNodeAddresses());

    Duration maxDuration = new Duration(CommandRepository.getMainCommand().getRequestTimeoutMillis(), TimeUnit.MILLISECONDS);
    try {
      Awaitility.await()
          .pollInterval(Duration.ONE_SECOND)
          .atMost(maxDuration)
          .until(isClusterStartupSuccessful(cluster));
    } catch (ConditionTimeoutException e) {
      throw new RuntimeException(
          String.format(
              "All nodes in cluster: %s did not come back up as Actives or Passives in: %s%s",
              cluster.getNodeAddresses(),
              maxDuration.getValue(),
              maxDuration.getTimeUnit()
          )
      );
    }
  }

  private Callable<Boolean> isClusterStartupSuccessful(Cluster cluster) {
    return () -> cluster.getNodeAddresses().stream()
        .map(endpoint -> createOrGetConnection(cluster).getDiagnosticService(endpoint).get())
        .map(DiagnosticService::getLogicalServerState)
        .allMatch(state -> state == LogicalServerState.ACTIVE || state == LogicalServerState.PASSIVE);
  }

  private void restartNodes(Cluster cluster) {
    LOGGER.debug("Asking all cluster nodes: {} to restart themselves", cluster.getNodeAddresses());
    getTopologyServiceStream(cluster).forEach(topologyService -> {
      try {
        topologyService.restart();
      } catch (DiagnosticOperationTimeoutException e) {
        // This operation times out because the nodes have shut down. All good.
        closeConnection();
      }
    });
  }

  private boolean ensureConsistentTopologies(Cluster cluster) {
    return nodeAddressDiscovery.discover(node).t2.stream().map(this::getCluster).anyMatch(c -> !c.equals(cluster));
  }

  private boolean isActivated(Cluster cluster) {
    return cluster.getStripes().stream().flatMap(stripe -> stripe.getNodes().stream()).anyMatch(node1 -> node1.getClusterName() != null);
  }

  private Cluster getCluster(InetSocketAddress node) {
    try (MultiDiagnosticServiceConnection connection = connectionFactory.createConnection(Collections.singletonList(node))) {
      return connection.getDiagnosticService(node).get().getProxy(TopologyService.class).getTopology();
    }
  }

  private void updateClusterAndStripeNames(Cluster cluster) {
    AtomicInteger stripeIndex = new AtomicInteger();
    cluster.getStripes()
        .stream()
        .flatMap(stripe -> {
          stripeIndex.incrementAndGet();
          return stripe.getNodes().stream();
        })
        .forEach(node -> {
          node.setStripeName("stripe-" + stripeIndex.get());
          node.setClusterName(clusterName);
        });
  }

  private void prepareClusterForActivation(Cluster cluster) {
    LOGGER.debug("Contacting all cluster nodes: {} to prepare for activation", cluster.getNodeAddresses());
    getTopologyServiceStream(cluster).forEach(dcs -> dcs.prepareActivation(cluster));
  }

  private void runNomadChange(Cluster cluster) {
    MainCommand mainCommand = CommandRepository.getMainCommand();
    long requestTimeout = mainCommand.getRequestTimeoutMillis();
    boolean isVerbose = mainCommand.isVerbose();

    NomadManager nomadManager = new NomadManager(new NomadClientFactory(connectionFactory, new ConcurrencySizing(), new NomadEnvironment(), requestTimeout));
    nomadManager.runChange(cluster.getNodeAddresses(), new ClusterActivationNomadChange(clusterName, cluster), isVerbose);
  }

  private void updateClusterTopology(Cluster cluster) {
    LOGGER.debug("Contacting all cluster nodes: {} to update cluster topology", cluster.getNodeAddresses());
    getTopologyServiceStream(cluster).forEach(dcs -> dcs.setTopology(cluster));
  }

  private Stream<TopologyService> getTopologyServiceStream(Cluster cluster) {
    return cluster.getNodeAddresses().stream()
        .map(endpoint -> createOrGetConnection(cluster).getDiagnosticService(endpoint).get())
        .map(ds -> ds.getProxy(TopologyService.class));
  }

  private void installLicense(Cluster cluster) {
    LicenseManager licenseManager = new LicenseManager();
    licenseManager.validateLicense(cluster, licenseFile);
    LOGGER.debug("License validation successful");

    String validatedLicense;
    try {
      validatedLicense = Files.readAllLines(Paths.get(licenseFile)).stream().collect(Collectors.joining(System.lineSeparator()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    LOGGER.debug("Contacting all cluster nodes: {} to install license", cluster.getNodeAddresses());
    cluster.getNodeAddresses().stream()
        .map(endpoint -> createOrGetConnection(cluster).getDiagnosticService(endpoint).get())
        .map(ds -> ds.getProxy(LicensingService.class))
        .forEach(dcs -> dcs.installLicense(validatedLicense));
  }

  private MultiDiagnosticServiceConnection createOrGetConnection(Cluster cluster) {
    if (connection == null) {
      connection = connectionFactory.createConnection(cluster.getNodeAddresses());
    }
    return connection;
  }

  private void closeConnection() {
    connection.close();
    connection = null;
  }
}
