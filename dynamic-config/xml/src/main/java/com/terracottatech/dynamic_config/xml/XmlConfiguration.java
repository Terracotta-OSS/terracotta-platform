/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.utilities.PathResolver;

public class XmlConfiguration {
  private final ServerConfiguration serverConfiguration;

  public XmlConfiguration(Cluster cluster, int stripeId, String nodeName, PathResolver pathResolver) {
    ClusterConfiguration clusterConfiguration = new ClusterConfiguration(cluster, stripeId, pathResolver);

    StripeConfiguration stripeConfiguration = clusterConfiguration.get(stripeId);
    if (stripeConfiguration == null) {
      throw new IllegalArgumentException(
          String.format(
              "Stripe with ID: %s is not present in the cluster config: %s",
              stripeId,
              clusterConfiguration
          )
      );
    }

    ServerConfiguration serverConfiguration = stripeConfiguration.get(nodeName);
    if (serverConfiguration == null) {
      throw new IllegalArgumentException(
          String.format(
              "Node with name: %s is not present in the cluster config: %s",
              nodeName,
              clusterConfiguration
          )
      );
    }

    this.serverConfiguration = serverConfiguration;

    // create a special version that contains exactly the same relative paths as given by the topology model
    // this is to be able to keep user input but still be able to have a tc-config file that the servers can use
    // with all the folders inside pointing to the right location (ie %(user.dir)/...) instead of being relative to
    // the location of the tc-config file which is inside a repository path sub-folder now
    ClusterConfiguration unresolvedClusterConfiguration = new ClusterConfiguration(cluster, stripeId, PathResolver.NOOP);
    this.serverConfiguration.addClusterTopology(unresolvedClusterConfiguration.getClusterElement());
  }

  @Override
  public String toString() {
    return this.serverConfiguration.toString();
  }
}
