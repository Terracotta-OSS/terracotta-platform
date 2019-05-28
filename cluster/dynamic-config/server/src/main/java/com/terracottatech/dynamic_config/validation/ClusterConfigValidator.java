/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.validation;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.exception.MalformedClusterConfigException;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClusterConfigValidator {
  public static void validate(Cluster cluster) {
    validateSecurity(cluster);
    validateClientSettings(cluster);
    validateServerSettings(cluster);
  }

  private static void validateSecurity(Cluster cluster) {
    validate(cluster, Node::getSecurityAuthc, "Authentication setting of all nodes should match");
    validate(cluster, Node::isSecuritySslTls, "SSL/TLS setting of all nodes should match");
    validate(cluster, Node::isSecurityWhitelist, "Whitelist setting of all nodes should match");
  }

  private static void validateClientSettings(Cluster cluster) {
    validate(cluster, Node::getClientLeaseDuration, "Client lease duration of all nodes should match");
    validate(cluster, Node::getClientReconnectWindow, "Client reconnect window of all nodes should match");
  }

  private static void validateServerSettings(Cluster cluster) {
    validate(cluster, Node::getOffheapResources, "Offheap resources of all nodes should match");
    validate(cluster, node -> node.getDataDirs().keySet(), "Data directory names of all nodes should match");
    validate(cluster, Node::getClusterName, "All the nodes should belong to the same cluster");
    validate(cluster, Node::getFailoverPriority, "Failover setting of all nodes should match");
  }

  private static void validate(Cluster cluster, Function<? super Node, Object> function, String errorMsg) {
    Set<Object> settings = cluster.getStripes().stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .map(function)
        .collect(Collectors.toSet());
    if (settings.size() != 1) {
      throw new MalformedClusterConfigException(errorMsg + ", but found the following mismatches: " + settings);
    }
  }
}
