/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.topology.config.xmlobjects.ObjectFactory;
import org.w3c.dom.Element;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ClusterConfiguration {
  private final Map<Integer, StripeConfiguration> stripeIdConfigInfo = new HashMap<>();
  private final String clusterName;
  private final int stripeId;

  ClusterConfiguration(Cluster cluster, int stripeId, Supplier<Path> baseDir) {
    this.stripeId = stripeId;
    List<Stripe> stripes = cluster.getStripes();
    for (int i = 0; i < stripes.size(); i++) {
      stripeIdConfigInfo.put(i + 1, new StripeConfiguration(stripes.get(i), baseDir));
    }
    this.clusterName = cluster.getName();
  }

  Element getClusterElement() {
    ObjectFactory factory = new ObjectFactory();

    com.terracottatech.topology.config.xmlobjects.Cluster cluster = factory.createCluster();
    cluster.setName(clusterName);
    cluster.setCurrentStripeId(stripeId);

    for (Map.Entry<Integer, StripeConfiguration> entry : stripeIdConfigInfo.entrySet()) {
      cluster.getStripes().add(entry.getValue().getClusterConfigStripe(factory));
    }

    return Utils.createElement(factory.createCluster(cluster));
  }

  StripeConfiguration get(int stripeId) {
    return stripeIdConfigInfo.get(stripeId);
  }

  @Override
  public String toString() {
    return "ClusterConfiguration{" +
        "stripeIdConfigInfo=" + stripeIdConfigInfo +
        ", clusterName='" + clusterName + '\'' +
        '}';
  }
}
