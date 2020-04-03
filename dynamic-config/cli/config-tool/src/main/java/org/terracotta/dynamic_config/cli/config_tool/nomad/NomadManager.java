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
package org.terracotta.dynamic_config.cli.config_tool.nomad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.entity.Entity;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeNomadChange;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.change.ChangeResultReceiver;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.client.results.LoggingResultReceiver;
import org.terracotta.nomad.client.results.MultiChangeResultReceiver;
import org.terracotta.nomad.client.results.MultiRecoveryResultReceiver;
import org.terracotta.nomad.client.status.MultiDiscoveryResultReceiver;
import org.terracotta.nomad.entity.client.NomadEntity;
import org.terracotta.nomad.entity.client.NomadEntityProvider;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE_RECONNECTING;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.STARTING;

public class NomadManager<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadManager.class);

  private static final EnumSet<LogicalServerState> ALLOWED = EnumSet.of(
      ACTIVE,
      PASSIVE,
      ACTIVE_RECONNECTING,
      STARTING // this mode is when a server is forced to start in diagnostic mode for repair
  );

  private final NomadEnvironment environment;
  private final MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  private final NomadEntityProvider nomadEntityProvider;

  public NomadManager(NomadEnvironment environment, MultiDiagnosticServiceProvider multiDiagnosticServiceProvider, NomadEntityProvider nomadEntityProvider) {
    this.environment = environment;
    this.multiDiagnosticServiceProvider = multiDiagnosticServiceProvider;
    this.nomadEntityProvider = nomadEntityProvider;
  }

  public void runConfigurationDiscovery(Map<InetSocketAddress, LogicalServerState> nodes, DiscoverResultsReceiver<T> results) {
    LOGGER.debug("Attempting to discover nodes: {}", nodes);
    List<InetSocketAddress> orderedList = keepOnlineAndOrderPassivesFirst(nodes);
    try (NomadClient<T> client = createDiagnosticNomadClient(orderedList)) {
      client.tryDiscovery(new MultiDiscoveryResultReceiver<>(asList(new LoggingResultReceiver<>(), results)));
    }
  }

  public void runClusterActivation(Collection<InetSocketAddress> nodes, Cluster cluster, ChangeResultReceiver<T> results) {
    LOGGER.debug("Attempting to activate cluster: {}", cluster.toShapeString());
    try (NomadClient<T> client = createDiagnosticNomadClient(new ArrayList<>(nodes))) {
      client.tryApplyChange(new MultiChangeResultReceiver<>(asList(new LoggingResultReceiver<>(), results)), new ClusterActivationNomadChange(cluster));
    }
  }

  public void runConfigurationChange(Map<InetSocketAddress, LogicalServerState> onlineNodes, MultiSettingNomadChange changes, ChangeResultReceiver<T> results) {
    LOGGER.debug("Attempting to make co-ordinated configuration change: {} on nodes: {}", changes, onlineNodes);
    checkServerStates(onlineNodes);
    List<InetSocketAddress> orderedList = keepOnlineAndOrderPassivesFirst(onlineNodes);
    try (NomadClient<T> client = createDiagnosticNomadClient(orderedList)) {
      client.tryApplyChange(new MultiChangeResultReceiver<>(asList(new LoggingResultReceiver<>(), results)), changes);
    }
  }

  public void runConfigurationRepair(Map<InetSocketAddress, LogicalServerState> nodes, RecoveryResultReceiver<T> results, ChangeRequestState forcedState) {
    LOGGER.debug("Attempting to repair configuration on nodes: {}", nodes);
    List<InetSocketAddress> orderedList = keepOnlineAndOrderPassivesFirst(nodes);
    try (NomadClient<T> client = createDiagnosticNomadClient(orderedList)) {
      client.tryRecovery(new MultiRecoveryResultReceiver<>(asList(new LoggingResultReceiver<>(), results)), nodes.size(), forcedState);
    }
  }

  public void runTopologyChange(Cluster destinationCluster, Map<InetSocketAddress, LogicalServerState> onlineNodes, NodeNomadChange change, ChangeResultReceiver<T> results) {
    LOGGER.debug("Attempting to apply topology change: {} on cluster {}", change, destinationCluster);
    checkServerStates(onlineNodes);
    try (NomadClient<T> client = createTopologyChangeNomadClient(destinationCluster, onlineNodes)) {
      client.tryApplyChange(new MultiChangeResultReceiver<>(asList(new LoggingResultReceiver<>(), results)), change);
    }
  }

  private NomadClient<T> createDiagnosticNomadClient(List<InetSocketAddress> expectedOnlineNodes) {
    LOGGER.trace("createDiagnosticNomadClient({})", expectedOnlineNodes);
    // create normal diagnostic endpoints
    List<NomadEndpoint<T>> nomadEndpoints = createDiagnosticNomadEndpoints(expectedOnlineNodes);
    // create the client
    String host = environment.getHost();
    String user = environment.getUser();
    Clock clock = environment.getClock();
    return new NomadClient<>(nomadEndpoints, host, user, clock);
  }

  private NomadClient<T> createTopologyChangeNomadClient(Cluster destinationCluster, Map<InetSocketAddress, LogicalServerState> onlineNodes) {
    LOGGER.trace("createPassiveChangeNomadClient({}, {})", destinationCluster, onlineNodes);

    checkServerStates(onlineNodes);

    // collect all online addresses by stripe
    List<List<InetSocketAddress>> onlineNodesPerStripe = destinationCluster.getStripes().stream().map(stripe -> {
      List<InetSocketAddress> stripeNodes = new ArrayList<>(stripe.getNodeAddresses());
      stripeNodes.retainAll(onlineNodes.keySet());
      return stripeNodes;
    }).collect(toList());

    // throw if stripe not online
    for (int i = 0; i < onlineNodesPerStripe.size(); i++) {
      final List<InetSocketAddress> addresses = onlineNodesPerStripe.get(i);
      if (addresses.isEmpty()) {
        throw new IllegalStateException("Entire stripe ID " + (i + 1) + " is not online in cluster: " + destinationCluster);
      }
    }

    // connect to each stripes ans get the Nomad entity
    final List<NomadEntity<T>> nomadEntities = new ArrayList<>(onlineNodesPerStripe.size());
    final Runnable cleanup = () -> nomadEntities.forEach(Entity::close); // close() implementation is not expected to throw. See implementation code.
    for (int i = 0; i < onlineNodesPerStripe.size(); i++) {
      final List<InetSocketAddress> addresses = onlineNodesPerStripe.get(i);
      try {
        LOGGER.trace("Connecting to stripe ID: {} using nodes: {}", i + 1, addresses);
        nomadEntities.add(nomadEntityProvider.fetchNomadEntity(addresses));
      } catch (ConnectionException e) {
        cleanup.run();
        throw new IllegalStateException("Unable to connect to stripe ID " + (i + 1) + ": " + e.getMessage(), e);
      }
    }

    // create a nomad endpoint per stripe for the commit phase
    final List<NomadEndpoint<T>> stripeEndpoints = IntStream.range(0, nomadEntities.size()).mapToObj(i -> {
      final InetSocketAddress firstAddress = onlineNodesPerStripe.get(i).get(0);
      return new NomadEndpoint<T>(firstAddress, nomadEntities.get(i));
    }).collect(toList());

    // create normal diagnostic endpoints for the prepare phase
    List<NomadEndpoint<T>> nomadEndpoints;
    try {
      List<InetSocketAddress> orderedList = keepOnlineAndOrderPassivesFirst(onlineNodes);
      Collections.reverse(orderedList); // put actives first
      LOGGER.trace("Connecting to diagnostic ports: {}", orderedList);
      nomadEndpoints = createDiagnosticNomadEndpoints(orderedList);
    } catch (RuntimeException e) {
      // close the entity channels if we cannot manage to open the endpoints
      cleanup.run();
      throw e;
    }

    // override the diagnostic endpoints to go over the entity channel for the nomad commit phase
    ConcurrentMap<Integer, CompletableFuture<AcceptRejectResponse>> cache = new ConcurrentHashMap<>(stripeEndpoints.size());
    nomadEndpoints = nomadEndpoints.stream().map(e -> new NomadEndpoint<T>(e.getAddress(), e) {
      @SuppressWarnings("OptionalGetWithoutIsPresent")
      @Override
      public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
        // This method is called for each online node.
        // But for a stripe, we only need to do 1 call, to the active, which will be replicated to the passive servers.
        // So we cache the first response we got from a stripe, to return it immediately after for the other calls.
        // We consider that the commit response on the passive servers will be the same on the active servers.
        InetSocketAddress address = getAddress();
        int stripeId = destinationCluster.getStripeId(address).getAsInt();
        CompletableFuture<AcceptRejectResponse> result = cache.computeIfAbsent(stripeId, sid -> {
          LOGGER.info("Committing topology change to stripe ID: {}... (this operation is blocking and can take time in case a failover happens)", stripeId);

          LOGGER.trace("Sending commit message: {} to stripe ID: {}", message, stripeId);
          CompletableFuture<AcceptRejectResponse> c = new CompletableFuture<>();
          try {
            AcceptRejectResponse acceptRejectResponse = stripeEndpoints.get(stripeId - 1).commit(message);
            LOGGER.trace("Received commit response: {} from stripe ID: {}", message, stripeId);
            c.complete(acceptRejectResponse);
          } catch (NomadException | RuntimeException e) {
            LOGGER.trace("Received commit failure: '{}' from stripe ID: {}", e.getMessage(), stripeId, e);
            c.completeExceptionally(e);
          }
          return c;
        });
        try {
          return result.get();
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new NomadException(ie);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof Error) {
            throw (Error) cause;
          }
          if (cause instanceof NomadException) {
            throw (NomadException) cause;
          }
          if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
          }
          throw new NomadException(e.getMessage(), e);
        }
      }
    }).collect(toList());

    // create the client
    String host = environment.getHost();
    String user = environment.getUser();
    Clock clock = environment.getClock();
    return new NomadClient<T>(nomadEndpoints, host, user, clock) {
      @Override
      public void close() {
        try {
          super.close();
        } finally {
          cleanup.run();
        }
      }
    };
  }

  /**
   * build a list of endpoints through diagnostic port, keeping the same order wanted by user
   */
  private List<NomadEndpoint<T>> createDiagnosticNomadEndpoints(List<InetSocketAddress> expectedOnlineNodes) {
    LOGGER.trace("createDiagnosticNomadEndpoints({})", expectedOnlineNodes);

    // connect and concurrently open a diagnostic connection
    DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(expectedOnlineNodes);

    // build a list of endpoints, keeping the same order wanted by user
    return expectedOnlineNodes.stream().map(addr -> {
      DiagnosticService diagnosticService = diagnosticServices.getDiagnosticService(addr).get();
      @SuppressWarnings("unchecked")
      NomadServer<T> nomadServer = diagnosticService.getProxy(NomadServer.class);
      return new NomadEndpoint<T>(addr, nomadServer) {
        @Override
        public void close() {
          diagnosticService.close();
        }
      };
    }).collect(toList());
  }

  /**
   * Put passive firsts and then actives last and filter out offline nodes
   */
  private static List<InetSocketAddress> keepOnlineAndOrderPassivesFirst(Map<InetSocketAddress, LogicalServerState> expectedOnlineNodes) {
    Predicate<Map.Entry<InetSocketAddress, LogicalServerState>> online = e -> !e.getValue().isUnknown() && !e.getValue().isUnreacheable();
    Predicate<Map.Entry<InetSocketAddress, LogicalServerState>> actives = e -> e.getValue().isActive();
    return Stream.concat(
        expectedOnlineNodes.entrySet().stream().filter(online.and(actives.negate())),
        expectedOnlineNodes.entrySet().stream().filter(online.and(actives))
    ).map(Map.Entry::getKey).collect(toList());
  }

  private static void checkServerStates(Map<InetSocketAddress, LogicalServerState> expectedOnlineNodes) {
    // find any illegal state that should prevent any Nomad access
    for (Map.Entry<InetSocketAddress, LogicalServerState> entry : expectedOnlineNodes.entrySet()) {
      if (!ALLOWED.contains(entry.getValue())) {
        throw new IllegalStateException("Nomad system is currently not accessible. Node: " + entry.getKey() + " is in state: " + entry.getValue());
      }
    }
  }
}
