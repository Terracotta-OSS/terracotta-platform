/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.config.Cluster;
import com.terracottatech.dynamic_config.config.Node;
import com.terracottatech.dynamic_config.config.Stripe;
import com.terracottatech.dynamic_config.util.ConfigUtils;
import com.terracottatech.dynamic_config.validation.NodeParamsValidator;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.util.ParameterSubstitutor;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.Constants.DEFAULT_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_CONFIG_DIR;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_DATA_DIR;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_GROUP_PORT;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_HOSTNAME;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_LOG_DIR;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_METADATA_DIR;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_OFFHEAP_RESOURCE;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_PORT;
import static com.terracottatech.dynamic_config.Constants.PARAM_INTERNAL_SEP;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.config.CommonOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.config.CommonOptions.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_CONFIG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_NAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.config.CommonOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.util.CommonParamsUtils.splitQuantityUnit;
import static com.terracottatech.dynamic_config.util.ConsoleParamsUtils.addDashDash;
import static java.lang.System.lineSeparator;


public class ConsoleParamsParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleParamsParser.class);

  public static Cluster parse(Map<String, String> paramValueMap) {
    NodeParamsValidator.validate(paramValueMap);
    Node node = new Node();
    paramValueMap.forEach((param, value) -> NodeParameterSetter.set(param, value, node));
    Map<String, String> defaultsAdded = fillDefaultsIfNeeded(node);
    printParams(paramValueMap, defaultsAdded);
    return createCluster(node);
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
    String suppliedParameters = supplied.entrySet().stream()
        .map(entry -> addDashDash(entry.getKey()) + "=" + ParameterSubstitutor.substitute(entry.getValue()))
        .collect(Collectors.joining(lineSeparator() + "    ", "    ", ""));
    if (suppliedParameters.trim().isEmpty()) {
      suppliedParameters = "[]";
    } else {
      suppliedParameters = lineSeparator() + suppliedParameters;
    }
    return suppliedParameters;
  }

  private static Map<String, String> fillDefaultsIfNeeded(Node node) {
    Map<String, String> defaultOptions = new HashMap<>();
    if (node.getNodeName() == null) {
      String generateNodeName = ConfigUtils.generateNodeName();
      node.setNodeName(generateNodeName);
      defaultOptions.put(NODE_NAME, generateNodeName);
    }

    if (node.getNodeHostname() == null) {
      node.setNodeHostname(DEFAULT_HOSTNAME);
      defaultOptions.put(NODE_HOSTNAME, DEFAULT_HOSTNAME);
    }

    if (node.getNodePort() == 0) {
      node.setNodePort(Integer.parseInt(DEFAULT_PORT));
      defaultOptions.put(NODE_PORT, DEFAULT_PORT);
    }

    if (node.getNodeGroupPort() == 0) {
      node.setNodeGroupPort(Integer.parseInt(DEFAULT_GROUP_PORT));
      defaultOptions.put(NODE_GROUP_PORT, DEFAULT_GROUP_PORT);
    }

    if (node.getOffheapResources().isEmpty()) {
      String[] split = DEFAULT_OFFHEAP_RESOURCE.split(PARAM_INTERNAL_SEP);
      String[] quantityUnit = splitQuantityUnit(split[1]);
      node.setOffheapResource(split[0], MemoryUnit.valueOf(quantityUnit[1]).toBytes(Long.parseLong(quantityUnit[0])));
      defaultOptions.put(OFFHEAP_RESOURCES, DEFAULT_OFFHEAP_RESOURCE);
    }

    if (node.getDataDirs().isEmpty()) {
      String[] split = DEFAULT_DATA_DIR.split(PARAM_INTERNAL_SEP);
      node.setDataDir(split[0], Paths.get(split[1]));
      defaultOptions.put(DATA_DIRS, DEFAULT_DATA_DIR);
    }

    if (node.getNodeBindAddress() == null) {
      node.setNodeBindAddress(DEFAULT_BIND_ADDRESS);
      defaultOptions.put(NODE_BIND_ADDRESS, DEFAULT_BIND_ADDRESS);
    }

    if (node.getNodeGroupBindAddress() == null) {
      node.setNodeGroupBindAddress(DEFAULT_GROUP_BIND_ADDRESS);
      defaultOptions.put(NODE_GROUP_BIND_ADDRESS, DEFAULT_GROUP_BIND_ADDRESS);
    }

    if (node.getNodeConfigDir() == null) {
      node.setNodeConfigDir(Paths.get(DEFAULT_CONFIG_DIR));
      defaultOptions.put(NODE_CONFIG_DIR, DEFAULT_CONFIG_DIR);
    }

    if (node.getNodeLogDir() == null) {
      node.setNodeLogDir(Paths.get(DEFAULT_LOG_DIR));
      defaultOptions.put(NODE_LOG_DIR, DEFAULT_LOG_DIR);
    }

    if (node.getNodeMetadataDir() == null) {
      node.setNodeMetadataDir(Paths.get(DEFAULT_METADATA_DIR));
      defaultOptions.put(NODE_METADATA_DIR, DEFAULT_METADATA_DIR);
    }

    if (node.getFailoverPriority() == null) {
      node.setFailoverPriority(DEFAULT_FAILOVER_PRIORITY);
      defaultOptions.put(FAILOVER_PRIORITY, DEFAULT_FAILOVER_PRIORITY);
    }

    if (node.getClientReconnectWindow() == 0) {
      String[] quantityUnit = splitQuantityUnit(DEFAULT_CLIENT_RECONNECT_WINDOW);
      node.setClientReconnectWindow(TimeUnit.from(quantityUnit[1]).get().toSeconds(Long.parseLong(quantityUnit[0])));
      defaultOptions.put(CLIENT_RECONNECT_WINDOW, DEFAULT_CLIENT_RECONNECT_WINDOW);
    }

    if (node.getClientLeaseDuration() == 0) {
      String[] quantityUnit = splitQuantityUnit(DEFAULT_CLIENT_LEASE_DURATION);
      node.setClientLeaseDuration((long) TimeUnit.from(quantityUnit[1]).get().toMillis(Long.parseLong(quantityUnit[0])));
      defaultOptions.put(CLIENT_LEASE_DURATION, DEFAULT_CLIENT_LEASE_DURATION);
    }

    return defaultOptions;
  }

  private static Cluster createCluster(Node node) {
    List<Stripe> stripes = new ArrayList<>();
    stripes.add(new Stripe(Collections.singleton(node)));
    return new Cluster(stripes);
  }
}
