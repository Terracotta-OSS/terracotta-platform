/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;

import java.net.InetSocketAddress;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toList;
import static org.terracotta.dynamic_config.api.model.Requirement.ALL_NODES_ONLINE;

public abstract class ConfigurationMutationCommand extends ConfigurationCommand {

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
      c.apply(updatedCluster);
    }
    new ClusterValidator(updatedCluster).validate();

    // get the current state of the nodes
    // this call can take some time and we can have some timeout
    Map<InetSocketAddress, LogicalServerState> onlineNodes = findOnlineRuntimePeers(node);
    logger.debug("Online nodes: {}", onlineNodes);

    boolean allOnlineNodesActivated = areAllNodesActivated(onlineNodes.keySet());

    if (allOnlineNodesActivated) {
      licenseValidation(node, updatedCluster);
    }

    logger.debug("New configuration change(s) can be sent");

    if (allOnlineNodesActivated) {
      // cluster is active, we need to run a nomad change and eventually a restart

      // validate that all the online nodes are either actives or passives
      ensureNodesAreEitherActiveOrPassive(onlineNodes);

      if (requiresAllNodesAlive()) {
        // Check passive nodes as well if the setting requires all nodes to be online
        ensurePassivesAreAllOnline(originalCluster, onlineNodes);
      }

      ensureActivesAreAllOnline(originalCluster, onlineNodes);
      logger.info("Applying new configuration change(s) to activated cluster: {}", toString(onlineNodes.keySet()));
      MultiSettingNomadChange changes = getNomadChanges(updatedCluster);
      runConfigurationChange(updatedCluster, onlineNodes, changes);

      // do we need to restart to apply the changes ?
      if (mustBeRestarted(node)) {
        logger.warn(lineSeparator() +
            "====================================================================" + lineSeparator() +
            "IMPORTANT: A restart of the cluster is required to apply the changes" + lineSeparator() +
            "====================================================================" + lineSeparator() + lineSeparator());
      }

    } else {
      // cluster is not active, we just need to replace the topology
      logger.info("Applying new configuration change(s) to nodes: {}", toString(onlineNodes.keySet()));
      try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(onlineNodes.keySet())) {
        dynamicConfigServices(diagnosticServices)
            .map(Tuple2::getT2)
            .forEach(dynamicConfigService -> dynamicConfigService.setUpcomingCluster(updatedCluster));
      }
    }

    logger.info("Command successful!" + lineSeparator());
  }

  private MultiSettingNomadChange getNomadChanges(Cluster cluster) {
    // MultiSettingNomadChange will apply to whole change set given by the user as an atomic operation
    return new MultiSettingNomadChange(configurations.stream()
        .map(configuration -> {
          configuration.validate(operation);
          return SettingNomadChange.fromConfiguration(configuration, operation, cluster);
        })
        .collect(toList()));
  }

  private boolean requiresAllNodesAlive() {
    return configurations.stream().map(Configuration::getSetting).anyMatch(setting -> setting.requires(ALL_NODES_ONLINE));
  }
}
