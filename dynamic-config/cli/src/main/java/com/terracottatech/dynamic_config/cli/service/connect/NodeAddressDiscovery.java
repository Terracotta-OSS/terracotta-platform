/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.connect;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
public interface NodeAddressDiscovery {

  /**
   * Discover the nodes of a cluster by connecting to one of the cluster node.
   * <p>
   * The first tuple value is the configured host/port address of the node we are connected to.
   * In example, we could connect with 127.0.0.1:9410 but the configured host/port for the node might be localhost:9410.
   * <p>
   * The second value of the tuple is the list of addresses of the cluster nodes. The first tuple value is of course contained in this collection.
   */
  Collection<InetSocketAddress> discover(InetSocketAddress aNode);

}
