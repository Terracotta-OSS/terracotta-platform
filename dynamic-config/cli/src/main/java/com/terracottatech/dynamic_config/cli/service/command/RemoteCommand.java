/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;


import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import com.terracottatech.dynamic_config.cli.service.nomad.NomadManager;
import com.terracottatech.dynamic_config.cli.service.restart.RestartProgress;
import com.terracottatech.dynamic_config.cli.service.restart.RestartService;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.terracottatech.tools.detailed.state.LogicalServerState.UNREACHABLE;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

/**
 * @author Mathieu Carbou
 */
public abstract class RemoteCommand extends Command {

  @Resource public MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  @Resource public DiagnosticServiceProvider diagnosticServiceProvider;
  @Resource public NomadManager<NodeContext> nomadManager;
  @Resource public RestartService restartService;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final void ensureAddressWithinCluster(InetSocketAddress address) {
    logger.trace("ensureAddressWithinCluster({})", address);
    getRuntimeCluster(address).getNode(address)
        .orElseGet(() -> getUpcomingCluster(address).getNode(address)
            .orElseThrow(() -> new IllegalArgumentException("Targeted cluster does not contain any node with this address: " + address + ". Is it a mistake ? Are you connecting to the wrong cluster ? If not, please use the configured node hostname and port to connect.")));
  }

  protected final boolean isRestartRequired(InetSocketAddress address) {
    logger.trace("isRestartRequired({})", address);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(address)) {
      return diagnosticService.getProxy(TopologyService.class).isRestartRequired();
    }
  }

  protected final void runNomadChange(Collection<InetSocketAddress> expectedOnlineNodes, NomadChange change) {
    logger.trace("runNomadChange({}, {})", expectedOnlineNodes, change);
    NomadFailureRecorder<NodeContext> failures = new NomadFailureRecorder<>();
    nomadManager.runChange(expectedOnlineNodes, change, failures);
    failures.reThrow();
  }

  protected final Cluster getUpcomingCluster(InetSocketAddress address) {
    logger.trace("getUpcomingCluster({})", address);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(address)) {
      return diagnosticService.getProxy(TopologyService.class).getUpcomingNodeContext().getCluster();
    }
  }

  protected final Cluster getRuntimeCluster(InetSocketAddress address) {
    logger.trace("getRuntimeCluster({})", address);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(address)) {
      return diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext().getCluster();
    }
  }

  protected final void restartNodes(Collection<InetSocketAddress> addresses) {
    logger.trace("restartNodes({})", addresses);
    try {
      RestartProgress progress = restartService.restartNodes(addresses);
      Map<InetSocketAddress, Tuple2<String, Exception>> failures = progress.await();
      if (!failures.isEmpty()) {
        String failedNodes = failures.entrySet()
            .stream()
            .map(e -> e.getKey() + ": " + e.getValue().t1)
            .collect(joining("\n - "));
        throw new IllegalStateException("Some nodes failed to restart:\n - " + failedNodes);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Restart has been interrupted", e);
    }
  }

  protected final Collection<InetSocketAddress> findRuntimePeers(InetSocketAddress address) {
    logger.trace("findRuntimePeers({})", address);
    final Collection<InetSocketAddress> peers = getRuntimeCluster(address).getNodeAddresses();
    if (!peers.contains(address)) {
      throw new IllegalArgumentException("Node address " + address + " used to connect does not match any known node in cluster " + peers);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Discovered nodes:{} through: {}", toString(peers), address);
    }
    return peers;
  }

  protected final Map<InetSocketAddress, LogicalServerState> findOnlineRuntimePeers(InetSocketAddress address) {
    logger.trace("findOnlineRuntimePeers({})", address);
    Cluster cluster = getRuntimeCluster(address);
    Collection<InetSocketAddress> addresses = cluster.getNodeAddresses();
    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchDiagnosticServices(addresses)) {
      return addresses.stream()
          .collect(toMap(
              identity(),
              addr -> diagnosticServices.getDiagnosticService(address).map(DiagnosticService::getLogicalServerState).orElse(UNREACHABLE),
              (o1, o2) -> {
                throw new UnsupportedOperationException();
              },
              LinkedHashMap::new));
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
        throw new IllegalStateException("Cluster is badly formed as it contains a mix of activated and unconfigured nodes. " +
            "Activated: " + activations.get(Boolean.TRUE) + ", Unconfigured: " + activations.get(Boolean.FALSE));
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

  protected static Stream<Tuple2<InetSocketAddress, TopologyService>> topologyServices(DiagnosticServices diagnosticServices) {
    return diagnosticServices.map((address, diagnosticService) -> diagnosticService.getProxy(TopologyService.class));
  }

  protected static Stream<Tuple2<InetSocketAddress, DynamicConfigService>> dynamicConfigServices(DiagnosticServices diagnosticServices) {
    return diagnosticServices.map((address, diagnosticService) -> diagnosticService.getProxy(DynamicConfigService.class));
  }

  protected static String toString(Collection<InetSocketAddress> addresses) {
    return addresses.stream().map(InetSocketAddress::toString).collect(Collectors.joining(", "));
  }
}
