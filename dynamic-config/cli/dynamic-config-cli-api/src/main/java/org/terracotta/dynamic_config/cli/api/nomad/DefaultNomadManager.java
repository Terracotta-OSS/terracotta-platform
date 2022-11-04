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
package org.terracotta.dynamic_config.cli.api.nomad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.entity.Entity;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE_RECONNECTING;
import static org.terracotta.diagnostic.model.LogicalServerState.DIAGNOSTIC;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE;

public class DefaultNomadManager<T> implements NomadManager<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadManager.class);

  private static final EnumSet<LogicalServerState> ALLOWED = EnumSet.of(
      ACTIVE,
      PASSIVE,
      ACTIVE_RECONNECTING,
      DIAGNOSTIC // this mode is when a server is forced to start in diagnostic mode for repair
  );

  private final NomadEnvironment environment;
  private final MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  private final NomadEntityProvider nomadEntityProvider;

  public DefaultNomadManager(NomadEnvironment environment, MultiDiagnosticServiceProvider multiDiagnosticServiceProvider, NomadEntityProvider nomadEntityProvider) {
    this.environment = environment;
    this.multiDiagnosticServiceProvider = multiDiagnosticServiceProvider;
    this.nomadEntityProvider = nomadEntityProvider;
  }

  public void runConfigurationDiscovery(Map<Endpoint, LogicalServerState> nodes, DiscoverResultsReceiver<T> results) {
    LOGGER.debug("Attempting to discover nodes: {}", nodes);
    List<Endpoint> orderedList = keepOnlineAndOrderPassivesFirst(nodes);
    try (NomadClient<T> client = createDiagnosticNomadClient(orderedList)) {
      client.tryDiscovery(new MultiDiscoveryResultReceiver<>(asList(new LoggingResultReceiver<>(), results)));
    }
  }

  public void runClusterActivation(Collection<Endpoint> nodes, Cluster cluster, ChangeResultReceiver<T> results) {
    LOGGER.debug("Attempting to activate cluster: {}", cluster.toShapeString());
    try (NomadClient<T> client = createDiagnosticNomadClient(new ArrayList<>(nodes))) {
      client.tryApplyChange(new MultiChangeResultReceiver<>(asList(new LoggingResultReceiver<>(), results)), new ClusterActivationNomadChange(cluster));
    }
  }

  public void runConfigurationChange(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes,
                                     DynamicConfigNomadChange changes, ChangeResultReceiver<T> results) {
    LOGGER.debug("Attempting to make co-ordinated configuration change: {} on nodes: {}", changes, onlineNodes);
    checkServerStates(onlineNodes);
    try (NomadClient<T> client = createBiChannelNomadClient(destinationCluster, onlineNodes)) {
      client.tryApplyChange(new MultiChangeResultReceiver<>(asList(new LoggingResultReceiver<>(), results)), changes);
    }
  }

  public void runConfigurationRepair(Map<Endpoint, LogicalServerState> onlineActivatedNodes, int totalNodeCount, RecoveryResultReceiver<T> results, ChangeRequestState forcedState) {
    LOGGER.debug("Attempting to repair configuration on nodes: {}", onlineActivatedNodes.keySet());
    List<Endpoint> orderedList = keepOnlineAndOrderPassivesFirst(onlineActivatedNodes);
    try (NomadClient<T> client = createDiagnosticNomadClient(orderedList)) {
      client.tryRecovery(new MultiRecoveryResultReceiver<>(asList(new LoggingResultReceiver<>(), results)), totalNodeCount, forcedState);
    }
  }

  /**
   * create a nomad client that is preparing through diagnostic port and committing through diagnostic port
   */
  private NomadClient<T> createDiagnosticNomadClient(List<Endpoint> expectedOnlineNodes) {
    LOGGER.trace("createDiagnosticNomadClient({})", expectedOnlineNodes);
    // create normal diagnostic endpoints
    List<NomadEndpoint<T>> nomadEndpoints = createDiagnosticNomadEndpoints(expectedOnlineNodes);
    // create the client
    String host = environment.getHost();
    String user = environment.getUser();
    Clock clock = environment.getClock();
    return new NomadClient<>(nomadEndpoints, host, user, clock);
  }

  /**
   * create a nomad client that is preparing through diagnostic port and committing through entity channel
   */
  private NomadClient<T> createBiChannelNomadClient(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes) {
    LOGGER.trace("createBiChannelNomadClient({}, {})", destinationCluster, onlineNodes);

    checkServerStates(onlineNodes);

    // collect all online addresses by stripe
    Map<UID, List<Endpoint>> onlineNodesPerStripe = destinationCluster.getStripes().stream().collect(toMap(Stripe::getUID, stripe -> {
      List<Endpoint> stripeNodes = new ArrayList<>();
      for (Node node : stripe.getNodes()) {
        for (Endpoint endpoint : onlineNodes.keySet()) {
          if (endpoint.getNodeUID().equals(node.getUID())) {
            stripeNodes.add(node.determineEndpoint(endpoint));
          }
        }
      }
      return stripeNodes;
    }));

    // throw if stripe not online
    for (Map.Entry<UID, List<Endpoint>> entry : onlineNodesPerStripe.entrySet()) {
      if (entry.getValue().isEmpty()) {
        throw new IllegalStateException("Entire stripe UID: " + entry.getKey() + " is not online in cluster: " + destinationCluster.toShapeString());
      }
    }

    // connect to each stripes and get the Nomad entity
    final Map<UID, NomadEntity<T>> nomadEntities = new HashMap<>(onlineNodesPerStripe.size());
    final Runnable cleanup = () -> nomadEntities.values().forEach(Entity::close); // close() implementation is not expected to throw. See implementation code.
    for (Map.Entry<UID, List<Endpoint>> entry : onlineNodesPerStripe.entrySet()) {
      final List<Endpoint> addresses = entry.getValue();
      try {
        LOGGER.trace("Connecting to stripe UID: {} using nodes: {}", entry.getKey(), addresses);
        nomadEntities.put(entry.getKey(), nomadEntityProvider.fetchNomadEntity(addresses.stream().map(Endpoint::getAddress).collect(toList())));
      } catch (ConnectionException e) {
        cleanup.run();
        throw new IllegalStateException("Unable to connect to stripe UID: " + entry.getKey() + " using endpoints: " + entry.getValue() + ". Server states: " + onlineNodes + ". Error:: " + e.getMessage(), e);
      }
    }

    // create a nomad endpoint per stripe for the commit phase
    final Map<UID, NomadEndpoint<T>> stripeEndpoints = onlineNodesPerStripe.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> {
      final InetSocketAddress firstAddress = e.getValue().get(0).getAddress();
      return new NomadEndpoint<T>(firstAddress, nomadEntities.get(e.getKey()));
    }));

    // create normal diagnostic endpoints for the prepare phase
    List<NomadEndpoint<T>> nomadEndpoints;
    try {
      List<Endpoint> orderedList = keepOnlineAndOrderPassivesFirst(onlineNodes);
      Collections.reverse(orderedList); // put actives first
      LOGGER.trace("Connecting to diagnostic ports: {}", orderedList);
      nomadEndpoints = createDiagnosticNomadEndpoints(orderedList);
    } catch (RuntimeException e) {
      // close the entity channels if we cannot manage to open the endpoints
      cleanup.run();
      throw e;
    }

    // override the diagnostic endpoints to go over the entity channel for the nomad commit phase
    ConcurrentMap<UID, CompletableFuture<AcceptRejectResponse>> cache = new ConcurrentHashMap<>(stripeEndpoints.size());
    nomadEndpoints = nomadEndpoints.stream().map(e -> new NomadEndpoint<T>(e.getAddress(), e) {
      @SuppressWarnings("OptionalGetWithoutIsPresent")
      @Override
      public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
        // This method is called for each online node.
        // But for a stripe, we only need to do 1 call, to the active, which will be replicated to the passive servers.
        // So we cache the first response we got from a stripe, to return it immediately after for the other calls.
        // We consider that the commit response on the passive servers will be the same on the active servers.
        InetSocketAddress address = getAddress();
        UID stripeUID = onlineNodesPerStripe.entrySet()
            .stream()
            .filter(e -> e.getValue().stream().anyMatch(endpoint -> endpoint.getAddress().equals(address)))
            .findAny()
            .map(Map.Entry::getKey)
            .get();

        CompletableFuture<AcceptRejectResponse> result = cache.computeIfAbsent(stripeUID, uid -> {
          LOGGER.trace("Committing topology change to stripe UID: {}", stripeUID);

          LOGGER.trace("Sending commit message: {} to stripe UID: {}", message, stripeUID);
          CompletableFuture<AcceptRejectResponse> c = new CompletableFuture<>();
          try {
            AcceptRejectResponse acceptRejectResponse = stripeEndpoints.get(stripeUID).commit(message);
            LOGGER.trace("Received commit response: {} from stripe UID: {}", message, stripeUID);
            c.complete(acceptRejectResponse);
          } catch (NomadException | RuntimeException e) {
            LOGGER.trace("Received commit failure: '{}' from stripe UID: {}", e.getMessage(), stripeUID, e);
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
  private List<NomadEndpoint<T>> createDiagnosticNomadEndpoints(List<Endpoint> expectedOnlineNodes) {
    LOGGER.trace("createDiagnosticNomadEndpoints({})", expectedOnlineNodes);

    // connect and concurrently open a diagnostic connection
    DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(expectedOnlineNodes.stream().collect(toMap(Endpoint::getNodeUID, Endpoint::getAddress)));

    // build a list of endpoints, keeping the same order wanted by user
    return expectedOnlineNodes.stream().map(endpoint -> {
      DiagnosticService diagnosticService = diagnosticServices.getDiagnosticService(endpoint.getNodeUID()).get();
      @SuppressWarnings("unchecked")
      NomadServer<T> nomadServer = diagnosticService.getProxy(NomadServer.class);
      return new NomadEndpoint<T>(endpoint.getAddress(), nomadServer) {
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
  private static List<Endpoint> keepOnlineAndOrderPassivesFirst(Map<Endpoint, LogicalServerState> expectedOnlineNodes) {
    Predicate<Map.Entry<Endpoint, LogicalServerState>> online = e -> !e.getValue().isUnknown() && !e.getValue().isUnreacheable();
    Predicate<Map.Entry<Endpoint, LogicalServerState>> actives = e -> e.getValue().isActive();
    return Stream.concat(
        expectedOnlineNodes.entrySet().stream().filter(online.and(actives.negate())),
        expectedOnlineNodes.entrySet().stream().filter(online.and(actives))
    ).map(Map.Entry::getKey).collect(toList());
  }

  private static void checkServerStates(Map<Endpoint, LogicalServerState> expectedOnlineNodes) {
    // find any illegal state that should prevent any Nomad access
    for (Map.Entry<Endpoint, LogicalServerState> entry : expectedOnlineNodes.entrySet()) {
      if (!ALLOWED.contains(entry.getValue())) {
        throw new IllegalStateException("Nomad system is currently not accessible. Node: " + entry.getKey() + " is in state: " + entry.getValue());
      }
    }
  }
}
