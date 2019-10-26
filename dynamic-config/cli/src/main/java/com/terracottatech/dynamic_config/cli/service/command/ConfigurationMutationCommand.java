/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.Operation;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.model.validation.ClusterValidator;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.nomad.client.change.MultipleNomadChanges;
import com.terracottatech.tools.detailed.state.LogicalServerState;
import com.terracottatech.utilities.Tuple2;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static com.terracottatech.dynamic_config.model.Requirement.ALL_NODES_ONLINE;
import static com.terracottatech.dynamic_config.model.Requirement.RESTART;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public abstract class ConfigurationMutationCommand extends ConfigurationCommand {

  // TODO [DYNAMIC-CONFIG]: TDB-4710: IMPLEMENT TC-PROPERTIES CHANGE: for the tc properties we support with a config change handler dynamically, we can return false (do not need restart)
  private static final Collection<String> TC_PROPERTY_NAMES_NO_RESTART = Collections.emptyList();

  protected ConfigurationMutationCommand(Operation operation) {
    super(operation);
  }

  @Override
  public void run() {
    logger.debug("Validating the new configuration change(s) against the topology of: {}", node);

    // get the remote topology, apply the parameters, and validate that the cluster is still valid
    Cluster originalCluster = getUpcomingCluster(node);
    Cluster updatedCluster = originalCluster.clone();

    // applying the set/unset operation to the cluster in memory for validation
    for (Configuration c : configurations) {
      c.apply(updatedCluster, parameterSubstitutor);
    }
    new ClusterValidator(updatedCluster, parameterSubstitutor).validate();

    // get the current state of the nodes
    Map<InetSocketAddress, LogicalServerState> onlineNodes = findOnlineRuntimePeers(node);
    Collection<InetSocketAddress> onlineNodesAddresses = onlineNodes.keySet();
    boolean isActive = areAllNodesActivated(onlineNodesAddresses);

    if (isActive) {
      logger.debug("Validating the new configuration change(s) against the license");
      try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(node)) {
        diagnosticService.getProxy(DynamicConfigService.class).validateAgainstLicense(updatedCluster);
      }
    }

    logger.debug("New configuration change(s) can be sent");

    if (isActive) {
      // cluster is active, we need to run a nomad change and eventually a restart
      if (requiresAllNodesAlive()) {
        ensureAllNodesAlive(originalCluster, onlineNodesAddresses);
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
      try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(onlineNodesAddresses)) {
        dynamicConfigServices(diagnosticServices)
            .map(Tuple2::getT2)
            .forEach(dynamicConfigService -> dynamicConfigService.setUpcomingCluster(updatedCluster));
      }
    }

    logger.info("Command successful!\n");
  }

  /**
   * IMPORTANT NOTE:
   * - onlineNodes comes from the runtime topology
   * - cluster comes from the upcoming topology
   * So this method will also validate that if a restart is needed because a hostname/port change has been done,
   * if the hostname/port change that is pending impacts one of the active node, then we might not find the actives
   * in the stripes.
   */
  private void ensureAtLeastActivesAreOnline(Cluster cluster, Map<InetSocketAddress, LogicalServerState> onlineNodes) {
    // actives == list of current active nodes in the runtime topology
    List<InetSocketAddress> actives = onlineNodes.entrySet().stream().filter(e -> e.getValue().isActive()).map(Map.Entry::getKey).collect(toList());
    // Check for stripe count. Whether there is a pending dynamic config change or not, the stripe count is not changing.
    // The stripe count only changes in case of a runtime topology change, which is another case.
    if (cluster.getStripeCount() != actives.size()) {
      throw new IllegalStateException("Expected 1 active per stripe, but only these nodes are active: " + toString(actives));
    }
    for (int i = 0; i < cluster.getStripeCount(); i++) {
      Stripe stripe = cluster.getStripes().get(i);
      if (stripe.getNodeAddresses().stream().noneMatch(actives::contains)) {
        throw new IllegalStateException("Found no online active node for stripe " + (i + 1) + " in cluster: " + cluster
            + ". Either some nodes are shutdown, either a hostname/port change has been made and the cluster has not yet been restarted.");
      }
    }
  }

  /**
   * IMPORTANT NOTE:
   * - onlineNodes comes from the runtime topology
   * - cluster comes from the upcoming topology
   * So this method will also validate that if a restart is needed because a hostname/port change has been done,
   * if the hostname/port change that is pending impacts one of the active node, then we might not find the actives
   * in the stripes.
   */
  private void ensureAllNodesAlive(Cluster cluster, Collection<InetSocketAddress> onlineNodes) {
    if (!onlineNodes.containsAll(cluster.getNodeAddresses())) {
      throw new IllegalStateException("Not all cluster nodes are online: expected " + toString(cluster.getNodeAddresses()) + ", but only got: " + toString(onlineNodes)
          + ". Either some nodes are shutdown, either a hostname/port change has been made and the cluster has not yet been restarted.");
    }
  }

  private MultipleNomadChanges getNomadChanges(Cluster cluster) {
    // MultipleNomadChanges will apply to whole change set given by the user as an atomic operation
    return new MultipleNomadChanges(configurations.stream()
        .map(configuration -> {
          configuration.validate(operation, parameterSubstitutor);
          return SettingNomadChange.fromConfiguration(configuration, operation, cluster);
        })
        .collect(toList()));
  }

  private boolean requiresAllNodesAlive() {
    return configurations.stream().map(Configuration::getSetting).anyMatch(setting -> setting.requires(ALL_NODES_ONLINE));
  }

  private Collection<String> findSettingsRequiringRestart() {
    return configurations.stream()
        // Default behaviour is to check if we need a restart for a setting.
        // but for a tc-property where we support runtime dynamic change, we check if we do not need a restart
        .filter(config -> (config.getSetting() != Setting.TC_PROPERTIES || !TC_PROPERTY_NAMES_NO_RESTART.contains(config.getKey())) && config.getSetting().requires(RESTART))
        .map(Configuration::toString)
        .collect(toCollection(TreeSet::new));
  }
}
