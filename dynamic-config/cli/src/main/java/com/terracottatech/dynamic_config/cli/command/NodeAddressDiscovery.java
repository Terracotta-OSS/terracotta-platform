/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.command;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * Discover the nodes of a cluster by connecting to one of the cluster node.
 *
 * @author Mathieu Carbou
 */
public interface NodeAddressDiscovery {

  Collection<InetSocketAddress> discover(InetSocketAddress aNode) throws NodeAddressDiscoveryException;

}
