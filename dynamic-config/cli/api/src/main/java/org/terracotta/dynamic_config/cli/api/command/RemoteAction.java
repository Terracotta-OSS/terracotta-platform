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


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.terracotta.dynamic_config.api.model.LockTag;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.UnlockConfigNomadChange;
import org.terracotta.dynamic_config.api.service.ConfigurationConsistencyAnalyzer;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.nomad.LockAwareNomadManager;
import org.terracotta.dynamic_config.cli.api.nomad.NomadManager;
import org.terracotta.dynamic_config.cli.api.output.OutputService;
import org.terracotta.dynamic_config.cli.api.restart.RestartProgress;
import org.terracotta.dynamic_config.cli.api.restart.RestartService;
import org.terracotta.dynamic_config.cli.api.stop.StopProgress;
import org.terracotta.dynamic_config.cli.api.stop.StopService;
import org.terracotta.inet.HostPort;
import org.terracotta.json.Json;
import org.terracotta.nomad.client.results.NomadFailureReceiver;
import org.terracotta.nomad.server.ChangeRequestState;

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
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
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
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE_RELAY;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.SYNCHRONIZING;
import static org.terracotta.diagnostic.model.LogicalServerState.UNREACHABLE;

/**
 * @author Mathieu Carbou
 */
@SuppressFBWarnings("PA_PUBLIC_PRIMITIVE_ATTRIBUTE")
public abstract class RemoteAction implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemoteAction.class);

  @Inject
  public MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  @Inject
  public DiagnosticServiceProvider diagnosticServiceProvider;
  @Inject
  public NomadManager<NodeContext> nomadManager;
  @Inject
  public RestartService restartService;
  @Inject
  public StopService stopService;
  @Inject
  public OutputService output;
  @Inject
  public Json.Factory jsonFactory;

  protected String toPrettyJson(Object o) {
    return jsonFactory.pretty().create().toString(o);
  }

  protected final void licenseValidation(HostPort expectedOnlineNode, Cluster cluster) {
    LOGGER.trace("licenseValidation({}, {})", expectedOnlineNode, cluster);
    doWithTopologyService(expectedOnlineNode, topologyService -> {
      if (topologyService.validateAgainstLicense(cluster)) {
        output.info("License validation passed: configuration change(s) can be applied");
      } else {
        output.info("License validation skipped: no license installed");
      }
    });
  }

  private void activateNomadSystem(Collection<Endpoint> newNodes, Cluster cluster, String licenseContent) {
    output.info("Activating nodes: " + toString(newNodes));

    try (DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(endpointsToMap(newNodes))) {
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(service -> service.activate(cluster, licenseContent));
      if (licenseContent == null) {
        output.info("No license specified for activation. If a license was previously configured, it will take effect. If you are attaching a node, the license will be synced.");
      } else {
        output.info("License installation successful");
      }
    }
  }

  private void restartNodes(Collection<Endpoint> newNodes, Measure<TimeUnit> restartDelay, Measure<TimeUnit> restartWaitTime) {
    output.info("Restarting nodes: " + toString(newNodes));
    restartNodes(
        newNodes,
        Duration.ofMillis(restartWaitTime.getQuantity(TimeUnit.MILLISECONDS)),
        Duration.ofMillis(restartDelay.getQuantity(TimeUnit.MILLISECONDS)),
        // these are the list of states that we allow to consider a server has restarted
        // In dynamic config, restarted means that a node has reach a state that is after the STARTING state
        // and has consequently bootstrapped the configuration from Nomad.
        EnumSet.of(ACTIVE, ACTIVE_RECONNECTING, ACTIVE_SUSPENDED, PASSIVE, PASSIVE_SUSPENDED, SYNCHRONIZING, PASSIVE_RELAY));
    output.info("All nodes came back up");
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

    syncNomadChangesTo(newNodes, getChangeHistory(destination), cluster);

    restartNodes(newNodes, restartDelay, restartWaitTime);
  }

  private Optional<String> getLicenseContentFrom(Endpoint expectedOnlineNode) {
    LOGGER.trace("getLicenseContent({})", expectedOnlineNode);
    return withDynamicConfigService(expectedOnlineNode, DynamicConfigService::getLicenseContent);
  }

  protected final NomadChangeInfo[] getChangeHistory(Endpoint node) {
    return getChangeHistory(node.getHostPort());
  }

  protected final NomadChangeInfo[] getChangeHistory(HostPort expectedOnlineNode) {
    LOGGER.trace("getChangeHistory({})", expectedOnlineNode);
    return withTopologyService(expectedOnlineNode, TopologyService::getChangeHistory);
  }

  private void syncNomadChangesTo(Collection<Endpoint> newNodes, NomadChangeInfo[] nomadChanges, Cluster cluster) {
    output.info("Sync'ing nomad changes to nodes : {}", toString(newNodes));

    try (DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(endpointsToMap(newNodes))) {
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(service -> service.resetAndSync(nomadChanges, cluster));
      output.info("Nomad changes sync successful");
    }
  }

  protected final boolean mustBeRestarted(Endpoint endpoint) {
    return mustBeRestarted(endpoint.getHostPort());
  }

  protected final boolean mustBeRestarted(HostPort expectedOnlineNode) {
    LOGGER.trace("mustBeRestarted({})", expectedOnlineNode);
    return withTopologyService(expectedOnlineNode, TopologyService::mustBeRestarted);
  }

  protected final boolean hasIncompleteChange(Endpoint endpoint) {
    return hasIncompleteChange(endpoint.getHostPort());
  }

  protected final boolean hasIncompleteChange(HostPort expectedOnlineNode) {
    LOGGER.trace("hasIncompleteChange({})", expectedOnlineNode);
    return withTopologyService(expectedOnlineNode, TopologyService::hasIncompleteChange);
  }

  /**
   * Returns the current consistency of the configuration in the cluster
   */
  protected final ConfigurationConsistencyAnalyzer analyzeNomadConsistency(Map<Endpoint, LogicalServerState> allNodes) {
    LOGGER.trace("analyzeNomadConsistency({})", allNodes);
    Map<HostPort, LogicalServerState> addresses = allNodes.entrySet().stream().collect(toMap(e -> e.getKey().getHostPort(), Map.Entry::getValue));
    ConfigurationConsistencyAnalyzer configurationConsistencyAnalyzer = new ConfigurationConsistencyAnalyzer(addresses);
    nomadManager.runConfigurationDiscovery(allNodes, configurationConsistencyAnalyzer);
    return configurationConsistencyAnalyzer;
  }

  /**
   * Runs a Nomad recovery by providing a map of activated nodes plus their state.
   * This method will create an ordered list of nodes to contact by moving the passives first and actives last.
   */
  protected final void runConfigurationRepair(Map<Endpoint, LogicalServerState> onlineActivatedNodes, int totalNodeCount, ChangeRequestState forcedState) {
    LOGGER.trace("runConfigurationRepair({}, {})", toString(onlineActivatedNodes.keySet()), forcedState);
    NomadFailureReceiver<NodeContext> failures = new NomadFailureReceiver<>();
    nomadManager.runConfigurationRepair(onlineActivatedNodes, totalNodeCount, failures, forcedState);
    failures.reThrowReasons();
  }

  /**
   * Runs a Nomad change by providing a map of activated nodes plus their state.
   * This method will create an ordered list of nodes to contact by moving the passives first and actives last.
   * <p>
   * Nodes are expected to be online.
   */
  protected final void runConfigurationChange(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes, DynamicConfigNomadChange change) {
    LOGGER.trace("runConfigurationChange({}, {})", onlineNodes, change);
    NomadFailureReceiver<NodeContext> failures = new NomadFailureReceiver<>();
    nomadManager.runConfigurationChange(destinationCluster, onlineNodes, change, failures);
    failures.reThrowReasons();
  }

  protected final void runConfigurationChangeThroughDiagnostic(Map<Endpoint, LogicalServerState> onlineNodes, DynamicConfigNomadChange change) {
    NomadFailureReceiver<NodeContext> failures = new NomadFailureReceiver<>();
    nomadManager.runConfigurationChangeThroughDiagnostic(onlineNodes, change, failures);
    failures.reThrowReasons();
  }

  protected final Optional<HostPort> findScalingVetoer(Map<Endpoint, LogicalServerState> endpoints) {
    return findScalingVetoer(endpoints.keySet().stream().map(Endpoint::getHostPort).collect(toList()));
  }

  protected final Optional<HostPort> findScalingVetoer(Collection<HostPort> onlineNodes) {
    LOGGER.trace("findScaleInVeto({})", onlineNodes);
    // try to find at least one node not allowing the operation
    return onlineNodes.stream()
        .filter(hostPort -> withTopologyService(hostPort, TopologyService::isScalingDenied))
        .findFirst();
  }


  protected final void tryCatch(Runnable run, Consumer<RuntimeException> onError) {
    try {
      run.run();
    } catch (RuntimeException first) {
      try {
        onError.accept(first);
      } catch (RuntimeException second) {
        first.addSuppressed(second);
      }
      throw first;
    }
  }

  protected final void tryFinally(Runnable run, Runnable cleanup) {
    RuntimeException original = null;
    try {
      run.run();
    } catch (RuntimeException one) {
      original = one;
      throw original;
    } finally {
      if (original == null) {
        cleanup.run();
      } else {
        try {
          cleanup.run();
        } catch (RuntimeException two) {
          original.addSuppressed(two);
        }
      }
    }
  }

  protected final void allowScaling(Cluster cluster, Map<Endpoint, LogicalServerState> onlineNodes) {
    tryFinally(
        () -> lock(cluster, onlineNodes, LockTag.OWNER_PLATFORM, LockTag.ALLOW_SCALING),
        isLockAwareNomadManager() ? () -> {} : () -> unlock(cluster, onlineNodes));
  }

  protected final void denyScaleOut(Cluster cluster, Map<Endpoint, LogicalServerState> onlineNodes) {
    tryFinally(
        () -> lock(cluster, onlineNodes, LockTag.OWNER_PLATFORM, LockTag.DENY_SCALE_OUT),
        isLockAwareNomadManager() ? () -> {} : () -> unlock(cluster, onlineNodes));
  }

  protected final void denyScaleIn(Cluster cluster, Map<Endpoint, LogicalServerState> onlineNodes) {
    tryFinally(
        () -> lock(cluster, onlineNodes, LockTag.OWNER_PLATFORM, LockTag.DENY_SCALE_IN),
        isLockAwareNomadManager() ? () -> {} : () -> unlock(cluster, onlineNodes));
  }

  protected final String lock(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes, String ownerName, String tags) {
    // if we are currently running a locked nomad manager, and we want to add some new tags, we can add a new lock entry but keeping the same lock token
    String token = findLockAwareNomadManager().map(LockAwareNomadManager::getLockToken).orElseGet(() -> UUID.randomUUID().toString());
    return lock(destinationCluster, onlineNodes, new LockContext(token, ownerName, tags));
  }

  protected final String lock(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes, LockContext lockContext) {
    LOGGER.trace("lock({})", lockContext);
    runConfigurationChange(destinationCluster, onlineNodes, new LockConfigNomadChange(lockContext));
    // user must see the lock token
    LOGGER.trace("Config locked.");
    LOGGER.trace("Token: " + lockContext.getToken());
    LOGGER.trace("Reason: " + lockContext.getOwnerTags());
    // If the current nomad manager is already locked, unwrap since the lock might have changed
    if (nomadManager instanceof LockAwareNomadManager) {
      nomadManager = ((LockAwareNomadManager<NodeContext>) nomadManager).getUnderlying();
    }
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
    output.info("Trying to unlock the config...");
    runConfigurationChange(destinationCluster, onlineNodes, new UnlockConfigNomadChange(force));
    output.info("Config unlocked.");
    if (nomadManager instanceof LockAwareNomadManager) {
      this.nomadManager = ((LockAwareNomadManager<NodeContext>) nomadManager).getUnderlying();
    }
  }

  @SuppressFBWarnings("BC_VACUOUS_INSTANCEOF")
  protected final Optional<LockAwareNomadManager<NodeContext>> findLockAwareNomadManager() {
    return isLockAwareNomadManager() ? Optional.of((LockAwareNomadManager<NodeContext>) nomadManager) : Optional.empty();
  }

  protected final boolean isLockAwareNomadManager() {
    return nomadManager instanceof LockAwareNomadManager;
  }

  protected final void runTopologyChange(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes, TopologyNomadChange change) {
    LOGGER.trace("runTopologyChange({}, {})", onlineNodes, change);
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
    LOGGER.trace("runClusterActivation({}, {})", expectedOnlineNodes, cluster.toShapeString());
    NomadFailureReceiver<NodeContext> failures = new NomadFailureReceiver<>();
    nomadManager.runClusterActivation(expectedOnlineNodes, cluster, failures);
    failures.reThrowReasons();
    LOGGER.debug("Configuration directories have been created for all nodes");
  }

  protected final LogicalServerState getLogicalServerState(Endpoint expectedOnlineNode) {
    return getLogicalServerState(expectedOnlineNode.getHostPort());
  }

  protected final LogicalServerState getLogicalServerState(HostPort expectedOnlineNode) {
    LOGGER.trace("getLogicalServerState({})", expectedOnlineNode);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(expectedOnlineNode.createInetSocketAddress())) {
      return diagnosticService.getLogicalServerState();
    }
  }

  protected final Map<Endpoint, LogicalServerState> getLogicalServerStates(Collection<Endpoint> endpoints) {
    LOGGER.trace("getLogicalServerStates({})", endpoints);
    // null parameter is important here because some servers can be down
    try (DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchDiagnosticServices(endpointsToMap(endpoints), null)) {
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
          output.info(" - {} is not reachable", address);
        }
      });
      return status;
    }
  }

  protected final Cluster getUpcomingCluster(Endpoint expectedOnlineNode) {
    return getUpcomingCluster(expectedOnlineNode.getHostPort());
  }

  protected final Cluster getUpcomingCluster(HostPort expectedOnlineNode) {
    LOGGER.trace("getUpcomingCluster({})", expectedOnlineNode);
    return withTopologyService(expectedOnlineNode, TopologyService::getUpcomingNodeContext).getCluster();
  }

  protected final Cluster getUpcomingCluster(Collection<HostPort> nodes) {
    LOGGER.trace("getUpcomingCluster({})", nodes);
    return withAnyOnlineDiagnosticService(nodes, (hostPort, diagnosticService) -> diagnosticService.getProxy(TopologyService.class).getUpcomingNodeContext().getCluster());
  }

  protected final void setUpcomingCluster(Collection<Endpoint> expectedOnlineNodes, Cluster cluster) {
    LOGGER.trace("setUpcomingCluster({})", expectedOnlineNodes);
    expectedOnlineNodes.forEach(endpoint -> doWithDynamicConfigService(endpoint, dynamicConfigService -> dynamicConfigService.setUpcomingCluster(cluster)));
  }

  protected final Cluster getRuntimeCluster(Endpoint expectedOnlineNode) {
    return getRuntimeCluster(expectedOnlineNode.getHostPort());
  }

  protected final Cluster getRuntimeCluster(HostPort expectedOnlineNode) {
    LOGGER.trace("getRuntimeCluster({})", expectedOnlineNode);
    return getRuntimeNodeContext(expectedOnlineNode).t2.getCluster();
  }

  /**
   * Try to connect to one of the nodes to get the cluster topology.
   * At least one node must be online.
   */
  protected final Cluster getRuntimeCluster(Collection<HostPort> nodes) {
    LOGGER.trace("getRuntimeCluster({})", nodes);
    return withAnyOnlineDiagnosticService(nodes, (hostPort, diagnosticService) -> diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext().getCluster());
  }

  protected final <V> V withAnyOnlineDiagnosticService(Collection<HostPort> nodes, BiFunction<HostPort, DiagnosticService, V> fn) {
    try (DiagnosticServices<HostPort> diagnosticServices = multiDiagnosticServiceProvider.fetchAnyOnlineDiagnosticService(toAddr(nodes, identity(), HostPort::createInetSocketAddress))) {
      final Map.Entry<HostPort, DiagnosticService> entry = diagnosticServices.getOnlineEndpoints().entrySet().iterator().next();
      return fn.apply(entry.getKey(), entry.getValue());
    }
  }

  protected final <R> R withTopologyService(Endpoint endpoint, Function<TopologyService, R> fn) {
    return withTopologyService(endpoint.getHostPort(), fn);
  }

  protected final <R> R withTopologyService(HostPort hostPort, Function<TopologyService, R> fn) {
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(hostPort.createInetSocketAddress())) {
      return fn.apply(diagnosticService.getProxy(TopologyService.class));
    }
  }

  protected final void doWithTopologyService(Endpoint endpoint, Consumer<TopologyService> fn) {
    doWithTopologyService(endpoint.getHostPort(), fn);
  }

  protected final void doWithTopologyService(HostPort hostPort, Consumer<TopologyService> fn) {
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(hostPort.createInetSocketAddress())) {
      fn.accept(diagnosticService.getProxy(TopologyService.class));
    }
  }

  protected final <R> R withDynamicConfigService(Endpoint endpoint, Function<DynamicConfigService, R> fn) {
    return withDynamicConfigService(endpoint.getHostPort(), fn);
  }

  protected final <R> R withDynamicConfigService(HostPort hostPort, Function<DynamicConfigService, R> fn) {
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(hostPort.createInetSocketAddress())) {
      return fn.apply(diagnosticService.getProxy(DynamicConfigService.class));
    }
  }

  protected final void doWithDynamicConfigService(Endpoint endpoint, Consumer<DynamicConfigService> fn) {
    doWithDynamicConfigService(endpoint.getHostPort(), fn);
  }

  protected final void doWithDynamicConfigService(HostPort hostPort, Consumer<DynamicConfigService> fn) {
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(hostPort.createInetSocketAddress())) {
      fn.accept(diagnosticService.getProxy(DynamicConfigService.class));
    }
  }

  /**
   * This method will connect to the node using the provided address from the user.
   * It will grab the topology on this node and compare the address used to connect to
   * with the node addresses to determine the endpoint and group we have to use to connect
   * to other nodes (group: through bind addresses, hostname:port, or public addresses)
   */
  protected final Endpoint getEndpoint(HostPort expectedOnlineNode) {
    LOGGER.trace("getEndpoint({})", expectedOnlineNode);
    return getRuntimeNodeContext(expectedOnlineNode).t1;
  }

  protected final Tuple2<Endpoint, NodeContext> getRuntimeNodeContext(HostPort expectedOnlineNode) {
    LOGGER.trace("getRuntimeNodeContext({})", expectedOnlineNode);
    final NodeContext nodeContext = withTopologyService(expectedOnlineNode, TopologyService::getRuntimeNodeContext);
    return Tuple2.tuple2(nodeContext.getNode().determineEndpoint(expectedOnlineNode), nodeContext);
  }

  protected final Tuple2<Endpoint, NodeContext> getUpcomingNodeContext(HostPort expectedOnlineNode) {
    LOGGER.trace("getUpcomingNodeContext({})", expectedOnlineNode);
    final NodeContext nodeContext = withTopologyService(expectedOnlineNode, TopologyService::getUpcomingNodeContext);
    return Tuple2.tuple2(nodeContext.getNode().determineEndpoint(expectedOnlineNode), nodeContext);
  }

  protected final void restartNodes(Collection<Endpoint> endpoints, Duration maximumWaitTime, Duration restartDelay, Collection<LogicalServerState> acceptedStates) {
    LOGGER.trace("restartNodes({}, {})", endpoints, maximumWaitTime);
    RestartProgress progress = restartService.restartNodes(
        endpoints,
        restartDelay,
        acceptedStates);
    followRestart(progress, endpoints, maximumWaitTime);
  }

  protected final void restartNodesIfActives(Collection<Endpoint> endpoints, Duration maximumWaitTime, Duration restartDelay, Collection<LogicalServerState> acceptedStates) {
    LOGGER.trace("restartNodesIfActives({}, {})", endpoints, maximumWaitTime);
    RestartProgress progress = restartService.restartNodesIfActives(
        endpoints,
        restartDelay,
        acceptedStates);
    followRestart(progress, endpoints, maximumWaitTime);
  }

  protected final void restartNodesIfPassives(Collection<Endpoint> endpoints, Duration maximumWaitTime, Duration restartDelay, Collection<LogicalServerState> acceptedStates) {
    LOGGER.trace("restartNodesIfPassives({}, {})", endpoints, maximumWaitTime);
    RestartProgress progress = restartService.restartNodesIfPassives(
        endpoints,
        restartDelay,
        acceptedStates);
    followRestart(progress, endpoints, maximumWaitTime);
  }

  private void followRestart(RestartProgress progress, Collection<Endpoint> endpoints, Duration maximumWaitTime) {
    try {
      progress.getErrors().forEach((address, e) -> LOGGER.warn("Unable to ask node: {} to restart: please restart it manually.", address));
      progress.onRestarted((endpoint, state) -> output.info("Node: {} has restarted in state: {}", endpoint, state));
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
    LOGGER.trace("stopNodes({}, {})", addresses, maximumWaitTime);
    try {
      StopProgress progress = stopService.stopNodes(addresses, restartDelay);
      progress.getErrors().forEach((address, e) -> LOGGER.warn("Unable to ask node: {} to stop: please stop it manually.", address));
      progress.onStopped(endpoint -> output.info("Node: {} has stopped", endpoint));
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

  protected final Collection<Endpoint> findRuntimePeers(HostPort expectedOnlineNode) {
    LOGGER.trace("findRuntimePeers({})", expectedOnlineNode);
    Cluster cluster = getRuntimeCluster(expectedOnlineNode);
    Collection<Endpoint> peers = cluster.determineEndpoints(expectedOnlineNode);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Discovered nodes:{} through: {}", toString(peers), expectedOnlineNode);
    }
    return peers;
  }

  protected final Map<Endpoint, LogicalServerState> findRuntimePeersStatus(HostPort expectedOnlineNode) {
    LOGGER.trace("findRuntimePeersStatus({})", expectedOnlineNode);
    Cluster cluster = getRuntimeCluster(expectedOnlineNode);
    Collection<Endpoint> endpoints = cluster.determineEndpoints(expectedOnlineNode);
    output.info("Connecting to: {} (this can take time if some nodes are not reachable)", toString(endpoints));
    return getLogicalServerStates(endpoints);
  }

  protected final Map<Endpoint, LogicalServerState> findRuntimePeersStatus(Collection<HostPort> expectedOnlineNodes) {
    LOGGER.trace("findRuntimePeersStatus({})", expectedOnlineNodes);
    Cluster cluster = getRuntimeCluster(expectedOnlineNodes);
    final Collection<Endpoint> endpoints = cluster.determineEndpoints(expectedOnlineNodes);
    output.info("Connecting to: {} (this can take time if some nodes are not reachable)", toString(endpoints));
    return getLogicalServerStates(endpoints);
  }

  protected final Map<Endpoint, LogicalServerState> findOnlineRuntimePeers(Endpoint expectedOnlineNode) {
    return findOnlineRuntimePeers(expectedOnlineNode.getHostPort());
  }

  protected final Map<Endpoint, LogicalServerState> findOnlineRuntimePeers(HostPort expectedOnlineNode) {
    LOGGER.trace("findOnlineRuntimePeers({})", expectedOnlineNode);
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
    // current actives
    Collection<String> actives = onlineNodes.entrySet()
        .stream()
        .filter(e -> e.getValue().isActive())
        .map(Map.Entry::getKey)
        .map(Endpoint::getNodeName)
        .collect(Collectors.toCollection(TreeSet::new));
    // current passives
    Collection<String> passives = onlineNodes.entrySet()
        .stream()
        .filter(e -> e.getValue().isPassive())
        .map(Map.Entry::getKey)
        .map(Endpoint::getNodeName)
        .collect(Collectors.toCollection(TreeSet::new));
    // expected passives
    Collection<String> expectedPassives = cluster.getNodes()
        .stream()
        .map(Node::getName)
        .collect(Collectors.toCollection(TreeSet::new));
    expectedPassives.removeAll(actives);
    if (!passives.containsAll(expectedPassives)) {
      Collection<String> missing = new TreeSet<>(expectedPassives);
      missing.removeAll(passives);
      throw new IllegalStateException("Expected these nodes to be in PASSIVE state: " + toString(expectedPassives) + ", but nodes: " + toString(missing) + " are not");
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
    for (Map.Entry<Endpoint, LogicalServerState> entry : onlineNodes.entrySet()) {
      if (entry.getValue().isStarting() || entry.getValue().isSynchronizing()) {
        // this node will become passive in a few... Just wait instead of failing...
        LOGGER.info("Waiting for node: {} to become passive or active...", entry.getKey());
        while (entry.getValue().isSynchronizing() || entry.getValue().isStarting()) {
          try {
            Thread.sleep(1_000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
          entry.setValue(getLogicalServerState(entry.getKey()));
        }
        LOGGER.info("Node: {} is: {}", entry.getKey(), entry.getValue());
      }
    }

    onlineNodes.forEach((addr, state) -> {
      if (!state.isActive() && !state.isPassive()) {
        throw new IllegalStateException("Unable to update node: " + addr + " that is currently in state: " + state
            + ". Please ensure all online nodes are either ACTIVE or PASSIVE before sending any update.");
      }
    });
  }

  protected final boolean isActivated(Endpoint expectedOnlineNode) {
    return isActivated(expectedOnlineNode.getHostPort());
  }

  protected final boolean isActivated(HostPort expectedOnlineNode) {
    LOGGER.trace("isActivated({})", expectedOnlineNode);
    return withTopologyService(expectedOnlineNode, TopologyService::isActivated);
  }

  protected final Endpoint findAnyOnlineNode(Collection<HostPort> nodes) {
    LOGGER.trace("findAnyOnlineNode({})", nodes);
    final NodeContext nodeContext = withAnyOnlineDiagnosticService(nodes, (hostPort, diagnosticService) -> diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext());
    final Cluster cluster = nodeContext.getCluster();
    return cluster.determineEndpoint(nodeContext.getNodeUID(), nodes).get(); // get() because node is not missing
  }

  protected final void resetAndStop(HostPort expectedOnlineNode) {
    output.info("Reset node: {}. Node will stop...", expectedOnlineNode);
    doWithDynamicConfigService(expectedOnlineNode, proxy -> {
      proxy.reset();
      proxy.stop(Duration.ofSeconds(5));
    });
  }

  protected final void reset(Endpoint expectedOnlineNode) {
    output.info("Reset node: {}", expectedOnlineNode.getHostPort());
    doWithDynamicConfigService(expectedOnlineNode, DynamicConfigService::reset);
  }

  protected final boolean areAllNodesActivated(Collection<Endpoint> expectedOnlineNodes) {
    LOGGER.trace("areAllNodesActivated({})", expectedOnlineNodes);
    final Map<UID, InetSocketAddress> map = endpointsToMap(expectedOnlineNodes);
    try (DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(map)) {
      final Map<UID, HostPort> map2 = map.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> HostPort.create(e.getValue())));
      Map<Boolean, Collection<HostPort>> activations = topologyServices(diagnosticServices)
          .map(tuple -> tuple.map(identity(), TopologyService::isActivated))
          .collect(groupingBy(Tuple2::getT2, mapping(tuple -> map2.get(tuple.getT1()), toCollection(() -> new TreeSet<>(Comparator.comparing(HostPort::toString))))));
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
    LOGGER.trace("upgradeLicense({}, {})", expectedOnlineNodes, licenseFile);
    final String xml;
    try {
      xml = licenseFile == null ? null : new String(Files.readAllBytes(licenseFile), StandardCharsets.UTF_8);
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
              LOGGER.debug("License upgrade failed on node {}: {}", tuple.t1, e.getMessage());
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

  protected static Map<UID, InetSocketAddress> endpointsToMap(Collection<Endpoint> nodes) {
    return toAddr(nodes, Endpoint::getNodeUID, endpoint -> endpoint.getHostPort().createInetSocketAddress());
  }

  protected static Map<HostPort, InetSocketAddress> hostPortsToMap(Collection<HostPort> nodes) {
    return toAddr(nodes, identity(), HostPort::createInetSocketAddress);
  }

  protected static <K, E> Map<K, InetSocketAddress> toAddr(Collection<E> keys, Function<E, K> key, Function<E, InetSocketAddress> val) {
    return keys.stream().collect(toMap(key, val, (res, el) -> el));
  }

  protected static <K> Stream<Tuple2<K, TopologyService>> topologyServices(DiagnosticServices<K> diagnosticServices) {
    return diagnosticServices.map((uid, diagnosticService) -> diagnosticService.getProxy(TopologyService.class));
  }

  protected static <K> Stream<Tuple2<K, DynamicConfigService>> dynamicConfigServices(DiagnosticServices<K> diagnosticServices) {
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
