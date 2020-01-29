/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.nomad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.nomad.client.change.ChangeResultReceiver;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.client.results.LoggingResultReceiver;
import org.terracotta.nomad.client.results.MultiChangeResultReceiver;
import org.terracotta.nomad.client.results.MultiRecoveryResultReceiver;
import org.terracotta.nomad.client.status.MultiDiscoveryResultReceiver;
import org.terracotta.nomad.server.ChangeRequestState;

import java.net.InetSocketAddress;
import java.util.List;

import static java.util.Arrays.asList;

public class NomadManager<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadManager.class);

  private final NomadClientFactory<T> clientFactory;

  public NomadManager(NomadClientFactory<T> clientFactory) {
    this.clientFactory = clientFactory;
  }

  public void runChange(List<InetSocketAddress> expectedOnlineNodes, NomadChange change, ChangeResultReceiver<T> results) {
    LOGGER.debug("Attempting to make co-ordinated configuration change: {} on nodes: {}", change, expectedOnlineNodes);

    try (CloseableNomadClient<T> client = clientFactory.createClient(expectedOnlineNodes)) {
      client.tryApplyChange(new MultiChangeResultReceiver<>(asList(new LoggingResultReceiver<>(), results)), change);
    }
  }

  public void runRecovery(List<InetSocketAddress> expectedOnlineNodes, RecoveryResultReceiver<T> results, int expectedNodeCount, ChangeRequestState forcedState) {
    LOGGER.debug("Attempting to recover nodes: {}", expectedOnlineNodes);

    try (CloseableNomadClient<T> client = clientFactory.createClient(expectedOnlineNodes)) {
      client.tryRecovery(new MultiRecoveryResultReceiver<>(asList(new LoggingResultReceiver<>(), results)), expectedNodeCount, forcedState);
    }
  }

  public void runDiscovery(List<InetSocketAddress> expectedOnlineNodes, DiscoverResultsReceiver<T> results) {
    LOGGER.debug("Attempting to discover nodes: {}", expectedOnlineNodes);

    try (CloseableNomadClient<T> client = clientFactory.createClient(expectedOnlineNodes)) {
      client.tryDiscovery(new MultiDiscoveryResultReceiver<>(asList(new LoggingResultReceiver<>(), results)));
    }
  }
}
