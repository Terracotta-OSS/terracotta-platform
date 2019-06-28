/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.CommandRepository;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.cli.service.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.service.nomad.NomadClientFactory;
import com.terracottatech.dynamic_config.cli.service.nomad.NomadManager;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.ClusterValidator;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.dynamic_config.nomad.NomadEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

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
    LOGGER.info("Validated cluster topology");

    updateClusterAndStripeNames(cluster);
    LOGGER.info("Setting stripe and cluster name successful");

    prepareClusterForActivation(cluster);
    LOGGER.info("Setting nomad writable successful");

    runNomadChange(cluster);
    LOGGER.info("Nomad change run successful");

    updateClusterTopology(cluster);
    LOGGER.info("Cluster topology update successful");

    reconfigureTopologyEntity();
    installLicense(cluster);
    LOGGER.info("Command successful!\n");
  }

  private boolean ensureConsistentTopologies(Cluster cluster) {
    return nodeAddressDiscovery.discover(node).t2.stream().map(this::getCluster).anyMatch(c -> !c.equals(cluster));
  }

  private boolean isActivated(Cluster cluster) {
    return cluster.getStripes().stream().flatMap(stripe -> stripe.getNodes().stream()).anyMatch(node1 -> node1.getClusterName() != null);
  }

  private Cluster getCluster(InetSocketAddress node) {
    try (MultiDiagnosticServiceConnection connection = connectionFactory.createConnection(Collections.singletonList(node))) {
      return connection.getDiagnosticService(node).get().getProxy(DynamicConfigService.class).getTopology();
    }
  }

  private void updateClusterAndStripeNames(Cluster cluster) {
    AtomicInteger stripeIndex = new AtomicInteger();
    cluster.getStripes()
        .stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .forEach(node -> {
          node.setStripeName("stripe-" + stripeIndex.incrementAndGet());
          node.setClusterName(clusterName);
        });
  }

  private void prepareClusterForActivation(Cluster cluster) {
    try (MultiDiagnosticServiceConnection connection = connectionFactory.createConnection(cluster.getNodeAddresses())) {
      LOGGER.info("Contacting all cluster nodes to prepare for activation: {}.", cluster.getNodeAddresses());
      cluster.getNodeAddresses().stream()
          .map(endpoint -> connection.getDiagnosticService(endpoint).get())
          .map(ds -> ds.getProxy(DynamicConfigService.class))
          .forEach(dcs -> dcs.prepareActivation(cluster));
    }
  }

  private void runNomadChange(Cluster cluster) {
    long requestTimeout = ((MainCommand) CommandRepository.getCommand(MainCommand.NAME)).getRequestTimeoutMillis();
    NomadManager nomadManager = new NomadManager(new NomadClientFactory(connectionFactory, new ConcurrencySizing(), new NomadEnvironment(), requestTimeout));
    nomadManager.runChange(cluster.getNodeAddresses(), new ClusterActivationNomadChange(clusterName, cluster));
  }

  private void updateClusterTopology(Cluster cluster) {
    try (MultiDiagnosticServiceConnection connection = connectionFactory.createConnection(cluster.getNodeAddresses())) {
      LOGGER.info("Contacting all cluster nodes to update cluster topology: {}.", cluster.getNodeAddresses());
      cluster.getNodeAddresses().stream()
          .map(endpoint -> connection.getDiagnosticService(endpoint).get())
          .map(ds -> ds.getProxy(DynamicConfigService.class))
          .forEach(dcs -> dcs.setTopology(cluster));
    }
  }

  private void reconfigureTopologyEntity() {
    //TODO [DYNAMIC-CONFIG]: Figure out a way to reconfigure topology entity
  }

  private void installLicense(Cluster cluster) {
    LicenseManager licenseManager = new LicenseManager();
    licenseManager.validateLicense(cluster, licenseFile);
    LOGGER.info("License validated");
    //TODO [DYNAMIC-CONFIG]: Figure out a way to install license
  }
}
