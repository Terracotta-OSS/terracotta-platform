/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;

/**
 * Handler for <pre>org.terracotta.dynamic-config.simulate</pre>
 * <p>
 * <p>
 * Simulate a missing value with:
 * <pre>set -c stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate=</pre>
 * <p>
 * Simulate a Nomad prepare failure with:
 * <pre>set -c stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate=TRACE</pre>
 * <p>
 * Simulate a Nomad commit failure with:
 * <pre>set -c stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate=INFO</pre>
 *
 * @author Mathieu Carbou
 */
public class SimulationHandler implements ConfigChangeHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimulationHandler.class);

  private volatile String state = "";

  @Override
  public void validate(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    LOGGER.info("Received: {}", change);

    if (!change.hasValue()) {
      throw new InvalidConfigChangeException("Operation not supported: " + change);
    }

    if ("TRACE".equals(change.getValue().get())) {
      throw new InvalidConfigChangeException("Simulate prepare failure");
    }
  }

  @Override
  public void apply(Configuration change) {
    LOGGER.info("Received: {}", change);

    switch (change.getValue().get()) {

      case "DEBUG":
        if (state.equals("failed") || state.equals("recovered")) {
          state = "recovered";
        } else {
          state = "failed";
          throw new IllegalStateException("Simulate temporary commit failure");
        }
        break;

      case "INFO":
        throw new IllegalStateException("Simulate permanent commit failure");

      default:
    }
  }
}
