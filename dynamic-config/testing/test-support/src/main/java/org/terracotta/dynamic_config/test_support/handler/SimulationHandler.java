/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.test_support.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.service.InvalidConfigChangeException;

/**
 * Handler for <pre>org.terracotta.dynamic-config.simulate</pre>
 * <p>
 * <p>
 * Simulate a missing value with:
 * <pre>set -c stripe.1.node.1.node-logger-overrides.org.terracotta.dynamic-config.simulate=</pre>
 * <p>
 * Simulate a Nomad prepare failure with:
 * <pre>set -c stripe.1.node.1.node-logger-overrides.org.terracotta.dynamic-config.simulate=TRACE</pre>
 * <p>
 * Simulate a Nomad commit failure with:
 * <pre>set -c stripe.1.node.1.node-logger-overrides.org.terracotta.dynamic-config.simulate=INFO</pre>
 *
 * @author Mathieu Carbou
 */
public class SimulationHandler implements ConfigChangeHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimulationHandler.class);

  private volatile String state = "";

  @Override
  public void validate(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    LOGGER.info("Received: {}", change);

    if (change.getValue() == null) {
      throw new InvalidConfigChangeException("Invalid change: " + change);
    }

    if ("TRACE".equals(change.getValue())) {
      throw new InvalidConfigChangeException("Simulate prepare failure");
    }
  }

  @Override
  public void apply(Configuration change) {
    LOGGER.info("Received: {}", change);

    switch (change.getValue()) {

      case "DEBUG":
        if (state.equals("failed")) {
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
