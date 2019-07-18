/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.nomad;

import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.client.change.NomadChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;

public class NomadManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadManager.class);

  private final NomadClientFactory clientFactory;
  private final boolean isVerbose;

  public NomadManager(NomadClientFactory clientFactory, boolean isVerbose) {
    this.clientFactory = clientFactory;
    this.isVerbose = isVerbose;
  }

  public void runChange(Collection<InetSocketAddress> connectionServers, NomadChange change) {
    LOGGER.debug("Attempting to make co-ordinated configuration change: {} on nodes: {}", change, connectionServers);

    try (CloseableNomadClient client = clientFactory.createClient(connectionServers)) {
      ChangeResultReceiver results = isVerbose ? new LoggingChangeResultReceiver() : new NoopChangeResultReceiver();
      client.tryApplyChange(results, change);
    }
  }
}
