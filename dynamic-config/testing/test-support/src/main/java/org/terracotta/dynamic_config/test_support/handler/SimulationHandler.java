/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.test_support.handler;

import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.service.InvalidConfigChangeException;

/**
 * Handler for <pre>org.terracotta.dynamic-config.simulate</pre>
 * <p>
 * <p>
 * Simulate a missing value with:
 * <pre>set -c stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=</pre>
 * <p>
 * Simulate a Nomad prepare failure with:
 * <pre>set -c stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=prepare-failure</pre>
 * <p>
 * Simulate a Nomad commit failure with:
 * <pre>set -c stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=commit-failure</pre>
 * <p>
 * Simulate a Nomad change requiring a restart with:
 * <pre>set -c stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=restart-required</pre>
 * <p>
 *
 * @author Mathieu Carbou
 */
public class SimulationHandler implements ConfigChangeHandler {

  private volatile String state = "";

  @Override
  public void validate(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    if (change.getValue() == null) {
      throw new InvalidConfigChangeException("Invalid change: " + change);
    }

    if ("prepare-failure".equals(change.getValue())) {
      throw new InvalidConfigChangeException("Simulate prepare failure");
    }
  }

  @Override
  public void apply(Configuration change) {
    switch (change.getValue()) {

      case "recover-needed":
        if (state.equals("failed")) {
          state = "recovered";
        } else {
          state = "failed";
          throw new IllegalStateException("Simulate commit failure");
        }
        break;

      case "commit-failure":
        throw new IllegalStateException("Simulate commit failure");

      case "restart-required":
        break;

      default:
    }
  }
}
