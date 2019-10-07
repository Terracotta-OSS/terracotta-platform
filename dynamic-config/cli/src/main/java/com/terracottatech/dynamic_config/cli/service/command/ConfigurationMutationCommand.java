/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.Operation;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.model.validation.ClusterValidator;
import com.terracottatech.nomad.client.change.MultipleNomadChanges;
import com.terracottatech.tools.detailed.state.LogicalServerState;
import com.terracottatech.utilities.Tuple2;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.terracottatech.dynamic_config.model.Requirement.ALL_NODES_ONLINE;
import static com.terracottatech.dynamic_config.model.Requirement.RESTART;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public abstract class ConfigurationMutationCommand extends ConfigurationCommand {

  protected ConfigurationMutationCommand(Operation operation) {
    super(operation);
  }

  @Override
  public void run() {
    logger.debug("Validating the new configuration change(s) against the topology of: {}", node);

    // get the remote topology, apply the parameters, and validate that the cluster is still valid
    Cluster originalCluster = getRemoteTopology(node);
    Cluster updatedCluster = originalCluster.clone();

    // applying the set/unset operation to the cluster in memory for validation
    for (Configuration c : configurations) {
      c.apply(operation, updatedCluster, parameterSubstitutor);
    }
    new ClusterValidator(updatedCluster, parameterSubstitutor).validate();

    // get the current state of the nodes
    Map<InetSocketAddress, LogicalServerState> onlineNodes = findOnlineNodes(originalCluster);
    final Set<InetSocketAddress> onlineNodesAddresses = onlineNodes.keySet();
    boolean isActive = validateActivationState(onlineNodesAddresses);

    if (isActive) {
      logger.debug("Validating the new configuration change(s) against the license");
      try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(node)) {
        diagnosticService.getProxy(TopologyService.class).validateAgainstLicense(updatedCluster);
      }
    }

    logger.debug("New configuration change(s) can be sent");

    if (isActive) {
      // cluster is active, we need to run a nomad change and eventually a restart
      if (requiresAllNodesAlive()) {
        ensureAllNodesAlive(originalCluster, onlineNodes);
        logger.info("Applying new configuration change(s) to activated cluster: {}", toString(onlineNodesAddresses));
        runNomadChange(onlineNodesAddresses, getNomadChanges(updatedCluster));
      } else {
        ensureAtLeastActivesAreOnline(originalCluster, onlineNodes);
        logger.info("Applying new configuration change(s) to activated nodes: {}", toString(onlineNodesAddresses));
        runNomadChange(onlineNodesAddresses, getNomadChanges(updatedCluster));
      }
      Collection<String> settingsRequiringRestart = findSettingsRequiringRestart();
      if (!settingsRequiringRestart.isEmpty()) {
        logger.info("=========\nIMPORTANT\n=========\n\nA restart of the cluster is required to apply the following changes:" +
            "\n - {}\n", String.join("\n - ", settingsRequiringRestart));
      }
    } else {
      // cluster is not active, we just need to replace the topology
      logger.info("Applying new configuration change(s) to nodes: {}", toString(onlineNodesAddresses));
      try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchDiagnosticServices(onlineNodesAddresses)) {
        topologyServices(diagnosticServices)
            .map(Tuple2::getT2)
            .forEach(topologyService -> topologyService.setCluster(updatedCluster));
      }
    }

    logger.info("Command successful!\n");
  }

  private void ensureAtLeastActivesAreOnline(Cluster cluster, Map<InetSocketAddress, LogicalServerState> onlineNodes) {
    List<InetSocketAddress> actives = onlineNodes.entrySet().stream().filter(e -> e.getValue().isActive()).map(Map.Entry::getKey).collect(toList());
    if (cluster.getStripeCount() != actives.size()) {
      throw new IllegalStateException("Expected 1 active per stripe, but only this nodes are active: " + toString(actives));
    }
    for (int i = 0; i < cluster.getStripeCount(); i++) {
      Stripe stripe = cluster.getStripes().get(i);
      if (stripe.getNodeAddresses().stream().noneMatch(actives::contains)) {
        throw new IllegalStateException("Found no online active node for stripe " + (i + 1) + " in cluster: " + cluster);
      }
    }
  }

  private void ensureAllNodesAlive(Cluster cluster, Map<InetSocketAddress, LogicalServerState> onlineNodes) {
    if (!onlineNodes.keySet().containsAll(cluster.getNodeAddresses())) {
      throw new IllegalStateException("Not all cluster nodes are online: expected " + toString(cluster.getNodeAddresses()) + ", but only got: " + toString(onlineNodes.keySet()));
    }
  }

  private MultipleNomadChanges getNomadChanges(Cluster cluster) {
    // MultipleNomadChanges will apply to whole change set given by the user as an atomic operation
    return new MultipleNomadChanges(configurations.stream().map(configuration -> configuration.toSettingNomadChange(operation, cluster, parameterSubstitutor)).collect(toList()));
  }

  private boolean requiresAllNodesAlive() {
    return configurations.stream().map(Configuration::getSetting).anyMatch(setting -> setting.requires(ALL_NODES_ONLINE));
  }

  private Collection<String> findSettingsRequiringRestart() {
    return configurations.stream()
        .filter(config -> config.getSetting().requires(RESTART))
        .map(Configuration::toString)
        .collect(toCollection(TreeSet::new));
  }
}
