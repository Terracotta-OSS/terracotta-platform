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
package org.terracotta.dynamic_config.entity.topology.client;

import org.terracotta.connection.ConnectionException;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;

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

  public DynamicTopologyEntity fetchDynamicTopologyEntity(Collection<InetSocketAddress> addresses) throws ConnectionException {
    return DynamicTopologyEntityFactory.fetch(addresses, connectionName, connectTimeout, settings, securityRootDirectory);
  }
}
