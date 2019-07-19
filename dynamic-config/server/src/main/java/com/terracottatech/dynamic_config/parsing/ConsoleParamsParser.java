/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.model.util.ConsoleParamsUtils;
import com.terracottatech.dynamic_config.model.validation.NodeParamsValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.util.ParameterSubstitutor;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;

public class ConsoleParamsParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleParamsParser.class);

  public static Cluster parse(Map<String, String> paramValueMap) {
    NodeParamsValidator.validate(paramValueMap);
    Node node = new Node();
    Cluster cluster = new Cluster(new Stripe(node));
    paramValueMap.forEach((param, value) -> ParameterSetter.set(param, value, cluster));
    Map<String, String> defaultsAdded = new TreeMap<>();
    node.fillDefaults(defaultsAdded::put);
    printParams(paramValueMap, defaultsAdded);
    return cluster;
  }

  private static void printParams(Map<String, String> supplied, Map<String, String> defaulted) {
    LOGGER.info(
        String.format(
            "%sRead the following parameters: %s%sAdded the following defaults: %s",
            lineSeparator(),
            toDisplayParams(supplied),
            lineSeparator(),
            toDisplayParams(defaulted)
        )
    );
  }

  private static String toDisplayParams(Map<String, String> supplied) {
    String suppliedParameters = supplied.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
        .map(entry -> ConsoleParamsUtils.addDashDash(entry.getKey()) + "=" + ParameterSubstitutor.substitute(entry.getValue()))
        .collect(Collectors.joining(lineSeparator() + "    ", "    ", ""));
    if (suppliedParameters.trim().isEmpty()) {
      suppliedParameters = "[]";
    } else {
      suppliedParameters = lineSeparator() + suppliedParameters;
    }
    return suppliedParameters;
  }
}
