/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.config.Cluster;
import com.terracottatech.dynamic_config.config.Node;
import com.terracottatech.dynamic_config.config.Stripe;
import com.terracottatech.dynamic_config.exception.MalformedConfigFileException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.terracottatech.dynamic_config.config.AllOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.config.AllOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.config.AllOptions.CLUSTER_NAME;
import static com.terracottatech.dynamic_config.config.AllOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.config.AllOptions.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_CONFIG_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_NAME;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.config.AllOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.config.AllOptions.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.config.AllOptions.SECURITY_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.config.AllOptions.SECURITY_WHITELIST;


public class ConfigFileParser {
  public static Cluster parse(File file) {
    Properties properties = loadProperties(file);
    return initCluster(properties);
  }

  private static Properties loadProperties(File propertiesFile) {
    Properties props = new Properties();
    try (Reader in = new InputStreamReader(new FileInputStream(propertiesFile), StandardCharsets.UTF_8)) {
      props.load(in);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return props;
  }

  static Cluster initCluster(Properties properties) {
    Set<String> clusterSet = new HashSet<>();
    Set<String> stripeSet = new HashSet<>();
    Set<String> stripeNodeSet = new HashSet<>();

    properties.forEach((key, value) -> {
      // my-cluster.stripe-1.node-1.node-name=node-1
      String[] split = splitKey(key);
      if (split.length < 4) {
        throw new MalformedConfigFileException("Each line read from config file must have at least 4 period-separated fields");
      }

      clusterSet.add(split[0]);
      stripeSet.add(split[1]);
      stripeNodeSet.add(split[1] + " " + split[2]); //Using space as separator
    });

    if (clusterSet.size() != 1) {
      throw new MalformedConfigFileException("File should contain a single cluster information only");
    }

    List<Stripe> stripes = new ArrayList<>();
    Map<String, Node> nodeMap = new HashMap<>();
    for (String stripeName : stripeSet) {
      List<Node> stripeNodes = new ArrayList<>();
      stripeNodeSet.stream()
          .filter(s -> s.split(" ")[0].equals(stripeName))
          .map(s -> s.split(" ")[1])
          .forEach(nodeName -> {
            Node node = new Node();
            stripeNodes.add(node);
            nodeMap.put(nodeName, node);
          });
      stripes.add(new Stripe(stripeNodes));
    }
    Cluster cluster = new Cluster(stripes);
    Setting.set(properties, nodeMap);
    return cluster;
  }

  private static String[] splitKey(Object key) {
    return key.toString().split("\\.");
  }

  private static class Setting {
    private static final Map<String, TriConsumer<Node, String[], String>> PARAM_ACTION_MAP = populateMap();

    private static Map<String, TriConsumer<Node, String[], String>> populateMap() {
      HashMap<String, TriConsumer<Node, String[], String>> paramActionMap = new HashMap<>();
      paramActionMap.put(NODE_NAME, (node, split, value) -> node.setNodeName(value));
      paramActionMap.put(NODE_HOSTNAME, (node, split, value) -> node.setNodeHostname(value));
      paramActionMap.put(NODE_PORT, (node, split, value) -> node.setNodePort(Integer.parseInt(value)));
      paramActionMap.put(NODE_GROUP_PORT, (node, split, value) -> node.setNodeGroupPort(Integer.parseInt(value)));
      paramActionMap.put(NODE_BIND_ADDRESS, (node, split, value) -> node.setNodeBindAddress(value));
      paramActionMap.put(NODE_GROUP_BIND_ADDRESS, (node, split, value) -> node.setNodeGroupBindAddress(value));
      paramActionMap.put(NODE_CONFIG_DIR, (node, split, value) -> node.setNodeConfigDir(Paths.get(value)));
      paramActionMap.put(NODE_METADATA_DIR, (node, split, value) -> node.setNodeMetadataDir(Paths.get(value)));
      paramActionMap.put(NODE_LOG_DIR, (node, split, value) -> node.setNodeLogDir(Paths.get(value)));
      paramActionMap.put(NODE_BACKUP_DIR, (node, split, value) -> node.setNodeBackupDir(Paths.get(value)));
      paramActionMap.put(SECURITY_DIR, (node, split, value) -> node.setSecurityDir(Paths.get(value)));
      paramActionMap.put(SECURITY_AUDIT_LOG_DIR, (node, split, value) -> node.setSecurityAuditLogDir(Paths.get(value)));
      paramActionMap.put(SECURITY_AUTHC, (node, split, value) -> node.setSecurityAuthc(value));
      paramActionMap.put(SECURITY_SSL_TLS, (node, split, value) -> node.setSecuritySslTls(Boolean.valueOf(value)));
      paramActionMap.put(SECURITY_WHITELIST, (node, split, value) -> node.setSecurityWhitelist(Boolean.valueOf(value)));
      paramActionMap.put(FAILOVER_PRIORITY, (node, split, value) -> node.setFailoverPriority(value));
      paramActionMap.put(CLIENT_RECONNECT_WINDOW, (node, split, value) -> node.setClientReconnectWindow(value));
      paramActionMap.put(CLIENT_LEASE_DURATION, (node, split, value) -> node.setClientLeaseDuration(value));
      paramActionMap.put(OFFHEAP_RESOURCES, (node, split, value) -> node.setOffheapResource(split[4], value));
      paramActionMap.put(DATA_DIRS, (node, split, value) -> node.setDataDir(split[4], Paths.get(value)));
      paramActionMap.put(CLUSTER_NAME, (node, split, value) -> node.setClusterName(value));
      return paramActionMap;
    }

    static void set(Properties properties, Map<String, Node> nodeMap) {
      properties.forEach((key, value) -> {
        // my-cluster.stripe-1.node-1.node-name=node-1
        String[] keys = splitKey(key);
        final Node node = nodeMap.get(keys[2]);
        set(node, keys, value.toString());
      });
    }

    private static void set(Node node, String[] split, String value) {
      String property = split[3];
      PARAM_ACTION_MAP.get(property).accept(node, split, value);
    }

    private static String[] splitKey(Object key) {
      return key.toString().split("\\.");
    }

    private interface TriConsumer<T, U, V> {
      void accept(T t, U u, V v);
    }
  }
}
