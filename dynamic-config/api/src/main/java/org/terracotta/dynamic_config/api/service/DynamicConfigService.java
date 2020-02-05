/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.service;

import com.tc.classloader.CommonComponent;
import org.terracotta.dynamic_config.api.model.Cluster;

import java.time.Duration;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface DynamicConfigService {

  /**
   * Restarts this node by invoking the appropriate platform APIs. Useful when a node needs to be restarted after activation.
   *
   * @param delay initial delay before restart happens
   */
  void restart(Duration delay);

  /**
   * Changes the in-memory cluster to a new one for this node while it is still not activated.
   * The cluster topology will become effective when the nodes will be activated and restarted.
   */
  void setUpcomingCluster(Cluster cluster);

  /**
   * Activates the Nomad system so that we can write a first config repository version.
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
}