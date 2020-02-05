/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.service;

import com.tc.classloader.CommonComponent;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface DynamicConfigListener {

  /**
   * Listener that will be called when a new configuration has been applied at runtime on a server
   * <p>
   * The method is called with the topology equivalent to {@link TopologyService#getRuntimeNodeContext()} and the change that has been applied
   */
  default void onNewConfigurationAppliedAtRuntime(NodeContext nodeContext, Configuration configuration) {}

  /**
   * Listener that will be called when a new configuration has been accepted but is pending a restart to be applied
   * <p>
   * The method is called with the topology equivalent to {@link TopologyService#getUpcomingNodeContext()} ()} and the change that has been applied
   */
  default void onNewConfigurationPendingRestart(NodeContext nodeContext, Configuration configuration) {}

  /**
   * Listener that will be called when a new configuration has been stored in a new configuration repository version
   * <p>
   * The method is called with the future topology equivalent to {@link TopologyService#getUpcomingNodeContext()} that will be applied after restart
   */
  default void onNewConfigurationSaved(NodeContext nodeContext, Long version) {}

  default void onNomadPrepare(PrepareMessage message, AcceptRejectResponse response) {}

  default void onNomadCommit(CommitMessage message, AcceptRejectResponse response) {}

  default void onNomadRollback(RollbackMessage message, AcceptRejectResponse response) {}
}