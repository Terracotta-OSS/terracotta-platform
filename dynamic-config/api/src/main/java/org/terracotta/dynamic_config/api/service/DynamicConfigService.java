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
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Cluster;

import java.time.Duration;
import java.util.Optional;

/**
 * @author Mathieu Carbou
 */
public interface DynamicConfigService {

  /**
   * Changes the in-memory cluster to a new one for this node while it is still not activated.
   * The cluster topology will become effective when the nodes will be activated and restarted.
   */
  void setUpcomingCluster(Cluster cluster);

  /**
   * Activates the Nomad system so that we can write a first configuration directory version.
   * This requires the topology to set plus eventually the license installed.
   * <p>
   * License can be null.
   */
  void activate(Cluster validatedTopology, String licenseContent);

  /**
   * Validate and install a new license over an existing one, or for the first time.
   * <p>
   * Can also remove an existing license if null is passed.
   *
   * @param licenseContent license file content
   */
  void upgradeLicense(String licenseContent);

  /**
   * Reset
   * <p>
   * This method will back up and reset the configurations and Nomad append log,
   * <p>
   * The node will restart in diagnostic mode if restarted.
   */
  void reset();

  /**
   * Restarts this node by invoking the appropriate platform APIs. Useful when a node needs to be restarted after activation.
   *
   * @param delay initial delay before restart happens
   */
  void restart(Duration delay);

  /**
   * Restart this not after the specified delay only if its physical server state is PASSIVE (the node could be blocked or not)
   *
   * @param delay initial delay before restart happens
   */
  void restartIfPassive(Duration delay);

  /**
   * Restart this not after the specified delay only if its physical server state is ACTIVE (the node could be blocked,
   * in a reconnect window, or serving clients)
   *
   * @param delay initial delay before restart happens
   */
  void restartIfActive(Duration delay);

  /**
   * Stops an activated node.
   * <p>
   * This method will zap and stop the node.
   */
  void stop(Duration delay);

  /**
   * Get the content of the installed license if any
   */
  Optional<String> getLicenseContent();

  /**
   * Reset and sync this node's append log with the provided nomad changes and update the configurations accordingly.
   */
  void resetAndSync(NomadChangeInfo[] nomadChanges, Cluster cluster);
}
