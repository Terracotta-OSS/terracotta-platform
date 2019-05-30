/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.diagnostic;

import com.terracottatech.dynamic_config.model.Cluster;

/**
 * @author Mathieu Carbou
 */
public interface DynamicConfigService {

  /**
   * @return The topology in memory that is currently being built before activation of the cluster.
   * The initial topology for a node is created based on the script parameters ot config file passed
   * to the CLI.
   */
  Cluster getPendingTopology();

  /**
   * Change the in-memory topology to a new one for this node. (i.e. through the CLI attach command)
   */
  void setPendingTopology(Cluster pendingTopology);

  /**
   * This method is called by the activate CLI command when topology has been validated for
   * consistency. This method will activate the Nomad system so that we can write a first config
   * repository version
   */
  //TODO: TO BE COMPLETED: DO WE NEED SOME PARAMETERS AND RETURN SOMETHING ?
  void prepareActivation(Cluster validatedTopology);
}
