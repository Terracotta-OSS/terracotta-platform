/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.cli.api.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.api.model.PropertyHolder;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.inet.HostPort;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import static java.lang.System.lineSeparator;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE_RECONNECTING;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE_RELAY;
import static org.terracotta.diagnostic.model.LogicalServerState.UNREACHABLE;
import static org.terracotta.dynamic_config.api.model.ClusterState.ACTIVATED;
import static org.terracotta.dynamic_config.api.model.ClusterState.CONFIGURING;
import static org.terracotta.dynamic_config.api.model.Requirement.CLUSTER_ONLINE;
import static org.terracotta.dynamic_config.api.model.Requirement.CLUSTER_RESTART;
import static org.terracotta.dynamic_config.api.model.Requirement.NODE_RESTART;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;

public abstract class ConfigurationMutationAction extends ConfigurationAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationMutationAction.class);

  protected boolean autoRestart;
  protected boolean force;
  protected Measure<TimeUnit> restartWaitTime = Measure.of(120, TimeUnit.SECONDS);
  protected Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  protected ConfigurationMutationAction(Operation operation) {
    super(operation);
  }

  public void setAutoRestart(boolean autoRestart) {
    this.autoRestart = autoRestart;
  }

  public void setRestartWaitTime(Measure<TimeUnit> restartWaitTime) {
    this.restartWaitTime = restartWaitTime;
  }

  public void setRestartDelay(Measure<TimeUnit> restartDelay) {
    this.restartDelay = restartDelay;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  /*@Override
  public void validate() {
    super.validate();

    // If cluster is activated then do following checks
    // If normal cluster, don't allow set/unset of RELAY_SOURCE
        // You can check if cluster is in PASSIVE_RELAY or not.

    //IN replica cluster, you allow it and can't be more than one change
  }*/

  @Override
  protected void validate() {
    super.validate();

    Map<Endpoint, LogicalServerState> onlineNodes = findOnlineRuntimePeers(node);
    boolean allOnlineNodesActivated = areAllNodesActivated(onlineNodes.keySet());

    if (allOnlineNodesActivated) {
      Cluster cluster = getUpcomingCluster(node);
      Cluster updatedCluster = cluster.clone();

      MultiSettingNomadChange changes = getNomadChanges(updatedCluster);

      boolean isPassiveRelayCluster = onlineNodes.values().stream()
        .anyMatch(state -> state == LogicalServerState.PASSIVE_RELAY);

      long relaySourceChanges = changes.getChanges().stream()
        .filter(change -> change.getSetting() == Setting.RELAY_SOURCE)
        .count();

      if (!isPassiveRelayCluster && relaySourceChanges > 0) {
        throw new IllegalArgumentException("Setting or unsetting RELAY_SOURCE is not allowed in a normal activated cluster.");
      }

      if (isPassiveRelayCluster && relaySourceChanges > 1) {
        throw new IllegalArgumentException("Only one RELAY_SOURCE change is allowed in a PASSIVE_RELAY cluster.");
      }
    }
  }

  @Override
  public void run() {
    validate();
    if (configurations.isEmpty()) {
      // no remaining configuration changes left to process
      output.info("Command successful!");
      return;
    }
    LOGGER.debug("Validating the new configuration change(s) against the topology of: {}", node);

    // get the remote topology, apply the parameters, and validate that the cluster is still valid
    Cluster originalCluster = getUpcomingCluster(node);
    Cluster updatedCluster = originalCluster.clone();

    // will keep track of the targeted nodes for the changes of a node setting
    Collection<String> nodesRequiringRestart = new TreeSet<>();
    Collection<String> targetedNodes = new TreeSet<>();

    // applying the set/unset operation to the cluster in memory for validation
    for (Configuration c : configurations) {
      Collection<? extends PropertyHolder> targets = c.apply(updatedCluster);

      // keep track of the node targeted by this configuration line
      // remember: a node setting can be set using a cluster or stripe namespace to target several nodes at once
      // Note: this validation is only for node-specific settings
      targets.stream()
          .filter(o -> o.getScope() == NODE)
          .map(PropertyHolder::getName)
          .filter(originalCluster::containsNode)
          .forEach(name -> {
            targetedNodes.add(name);
            if (c.getSetting().requires(NODE_RESTART)) {
              nodesRequiringRestart.add(name);
            }
          });
    }
    if (updatedCluster.equals(originalCluster)) {
      String message = "The requested update will not result in any change to the cluster configuration.";
      output.out(message);
      LOGGER.warn(lineSeparator() +
          "=======================================================================================" + lineSeparator() +
          message + lineSeparator() +
          "=======================================================================================" + lineSeparator());
      return;
    }

    // get the current state of the nodes
    // this call can take some time, and we can have some timeout
    Map<Endpoint, LogicalServerState> onlineNodes = findOnlineRuntimePeers(node);
    LOGGER.debug("Online nodes: {}", onlineNodes);

    boolean allOnlineNodesActivated = areAllNodesActivated(onlineNodes.keySet());

    new ClusterValidator(updatedCluster).validate(allOnlineNodesActivated ? ACTIVATED : CONFIGURING);

    if (allOnlineNodesActivated) {
      licenseValidation(node, updatedCluster);
    }

    // ensure that the nodes targeted by the set or unset command in the namespaces are all online so that they can validate the change
    Collection<String> missingTargetedNodes = new TreeSet<>(targetedNodes);
    onlineNodes.keySet().stream().map(Endpoint::getNodeName).forEach(missingTargetedNodes::remove);
    if (!missingTargetedNodes.isEmpty()) {
      throw new IllegalStateException("Some nodes that are targeted by the change are not reachable and thus cannot be validated. " +
          "Please ensure these nodes are online, or remove them from the request: " + toString(missingTargetedNodes));
    }

    LOGGER.debug("New configuration change(s) can be sent");

    if (allOnlineNodesActivated) {
      // cluster is active, we need to run a nomad change and eventually a restart

      // validate that all the online nodes are either actives or passives
      ensureNodesAreEitherActiveOrPassive(onlineNodes);

      if (requiresAllNodesAlive()) {
        // Check passive nodes as well if the setting requires all nodes to be online
        ensurePassivesAreAllOnline(originalCluster, onlineNodes);
      }

      ensureActivesAreAllOnline(originalCluster, onlineNodes);
      output.info("Applying new configuration change(s) to activated nodes: {}", toString(onlineNodes.keySet()));
      MultiSettingNomadChange changes = getNomadChanges(updatedCluster);

      // Separate RELAY_SOURCE changes from others
      List<SettingNomadChange> relaySourceChanges = new ArrayList<>();
      List<SettingNomadChange> otherChanges = new ArrayList<>();

      for (SettingNomadChange change : changes.getChanges()) {
        if (change.getSetting() == Setting.RELAY_SOURCE) {
          relaySourceChanges.add(change);
        } else {
          otherChanges.add(change);
        }
      }

      // Apply RELAY_SOURCE changes through diagnostic
      if (!relaySourceChanges.isEmpty()) {
        runConfigurationChangeThroughDiagnostic(onlineNodes, new MultiSettingNomadChange(relaySourceChanges));
      }

      // Apply all other changes normally
      if (!otherChanges.isEmpty()) {
        runConfigurationChange(updatedCluster, onlineNodes, new MultiSettingNomadChange(otherChanges));
      }

      // do we need to restart to apply the changes ?
      if (changes.getChanges().stream().map(SettingNomadChange::getSetting).anyMatch(setting -> setting.requires(CLUSTER_RESTART))) {
        output.out("Restart required for cluster");
        if (autoRestart) {
          rollingRestart(updatedCluster, onlineNodes.keySet().stream().collect(toMap(Endpoint::getNodeName, identity())));
        } else {
          LOGGER.warn(lineSeparator() +
              "====================================================================" + lineSeparator() +
              "IMPORTANT: A manual restart of the cluster is required to apply the changes" + lineSeparator() +
              "====================================================================" + lineSeparator());
        }
      } else {
        final long numberOfDifferentSettingsToChange = configurations.stream()
            .map(Configuration::getSetting)
            .distinct()
            .count();
        final long numberOfDifferentSettingsRequiringRestart = configurations.stream()
            .map(Configuration::getSetting)
            .distinct()
            .filter(setting -> setting.requires(NODE_RESTART))
            .count();

        if (numberOfDifferentSettingsToChange != numberOfDifferentSettingsRequiringRestart) {
          // if the user updates more than 1 setting that does not require a restart, or a mix of settings which
          // do and do not require a restart, we will go there.
          // I.e. set backup-dir
          // I.e. set log-dir + backup-dir
          // In that case, we might already have some nodes inside nodesRequiringRestart.
          // But we need to add into this collection the other nodes that are targeted AND could have vetoed a change to be applied at runtime
          for (Endpoint endpoint : new LinkedHashSet<>(onlineNodes.keySet())) {
            try {
              if (targetedNodes.contains(endpoint.getNodeName()) && mustBeRestarted(endpoint)) {
                nodesRequiringRestart.add(endpoint.getNodeName());
              }
            } catch (RuntimeException e) {
              // some nodes might have failed over and not be reachable anymore
              LOGGER.warn("Node: " + endpoint + " is not reachable anymore: {}", e.getMessage(), e);
              onlineNodes.remove(endpoint);
            }
          }
        }

        if (!nodesRequiringRestart.isEmpty()) {
          List<HostPort> addresses = onlineNodes.keySet()
              .stream()
              .filter(endpoint -> nodesRequiringRestart.contains(endpoint.getNodeName()))
              .map(Endpoint::getHostPort)
              .collect(toList());
          output.out("Restart required for nodes: {} ", toString(addresses));
          if (autoRestart) {
            rollingRestart(updatedCluster, onlineNodes.keySet().stream()
                .filter(endpoint -> nodesRequiringRestart.contains(endpoint.getNodeName()))
                .collect(toMap(Endpoint::getNodeName, identity())));
          } else {
            LOGGER.warn(lineSeparator() +
                "=======================================================================================" + lineSeparator() +
                "IMPORTANT: A manual restart of nodes: " + toString(nodesRequiringRestart) + " is required to apply the changes" + lineSeparator() +
                "=======================================================================================" + lineSeparator());
          }
        }
      }

    } else {
      // cluster is not active, we just need to replace the topology
      output.info("Applying new configuration change(s) to nodes: {}", toString(onlineNodes.keySet()));
      try (DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(endpointsToMap(onlineNodes.keySet()))) {
        dynamicConfigServices(diagnosticServices)
            .map(Tuple2::getT2)
            .forEach(dynamicConfigService -> dynamicConfigService.setUpcomingCluster(updatedCluster));
      }
    }

    output.info("Command successful!");
  }

  private MultiSettingNomadChange getNomadChanges(Cluster cluster) {
    // MultiSettingNomadChange will apply to whole change set given by the user as an atomic operation
    return new MultiSettingNomadChange(configurations.stream()
        .map(configuration -> {
          configuration.validate(clusterState, operation);
          return SettingNomadChange.fromConfiguration(configuration, operation, cluster);
        })
        .collect(toList()));
  }

  private boolean requiresAllNodesAlive() {
    // By default, we require all nodes to be up to allow a DC change: the consensus system needs all nodes to be able to vote and prepare the change.
    // This will also solve any potential partitioned config to happen (or loosing a change if a failover happens just after),
    // in the case a nomad change is prepared at the same time a passive is synchronizing the DC configuration from active server.
    // This can be overridden by the user if he knows what he is doing with: -force
    // In any case, if the changes requires all nodes to be up (like SSL), we will require all nodes to be up even if -force is used.
    return !force || configurations.stream().map(Configuration::getSetting).anyMatch(setting -> setting.requires(CLUSTER_ONLINE));
  }

  private void rollingRestart(Cluster cluster, Map<String, Endpoint> onlineNodesToRestart) {
    // node that we cannot restart
    Collection<Endpoint> cannotRestart = new LinkedHashSet<>();

    // nodes that we will restart
    Collection<Endpoint> actives = new LinkedList<>();
    Collection<Endpoint> others = new LinkedList<>();

    // first try to grasp the shape of the online topology
    // to determine which nodes can be restarted
    for (Stripe stripe : cluster.getStripes()) {
      List<Endpoint> onlineNodesPerStripe = onlineNodesToRestart.values().stream().filter(endpoint -> stripe.containsNode(endpoint.getNodeName())).collect(toList());

      if (onlineNodesPerStripe.isEmpty()) {
        LOGGER.warn("No node in stripe '{}' seem to be online", stripe.getName());

      } else if (onlineNodesPerStripe.size() == 1) {
        Endpoint alone = onlineNodesPerStripe.get(0);
        LOGGER.warn("Unable to restart node: {} in stripe '{}' because this is the only online node", alone, stripe.getName());
        cannotRestart.add(alone);

      } else {
        for (Endpoint endpoint : onlineNodesPerStripe) {
          // get the node state again (we are searching for actives)
          LogicalServerState state = UNREACHABLE;
          try {
            state = getLogicalServerState(endpoint);
          } catch (RuntimeException e) {
            LOGGER.warn("Node: {} in stripe '{}' is not reachable anymore: {}", endpoint, stripe.getName(), e.getMessage(), e);
            cannotRestart.add(endpoint);
          }
          if (state.isActive()) {
            actives.add(endpoint);
          } else {
            others.add(endpoint);
          }
        }
      }
    }

    // Restarting all but active nodes.
    // We except the nodes to be restarted in either active or passive state.
    // If not, the restart will throw.
    // This is required because next step is to restart the remaining nodes...
    // Which are active, so we have to ensure that a passive not will be there to take over the active role
    LOGGER.info("Restarting non active nodes: {}...", toString(others));
    restartNodesIfPassives(
        others,
        Duration.ofMillis(restartWaitTime.getQuantity(TimeUnit.MILLISECONDS)),
        Duration.ofMillis(restartDelay.getQuantity(TimeUnit.MILLISECONDS)),
        EnumSet.of(ACTIVE, ACTIVE_RECONNECTING, PASSIVE, PASSIVE_RELAY));

    // Restarting actives. This will trigger failovers and active will restart as passives.
    LOGGER.info("Restarting active nodes: {}...", toString(actives));
    restartNodesIfActives(
        actives,
        Duration.ofMillis(restartWaitTime.getQuantity(TimeUnit.MILLISECONDS)),
        Duration.ofMillis(restartDelay.getQuantity(TimeUnit.MILLISECONDS)),
        EnumSet.of(ACTIVE, ACTIVE_RECONNECTING, PASSIVE, PASSIVE_RELAY));

    // let's check if some nodes were not restarted because their state has changed from the last time we got their state
    concat(actives.stream(), others.stream())
        .filter(endpoint -> {
          try {
            return mustBeRestarted(endpoint);
          } catch (RuntimeException e) {
            LOGGER.warn("Node: {} is not reachable anymore: {}", endpoint, e.getMessage(), e);
            return true;
          }
        }).forEach(cannotRestart::add);

    if (!cannotRestart.isEmpty()) {
      LOGGER.warn(lineSeparator() +
          "=======================================================================================" + lineSeparator() +
          "IMPORTANT: A manual restart of nodes: " + toString(cannotRestart) + " will be required to apply the changes" + lineSeparator() +
          "=======================================================================================" + lineSeparator());
    }
  }
}
