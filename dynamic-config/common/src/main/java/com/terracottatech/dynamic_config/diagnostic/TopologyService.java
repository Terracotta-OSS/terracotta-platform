/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.diagnostic;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;

import java.net.InetSocketAddress;

/**
 * @author Mathieu Carbou
 */
public interface TopologyService {

  /**
   * @return this node information
   */
  Node getThisNode();

  InetSocketAddress getThisNodeAddress();

  void restart();

  /**
   * @return The topology in memory that is currently being built before activation of the cluster.
   * The initial topology for a node is created based on the script parameters ot config file passed
   * to the CLI.
   * When the node is activated, returns the equivalent of the config repository.
   */
  Cluster getTopology();

  /**
   * Change the in-memory topology to a new one for this node. (i.e. through the CLI attach command)
   * <p>
   * When the node is activated, this operations is used to change the topology at runtime.
   */
  void setTopology(Cluster cluster);

  /**
   * This method is called by the activate CLI command when topology has been validated for
   * consistency. This method will activate the Nomad system so that we can write a first config
   * repository version
   */
  void prepareActivation(Cluster validatedTopology);
}
