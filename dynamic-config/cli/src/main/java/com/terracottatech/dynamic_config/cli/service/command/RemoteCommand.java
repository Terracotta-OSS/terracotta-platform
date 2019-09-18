/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;


import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import com.terracottatech.dynamic_config.cli.service.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.service.nomad.NomadManager;
import com.terracottatech.dynamic_config.cli.service.restart.RestartProgress;
import com.terracottatech.dynamic_config.cli.service.restart.RestartService;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.client.results.NomadFailureRecorder;
import com.terracottatech.tools.detailed.state.LogicalServerState;
import com.terracottatech.utilities.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author Mathieu Carbou
 */
public abstract class RemoteCommand extends Command {

  @Resource public NodeAddressDiscovery nodeAddressDiscovery;
  @Resource public MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  @Resource public DiagnosticServiceProvider diagnosticServiceProvider;
  @Resource public NomadManager<NodeContext> nomadManager;
  @Resource public RestartService restartService;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final void runNomadChange(Collection<InetSocketAddress> expectedOnlineNodes, NomadChange change) {
    NomadFailureRecorder<NodeContext> failures = new NomadFailureRecorder<>();
    nomadManager.runChange(expectedOnlineNodes, change, failures);
    failures.reThrow();
  }

  protected final Cluster getRemoteTopology(InetSocketAddress node) {
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(node)) {
      return diagnosticService.getProxy(TopologyService.class).getCluster();
    }
  }

  protected final void restartNodes(Collection<InetSocketAddress> addresses) {
    try {
      RestartProgress progress = restartService.restartNodes(addresses);
      Map<InetSocketAddress, Tuple2<String, Exception>> failures = progress.await();
      if (failures.isEmpty()) {
        logger.info("All nodes: {} came back up", toString(addresses));
      } else {
        String failedNodes = failures.entrySet()
            .stream()
            .map(e -> e.getKey() + ": " + e.getValue().t1)
            .collect(joining("\n - "));
        throw new IllegalStateException("Some nodes failed to restart:\n - " + failedNodes);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Restart has been interrupted");
    }
  }

  protected final Collection<InetSocketAddress> findPeers(InetSocketAddress nodeAddress) {
    logger.debug("Discovering nodes through {}", nodeAddress);
    Collection<InetSocketAddress> discovered = nodeAddressDiscovery.discover(nodeAddress);
    if (logger.isDebugEnabled()) {
      logger.debug("Discovered nodes {} through {}", toString(discovered), nodeAddress);
    }
    return discovered;
  }

  protected final Map<InetSocketAddress, LogicalServerState> findOnlineNodes(Cluster cluster) {
    //TODO [DYNAMIC-CONFIG]: TDB-4601: Allows to only connect to the online nodes, return only online nodes (fetchDiagnosticServices is throwing at the moment)
    try (DiagnosticServices connection = multiDiagnosticServiceProvider.fetchDiagnosticServices(cluster.getNodeAddresses())) {
      return connection.map((inetSocketAddress, diagnosticService) -> diagnosticService.getLogicalServerState()).collect(toMap(
          Tuple2::getT1,
          Tuple2::getT2, (o1, o2) -> {
            throw new UnsupportedOperationException();
          },
          LinkedHashMap::new));
    }
  }

  protected final boolean validateActivationState(Collection<InetSocketAddress> expectedOnlineNodes) {
    logger.debug("Contacting all cluster nodes: {} to check for activation", toString(expectedOnlineNodes));
    try (DiagnosticServices connection = multiDiagnosticServiceProvider.fetchDiagnosticServices(expectedOnlineNodes)) {
      Map<Boolean, List<InetSocketAddress>> activations = topologyServices(connection)
          .map(tuple -> tuple.map(identity(), TopologyService::isActivated))
          .collect(groupingBy(Tuple2::getT2, mapping(Tuple2::getT1, toList())));
      if (activations.isEmpty()) {
        throw new IllegalArgumentException("Cluster is empty or offline");
      }
      if (activations.size() == 2) {
        throw new IllegalStateException("Cluster is badly formed of activated and non activated nodes. Activated: " + activations.get(Boolean.TRUE) + ", Non activated: " + activations.get(Boolean.FALSE));
      }
      return activations.keySet().iterator().next();
    }
  }

  protected final void upgradeLicense(Collection<InetSocketAddress> expectedOnlineNodes, Path licenseFile) {
    logger.debug("Reading license");
    String xml;
    try {
      xml = new String(Files.readAllBytes(licenseFile), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    logger.debug("Contacting all cluster nodes: {} to upgrade the license", toString(expectedOnlineNodes));
    try (DiagnosticServices connection = multiDiagnosticServiceProvider.fetchDiagnosticServices(expectedOnlineNodes)) {
      topologyServices(connection)
          .map(tuple -> {
            try {
              tuple.t2.upgradeLicense(xml);
              return null;
            } catch (RuntimeException e) {
              logger.warn("License upgrade failed on node {}: {}", tuple.t1, e.getMessage());
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

  protected static Stream<Tuple2<InetSocketAddress, TopologyService>> topologyServices(DiagnosticServices connection) {
    return connection.map((address, diagnosticService) -> diagnosticService.getProxy(TopologyService.class));
  }

  protected static String toString(Collection<InetSocketAddress> addresses) {
    return addresses.stream().map(InetSocketAddress::toString).collect(Collectors.joining(", "));
  }
}
