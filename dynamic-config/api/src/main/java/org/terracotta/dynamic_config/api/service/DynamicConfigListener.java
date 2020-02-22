/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.service;

import com.tc.classloader.CommonComponent;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.NomadChangeInfo;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface DynamicConfigListener {

  /**
   * Listener that will be called when a new configuration has been stored on disk, which happens in Nomad PREPARE phase
   * <p>
   * The method is called with the future topology equivalent to {@link TopologyService#getUpcomingNodeContext()} that will be applied after restart
   * <p>
   * All the nodes are called during PREPARE to save a new configuration, regardless of this applicability level.
   * So this listener will be called on every node.
   */
  void onNewConfigurationSaved(NodeContext nodeContext, Long version);

  /**
   * Listener that will be called when a new configuration has been applied at runtime on a server, through a {@link ConfigChangeHandler}
   * <p>
   * The method is called with the topology equivalent to {@link TopologyService#getRuntimeNodeContext()} or
   * {@link TopologyService#getUpcomingNodeContext()} ()} and the change that has been applied, depending whether the change requires a restart or not
   * <p>
   * Only the nodes targeted by the applicability filter will be called through this listener after the {@link ConfigChangeHandler} is called
   */
  void onSettingChanged(SettingNomadChange change, Cluster updated);

  /**
   * Listener that will be called when some nodes have been removed from a stripe
   * <p>
   * The method is called with the topology equivalent to {@link TopologyService#getRuntimeNodeContext()} and the change that has been applied
   * <p>
   * Only the nodes targeted by the applicability filter will be called through this listener after the {@link ConfigChangeHandler} is called
   *
   * @param removedNode the details about the removed node
   */
  void onNodeRemoval(int stripeId, Node removedNode);

  /**
   * Listener that will be called when some nodes have been added to a stripe
   * <p>
   * The method is called with the topology equivalent to {@link TopologyService#getRuntimeNodeContext()} and the change that has been applied
   * <p>
   * Only the nodes targeted by the applicability filter will be called through this listener after the {@link ConfigChangeHandler} is called
   *
   * @param stripeId  the stripe ID where the nodes have been added
   * @param addedNode the details of the added node
   */
  void onNodeAddition(int stripeId, Node addedNode);

  void onNomadPrepare(PrepareMessage message, AcceptRejectResponse response);

  void onNomadCommit(CommitMessage message, AcceptRejectResponse response, NomadChangeInfo changeInfo);

  void onNomadRollback(RollbackMessage message, AcceptRejectResponse response);
}
