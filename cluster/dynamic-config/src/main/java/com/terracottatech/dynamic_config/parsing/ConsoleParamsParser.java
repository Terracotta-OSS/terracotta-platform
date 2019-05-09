/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.config.Cluster;
import com.terracottatech.dynamic_config.config.Node;
import com.terracottatech.dynamic_config.config.Stripe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
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
import static com.terracottatech.dynamic_config.config.Util.addDashDash;
import static com.terracottatech.dynamic_config.config.Util.stripDashDash;


public class ConsoleParamsParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleParamsParser.class);

  public static Cluster parse(Map<String, String> paramValueMap) {
    Node node = new Node();
    paramValueMap.forEach((param, value) -> Setting.set(stripDashDash(param), value, node));
    fillDefaultsIfNeeded(node);
    return createCluster(node);
  }

  private static void fillDefaultsIfNeeded(Node node) {
    List<Object> defaultOptions = new ArrayList<>();
    if (node.getNodeName() == null) {
      defaultOptions.add(NODE_NAME);
      node.setNodeName(getDefaultNodeName());
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
      String[] split = DEFAULT_OFFHEAP_RESOURCE.split(":");
      node.setOffheapResource(split[0], split[1]);
    }

    if (node.getDataDirs().isEmpty()) {
      defaultOptions.add(DATA_DIRS);
      String[] split = DEFAULT_DATA_DIR.split(":");
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

    if (node.getClientReconnectWindow() == null) {
      defaultOptions.add(CLIENT_RECONNECT_WINDOW);
      node.setClientReconnectWindow(DEFAULT_CLIENT_RECONNECT_WINDOW);
    }

    if (node.getClientLeaseDuration() == null) {
      defaultOptions.add(CLIENT_LEASE_DURATION);
      node.setClientLeaseDuration(DEFAULT_CLIENT_LEASE_DURATION);
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

  private static String getDefaultNodeName() {
    UUID uuid = UUID.randomUUID();
    byte[] data = new byte[16];
    long msb = uuid.getMostSignificantBits();
    long lsb = uuid.getLeastSignificantBits();
    for (int i = 0; i < 8; i++) {
      data[i] = (byte) (msb & 0xff);
      msb >>>= 8;
    }
    for (int i = 8; i < 16; i++) {
      data[i] = (byte) (lsb & 0xff);
      lsb >>>= 8;
    }

    return "node-" + DatatypeConverter.printBase64Binary(data)
        // java-8 and other - compatible B64 url decoder use - and _ instead of + and /
        // padding can be ignored to shorten the UUID
        .replace('+', '-')
        .replace('/', '_')
        .replace("=", "");
  }

  private static class Setting {
    private static final Map<String, BiConsumer<Node, String>> PARAM_ACTION_MAP = new HashMap<>();

    static {
      PARAM_ACTION_MAP.put(NODE_NAME, Node::setNodeName);
      PARAM_ACTION_MAP.put(NODE_HOSTNAME, Node::setNodeHostname);
      PARAM_ACTION_MAP.put(NODE_PORT, (node, value) -> node.setNodePort(Integer.parseInt(value)));
      PARAM_ACTION_MAP.put(NODE_GROUP_PORT, (node, value) -> node.setNodeGroupPort(Integer.parseInt(value)));
      PARAM_ACTION_MAP.put(NODE_BIND_ADDRESS, Node::setNodeBindAddress);
      PARAM_ACTION_MAP.put(NODE_GROUP_BIND_ADDRESS, Node::setNodeGroupBindAddress);
      PARAM_ACTION_MAP.put(NODE_CONFIG_DIR, (node, value) -> node.setNodeConfigDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(NODE_METADATA_DIR, (node, value) -> node.setNodeMetadataDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(NODE_LOG_DIR, (node, value) -> node.setNodeLogDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(NODE_BACKUP_DIR, (node, value) -> node.setNodeBackupDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(SECURITY_DIR, (node, value) -> node.setSecurityDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(SECURITY_AUDIT_LOG_DIR, (node, value) -> node.setSecurityAuditLogDir(Paths.get(value)));
      PARAM_ACTION_MAP.put(SECURITY_AUTHC, Node::setSecurityAuthc);
      PARAM_ACTION_MAP.put(SECURITY_SSL_TLS, (node, value) -> node.setSecuritySslTls(Boolean.valueOf(value)));
      PARAM_ACTION_MAP.put(SECURITY_WHITELIST, (node, value) -> node.setSecurityWhitelist(Boolean.valueOf(value)));
      PARAM_ACTION_MAP.put(FAILOVER_PRIORITY, Node::setFailoverPriority);
      PARAM_ACTION_MAP.put(CLIENT_RECONNECT_WINDOW, Node::setClientReconnectWindow);
      PARAM_ACTION_MAP.put(CLIENT_LEASE_DURATION, Node::setClientLeaseDuration);
      PARAM_ACTION_MAP.put(CLUSTER_NAME, Node::setClusterName);
      PARAM_ACTION_MAP.put(OFFHEAP_RESOURCES, (node, value) -> Arrays.asList(value.split(",")).forEach(ofr -> {
        String[] split = ofr.split(":");
        node.setOffheapResource(split[0], split[1]);
      }));
      PARAM_ACTION_MAP.put(DATA_DIRS, (node, value) -> Arrays.asList(value.split(",")).forEach(dir -> {
        String[] split = dir.split(":");
        node.setDataDir(split[0], Paths.get(split[1]));
      }));
    }

    static void set(String param, String value, Node node) {
      BiConsumer<Node, String> action = PARAM_ACTION_MAP.get(param);
      if (action == null) {
        throw new AssertionError("Unrecognized param: " + param);
      }
      action.accept(node, value);
    }
  }
}
