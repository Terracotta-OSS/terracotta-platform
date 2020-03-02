/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.entity.client;

import org.terracotta.connection.ConnectionException;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Connect to a Nomad entity on a stripe, given the addresses of the nodes on that stripe
 *
 * @author Mathieu Carbou
 */
public class DynamicTopologyEntityProvider {
  private final String connectionName;
  private final DynamicTopologyEntity.Settings settings;
  private final Duration connectTimeout;
  private final String securityRootDirectory;

  public DynamicTopologyEntityProvider(String connectionName, Duration connectTimeout, DynamicTopologyEntity.Settings settings, String securityRootDirectory) {
    this.connectionName = requireNonNull(connectionName);
    this.settings = requireNonNull(settings);
    this.connectTimeout = requireNonNull(connectTimeout);
    this.securityRootDirectory = securityRootDirectory;
  }

  public DynamicTopologyEntity fetchDynamicTopologyEntity(List<InetSocketAddress> addresses) throws ConnectionException {
    return DynamicTopologyEntityFactory.fetch(addresses, connectionName, connectTimeout, settings, securityRootDirectory);
  }
}
