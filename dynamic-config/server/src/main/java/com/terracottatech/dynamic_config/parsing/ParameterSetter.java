/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.config.CommonOptions;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.terracottatech.dynamic_config.DynamicConfigConstants.MULTI_VALUE_SEP;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.PARAM_INTERNAL_SEP;

/**
 * Sets pre-validated parameters to their corresponding values in {@code Node} object.
 */
class ParameterSetter {
  private static final Map<String, BiConsumer<Cluster, String>> PARAM_ACTION_MAP = new HashMap<>();

  static {
    PARAM_ACTION_MAP.put(CommonOptions.NODE_NAME, toNode(Node::setNodeName));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_HOSTNAME, toNode(Node::setNodeHostname));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_PORT, toNode((node, value) -> node.setNodePort(Integer.parseInt(value))));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_GROUP_PORT, toNode((node, value) -> node.setNodeGroupPort(Integer.parseInt(value))));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_BIND_ADDRESS, toNode(Node::setNodeBindAddress));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_GROUP_BIND_ADDRESS, toNode(Node::setNodeGroupBindAddress));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_CONFIG_DIR, toNode((node, value) -> node.setNodeConfigDir(Paths.get(value))));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_METADATA_DIR, toNode((node, value) -> node.setNodeMetadataDir(Paths.get(value))));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_LOG_DIR, toNode((node, value) -> node.setNodeLogDir(Paths.get(value))));
    PARAM_ACTION_MAP.put(CommonOptions.NODE_BACKUP_DIR, toNode((node, value) -> node.setNodeBackupDir(Paths.get(value))));
    PARAM_ACTION_MAP.put(CommonOptions.SECURITY_DIR, toNode((node, value) -> node.setSecurityDir(Paths.get(value))));
    PARAM_ACTION_MAP.put(CommonOptions.SECURITY_AUDIT_LOG_DIR, toNode((node, value) -> node.setSecurityAuditLogDir(Paths.get(value))));
    PARAM_ACTION_MAP.put(CommonOptions.SECURITY_AUTHC, toNode(Node::setSecurityAuthc));
    PARAM_ACTION_MAP.put(CommonOptions.SECURITY_SSL_TLS, toNode((node, value) -> node.setSecuritySslTls(Boolean.valueOf(value))));
    PARAM_ACTION_MAP.put(CommonOptions.SECURITY_WHITELIST, toNode((node, value) -> node.setSecurityWhitelist(Boolean.valueOf(value))));
    PARAM_ACTION_MAP.put(CommonOptions.FAILOVER_PRIORITY, toNode(Node::setFailoverPriority));
    PARAM_ACTION_MAP.put(CommonOptions.CLIENT_RECONNECT_WINDOW, toNode((node, clientReconnectWindow) -> node.setClientReconnectWindow(Measure.parse(clientReconnectWindow, TimeUnit.class))));
    PARAM_ACTION_MAP.put(CommonOptions.CLIENT_LEASE_DURATION, toNode((node, clientLeaseDuration) -> node.setClientLeaseDuration(Measure.parse(clientLeaseDuration, TimeUnit.class))));
    PARAM_ACTION_MAP.put(CommonOptions.CLUSTER_NAME, Cluster::setName);
    PARAM_ACTION_MAP.put(CommonOptions.OFFHEAP_RESOURCES, toNode((node, value) -> Arrays.asList(value.split(MULTI_VALUE_SEP)).forEach(ofr -> {
      String[] split = ofr.split(PARAM_INTERNAL_SEP);
      node.setOffheapResource(split[0], Measure.parse(split[1], MemoryUnit.class));
    })));
    PARAM_ACTION_MAP.put(CommonOptions.DATA_DIRS, toNode((node, value) -> Arrays.asList(value.split(MULTI_VALUE_SEP)).forEach(dir -> {
      int firstColon = dir.indexOf(PARAM_INTERNAL_SEP);
      node.setDataDir(dir.substring(0, firstColon), Paths.get(dir.substring(firstColon + 1)));
    })));
  }

  static void set(String param, String value, Cluster cluster) {
    BiConsumer<Cluster, String> action = PARAM_ACTION_MAP.get(param);
    if (action == null) {
      throw new AssertionError("Unrecognized param: " + param);
    }
    action.accept(cluster, value);
  }

  private static BiConsumer<Cluster, String> toNode(BiConsumer<Node, String> consumer) {
    return (cluster, s) -> cluster.getSingleNode().ifPresent(node -> consumer.accept(node, s));
  }

}
