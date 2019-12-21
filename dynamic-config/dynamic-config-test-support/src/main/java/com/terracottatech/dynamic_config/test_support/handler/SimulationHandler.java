/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test_support.handler;

import com.terracottatech.dynamic_config.handler.ConfigChangeHandler;
import com.terracottatech.dynamic_config.handler.InvalidConfigChangeException;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.NodeContext;

/**
 * Handler for <pre>com.terracottatech.dynamic-config.simulate</pre>
 * <p>
 * <p>
 * Simulate a missing value with:
 * <pre>set -c stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=</pre>
 * <p>
 * Simulate a Nomad prepare failure with:
 * <pre>set -c stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=prepare-failure</pre>
 * <p>
 * Simulate a Nomad commit failure with:
 * <pre>set -c stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=commit-failure</pre>
 * <p>
 * Simulate a Nomad change requiring a restart with:
 * <pre>set -c stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=restart-required</pre>
 * <p>
 * Simulate a Nomad change applied at runtime with any value:
 * <pre>set -c stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=foo</pre>
 *
 * @author Mathieu Carbou
 */
public class SimulationHandler implements ConfigChangeHandler {

  private volatile String state = "";

  @Override
  public Cluster tryApply(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    if (change.getValue() == null) {
      throw new InvalidConfigChangeException("Invalid change: " + change);
    }

    if ("prepare-failure".equals(change.getValue())) {
      throw new InvalidConfigChangeException("Simulate prepare failure");
    }

    try {
      Cluster updatedCluster = baseConfig.getCluster();
      change.apply(updatedCluster);
      return updatedCluster;
    } catch (RuntimeException e) {
      throw new InvalidConfigChangeException(e.getMessage(), e);
    }
  }

  @Override
  public boolean apply(Configuration change) {
    switch (change.getValue()) {

      case "recover-needed":
        if (state.equals("failed")) {
          state = "recovered";
          return true;
        } else {
          state = "failed";
          throw new IllegalStateException("Simulate commit failure");
        }

      case "commit-failure":
        throw new IllegalStateException("Simulate commit failure");

      case "restart-required":
        return false;

      case "runtime-applied":
        return true;

      default:
        // we do not change state
        return true;
    }
  }
}
