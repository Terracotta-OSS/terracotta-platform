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


import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer;
import org.terracotta.dynamic_config.cli.config_tool.nomad.NomadManager;
import org.terracotta.dynamic_config.cli.config_tool.restart.RestartProgress;
import org.terracotta.dynamic_config.cli.config_tool.restart.RestartService;
import org.terracotta.dynamic_config.cli.config_tool.stop.StopProgress;
import org.terracotta.dynamic_config.cli.config_tool.stop.StopService;
import org.terracotta.inet.InetSocketAddressUtils;
import org.terracotta.nomad.client.results.NomadFailureReceiver;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadChangeInfo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE_RECONNECTING;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.SYNCHRONIZING;
import static org.terracotta.diagnostic.model.LogicalServerState.UNREACHABLE;

/**
 * @author Mathieu Carbou
 */
public abstract class RemoteCommand extends Command {

  @Inject public MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  @Inject public DiagnosticServiceProvider diagnosticServiceProvider;
  @Inject public NomadManager<NodeContext> nomadManager;
  @Inject public RestartService restartService;
  @Inject public StopService stopService;

  protected void licenseValidation(InetSocketAddress node, Cluster cluster) {
    logger.trace("licenseValidation({}, {})", node, cluster);
    logger.debug("Validating the new configuration change(s) against the license");
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(node)) {
      if (diagnosticService.getProxy(TopologyService.class).validateAgainstLicense(cluster)) {
        logger.info("License validation passed: configuration change(s) can be applied");
      } else {
        logger.warn("License validation skipped: no license installed");
      }
    }
  }

  private void activateNomadSystem(Collection<InetSocketAddress> newNodes, Cluster cluster, String licenseContent) {
    logger.info("Activating nodes: {}", toString(newNodes));

    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(newNodes)) {
      dynamicConfigServices(diagnosticServices)
        .map(Tuple2::getT2)
        .forEach(service -> service.activate(cluster, licenseContent));
      if (licenseContent == null) {
        logger.info("No license installed. If you are attaching a node, the license will be synced.");
      } else {
        logger.info("License installation successful");
      }
    }
  }

  private void restartNodes(Collection<InetSocketAddress> newNodes, Measure<TimeUnit> restartDelay, Measure<TimeUnit> restartWaitTime) {
    logger.info("Restarting nodes: {}", toString(newNodes));
    restartNodes(
      newNodes,
      Duration.ofMillis(restartWaitTime.getQuantity(TimeUnit.MILLISECONDS)),
      Duration.ofMillis(restartDelay.getQuantity(TimeUnit.MILLISECONDS)),
      // these are the list of states tha twe allow to consider a server has restarted
      // In dynamic config, restarted means that a node has reach a state that is after the STARTING state
      // and has consequently bootstrapped the configuration from Nomad.
      EnumSet.of(ACTIVE, ACTIVE_RECONNECTING, ACTIVE_SUSPENDED, PASSIVE, PASSIVE_SUSPENDED, SYNCHRONIZING));
    logger.info("All nodes came back up");
  }

  protected final void activateNodes(Collection<InetSocketAddress> newNodes, Cluster cluster, Path licenseFile,
                                     Measure<TimeUnit> restartDelay, Measure<TimeUnit> restartWaitTime) {
    activateNomadSystem(newNodes, cluster, read(licenseFile));

    runClusterActivation(newNodes, cluster);

    restartNodes(newNodes, restartDelay, restartWaitTime);
  }

  protected final void activateStripe(Collection<InetSocketAddress> newNodes, Cluster cluster, InetSocketAddress destination,
                                      Measure<TimeUnit> restartDelay, Measure<TimeUnit> restartWaitTime) {
    activateNomadSystem(newNodes, cluster, getLicenseContentFrom(destination).orElse(null));

    runClusterActivation(newNodes, cluster);

    syncNomadChangesTo(newNodes, getAllNomadChangesFrom(destination));

    restartNodes(newNodes, restartDelay, restartWaitTime);
  }

  private Optional<String> getLicenseContentFrom(InetSocketAddress node) {
    logger.trace("getLicenseContent({})", node);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(node)) {
      return diagnosticService.getProxy(DynamicConfigService.class).getLicenseContent();
    }
  }

  private NomadChangeInfo[] getAllNomadChangesFrom(InetSocketAddress node) {
    logger.trace("getChangeHistory({})", node);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(node)) {
      return diagnosticService.getProxy(TopologyService.class).getChangeHistory();
    }
  }

  private void syncNomadChangesTo(Collection<InetSocketAddress> newNodes, NomadChangeInfo[] nomadChanges) {
    logger.info("Sync'ing nomad changes to nodes : {}", toString(newNodes));

    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(newNodes)) {
      dynamicConfigServices(diagnosticServices)
        .map(Tuple2::getT2)
        .forEach(service -> service.resetAndSync(nomadChanges));
      logger.info("Nomad changes sync successful");
    }
  }

  /**
   * Ensure that the input address is really an address that can be used to connect to a node of a cluster
   */
  protected final void validateAddress(InetSocketAddress expectedOnlineNode) {
    logger.trace("Validating node address: {} (this can take time if the node is not reachable)", expectedOnlineNode);
    getRuntimeCluster(expectedOnlineNode).getNode(expectedOnlineNode)
        .orElseGet(() -> getUpcomingCluster(expectedOnlineNode).getNode(expectedOnlineNode)
            .orElseThrow(() -> new IllegalArgumentException("Targeted cluster does not contain any node with this address: " + expectedOnlineNode + ". Is it a mistake ? Are you connecting to the wrong cluster ? If not, please use the configured node hostname and port to connect.")));
  }

  protected final boolean mustBeRestarted(InetSocketAddress expectedOnlineNode) {
    logger.trace("mustBeRestarted({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      return diagnosticService.getProxy(TopologyService.class).mustBeRestarted();
    }
  }

  protected final boolean hasIncompleteChange(InetSocketAddress expectedOnlineNode) {
    logger.trace("hasIncompleteChange({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      return diagnosticService.getProxy(TopologyService.class).hasIncompleteChange();
    }
  }

  /**
   * Returns the current consistency of the configuration in the cluster
   */
  protected final ConsistencyAnalyzer<NodeContext> analyzeNomadConsistency(Map<InetSocketAddress, LogicalServerState> allNodes) {
    logger.trace("analyzeNomadConsistency({})", allNodes);
    ConsistencyAnalyzer<NodeContext> consistencyAnalyzer = new ConsistencyAnalyzer<>(allNodes);
    nomadManager.runConfigurationDiscovery(allNodes, consistencyAnalyzer);
    return consistencyAnalyzer;
  }

  /**
   * Runs a Nomad recovery by providing a map of activated nodes plus their state.
   * This method will create an ordered list of nodes to contact by moving the passives first and actives last.
   */
  protected final void runConfigurationRepair(ConsistencyAnalyzer<NodeContext> consistencyAnalyzer, ChangeRequestState forcedState) {
    logger.trace("runConfigurationRepair({}, {})", toString(consistencyAnalyzer.getAllNodes().keySet()), forcedState);
    NomadFailureReceiver<NodeContext> failures = new NomadFailureReceiver<>();
    nomadManager.runConfigurationRepair(consistencyAnalyzer, failures, forcedState);
    failures.reThrow();
  }

  /**
   * Runs a Nomad change by providing a map of activated nodes plus their state.
   * This method will create an ordered list of nodes to contact by moving the passives first and actives last.
   * <p>
   * Nodes are expected to be online.
   */
  protected final void runConfigurationChange(Cluster destinationCluster, Map<InetSocketAddress, LogicalServerState> onlineNodes, MultiSettingNomadChange change) {
    logger.trace("runConfigurationChange({}, {})", onlineNodes, change);
    NomadFailureReceiver<NodeContext> failures = new NomadFailureReceiver<>();
    nomadManager.runConfigurationChange(destinationCluster, onlineNodes, change, failures);
    failures.reThrow();
  }

  protected final void runTopologyChange(Cluster destinationCluster, Map<InetSocketAddress, LogicalServerState> onlineNodes, TopologyNomadChange change) {
    logger.trace("runTopologyChange({}, {})", onlineNodes, change);
    NomadFailureReceiver<NodeContext> failures = new NomadFailureReceiver<>();
    nomadManager.runTopologyChange(destinationCluster, onlineNodes, change, failures);
    failures.reThrow();
  }

  /**
   * Runs a Nomad change by providing an ordered list of nodes, to send them the change in order
   * <p>
   * Nodes are expected to be online.
   */
  protected final void runClusterActivation(Collection<InetSocketAddress> expectedOnlineNodes, Cluster cluster) {
    logger.trace("runClusterActivation({}, {})", expectedOnlineNodes, cluster.toShapeString());
    NomadFailureReceiver<NodeContext> failures = new NomadFailureReceiver<>();
    nomadManager.runClusterActivation(expectedOnlineNodes, cluster, failures);
    failures.reThrow();
    logger.debug("Configuration directories have been created for all nodes");
  }

  protected final LogicalServerState getState(InetSocketAddress expectedOnlineNode) {
    logger.trace("getUpcomingCluster({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      return diagnosticService.getLogicalServerState();
    }
  }

  protected final Cluster getUpcomingCluster(InetSocketAddress expectedOnlineNode) {
    logger.trace("getUpcomingCluster({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      return diagnosticService.getProxy(TopologyService.class).getUpcomingNodeContext().getCluster();
    }
  }

  protected final void setUpcomingCluster(Collection<InetSocketAddress> expectedOnlineNodes, Cluster cluster) {
    logger.trace("setUpcomingCluster({})", expectedOnlineNodes);
    for (InetSocketAddress expectedOnlineNode : expectedOnlineNodes) {
      try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
        diagnosticService.getProxy(DynamicConfigService.class).setUpcomingCluster(cluster);
      }
    }
  }

  protected final Cluster getRuntimeCluster(InetSocketAddress expectedOnlineNode) {
    logger.trace("getRuntimeCluster({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      return diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext().getCluster();
    }
  }

  protected final void restartNodes(Collection<InetSocketAddress> addresses, Duration maximumWaitTime, Duration restartDelay, Collection<LogicalServerState> acceptedStates) {
    logger.trace("restartNodes({}, {})", addresses, maximumWaitTime);
    try {
      RestartProgress progress = restartService.restartNodes(
          addresses,
          restartDelay,
          acceptedStates);
      progress.getErrors().forEach((address, e) -> logger.warn("Unable to ask node: {} to restart: please restart it manually.", address));
      progress.onRestarted((address, state) -> logger.info("Node: {} has restarted in state: {}", address, state));
      Map<InetSocketAddress, LogicalServerState> restarted = progress.await(maximumWaitTime);
      // check where we are
      Collection<InetSocketAddress> missing = new TreeSet<>(Comparator.comparing(InetSocketAddress::toString));
      missing.addAll(addresses);
      missing.removeAll(progress.getErrors().keySet()); // remove nodes that we were not able to contact
      missing.removeAll(restarted.keySet()); // remove nodes that have been restarted
      if (!missing.isEmpty()) {
        throw new IllegalStateException("Some nodes failed to restart within " + maximumWaitTime.getSeconds() + " seconds:" + lineSeparator()
            + " - " + missing.stream().map(InetSocketAddress::toString).collect(joining(lineSeparator() + " - ")));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Restart has been interrupted", e);
    }
  }

  protected final void stopNodes(Collection<InetSocketAddress> addresses, Duration maximumWaitTime, Duration restartDelay) {
    logger.trace("stopNodes({}, {})", addresses, maximumWaitTime);
    try {
      StopProgress progress = stopService.stopNodes(addresses, restartDelay);
      progress.getErrors().forEach((address, e) -> logger.warn("Unable to ask node: {} to stop: please stop it manually.", address));
      progress.onStopped(address -> logger.info("Node: {} has stopped", address));
      Collection<InetSocketAddress> stopped = progress.await(maximumWaitTime);
      // check where we are
      Collection<InetSocketAddress> missing = new TreeSet<>(Comparator.comparing(InetSocketAddress::toString));
      missing.addAll(addresses);
      missing.removeAll(progress.getErrors().keySet()); // remove nodes that we were not able to contact
      missing.removeAll(stopped); // remove nodes that have been restarted
      if (!missing.isEmpty()) {
        throw new IllegalStateException("Some nodes failed to stop within " + maximumWaitTime.getSeconds() + " seconds:" + lineSeparator()
            + " - " + missing.stream().map(InetSocketAddress::toString).collect(joining(lineSeparator() + " - ")));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Stop has been interrupted", e);
    }
  }

  protected final Collection<InetSocketAddress> findRuntimePeers(InetSocketAddress expectedOnlineNode) {
    logger.trace("findRuntimePeers({})", expectedOnlineNode);
    Collection<InetSocketAddress> peers = getRuntimeCluster(expectedOnlineNode).getNodeAddresses();
    if (logger.isDebugEnabled()) {
      logger.debug("Discovered nodes:{} through: {}", toString(peers), expectedOnlineNode);
    }
    return peers;
  }

  protected final Map<InetSocketAddress, LogicalServerState> findRuntimePeersStatus(InetSocketAddress expectedOnlineNode) {
    logger.trace("findRuntimePeersStatus({})", expectedOnlineNode);
    Cluster cluster = getRuntimeCluster(expectedOnlineNode);
    logger.info("Connecting to: {} (this can take time if some nodes are not reachable)", toString(cluster.getNodeAddresses()));
    Collection<InetSocketAddress> addresses = cluster.getNodeAddresses();
    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchDiagnosticServices(addresses)) {
      LinkedHashMap<InetSocketAddress, LogicalServerState> status = addresses.stream()
          .collect(toMap(
              identity(),
              addr -> diagnosticServices.getDiagnosticService(addr).map(DiagnosticService::getLogicalServerState).orElse(UNREACHABLE),
              (o1, o2) -> {
                throw new UnsupportedOperationException();
              },
              LinkedHashMap::new));
      status.forEach((address, state) -> {
        if (state.isUnreacheable()) {
          logger.info(" - {} is not reachable", address);
        }
      });
      return status;
    }
  }

  protected final Map<InetSocketAddress, LogicalServerState> findOnlineRuntimePeers(InetSocketAddress expectedOnlineNode) {
    logger.trace("findOnlineRuntimePeers({})", expectedOnlineNode);
    Map<InetSocketAddress, LogicalServerState> nodes = findRuntimePeersStatus(expectedOnlineNode);
    return filterOnlineNodes(nodes);
  }

  protected final LinkedHashMap<InetSocketAddress, LogicalServerState> filterOnlineNodes(Map<InetSocketAddress, LogicalServerState> nodes) {
    return filter(nodes, (addr, state) -> !state.isUnknown() && !state.isUnreacheable());
  }

  protected final LinkedHashMap<InetSocketAddress, LogicalServerState> filter(Map<InetSocketAddress, LogicalServerState> nodes, BiPredicate<InetSocketAddress, LogicalServerState> predicate) {
    return nodes.entrySet()
        .stream()
        .filter(e -> predicate.test(e.getKey(), e.getValue()))
        .collect(toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (o1, o2) -> {
              throw new UnsupportedOperationException();
            },
            LinkedHashMap::new));
  }

  /**
   * IMPORTANT NOTE:
   * - onlineNodes comes from the runtime topology
   * - cluster comes from the upcoming topology
   * So this method will also validate that if a restart is needed because a hostname/port change has been done,
   * if the hostname/port change that is pending impacts one of the active node, then we might not find the actives
   * in the stripes.
   */
  protected final void ensurePassivesAreAllOnline(Cluster cluster, Map<InetSocketAddress, LogicalServerState> onlineNodes) {
    List<InetSocketAddress> actives = onlineNodes.entrySet().stream().filter(e -> e.getValue().isActive()).map(Map.Entry::getKey).collect(toList());
    List<InetSocketAddress> passives = onlineNodes.entrySet().stream().filter(e -> e.getValue().isPassive()).map(Map.Entry::getKey).collect(toList());
    Set<InetSocketAddress> expectedPassives = new HashSet<>(cluster.getNodeAddresses());
    expectedPassives.removeAll(actives);
    if (!InetSocketAddressUtils.containsAll(passives, expectedPassives)) {
      throw new IllegalStateException("Not all cluster nodes are online: expected passive nodes " + toString(expectedPassives) + ", but only got: " + toString(passives)
          + ". Either some nodes are shutdown, either a hostname/port change has been made and the cluster has not yet been restarted.");
    }
  }

  protected final void ensureActivesAreAllOnline(Cluster cluster, Map<InetSocketAddress, LogicalServerState> onlineNodes) {
    if (onlineNodes.isEmpty()) {
      throw new IllegalStateException("Expected 1 active per stripe, but found no online node.");
    }
    // actives == list of current active nodes in the runtime topology
    List<InetSocketAddress> actives = onlineNodes.entrySet().stream().filter(e -> e.getValue().isActive()).map(Map.Entry::getKey).collect(toList());
    // Check for stripe count. Whether there is a pending dynamic config change or not, the stripe count is not changing.
    // The stripe count only changes in case of a runtime topology change, which is another case.
    if (cluster.getStripeCount() != actives.size()) {
      throw new IllegalStateException("Expected 1 active per stripe, but only these nodes are active: " + toString(actives));
    }
  }

  protected final void ensureNodesAreEitherActiveOrPassive(Map<InetSocketAddress, LogicalServerState> onlineNodes) {
    onlineNodes.forEach((addr, state) -> {
      if (!state.isActive() && !state.isPassive()) {
        throw new IllegalStateException("Unable to update node: " + addr + " that is currently in state: " + state
            + ". Please ensure all online nodes are either ACTIVE or PASSIVE before sending any update.");
      }
    });
  }

  protected final boolean isActivated(InetSocketAddress expectedOnlineNode) {
    logger.trace("isActivated({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      return diagnosticService.getProxy(TopologyService.class).isActivated();
    }
  }

  protected final void resetAndStop(InetSocketAddress expectedOnlineNode) {
    logger.info("Reset node: {}. Node will stop in 5 seconds", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      DynamicConfigService proxy = diagnosticService.getProxy(DynamicConfigService.class);
      proxy.reset();
      proxy.stop(Duration.ofSeconds(5));
    }
  }

  protected final void reset(InetSocketAddress expectedOnlineNode) {
    logger.info("Reset node: {}", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      DynamicConfigService proxy = diagnosticService.getProxy(DynamicConfigService.class);
      proxy.reset();
    }
  }

  protected final boolean areAllNodesActivated(Collection<InetSocketAddress> expectedOnlineNodes) {
    logger.trace("areAllNodesActivated({})", expectedOnlineNodes);
    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(expectedOnlineNodes)) {
      Map<Boolean, Collection<InetSocketAddress>> activations = topologyServices(diagnosticServices)
          .map(tuple -> tuple.map(identity(), TopologyService::isActivated))
          .collect(groupingBy(Tuple2::getT2, mapping(Tuple2::getT1, toCollection(() -> new TreeSet<>(Comparator.comparing(InetSocketAddress::toString))))));
      if (activations.isEmpty()) {
        throw new IllegalArgumentException("Cluster is empty or offline");
      }
      if (activations.size() == 2) {
        throw new IllegalStateException("Detected a mix of activated and unconfigured nodes (or being repaired). " +
            "Activated: " + activations.get(Boolean.TRUE) + ", " +
            "Unconfigured: " + activations.get(Boolean.FALSE));
      }
      return activations.keySet().iterator().next();
    }
  }

  protected final void upgradeLicense(Collection<InetSocketAddress> expectedOnlineNodes, Path licenseFile) {
    logger.trace("upgradeLicense({}, {})", expectedOnlineNodes, licenseFile);
    String xml;
    try {
      xml = new String(Files.readAllBytes(licenseFile), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(expectedOnlineNodes)) {
      dynamicConfigServices(diagnosticServices)
          .map(tuple -> {
            try {
              tuple.t2.upgradeLicense(xml);
              return null;
            } catch (RuntimeException e) {
              logger.debug("License upgrade failed on node {}: {}", tuple.t1, e.getMessage());
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

  protected static Stream<Tuple2<InetSocketAddress, TopologyService>> topologyServices(DiagnosticServices diagnosticServices) {
    return diagnosticServices.map((address, diagnosticService) -> diagnosticService.getProxy(TopologyService.class));
  }

  protected static Stream<Tuple2<InetSocketAddress, DynamicConfigService>> dynamicConfigServices(DiagnosticServices diagnosticServices) {
    return diagnosticServices.map((address, diagnosticService) -> diagnosticService.getProxy(DynamicConfigService.class));
  }

  protected static String toString(Collection<InetSocketAddress> addresses) {
    return addresses.stream().map(InetSocketAddress::toString).sorted().collect(Collectors.joining(", "));
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
