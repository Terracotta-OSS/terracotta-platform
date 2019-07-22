/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.validation;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.exception.MalformedClusterConfigException;
import com.terracottatech.utilities.Validator;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClusterValidator implements Validator {
  private final Cluster cluster;

  public ClusterValidator(Cluster cluster) {
    this.cluster = cluster;
  }

  @Override
  public void validate() throws MalformedClusterConfigException {
    validateSecurity();
    validateClientSettings();
    validateServerSettings();
  }

  private void validateSecurity() {
    validate(Node::getSecurityAuthc, "Authentication setting of all nodes should match");
    validate(Node::isSecuritySslTls, "SSL/TLS setting of all nodes should match");
    validate(Node::isSecurityWhitelist, "Whitelist setting of all nodes should match");
  }

  private void validateClientSettings() {
    validate(Node::getClientLeaseDuration, "Client lease duration of all nodes should match");
    validate(Node::getClientReconnectWindow, "Client reconnect window of all nodes should match");
  }

  private void validateServerSettings() {
    validate(Node::getOffheapResources, "Offheap resources of all nodes should match");
    validate(node -> node.getDataDirs().keySet(), "Data directory names of all nodes should match");
    validate(Node::getFailoverPriority, "Failover setting of all nodes should match");
  }

  private void validate(Function<? super Node, Object> function, String errorMsg) {
    Collection<Object> settings = cluster.getNodes().stream()
        .map(function)
        .collect(Collectors.toSet());
    if (settings.size() != 1) {
      throw new MalformedClusterConfigException(errorMsg + ", but found the following mismatches: " + settings);
    }
  }
}
