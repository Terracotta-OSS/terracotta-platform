/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.config;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.utilities.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getNodeId;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getProperty;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getStripeId;
import static com.terracottatech.utilities.Assertion.assertNonNull;

public class ConfigFileContainer {
  private final Properties properties;
  private final String clusterName;
  private final IParameterSubstitutor paramSubstitutor;

  public ConfigFileContainer(String fileName, Properties properties) {
    this(fileName, properties, null);
  }

  public ConfigFileContainer(String fileName, Properties properties, String clusterName) {
    this(fileName, properties, clusterName, IParameterSubstitutor.identity());
  }

  public ConfigFileContainer(String fileName, Properties properties, String clusterName, IParameterSubstitutor paramSubstitutor) {
    this.properties = properties;
    this.paramSubstitutor = paramSubstitutor;
    this.clusterName = extractClusterName(fileName, clusterName);
    assertNonNull(this.clusterName);
  }

  public Cluster createCluster() {
    Set<Integer> stripeSet = new HashSet<>();
    Map<Tuple2<Integer, String>, Node> uniqueServerToNodeMapping = new HashMap<>();
    Map<Tuple2<Integer, String>, NodeParameterSetter> nodeParameterSetters = new HashMap<>();

    properties.forEach((key, value) -> {
      // stripe.1.node.1.node-name=node-1
      stripeSet.add(getStripeId(key.toString()));
      Tuple2<Integer, String> nodeIdentifier = Tuple2.tuple2(getStripeId(key.toString()), getNodeId(key.toString()));
      uniqueServerToNodeMapping.putIfAbsent(nodeIdentifier, new Node());
      nodeParameterSetters.putIfAbsent(nodeIdentifier, new NodeParameterSetter(uniqueServerToNodeMapping.get(nodeIdentifier), paramSubstitutor));
    });

    List<Stripe> stripes = new ArrayList<>();
    for (Integer stripeId : stripeSet) {
      List<Node> stripeNodes = new ArrayList<>();
      uniqueServerToNodeMapping.entrySet().stream()
          .filter(entry -> entry.getKey().getT1().equals(stripeId))
          .forEach(entry -> stripeNodes.add(entry.getValue()));
      stripes.add(new Stripe(stripeNodes));
    }

    Cluster cluster = new Cluster(clusterName, stripes);
    properties.forEach((key, value) -> {
      Tuple2<Integer, String> nodeIdentifier = Tuple2.tuple2(getStripeId(key.toString()), getNodeId(key.toString()));
      NodeParameterSetter nodeParameterSetter = nodeParameterSetters.get(nodeIdentifier);
      nodeParameterSetter.set(getProperty(key.toString()), value.toString());
    });

    uniqueServerToNodeMapping.values().forEach(Node::fillDefaults);
    return cluster;
  }

  public String getClusterName() {
    return clusterName;
  }

  private String extractClusterName(String fileName, String optionalClusterName) {
    if (optionalClusterName != null) {
      return optionalClusterName;
    }
    optionalClusterName = fileName;
    int point = optionalClusterName.lastIndexOf('.');
    if (point != -1) {
      // removes the extension
      optionalClusterName = optionalClusterName.substring(0, point);
    }
    return optionalClusterName;
  }
}
