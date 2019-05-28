/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.config.Node;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.terracottatech.dynamic_config.Constants.MULTI_VALUE_SEP;
import static com.terracottatech.dynamic_config.Constants.PARAM_INTERNAL_SEP;
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
import static com.terracottatech.dynamic_config.util.CommonParamsUtils.splitQuantityUnit;

/**
 * Sets pre-validated parameters to their corresponding values in {@code Node} object.
 */
class NodeParameterSetter {
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
    PARAM_ACTION_MAP.put(CLIENT_RECONNECT_WINDOW, (node, clientReconnectWindow) -> {
      String[] quantityUnit = splitQuantityUnit(clientReconnectWindow);
      node.setClientReconnectWindow(Long.parseLong(quantityUnit[0]), TimeUnit.from(quantityUnit[1]).get());
    });
    PARAM_ACTION_MAP.put(CLIENT_LEASE_DURATION, (node, clientLeaseDuration) -> {
      String[] quantityUnit = splitQuantityUnit(clientLeaseDuration);
      node.setClientLeaseDuration(Long.parseLong(quantityUnit[0]), TimeUnit.from(quantityUnit[1]).get());
    });
    PARAM_ACTION_MAP.put(CLUSTER_NAME, Node::setClusterName);
    PARAM_ACTION_MAP.put(OFFHEAP_RESOURCES, (node, value) -> Arrays.asList(value.split(MULTI_VALUE_SEP)).forEach(ofr -> {
      String[] split = ofr.split(PARAM_INTERNAL_SEP);
      String[] quantityUnit = splitQuantityUnit(split[1]);
      node.setOffheapResource(split[0], Long.parseLong(quantityUnit[0]), MemoryUnit.valueOf(quantityUnit[1]));
    }));
    PARAM_ACTION_MAP.put(DATA_DIRS, (node, value) -> Arrays.asList(value.split(MULTI_VALUE_SEP)).forEach(dir -> {
      String[] split = dir.split(PARAM_INTERNAL_SEP);
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
