/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.ObjectFactory;
import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.TcCluster;
import com.terracottatech.utilities.PathResolver;
import org.w3c.dom.Element;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClusterConfiguration {
  // please keep an ordering
  private final Map<Integer, StripeConfiguration> stripeIdConfigInfo = new LinkedHashMap<>();
  private final String clusterName;
  private final int stripeId;

  ClusterConfiguration(Cluster cluster, int stripeId, PathResolver pathResolver) {
    this.stripeId = stripeId;
    List<Stripe> stripes = cluster.getStripes();
    for (int i = 0; i < stripes.size(); i++) {
      stripeIdConfigInfo.put(i + 1, new StripeConfiguration(stripes.get(i), pathResolver));
    }
    this.clusterName = cluster.getName();
  }

  Element getClusterElement() {
    ObjectFactory factory = new ObjectFactory();

    TcCluster cluster = factory.createTcCluster();
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
