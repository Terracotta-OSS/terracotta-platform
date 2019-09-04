/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.config;

import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
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
import static com.terracottatech.dynamic_config.model.config.CommonOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_NAME;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_WHITELIST;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.TC_PROPERTIES;

/**
 * Sets pre-validated parameters to their corresponding values in {@code Node} object.
 */
public class NodeParameterSetter {
  private final Map<String, BiConsumer<Node, String>> PARAM_ACTION_MAP = new HashMap<>();

  private void buildMap() {
    PARAM_ACTION_MAP.put(NODE_NAME, Node::setNodeName);
    PARAM_ACTION_MAP.put(NODE_HOSTNAME, (node1, nodeHostname) -> node1.setNodeHostname(parameterSubstitutor.substitute(nodeHostname)));
    PARAM_ACTION_MAP.put(NODE_PORT, (node, value) -> node.setNodePort(Integer.parseInt(value)));
    PARAM_ACTION_MAP.put(NODE_GROUP_PORT, (node, value) -> node.setNodeGroupPort(Integer.parseInt(value)));
    PARAM_ACTION_MAP.put(NODE_BIND_ADDRESS, Node::setNodeBindAddress);
    PARAM_ACTION_MAP.put(NODE_GROUP_BIND_ADDRESS, Node::setNodeGroupBindAddress);
    PARAM_ACTION_MAP.put(NODE_METADATA_DIR, (node, value) -> node.setNodeMetadataDir(Paths.get(value)));
    PARAM_ACTION_MAP.put(NODE_LOG_DIR, (node, value) -> node.setNodeLogDir(Paths.get(value)));
    PARAM_ACTION_MAP.put(NODE_BACKUP_DIR, (node, value) -> node.setNodeBackupDir(Paths.get(value)));
    PARAM_ACTION_MAP.put(SECURITY_DIR, (node, value) -> node.setSecurityDir(Paths.get(value)));
    PARAM_ACTION_MAP.put(SECURITY_AUDIT_LOG_DIR, (node, value) -> node.setSecurityAuditLogDir(Paths.get(value)));
    PARAM_ACTION_MAP.put(SECURITY_AUTHC, Node::setSecurityAuthc);
    PARAM_ACTION_MAP.put(SECURITY_SSL_TLS, (node, value) -> node.setSecuritySslTls(Boolean.parseBoolean(value)));
    PARAM_ACTION_MAP.put(SECURITY_WHITELIST, (node, value) -> node.setSecurityWhitelist(Boolean.parseBoolean(value)));
    PARAM_ACTION_MAP.put(FAILOVER_PRIORITY, Node::setFailoverPriority);
    PARAM_ACTION_MAP.put(CLIENT_RECONNECT_WINDOW, (node, clientReconnectWindow) -> {
      Measure<TimeUnit> measure = Measure.parse(clientReconnectWindow, TimeUnit.class);
      node.setClientReconnectWindow(measure);
    });
    PARAM_ACTION_MAP.put(CLIENT_LEASE_DURATION, (node, clientLeaseDuration) -> {
      Measure<TimeUnit> measure = Measure.parse(clientLeaseDuration, TimeUnit.class);
      node.setClientLeaseDuration(measure);
    });
    PARAM_ACTION_MAP.put(OFFHEAP_RESOURCES, (node, value) -> Arrays.asList(value.split(MULTI_VALUE_SEP)).forEach(ofr -> {
      String[] split = ofr.split(PARAM_INTERNAL_SEP);
      node.setOffheapResource(split[0], Measure.parse(split[1], MemoryUnit.class));
    }));
    PARAM_ACTION_MAP.put(TC_PROPERTIES, (node, value) -> Arrays.asList(value.split(MULTI_VALUE_SEP)).forEach(ofr -> {
      String[] split = ofr.split(PARAM_INTERNAL_SEP);
      node.setTcProperty(split[0], split[1]);
    }));
    PARAM_ACTION_MAP.put(DATA_DIRS, (node, value) -> Arrays.asList(value.split(MULTI_VALUE_SEP)).forEach(dir -> {
      int firstColon = dir.indexOf(PARAM_INTERNAL_SEP);
      node.setDataDir(dir.substring(0, firstColon), Paths.get(dir.substring(firstColon + 1)));
    }));
  }

  private final Node node;
  private final IParameterSubstitutor parameterSubstitutor;

  public NodeParameterSetter(Node node, IParameterSubstitutor parameterSubstitutor) {
    this.node = node;
    this.parameterSubstitutor = parameterSubstitutor;
    buildMap();
  }

  public void set(String param, String value) {
    BiConsumer<Node, String> action = PARAM_ACTION_MAP.get(param);
    if (action == null) {
      throw new AssertionError("Unrecognized param: " + param);
    }
    action.accept(node, value);
  }
}
