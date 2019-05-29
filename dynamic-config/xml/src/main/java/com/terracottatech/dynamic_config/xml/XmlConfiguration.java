/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import com.terracottatech.dynamic_config.model.Cluster;

public class XmlConfiguration {
  private final ServerConfiguration serverConfiguration;

  public XmlConfiguration(Cluster cluster, String stripeName, String nodeName) {
    ClusterConfiguration clusterConfiguration = new ClusterConfiguration(cluster);

    StripeConfiguration stripeConfiguration = clusterConfiguration.get(stripeName);

    if (stripeConfiguration == null) {
      throw new IllegalArgumentException("Stripe with name `" + stripeName + "` is not present in the cluster config");
    }

    ServerConfiguration serverConfiguration = stripeConfiguration.get(nodeName);

    if (serverConfiguration == null) {
      throw new IllegalArgumentException("Node with name `" + nodeName + "` is not present in the cluster config");
    }

    this.serverConfiguration = serverConfiguration;
    this.serverConfiguration.addClusterTopology(clusterConfiguration.getClusterElement());
  }

  @Override
  public String toString() {
    return this.serverConfiguration.toString();
  }
}
