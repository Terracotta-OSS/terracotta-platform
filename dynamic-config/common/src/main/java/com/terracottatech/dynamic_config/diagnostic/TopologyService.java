/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.diagnostic;

import com.terracottatech.License;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.NodeContext;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * @author Mathieu Carbou
 */
public interface TopologyService {

  /***
   * Returns the information about this node, stripe, and containing cluster topology
   */
  NodeContext getThisNodeContext();

  /**
   * @return this node's Object representation
   */
  Node getThisNode();

  /**
   * @return this node's host-port
   */
  InetSocketAddress getThisNodeAddress();

  /**
   * Returns the in-memory cluster for this node. Once the node is activated, it returns the cluster equivalent of the
   * config repository XML versioned files
   *
   * @return the in-memory cluster
   */
  Cluster getCluster();

  /**
   * @return true if this node has been activated (is part of a named cluster that has been licensed)
   */
  boolean isActivated();

  /**
   * Get the current installed license information if any
   */
  Optional<License> getLicense();
}
