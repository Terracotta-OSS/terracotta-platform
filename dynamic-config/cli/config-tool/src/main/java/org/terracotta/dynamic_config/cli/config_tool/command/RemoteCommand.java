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
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.UnlockConfigNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer;
import org.terracotta.dynamic_config.cli.config_tool.nomad.LockAwareNomadManager;
import org.terracotta.dynamic_config.cli.config_tool.nomad.NomadManager;
import org.terracotta.dynamic_config.cli.config_tool.restart.RestartProgress;
import org.terracotta.dynamic_config.cli.config_tool.restart.RestartService;
import org.terracotta.dynamic_config.cli.config_tool.stop.StopProgress;
import org.terracotta.dynamic_config.cli.config_tool.stop.StopService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
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
import static java.util.stream.Collectors.toSet;
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

  @Inject public MultiDiagnosticServiceProvider<UID> multiDiagnosticServiceProvider;
  @Inject public DiagnosticServiceProvider diagnosticServiceProvider;
  @Inject public NomadManager<NodeContext> nomadManager;
  @Inject public RestartService restartService;
  @Inject public StopService stopService;
  @Inject public ClusterValidator clusterValidator;

  protected void licenseValidation(Endpoint endpoint, Cluster cluster) {
    licenseValidation(endpoint.getAddress(), cluster);
  }

  protected void licenseValidation(InetSocketAddress node, Cluster cluster) {
    logger.trace("licenseValidation({}, {})", node, cluster);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(node)) {
      if (diagnosticService.getProxy(TopologyService.class).validateAgainstLicense(cluster)) {
        logger.debug("License validation passed: configuration change(s) can be applied");
      } else {
        logger.debug("License validation skipped: no license installed");
      }
    }
  }

  private void activateNomadSystem(Collection<Endpoint> newNodes, Cluster cluster, String licenseContent) {
    logger.info("Activating nodes: {}", toString(newNodes));

    try (DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(endpointsToMap(newNodes))) {
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

  private void restartNodes(Collection<Endpoint> newNodes, Measure<TimeUnit> restartDelay, Measure<TimeUnit> restartWaitTime) {
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

  protected final void activateNodes(Collection<Endpoint> newNodes, Cluster cluster, Path licenseFile,
                                     Measure<TimeUnit> restartDelay, Measure<TimeUnit> restartWaitTime) {
    activateNomadSystem(newNodes, cluster, read(licenseFile));

    runClusterActivation(newNodes, cluster);

    restartNodes(newNodes, restartDelay, restartWaitTime);
  }

  protected final void activateStripe(Collection<Endpoint> newNodes, Cluster cluster, Endpoint destination,
                                      Measure<TimeUnit> restartDelay, Measure<TimeUnit> restartWaitTime) {
    activateNomadSystem(newNodes, cluster, getLicenseContentFrom(destination).orElse(null));

    runClusterActivation(newNodes, cluster);

    syncNomadChangesTo(newNodes, getAllNomadChangesFrom(destination), cluster);

    restartNodes(newNodes, restartDelay, restartWaitTime);
  }

  private Optional<String> getLicenseContentFrom(Endpoint node) {
    logger.trace("getLicenseContent({})", node);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(node.getAddress())) {
      return diagnosticService.getProxy(DynamicConfigService.class).getLicenseContent();
    }
  }

  private NomadChangeInfo[] getAllNomadChangesFrom(Endpoint node) {
    logger.trace("getChangeHistory({})", node);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(node.getAddress())) {
      return diagnosticService.getProxy(TopologyService.class).getChangeHistory();
    }
  }

  private void syncNomadChangesTo(Collection<Endpoint> newNodes, NomadChangeInfo[] nomadChanges, Cluster cluster) {
    logger.info("Sync'ing nomad changes to nodes : {}", toString(newNodes));

    try (DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(endpointsToMap(newNodes))) {
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(service -> service.resetAndSync(nomadChanges, cluster));
      logger.info("Nomad changes sync successful");
    }
  }

  protected final boolean mustBeRestarted(Endpoint endpoint) {
    return mustBeRestarted(endpoint.getAddress());
  }

  protected final boolean mustBeRestarted(InetSocketAddress expectedOnlineNode) {
    logger.trace("mustBeRestarted({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      return diagnosticService.getProxy(TopologyService.class).mustBeRestarted();
    }
  }

  protected final boolean hasIncompleteChange(Endpoint endpoint) {
    return hasIncompleteChange(endpoint.getAddress());
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
  protected final ConsistencyAnalyzer<NodeContext> analyzeNomadConsistency(Map<Endpoint, LogicalServerState> allNodes) {
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
    failures.reThrowReasons();
  }

  /**
   * Runs a Nomad change by providing a map of activated nodes plus their state.
   * This method will create an ordered list of nodes to contact by moving the passives first and actives last.
   * <p>
   * Nodes are expected to be online.
   */
  protected final void runConfigurationChange(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes, DynamicConfigNomadChange change) {
    logger.trace("runConfigurationChange({}, {})", onlineNodes, change);
    NomadFailureReceiver<NodeContext> failures = new NomadFailureReceiver<>();
    nomadManager.runConfigurationChange(destinationCluster, onlineNodes, change, failures);
    failures.reThrowReasons();
  }

  protected final String lock(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes,
                              String ownerName, String tags) {
    return lock(destinationCluster, onlineNodes, new LockContext(UUID.randomUUID().toString(), ownerName, tags));
  }

  protected final String lock(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes, LockContext lockContext) {
    logger.info("Trying to lock the config...");
    runConfigurationChange(destinationCluster, onlineNodes, new LockConfigNomadChange(lockContext));
    logger.info("Locked the config.");
    this.nomadManager = new LockAwareNomadManager<>(lockContext.getToken(), nomadManager);
    return lockContext.getToken();
  }

  protected final void unlock(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes) {
    unlockInternal(destinationCluster, onlineNodes, false);
  }

  protected final void forceUnlock(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes) {
    unlockInternal(destinationCluster, onlineNodes, true);
  }

  private void unlockInternal(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes, boolean force) {
    logger.info("Trying to unlock the config...");
    runConfigurationChange(destinationCluster, onlineNodes, new UnlockConfigNomadChange(force));
    logger.info("Unlocked the config.");
    if (nomadManager instanceof LockAwareNomadManager) {
      this.nomadManager = ((LockAwareNomadManager<NodeContext>)nomadManager).getUnderlying();
    }
  }

  protected final void runTopologyChange(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes, TopologyNomadChange change) {
    logger.trace("runTopologyChange({}, {})", onlineNodes, change);
    NomadFailureReceiver<NodeContext> failures = new NomadFailureReceiver<>();
    nomadManager.runConfigurationChange(destinationCluster, onlineNodes, change, failures);
    failures.reThrowReasons();
  }

  /**
   * Runs a Nomad change by providing an ordered list of nodes, to send them the change in order
   * <p>
   * Nodes are expected to be online.
   */
  protected final void runClusterActivation(Collection<Endpoint> expectedOnlineNodes, Cluster cluster) {
    logger.trace("runClusterActivation({}, {})", expectedOnlineNodes, cluster.toShapeString());
    NomadFailureReceiver<NodeContext> failures = new NomadFailureReceiver<>();
    nomadManager.runClusterActivation(expectedOnlineNodes, cluster, failures);
    failures.reThrowReasons();
    logger.debug("Configuration directories have been created for all nodes");
  }

  protected final LogicalServerState getState(Endpoint expectedOnlineNode) {
    return getState(expectedOnlineNode.getAddress());
  }

  protected final LogicalServerState getState(InetSocketAddress expectedOnlineNode) {
    logger.trace("getUpcomingCluster({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      return diagnosticService.getLogicalServerState();
    }
  }

  protected final Cluster getUpcomingCluster(Endpoint expectedOnlineNode) {
    return getUpcomingCluster(expectedOnlineNode.getAddress());
  }

  protected final Cluster getUpcomingCluster(InetSocketAddress expectedOnlineNode) {
    logger.trace("getUpcomingCluster({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      return diagnosticService.getProxy(TopologyService.class).getUpcomingNodeContext().getCluster();
    }
  }

  protected final void setUpcomingCluster(Collection<Endpoint> expectedOnlineNodes, Cluster cluster) {
    logger.trace("setUpcomingCluster({})", expectedOnlineNodes);
    for (Endpoint expectedOnlineNode : expectedOnlineNodes) {
      try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode.getAddress())) {
        diagnosticService.getProxy(DynamicConfigService.class).setUpcomingCluster(cluster);
      }
    }
  }

  protected final Cluster getRuntimeCluster(Endpoint expectedOnlineNode) {
    return getRuntimeCluster(expectedOnlineNode.getAddress());
  }

  protected final Cluster getRuntimeCluster(InetSocketAddress expectedOnlineNode) {
    logger.trace("getRuntimeCluster({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      return diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext().getCluster();
    }
  }

  protected final Endpoint getEndpoint(InetSocketAddress expectedOnlineNode) {
    logger.trace("getEndpoint({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      return diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext().getNode().getEndpoint(expectedOnlineNode);
    }
  }

  protected final void restartNodes(Collection<Endpoint> endpoints, Duration maximumWaitTime, Duration restartDelay, Collection<LogicalServerState> acceptedStates) {
    logger.trace("restartNodes({}, {})", endpoints, maximumWaitTime);
    try {
      RestartProgress progress = restartService.restartNodes(
          endpoints,
          restartDelay,
          acceptedStates);
      progress.getErrors().forEach((address, e) -> logger.warn("Unable to ask node: {} to restart: please restart it manually.", address));
      progress.onRestarted((endpoint, state) -> logger.info("Node: {} has restarted in state: {}", endpoint, state));
      Map<Endpoint, LogicalServerState> restarted = progress.await(maximumWaitTime);
      // check where we are
      Collection<Endpoint> missing = new TreeSet<>(Comparator.comparing(Endpoint::toString));
      missing.addAll(endpoints);
      missing.removeAll(progress.getErrors().keySet()); // remove nodes that we were not able to contact
      missing.removeAll(restarted.keySet()); // remove nodes that have been restarted
      if (!missing.isEmpty()) {
        throw new IllegalStateException("Some nodes may have failed to restart within " + maximumWaitTime.getSeconds() + " seconds. " + lineSeparator() +
            "This should be confirmed by examining the state of the nodes listed below." + lineSeparator() +
            "Note: if the cluster did not have security configured before activation but has security configured post-activation, or vice-versa, " +
            "then the nodes may have in fact successfully restarted.  This should be confirmed.  Nodes:" + lineSeparator()
            + " - " + missing.stream().map(Endpoint::toString).collect(joining(lineSeparator() + " - ")));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Restart has been interrupted", e);
    }
  }

  protected final void stopNodes(Collection<Endpoint> addresses, Duration maximumWaitTime, Duration restartDelay) {
    logger.trace("stopNodes({}, {})", addresses, maximumWaitTime);
    try {
      StopProgress progress = stopService.stopNodes(addresses, restartDelay);
      progress.getErrors().forEach((address, e) -> logger.warn("Unable to ask node: {} to stop: please stop it manually.", address));
      progress.onStopped(endpoint -> logger.info("Node: {} has stopped", endpoint));
      Collection<Endpoint> stopped = progress.await(maximumWaitTime);
      // check where we are
      Collection<Endpoint> missing = new TreeSet<>(Comparator.comparing(Endpoint::toString));
      missing.addAll(addresses);
      missing.removeAll(progress.getErrors().keySet()); // remove nodes that we were not able to contact
      missing.removeAll(stopped); // remove nodes that have been restarted
      if (!missing.isEmpty()) {
        throw new IllegalStateException("Some nodes failed to stop within " + maximumWaitTime.getSeconds() + " seconds:" + lineSeparator()
            + " - " + missing.stream().map(Endpoint::toString).collect(joining(lineSeparator() + " - ")));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Stop has been interrupted", e);
    }
  }

  protected final Collection<Endpoint> findRuntimePeers(InetSocketAddress expectedOnlineNode) {
    logger.trace("findRuntimePeers({})", expectedOnlineNode);
    Cluster cluster = getRuntimeCluster(expectedOnlineNode);
    Collection<Endpoint> peers = cluster.getEndpoints(expectedOnlineNode);
    if (logger.isDebugEnabled()) {
      logger.debug("Discovered nodes:{} through: {}", toString(peers), expectedOnlineNode);
    }
    return peers;
  }

  protected final Map<Endpoint, LogicalServerState> findRuntimePeersStatus(InetSocketAddress expectedOnlineNode) {
    logger.trace("findRuntimePeersStatus({})", expectedOnlineNode);
    Cluster cluster = getRuntimeCluster(expectedOnlineNode);
    Collection<Endpoint> endpoints = cluster.getEndpoints(expectedOnlineNode);
    logger.info("Connecting to: {} (this can take time if some nodes are not reachable)", toString(endpoints));
    try (DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchDiagnosticServices(endpointsToMap(endpoints))) {
      LinkedHashMap<Endpoint, LogicalServerState> status = endpoints.stream()
          .collect(toMap(
              identity(),
              endpoint -> diagnosticServices.getDiagnosticService(endpoint.getNodeUID()).map(DiagnosticService::getLogicalServerState).orElse(UNREACHABLE),
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

  protected final Map<Endpoint, LogicalServerState> findOnlineRuntimePeers(Endpoint expectedOnlineNode) {
    return findOnlineRuntimePeers(expectedOnlineNode.getAddress());
  }

  protected final Map<Endpoint, LogicalServerState> findOnlineRuntimePeers(InetSocketAddress expectedOnlineNode) {
    logger.trace("findOnlineRuntimePeers({})", expectedOnlineNode);
    Map<Endpoint, LogicalServerState> nodes = findRuntimePeersStatus(expectedOnlineNode);
    return filterOnlineNodes(nodes);
  }

  protected final LinkedHashMap<Endpoint, LogicalServerState> filterOnlineNodes(Map<Endpoint, LogicalServerState> nodes) {
    return filter(nodes, (addr, state) -> !state.isUnknown() && !state.isUnreacheable());
  }

  protected final <K> LinkedHashMap<K, LogicalServerState> filter(Map<K, LogicalServerState> nodes, BiPredicate<K, LogicalServerState> predicate) {
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
  protected final void ensurePassivesAreAllOnline(Cluster cluster, Map<Endpoint, LogicalServerState> onlineNodes) {
    Collection<String> actives = onlineNodes.entrySet().stream().filter(e -> e.getValue().isActive()).map(Map.Entry::getKey).map(Endpoint::getNodeName).collect(toSet());
    Collection<String> passives = onlineNodes.entrySet().stream().filter(e -> e.getValue().isPassive()).map(Map.Entry::getKey).map(Endpoint::getNodeName).collect(toSet());
    Collection<String> expectedPassives = cluster.getNodes().stream().map(Node::getName).collect(toSet());
    expectedPassives.removeAll(actives);
    if (!passives.containsAll(expectedPassives)) {
      throw new IllegalStateException("Expected all nodes to be online, but nodes: " + toString(expectedPassives) + " are not");
    }
  }

  protected final void ensureActivesAreAllOnline(Cluster cluster, Map<Endpoint, LogicalServerState> onlineNodes) {
    if (onlineNodes.isEmpty()) {
      throw new IllegalStateException("Expected 1 active per stripe, but found no online node.");
    }
    // actives == list of current active nodes in the runtime topology
    List<String> actives = onlineNodes.entrySet().stream().filter(e -> e.getValue().isActive()).map(Map.Entry::getKey).map(Endpoint::getNodeName).collect(toList());
    // Check for stripe count. Whether there is a pending dynamic config change or not, the stripe count is not changing.
    // The stripe count only changes in case of a runtime topology change, which is another case.
    if (cluster.getStripeCount() != actives.size()) {
      throw new IllegalStateException("Expected 1 active per stripe, but only these nodes are active: " + toString(actives));
    }
  }

  protected final void ensureNodesAreEitherActiveOrPassive(Map<Endpoint, LogicalServerState> onlineNodes) {
    onlineNodes.forEach((addr, state) -> {
      if (!state.isActive() && !state.isPassive()) {
        throw new IllegalStateException("Unable to update node: " + addr + " that is currently in state: " + state
            + ". Please ensure all online nodes are either ACTIVE or PASSIVE before sending any update.");
      }
    });
  }

  protected final boolean isActivated(Endpoint expectedOnlineNode) {
    return isActivated(expectedOnlineNode.getAddress());
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

  protected final void reset(Endpoint expectedOnlineNode) {
    reset(expectedOnlineNode.getAddress());
  }

  protected final void reset(InetSocketAddress expectedOnlineNode) {
    logger.info("Reset node: {}", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode)) {
      DynamicConfigService proxy = diagnosticService.getProxy(DynamicConfigService.class);
      proxy.reset();
    }
  }

  protected final boolean areAllNodesActivated(Collection<Endpoint> expectedOnlineNodes) {
    logger.trace("areAllNodesActivated({})", expectedOnlineNodes);
    final Map<UID, InetSocketAddress> map = endpointsToMap(expectedOnlineNodes);
    try (DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(map)) {
      Map<Boolean, Collection<InetSocketAddress>> activations = topologyServices(diagnosticServices)
          .map(tuple -> tuple.map(identity(), TopologyService::isActivated))
          .collect(groupingBy(Tuple2::getT2, mapping(tuple -> map.get(tuple.getT1()), toCollection(() -> new TreeSet<>(Comparator.comparing(InetSocketAddress::toString))))));
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

  protected final void upgradeLicense(Collection<Endpoint> expectedOnlineNodes, Path licenseFile) {
    logger.trace("upgradeLicense({}, {})", expectedOnlineNodes, licenseFile);
    String xml;
    try {
      xml = new String(Files.readAllBytes(licenseFile), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    try (DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(endpointsToMap(expectedOnlineNodes))) {
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

  protected static Map<UID, InetSocketAddress> endpointsToMap(Collection<Endpoint> newNodes) {
    return newNodes.stream().collect(toMap(Endpoint::getNodeUID, Endpoint::getAddress));
  }

  protected static Stream<Tuple2<UID, TopologyService>> topologyServices(DiagnosticServices<UID> diagnosticServices) {
    return diagnosticServices.map((uid, diagnosticService) -> diagnosticService.getProxy(TopologyService.class));
  }

  protected static Stream<Tuple2<UID, DynamicConfigService>> dynamicConfigServices(DiagnosticServices<UID> diagnosticServices) {
    return diagnosticServices.map((uid, diagnosticService) -> diagnosticService.getProxy(DynamicConfigService.class));
  }

  protected static String toString(Collection<?> items) {
    return items.stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
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
