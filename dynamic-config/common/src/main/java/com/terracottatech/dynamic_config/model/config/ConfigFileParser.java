/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.config;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.utilities.Tuple2;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getNodeId;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getSetting;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getStripeId;
import static com.terracottatech.utilities.Assertions.assertNonNull;
import static com.terracottatech.utilities.Tuple2.tuple2;
import static java.util.Objects.requireNonNull;

public class ConfigFileParser {
  private final Properties properties;
  private final String clusterName;
  private final IParameterSubstitutor paramSubstitutor;

  public ConfigFileParser(Path configFile, Properties properties, IParameterSubstitutor paramSubstitutor) {
    this.clusterName = extractClusterName(configFile);
    this.properties = properties;
    this.paramSubstitutor = paramSubstitutor;
    assertNonNull(this.clusterName);
  }

  public Cluster createCluster() {
    Set<Integer> stripeSet = new TreeSet<>();
    // keep the map ordered by stripe ID then node name
    Comparator<Tuple2<Integer, String>> comparator = Comparator.comparing(tuple -> tuple.t1 + tuple.t2);
    Map<Tuple2<Integer, String>, Node> uniqueServerToNodeMapping = new TreeMap<>(comparator);

    properties.forEach((key, value) -> {
      // stripe.1.node.1.node-name=node-1
      stripeSet.add(getStripeId(key.toString()));
      Tuple2<Integer, String> nodeIdentifier = tuple2(getStripeId(key.toString()), getNodeId(key.toString()));
      uniqueServerToNodeMapping.putIfAbsent(nodeIdentifier, new Node());
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
    properties.entrySet().stream()
        .filter(e -> e.getValue() != null && !e.getValue().toString().trim().isEmpty())
        .forEach(e -> {
          String key = e.getKey().toString();
          String value = e.getValue().toString();
          Tuple2<Integer, String> nodeIdentifier = tuple2(getStripeId(key), getNodeId(key));
          Node node = uniqueServerToNodeMapping.get(nodeIdentifier);
          Setting setting = getSetting(key);
          setting.setProperty(node, setting.requiresEagerSubstitution() ? paramSubstitutor.substitute(value) : value);
        });

    uniqueServerToNodeMapping.values().forEach(Node::fillDefaults);
    return cluster;
  }

  public String getClusterName() {
    return clusterName;
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private static String extractClusterName(Path configFile) {
    requireNonNull(configFile.getFileName());
    String clusterName = configFile.getFileName().toString();
    int point = clusterName.lastIndexOf('.');
    if (point != -1) {
      // removes the extension
      clusterName = clusterName.substring(0, point);
    }
    return clusterName;
  }
}
