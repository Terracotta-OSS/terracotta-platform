/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.nomad;

import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.client.recovery.RecoveryResultReceiver;
import com.terracottatech.nomad.client.results.DiscoverResultsReceiver;
import com.terracottatech.nomad.client.results.LoggingResultReceiver;
import com.terracottatech.nomad.client.results.MultiChangeResultReceiver;
import com.terracottatech.nomad.client.results.MultiRecoveryResultReceiver;
import com.terracottatech.nomad.client.status.MultiDiscoveryResultReceiver;
import com.terracottatech.nomad.server.ChangeRequestState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
