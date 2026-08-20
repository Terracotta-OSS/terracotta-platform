/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.dynamic_config.test_support.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.server.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.server.InvalidConfigChangeException;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.server.Server;

public class ReplicaSimulationHandler implements ConfigChangeHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReplicaSimulationHandler.class);

  public static final String SIMULATE_ACTION_TC_PROP = "org.terracotta.ReplicaSimulationHandler.action";
  private final TopologyService topologyService;
  private final Server server;
  private volatile boolean commitFailureFired;

  public ReplicaSimulationHandler(Server server, TopologyService topologyService) {
    this.server = server;
    this.topologyService = topologyService;
  }

  @Override
  public void validate(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    if (!server.isActive() && !server.isPassiveStandby() && !server.getState().contains("PASSIVE-REPLICA-START") && !server.getState().contains("PASSIVE-REPLICA")) {
      // maybe syncing some append log ? we do not want to crash there ;-)
      return;
    }
    LOGGER.info("Received: {}", change);

    if ("prepare-failure".equals(topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(SIMULATE_ACTION_TC_PROP))) {
      throw new InvalidConfigChangeException("Replica Simulate prepare failure from tc property");
    }
  }

  @Override
  public void apply(Configuration change) {
    if (!server.isActive() && !server.isPassiveStandby() && !server.getState().contains("PASSIVE-REPLICA-START") && !server.getState().contains("PASSIVE-REPLICA")) {
      // maybe syncing some append log ? we do not want to crash there ;-)
      return;
    }

    LOGGER.info("Received: {}", change);

    if ("commit-failure".equals(topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(SIMULATE_ACTION_TC_PROP))) {
      if (!commitFailureFired) {
        commitFailureFired = true;
        throw new IllegalArgumentException("Replica Simulate commit failure from tc property");
      }
    }
  }
}
