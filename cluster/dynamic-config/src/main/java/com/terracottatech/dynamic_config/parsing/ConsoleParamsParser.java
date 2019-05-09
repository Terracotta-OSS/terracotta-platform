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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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


public class ConsoleParamsParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleParamsParser.class);

  public static Cluster parse(Map<String, String> paramValueMap) {
    NodeParamsValidator.validate(paramValueMap);
    Node node = new Node();
    paramValueMap.forEach((param, value) -> NodeParameterSetter.set(param, value, node));
    fillDefaultsIfNeeded(node);
    return createCluster(node);
  }

  private static void fillDefaultsIfNeeded(Node node) {
    List<Object> defaultOptions = new ArrayList<>();
    if (node.getNodeName() == null) {
      defaultOptions.add(NODE_NAME);
      node.setNodeName(ConfigUtils.generateNodeName());
    }

    if (node.getNodeHostname() == null) {
      defaultOptions.add(NODE_HOSTNAME);
      node.setNodeHostname(DEFAULT_HOSTNAME);
    }

    if (node.getNodePort() == 0) {
      defaultOptions.add(NODE_PORT);
      node.setNodePort(DEFAULT_PORT);
    }

    if (node.getNodeGroupPort() == 0) {
      defaultOptions.add(NODE_GROUP_PORT);
      node.setNodeGroupPort(DEFAULT_GROUP_PORT);
    }

    if (node.getOffheapResources().isEmpty()) {
      defaultOptions.add(OFFHEAP_RESOURCES);
      String[] split = DEFAULT_OFFHEAP_RESOURCE.split(PARAM_INTERNAL_SEP);
      String[] quantityUnit = splitQuantityUnit(split[1]);
      node.setOffheapResource(split[0], MemoryUnit.valueOf(quantityUnit[1]).toBytes(Long.parseLong(quantityUnit[0])));
    }

    if (node.getDataDirs().isEmpty()) {
      defaultOptions.add(DATA_DIRS);
      String[] split = DEFAULT_DATA_DIR.split(PARAM_INTERNAL_SEP);
      node.setDataDir(split[0], Paths.get(split[1]));
    }

    if (node.getNodeBindAddress() == null) {
      defaultOptions.add(NODE_BIND_ADDRESS);
      node.setNodeBindAddress(DEFAULT_BIND_ADDRESS);
    }

    if (node.getNodeGroupBindAddress() == null) {
      defaultOptions.add(NODE_GROUP_BIND_ADDRESS);
      node.setNodeGroupBindAddress(DEFAULT_GROUP_BIND_ADDRESS);
    }

    if (node.getNodeConfigDir() == null) {
      defaultOptions.add(NODE_CONFIG_DIR);
      node.setNodeConfigDir(Paths.get(DEFAULT_CONFIG_DIR));
    }

    if (node.getNodeLogDir() == null) {
      defaultOptions.add(NODE_LOG_DIR);
      node.setNodeLogDir(Paths.get(DEFAULT_LOG_DIR));
    }

    if (node.getNodeMetadataDir() == null) {
      defaultOptions.add(NODE_METADATA_DIR);
      node.setNodeMetadataDir(Paths.get(DEFAULT_METADATA_DIR));
    }

    if (node.getFailoverPriority() == null) {
      defaultOptions.add(FAILOVER_PRIORITY);
      node.setFailoverPriority(DEFAULT_FAILOVER_PRIORITY);
    }

    if (node.getClientReconnectWindow() == 0) {
      defaultOptions.add(CLIENT_RECONNECT_WINDOW);
      String[] quantityUnit = splitQuantityUnit(DEFAULT_CLIENT_RECONNECT_WINDOW);
      node.setClientReconnectWindow(TimeUnit.from(quantityUnit[1]).get().toSeconds(Long.parseLong(quantityUnit[0])));
    }

    if (node.getClientLeaseDuration() == 0) {
      defaultOptions.add(CLIENT_LEASE_DURATION);
      String[] quantityUnit = splitQuantityUnit(DEFAULT_CLIENT_LEASE_DURATION);
      node.setClientLeaseDuration(TimeUnit.from(quantityUnit[1]).get().toMillis(Long.parseLong(quantityUnit[0])));
    }

    if (!defaultOptions.isEmpty()) {
      LOGGER.info("Added defaults values for: {}", defaultOptions.stream().map(o -> addDashDash(o.toString())).collect(Collectors.toList()));
    }
  }

  private static Cluster createCluster(Node node) {
    List<Stripe> stripes = new ArrayList<>();
    stripes.add(new Stripe(Collections.singleton(node)));
    return new Cluster(stripes);
  }
}
