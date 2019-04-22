/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.config.AllOptions;
import com.terracottatech.dynamic_config.config.Cluster;
import com.terracottatech.dynamic_config.config.Node;
import com.terracottatech.dynamic_config.config.Stripe;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_BACKUP_DIR;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_CONFIG_DIR;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_DATA_DIR;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_GROUP_PORT;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_HOSTNAME;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_LOG_DIR;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_METADATA_DIR;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_OFFHEAP_RESOURCE;
import static com.terracottatech.dynamic_config.util.Constants.DEFAULT_PORT;
import static com.terracottatech.utilities.HostAndIpValidator.isValidIPv6;


public class ConsoleParamsParser {
  public static Cluster parse(Map<String, String> paramValueMap) {
    Node node = new Node();
    paramValueMap.forEach((param, value) -> Setting.set(param, value, node));
    fillDefaultsIfNeeded(node);
    return createCluster(node);
  }

  private static Cluster createCluster(Node node) {
    List<Stripe> stripes = new ArrayList<>();
    stripes.add(new Stripe(Collections.singletonList(node)));
    return new Cluster(stripes);
  }

  private static void fillDefaultsIfNeeded(Node node) {
    if (node.getNodeName() == null) {
      node.setNodeName(getDefaultNodeName(node));
    }
    if (node.getNodeHostname() == null) {
      node.setNodeHostname(DEFAULT_HOSTNAME);
    }
    if (node.getNodePort() == 0) {
      node.setNodePort(DEFAULT_PORT);
    }
    if (node.getNodeGroupPort() == 0) {
      node.setNodeGroupPort(DEFAULT_GROUP_PORT);
    }
    if (node.getOffheapResources().isEmpty()) {
      String[] split = DEFAULT_OFFHEAP_RESOURCE.split(":");
      node.setOffheapResource(split[0], split[1]);
    }
    if (node.getDataDirs().isEmpty()) {
      String[] split = DEFAULT_DATA_DIR.split(":");
      node.setDataDir(split[0], Paths.get(split[1]));
    }
    if (node.getNodeBindAddress() == null) {
      node.setNodeBindAddress(DEFAULT_BIND_ADDRESS);
    }
    if (node.getNodeGroupBindAddress() == null) {
      node.setNodeGroupBindAddress(DEFAULT_GROUP_BIND_ADDRESS);
    }
    if (node.getNodeConfigDir() == null) {
      node.setNodeConfigDir(Paths.get(DEFAULT_CONFIG_DIR));
    }
    if (node.getNodeLogDir() == null) {
      node.setNodeLogDir(Paths.get(DEFAULT_LOG_DIR));
    }
    if (node.getNodeMetadataDir() == null) {
      node.setNodeMetadataDir(Paths.get(DEFAULT_METADATA_DIR));
    }
    if (node.getNodeBackupDir() == null) {
      node.setNodeBackupDir(Paths.get(DEFAULT_BACKUP_DIR));
    }
    if (node.getFailoverPriority() == null) {
      node.setFailoverPriority(DEFAULT_FAILOVER_PRIORITY);
    }
    if (node.getClientReconnectWindow() == null) {
      node.setClientReconnectWindow(DEFAULT_CLIENT_RECONNECT_WINDOW);
    }
    if (node.getClientLeaseDuration() == null) {
      node.setClientLeaseDuration(DEFAULT_CLIENT_LEASE_DURATION);
    }
  }

  private static String getDefaultNodeName(Node node) {
    String hostName = node.getNodeHostname();
    if (hostName == null) {
      hostName = DEFAULT_HOSTNAME;
    }

    int nodePort = node.getNodePort();
    if (nodePort == 0) {
      nodePort = DEFAULT_PORT;
    }
    return isValidIPv6(hostName) ? ("[" + hostName + "]:" + nodePort) : (hostName + ":" + nodePort);
  }

  private static class Setting {
    private static final Map<String, BiConsumer<Node, String>> PARAM_ACTION_MAP = populateMap();

    private static Map<String, BiConsumer<Node, String>> populateMap() {
      HashMap<String, BiConsumer<Node, String>> paramActionMap = new HashMap<>();
      paramActionMap.put(AllOptions.NODE_NAME, Node::setNodeName);
      paramActionMap.put(AllOptions.NODE_HOSTNAME, Node::setNodeHostname);
      paramActionMap.put(AllOptions.NODE_PORT, (node, value) -> node.setNodePort(Integer.parseInt(value)));
      paramActionMap.put(AllOptions.NODE_GROUP_PORT, (node, value) -> node.setNodeGroupPort(Integer.parseInt(value)));
      paramActionMap.put(AllOptions.NODE_BIND_ADDRESS, Node::setNodeBindAddress);
      paramActionMap.put(AllOptions.NODE_GROUP_BIND_ADDRESS, Node::setNodeGroupBindAddress);
      paramActionMap.put(AllOptions.NODE_CONFIG_DIR, (node, value) -> node.setNodeConfigDir(Paths.get(value)));
      paramActionMap.put(AllOptions.NODE_METADATA_DIR, (node, value) -> node.setNodeMetadataDir(Paths.get(value)));
      paramActionMap.put(AllOptions.NODE_LOG_DIR, (node, value) -> node.setNodeLogDir(Paths.get(value)));
      paramActionMap.put(AllOptions.NODE_BACKUP_DIR, (node, value) -> node.setNodeBackupDir(Paths.get(value)));
      paramActionMap.put(AllOptions.SECURITY_DIR, (node, value) -> node.setSecurityDir(Paths.get(value)));
      paramActionMap.put(AllOptions.SECURITY_AUDIT_LOG_DIR, (node, value) -> node.setSecurityAuditLogDir(Paths.get(value)));
      paramActionMap.put(AllOptions.SECURITY_AUTHC, Node::setSecurityAuthc);
      paramActionMap.put(AllOptions.SECURITY_SSL_TLS, (node, value) -> node.setSecuritySslTls(Boolean.valueOf(value)));
      paramActionMap.put(AllOptions.SECURITY_WHITELIST, (node, value) -> node.setSecurityWhitelist(Boolean.valueOf(value)));
      paramActionMap.put(AllOptions.FAILOVER_PRIORITY, Node::setFailoverPriority);
      paramActionMap.put(AllOptions.CLIENT_RECONNECT_WINDOW, Node::setClientReconnectWindow);
      paramActionMap.put(AllOptions.CLIENT_LEASE_DURATION, Node::setClientLeaseDuration);
      paramActionMap.put(AllOptions.CLUSTER_NAME, Node::setClusterName);
      paramActionMap.put(AllOptions.OFFHEAP_RESOURCES, (node, value) -> Arrays.asList(value.split(",")).forEach(ofr -> {
        String[] split = ofr.split(":");
        node.setOffheapResource(split[0], split[1]);
      }));
      paramActionMap.put(AllOptions.DATA_DIRS, (node, value) -> Arrays.asList(value.split(",")).forEach(dir -> {
        String[] split = dir.split(":");
        node.setDataDir(split[0], Paths.get(split[1]));
      }));
      return paramActionMap;
    }

    static void set(String param, String value, Node node) {
      PARAM_ACTION_MAP.get(param).accept(node, value);
    }
  }
}
