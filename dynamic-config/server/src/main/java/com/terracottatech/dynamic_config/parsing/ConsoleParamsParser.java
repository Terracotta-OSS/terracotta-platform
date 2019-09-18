/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.utilities.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static org.terracotta.config.util.ParameterSubstitutor.substitute;

public class ConsoleParamsParser implements Parser<Cluster> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleParamsParser.class);
  private final TreeMap<Setting, String> paramValueMap;
  private final IParameterSubstitutor parameterSubstitutor;

  public ConsoleParamsParser(Map<Setting, String> paramValueMap, IParameterSubstitutor parameterSubstitutor) {
    this.paramValueMap = new TreeMap<>(Comparator.comparing(Setting::toString));
    this.paramValueMap.putAll(paramValueMap);
    this.parameterSubstitutor = parameterSubstitutor;
  }

  @Override
  public Cluster parse() {
    Node node = new Node();
    String clusterName = paramValueMap.remove(Setting.CLUSTER_NAME);
    Cluster cluster = new Cluster(new Stripe(node));
    cluster.setName(clusterName);
    paramValueMap.forEach((setting, value) -> setting.setProperty(node, setting.requiresEagerSubstitution() ? parameterSubstitutor.substitute(value) : value));
    addDefaults(node);
    return cluster;
  }

  private void addDefaults(Node node) {
    Map<Setting, String> defaultsAdded = new TreeMap<>(Comparator.comparing(Setting::toString));
    if (node.getNodeHostname() == null) {
      // We can't hostname null during Node construction from the client side (e.g. during parsing a config properties
      // file in activate command). Therefore, this logic is here, and not in Node::fillDefaults
      String hostName = parameterSubstitutor.substitute(Setting.NODE_HOSTNAME.getDefaultValue());
      node.setNodeHostname(hostName);
      defaultsAdded.put(Setting.NODE_HOSTNAME, hostName);
    }

    node.fillDefaults(defaultsAdded::put);
    LOGGER.info(
        String.format(
            "%sRead the following parameters: %s%sAdded the following defaults: %s",
            lineSeparator(),
            toDisplayParams(paramValueMap),
            lineSeparator(),
            toDisplayParams(defaultsAdded)
        )
    );
  }

  private String toDisplayParams(Map<Setting, String> supplied) {
    String suppliedParameters = supplied.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
        .map(entry -> ConsoleParamsUtils.addDashDash(entry.getKey()) + "=" + substitute(entry.getValue()))
        .collect(Collectors.joining(lineSeparator() + "    ", "    ", ""));
    if (suppliedParameters.trim().isEmpty()) {
      suppliedParameters = "[]";
    } else {
      suppliedParameters = lineSeparator() + suppliedParameters;
    }
    return suppliedParameters;
  }
}
