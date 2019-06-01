/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.Constants;
import com.terracottatech.dynamic_config.config.CommonOptions;
import com.terracottatech.utilities.Measure;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Sets pre-validated parameters to their corresponding values in {@code Node} object.
 */
class NodeParameterSetter {
  private static final Map<String, BiConsumer<Node, String>> PARAM_ACTION_MAP = new HashMap<>();

  static {
    PARAM_ACTION_MAP.put(CommonOptions.NODE_NAME, Node::setNodeName);
    PARAM_ACTION_MAP.put(CommonOptions.NODE_HOSTNAME, Node::setNodeHostname);
    PARAM_ACTION_MAP.put(CommonOptions.NODE_PORT, (node, value) -> node.setNodePort(Integer.parseInt(value)));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_GROUP_PORT, (node, value) -> node.setNodeGroupPort(Integer.parseInt(value)));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_BIND_ADDRESS, Node::setNodeBindAddress);
    PARAM_ACTION_MAP.put(CommonOptions.NODE_GROUP_BIND_ADDRESS, Node::setNodeGroupBindAddress);
    PARAM_ACTION_MAP.put(CommonOptions.NODE_CONFIG_DIR, (node, value) -> node.setNodeConfigDir(Paths.get(value)));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_METADATA_DIR, (node, value) -> node.setNodeMetadataDir(Paths.get(value)));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_LOG_DIR, (node, value) -> node.setNodeLogDir(Paths.get(value)));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_BACKUP_DIR, (node, value) -> node.setNodeBackupDir(Paths.get(value)));
    PARAM_ACTION_MAP.put(CommonOptions.SECURITY_DIR, (node, value) -> node.setSecurityDir(Paths.get(value)));
    PARAM_ACTION_MAP.put(CommonOptions.SECURITY_AUDIT_LOG_DIR, (node, value) -> node.setSecurityAuditLogDir(Paths.get(value)));
    PARAM_ACTION_MAP.put(CommonOptions.SECURITY_AUTHC, Node::setSecurityAuthc);
    PARAM_ACTION_MAP.put(CommonOptions.SECURITY_SSL_TLS, (node, value) -> node.setSecuritySslTls(Boolean.valueOf(value)));
    PARAM_ACTION_MAP.put(CommonOptions.SECURITY_WHITELIST, (node, value) -> node.setSecurityWhitelist(Boolean.valueOf(value)));
    PARAM_ACTION_MAP.put(CommonOptions.FAILOVER_PRIORITY, Node::setFailoverPriority);
    PARAM_ACTION_MAP.put(CommonOptions.CLIENT_RECONNECT_WINDOW, (node, clientReconnectWindow) -> node.setClientReconnectWindow(Measure.parse(clientReconnectWindow, TimeUnit.class)));
    PARAM_ACTION_MAP.put(CommonOptions.CLIENT_LEASE_DURATION, (node, clientLeaseDuration) -> node.setClientLeaseDuration(Measure.parse(clientLeaseDuration, TimeUnit.class)));
    PARAM_ACTION_MAP.put(CommonOptions.CLUSTER_NAME, Node::setClusterName);
    PARAM_ACTION_MAP.put(CommonOptions.OFFHEAP_RESOURCES, (node, value) -> Arrays.asList(value.split(Constants.MULTI_VALUE_SEP)).forEach(ofr -> {
      String[] split = ofr.split(Constants.PARAM_INTERNAL_SEP);
      node.setOffheapResource(split[0], Measure.parse(split[1], MemoryUnit.class));
    }));
    PARAM_ACTION_MAP.put(CommonOptions.DATA_DIRS, (node, value) -> Arrays.asList(value.split(Constants.MULTI_VALUE_SEP)).forEach(dir -> {
      String[] split = dir.split(Constants.PARAM_INTERNAL_SEP);
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
