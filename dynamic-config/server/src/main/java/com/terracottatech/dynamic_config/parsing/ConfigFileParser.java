/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.config.DefaultSettings;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.config.NodeIdentifier;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.validation.ClusterConfigValidator;
import com.terracottatech.dynamic_config.validation.ConfigFileValidator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getNode;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getProperty;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getStripe;

public class ConfigFileParser {
  public static Cluster parse(File file) {
    Properties properties = ConfigFileValidator.validate(file);
    return createCluster(properties);
  }

  private static Cluster createCluster(Properties properties) {
    Set<String> stripeSet = new HashSet<>();
    Map<NodeIdentifier, Node> uniqueServerToNodeMapping = new HashMap<>();
    properties.forEach((key, value) -> {
      // stripe.1.node.1.node-name=node-1
      stripeSet.add(getStripe(key.toString()));
      uniqueServerToNodeMapping.putIfAbsent(new NodeIdentifier(getStripe(key.toString()), getNode(key.toString())), new Node());
    });

    List<Stripe> stripes = new ArrayList<>();
    for (String stripeName : stripeSet) {
      List<Node> stripeNodes = new ArrayList<>();
      uniqueServerToNodeMapping.entrySet().stream()
          .filter(entry -> entry.getKey().getStripeName().equals(stripeName))
          .forEach(entry -> stripeNodes.add(entry.getValue()));
      stripes.add(new Stripe(stripeNodes));
    }

    Cluster cluster = new Cluster(stripes);
    properties.forEach((key, value) -> {
      if (value.toString().isEmpty()) {
        return;
      }
      NodeIdentifier nodeIdentifier = new NodeIdentifier(getStripe(key.toString()), getNode(key.toString()));
      NodeParameterSetter.set(getProperty(key.toString()), value.toString(), uniqueServerToNodeMapping.get(nodeIdentifier));
    });

    uniqueServerToNodeMapping.values().forEach(DefaultSettings::fillDefaultsIfNeeded);
    ClusterConfigValidator.validate(cluster);
    return cluster;
  }
}
