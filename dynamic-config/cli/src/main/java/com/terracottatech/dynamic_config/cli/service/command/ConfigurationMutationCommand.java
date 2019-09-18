/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.dynamic_config.model.Cluster;
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
    // get the remote topology, apply the parameters, and validate that the cluster is still valid
    Cluster cluster = getRemoteTopology(node);

    // applying the set/unset operation to the cluster in memory for validation
    for (Configuration c : configurations) {
      c.apply(operation, cluster);
    }
    new ClusterValidator(cluster).validate();

    logger.info("Configuration has been validated");

    // get the current state of the nodes
    Map<InetSocketAddress, LogicalServerState> onlineNodes = findOnlineNodes(cluster);
    boolean isActive = validateActivationState(onlineNodes.keySet());

    if (isActive) {
      // cluster is active, we need to run a nomad change and eventually a restart
      if (requiresAllNodesAlive()) {
        logger.info("Applying new configuration to active cluster: {}", toString(cluster.getNodeAddresses()));
        ensureAllNodesAlive(cluster, onlineNodes);
        runNomadChange(cluster.getNodeAddresses(), getNomadChanges(cluster));
      } else {
        logger.info("Applying new configuration to active cluster: {}", toString(onlineNodes.keySet()));
        ensureAtLeastActivesAreOnline(cluster, onlineNodes);
        runNomadChange(onlineNodes.keySet(), getNomadChanges(cluster));
      }
      Collection<String> settingsRequiringRestart = findSettingsRequiringRestart();
      if (!settingsRequiringRestart.isEmpty()) {
        logger.warn("=======\nWARNING\n=======\n\nA restart of the cluster is required to apply the following changes:\n - {}\n", String.join("\n - ", settingsRequiringRestart));
      }
    } else {
      // cluster is not active, we just need to replace the topology
      logger.info("Applying new configuration to nodes: {}", toString(cluster.getNodeAddresses()));
      try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchDiagnosticServices(cluster.getNodeAddresses())) {
        topologyServices(diagnosticServices)
            .map(Tuple2::getT2)
            .forEach(topologyService -> topologyService.setCluster(cluster));
      }
    }

    logger.info("Command successful!\n");
  }

  private void ensureAtLeastActivesAreOnline(Cluster cluster, Map<InetSocketAddress, LogicalServerState> onlineNodes) {
    List<InetSocketAddress> actives = onlineNodes.entrySet().stream().filter(e -> e.getValue().isActive()).map(Map.Entry::getKey).collect(toList());
    if (cluster.getStripes().size() != actives.size()) {
      throw new IllegalStateException("Expected 1 active per stripe, but only this nodes are active: " + toString(actives));
    }
    for (int i = 0; i < cluster.getStripes().size(); i++) {
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
    return new MultipleNomadChanges(configurations.stream().map(configuration -> configuration.toSettingNomadChange(operation, cluster)).collect(toList()));
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
