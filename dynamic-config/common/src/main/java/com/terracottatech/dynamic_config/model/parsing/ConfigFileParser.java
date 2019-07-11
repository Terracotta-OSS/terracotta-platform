/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.parsing;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.validation.ClusterValidator;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.model.config.DefaultSettings;
import com.terracottatech.dynamic_config.model.util.ConfigFileParamsUtils;
import com.terracottatech.dynamic_config.model.validation.ConfigFileValidator;
import com.terracottatech.utilities.Tuple2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.terracottatech.dynamic_config.model.util.ConfigFileParamsUtils.getNode;
import static com.terracottatech.dynamic_config.model.util.ConfigFileParamsUtils.getStripe;

public class ConfigFileParser {
  public static Cluster parse(File file) {
    Properties properties = ConfigFileValidator.validate(file);
    return createCluster(properties);
  }

  private static Cluster createCluster(Properties properties) {
    Set<String> stripeSet = new HashSet<>();
    Map<Tuple2<String, String>, Node> uniqueServerToNodeMapping = new HashMap<>();
    properties.forEach((key, value) -> {
      // stripe.1.node.1.node-name=node-1
      stripeSet.add(getStripe(key.toString()));
      uniqueServerToNodeMapping.putIfAbsent(Tuple2.tuple2(getStripe(key.toString()), getNode(key.toString())), new Node());
    });

    List<Stripe> stripes = new ArrayList<>();
    for (String stripeName : stripeSet) {
      List<Node> stripeNodes = new ArrayList<>();
      uniqueServerToNodeMapping.entrySet().stream()
          .filter(entry -> entry.getKey().getT1().equals(stripeName))
          .forEach(entry -> stripeNodes.add(entry.getValue()));
      stripes.add(new Stripe(stripeNodes));
    }

    Cluster cluster = new Cluster(stripes);
    properties.forEach((key, value) -> {
      if (value.toString().isEmpty()) {
        return;
      }
      Tuple2<String, String> nodeIdentifier = Tuple2.tuple2(getStripe(key.toString()), getNode(key.toString()));
      NodeParameterSetter.set(ConfigFileParamsUtils.getProperty(key.toString()), value.toString(), uniqueServerToNodeMapping.get(nodeIdentifier));
    });

    uniqueServerToNodeMapping.values().forEach(DefaultSettings::fillDefaultsIfNeeded);
    ClusterValidator.validate(cluster);
    return cluster;
  }
}
