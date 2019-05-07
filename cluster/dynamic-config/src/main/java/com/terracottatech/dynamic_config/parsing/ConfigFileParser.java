/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.config.Cluster;
import com.terracottatech.dynamic_config.config.Node;
import com.terracottatech.dynamic_config.config.NodeIdentifier;
import com.terracottatech.dynamic_config.config.Stripe;
import com.terracottatech.dynamic_config.validation.ClusterConfigValidator;
import com.terracottatech.dynamic_config.validation.ConfigFileValidator;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLUSTER_NAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.config.CommonOptions.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_BACKUP_DIR;
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
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_WHITELIST;


public class ConfigFileParser {
  public static Cluster parse(File file) {
    Properties properties = ConfigFileValidator.validate(file);
    return initCluster(properties);
  }

  private static Cluster initCluster(Properties properties) {
    Set<String> stripeSet = new HashSet<>();
    Map<NodeIdentifier, Node> uniqueServerToNodeMapping = new HashMap<>();
    properties.forEach((key, value) -> {
      // my-cluster.stripe-1.node-1.node-name=node-1
      String[] split = splitKey(key);
      stripeSet.add(split[1]);
      uniqueServerToNodeMapping.putIfAbsent(new NodeIdentifier(split[1], split[2]), new Node());
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
    Setting.set(properties, uniqueServerToNodeMapping);
    ClusterConfigValidator.validate(cluster);
    return cluster;
  }

  private static String[] splitKey(Object key) {
    return key.toString().split("\\.");
  }

  private static class Setting {
    private static final Map<String, TriConsumer<Node, String[], String>> PARAM_ACTION_MAP = new HashMap<>();

    static {
      PARAM_ACTION_MAP.put(NODE_NAME, (node, split, value) -> node.setNodeName(value));
      PARAM_ACTION_MAP.put(NODE_HOSTNAME, (node, split, value) -> node.setNodeHostname(value));
      PARAM_ACTION_MAP.put(NODE_PORT, (node, split, value) -> node.setNodePort(Integer.parseInt(value)));
      PARAM_ACTION_MAP.put(NODE_GROUP_PORT, (node, split, value) -> node.setNodeGroupPort(Integer.parseInt(value)));
      PARAM_ACTION_MAP.put(NODE_BIND_ADDRESS, (node, split, value) -> node.setNodeBindAddress(value));
      PARAM_ACTION_MAP.put(NODE_GROUP_BIND_ADDRESS, (node, split, value) -> node.setNodeGroupBindAddress(value));
      PARAM_ACTION_MAP.put(NODE_CONFIG_DIR, (node, split, value) -> node.setNodeConfigDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(NODE_METADATA_DIR, (node, split, value) -> node.setNodeMetadataDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(NODE_LOG_DIR, (node, split, value) -> node.setNodeLogDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(NODE_BACKUP_DIR, (node, split, value) -> node.setNodeBackupDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(SECURITY_DIR, (node, split, value) -> node.setSecurityDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(SECURITY_AUDIT_LOG_DIR, (node, split, value) -> node.setSecurityAuditLogDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(SECURITY_AUTHC, (node, split, value) -> node.setSecurityAuthc(value));
      PARAM_ACTION_MAP.put(SECURITY_SSL_TLS, (node, split, value) -> node.setSecuritySslTls(Boolean.valueOf(value)));
      PARAM_ACTION_MAP.put(SECURITY_WHITELIST, (node, split, value) -> node.setSecurityWhitelist(Boolean.valueOf(value)));
      PARAM_ACTION_MAP.put(FAILOVER_PRIORITY, (node, split, value) -> node.setFailoverPriority(value));
      PARAM_ACTION_MAP.put(CLIENT_RECONNECT_WINDOW, (node, split, value) -> node.setClientReconnectWindow(value));
      PARAM_ACTION_MAP.put(CLIENT_LEASE_DURATION, (node, split, value) -> node.setClientLeaseDuration(value));
      PARAM_ACTION_MAP.put(OFFHEAP_RESOURCES, (node, split, value) -> node.setOffheapResource(split[4], value));
      PARAM_ACTION_MAP.put(DATA_DIRS, (node, split, value) -> node.setDataDir(split[4], Paths.get(value)));
      PARAM_ACTION_MAP.put(CLUSTER_NAME, (node, split, value) -> node.setClusterName(value));
    }

    static void set(Properties properties, Map<NodeIdentifier, Node> stripeNodeMap) {
      properties.forEach((key, value) -> {
        // my-cluster.stripe-1.node-1.node-name=node-1
        String[] keys = splitKey(key);
        final Node node = stripeNodeMap.get(new NodeIdentifier(keys[1], keys[2]));
        set(node, keys, value.toString());
      });
    }

    private static void set(Node node, String[] split, String value) {
      String property = split[3];
      TriConsumer<Node, String[], String> action = PARAM_ACTION_MAP.get(property);
      if (action == null) {
        throw new AssertionError("Unrecognized property: " + Arrays.toString(split));
      }
      action.accept(node, split, value);
    }

    private static String[] splitKey(Object key) {
      return key.toString().split("\\.");
    }

    private interface TriConsumer<T, U, V> {
      void accept(T t, U u, V v);
    }
  }
}
