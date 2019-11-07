/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service;

import com.tc.classloader.CommonComponent;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.NodeContext;

import java.util.function.BiConsumer;

@CommonComponent
public interface DynamicConfigEventing {

  /**
   * Register a listener that will be called when a new configuration has been applied at runtime on a server
   * <p>
   * The consumer is called with the topology equivalent to {@link TopologyService#getRuntimeNodeContext()} and the change that has been applied
   */
  EventRegistration onNewRuntimeConfiguration(BiConsumer<NodeContext, Configuration> consumer);

  /**
   * Register a listener that will be called when a new configuration has been accepted but is pending a restart to be applied
   * <p>
   * The consumer is called with the topology equivalent to {@link TopologyService#getUpcomingNodeContext()} ()} and the change that has been applied
   */
  EventRegistration onNewUpcomingConfiguration(BiConsumer<NodeContext, Configuration> consumer);

  /**
   * Register a listener that will be called when a new configuration has been stored in a new configuration repository version
   * <p>
   * The consumer is called with the future topology equivalent to {@link TopologyService#getUpcomingNodeContext()} that will be applied after restart
   */
  EventRegistration onNewTopologyCommitted(BiConsumer<Long, NodeContext> consumer);
}
