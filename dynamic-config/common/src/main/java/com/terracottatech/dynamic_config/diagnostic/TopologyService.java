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
   * @return this node's Object representation
   */
  Node getThisNode();

  /**
   * @return this node's host-port
   */
  InetSocketAddress getThisNodeAddress();

  /**
   * Restarts this node by invoking the appropriate platform APIs. Useful when a node needs to be restarted after activation.
   */
  void restart();

  /**
   * Returns the in-memory topology for this node. Once the node is activated, it returns the topology equivalent of the
   * config repository.
   *
   * @return the in-memory topology
   */
  Cluster getTopology();

  /**
   * @return true if this node has been activated (is part of a named cluster that has been licensed)
   */
  boolean isActivated();

  /**
   * Changes the in-memory topology to a new one for this node.
   */
  void setTopology(Cluster cluster);

  /**
   * Activates the Nomad system so that we can write a first config repository version
   */
  void prepareActivation(Cluster validatedTopology);
}
