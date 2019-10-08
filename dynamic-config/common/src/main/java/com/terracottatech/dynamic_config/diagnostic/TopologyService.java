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

  NodeContext getNodeContext();

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
   * Changes the in-memory cluster to a new one for this node while it is still not activated
   */
  void setCluster(Cluster cluster);

  /**
   * Activates the Nomad system so that we can write a first config repository version. This requires the topology to set plus the license installed
   */
  void prepareActivation(Cluster validatedTopology, String licenseContent);

  /**
   * Validate and install a new license over an existing one
   *
   * @param licenseContent license file content
   */
  void upgradeLicense(String licenseContent);

  Optional<License> getLicense();

  /**
   * Validate a cluster model against the license installed in the node
   */
  void validateAgainstLicense(Cluster cluster);
}
