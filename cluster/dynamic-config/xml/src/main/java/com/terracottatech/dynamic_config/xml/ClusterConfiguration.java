/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import org.w3c.dom.Element;

import com.terracottatech.dynamic_config.config.Cluster;
import com.terracottatech.dynamic_config.config.Node;
import com.terracottatech.dynamic_config.config.Stripe;
import com.terracottatech.topology.config.xmlobjects.ObjectFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ClusterConfiguration {
  private final Map<String, StripeConfiguration> clusterConfiguration = new HashMap<>();
  private final String clusterName;

  ClusterConfiguration(Cluster cluster) {
    List<Stripe> stripes = cluster.getStripes();
    for (int i = 0; i < stripes.size(); i++) {
      String stripeName = "stripe-" + (i + 1);
      clusterConfiguration.put(stripeName, new StripeConfiguration(stripeName, stripes.get(i)));
    }
    this.clusterName = getClusterName(cluster);
  }

  Element getClusterElement() {
    ObjectFactory factory = new ObjectFactory();

    com.terracottatech.topology.config.xmlobjects.Cluster cluster = factory.createCluster();
    cluster.setName(clusterName);

    for (Map.Entry<String, StripeConfiguration> entry : clusterConfiguration.entrySet()) {
      cluster.getStripes().add(entry.getValue().getClusterConfigStripe(factory));
    }

    return Utils.createElement(factory.createCluster(cluster));
  }

  StripeConfiguration get(String stripeName) {
    return clusterConfiguration.get(stripeName);
  }

  private static String getClusterName(Cluster cluster) {
    if (cluster != null) {
      List<Stripe> stripes = cluster.getStripes();
      if (stripes != null) {
        Stripe stripe = stripes.get(0);
        if (stripe != null) {
          Collection<Node> nodes = stripe.getNodes();
          if (nodes != null) {
            Iterator<Node> iterator = nodes.iterator();
            if (iterator.hasNext()) {
              return iterator.next().getClusterName();
            }
          }
        }
      }
    }

    return null;
  }
}
