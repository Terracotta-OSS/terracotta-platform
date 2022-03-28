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
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.nomad.server.NomadChangeInfo;

import java.util.Optional;

/**
 * @author Mathieu Carbou
 */
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
   * In this state, the nodes are currently within a Nomad transaction, or, a partial commit/rollback occurred and the node
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

  /**
   * Validate a cluster model against the license installed in the node
   * <p>
   * An exception is thrown if a license is installed and cluster is invalid
   *
   * @return true if a license is installed and cluster is validated,
   * false if no license installed so no validation done
   */
  boolean validateAgainstLicense(Cluster cluster);
}
