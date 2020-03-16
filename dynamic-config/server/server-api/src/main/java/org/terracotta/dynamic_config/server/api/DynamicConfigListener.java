/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.dynamic_config.server.api;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.NomadChangeInfo;

/**
 * @author Mathieu Carbou
 */
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
