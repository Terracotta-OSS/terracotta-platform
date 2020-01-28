/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.service;

import com.tc.classloader.CommonComponent;
import com.terracottatech.dynamic_config.api.model.License;
import com.terracottatech.dynamic_config.api.model.NodeContext;
import com.terracottatech.nomad.server.NomadChangeInfo;

import java.util.Optional;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface TopologyService {

  /**
   * Returns a copy of the information about this node, including stripe and cluster topology.
   * <p>
   * - If the node is not activated: returns the topology that is currently being built and will be effective after node activation and restart
   * <p>
   * - If the node is activated: returns the topology that has been lastly persisted in the config repository and will be effective after a restart.
   * <p>
   * This is possible that the upcoming topology equals the runtime topology if no configuration change has been made requiring a restart
   * <p>
   * If a configuration change is made, and this change does not require a restart, the change will be persisted in the config repository,
   * and the change will be directly applied to both the runtime topology and the upcoming one, so that they are equal.
   */
  NodeContext getUpcomingNodeContext();

  /**
   * Returns a copy of the information about this node, including stripe and cluster topology.
   * <p>
   * - If the node is not activated: has the same effect as {@link #getUpcomingNodeContext()}
   * <p>
   * - If the node is activated: returns the topology that is currently in effect at runtime.
   * This topology could be equal to the upcoming one in case a change can be applied at runtime
   * or when the node has just been started and no configuration change has been made
   */
  NodeContext getRuntimeNodeContext();

  /**
   * @return true if this node has been activated (is part of a named cluster that has been licensed)
   */
  boolean isActivated();

  /**
   * @return true if some dynamic changes have been done which cannot be applied at runtime and need a restart to be applied
   * This means that {@link #getUpcomingNodeContext()} will contains these changes whereas {@link #getRuntimeNodeContext()} wont'.
   */
  boolean mustBeRestarted();

  /**
   * @return true if a configuration as been prepared on this node, but it has not yet been committed or rolled back.
   * In this state, the nodes are currently within a Nomad transaction, or, a partial commit/rollback occured and the node
   * needs a check/repair
   */
  boolean hasIncompleteChange();

  /**
   * Get the current installed license information if any
   */
  Optional<License> getLicense();

  /**
   * @return the append log change history
   */
  NomadChangeInfo[] getChangeHistory();
}
