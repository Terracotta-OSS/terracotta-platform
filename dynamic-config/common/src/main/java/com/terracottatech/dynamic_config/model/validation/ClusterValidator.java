/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.validation;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.exception.MalformedClusterException;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.utilities.Validator;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.model.Setting.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_DIR;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_WHITELIST;

public class ClusterValidator implements Validator {
  private final Cluster cluster;
  private final IParameterSubstitutor parameterSubstitutor;

  public ClusterValidator(Cluster cluster, IParameterSubstitutor parameterSubstitutor) {
    this.cluster = cluster;
    this.parameterSubstitutor = parameterSubstitutor;
  }

  @Override
  public void validate() throws MalformedClusterException {
    validateNodeName();
    validateSecurity();
    validateClientSettings();
    validateServerSettings();
    validateSecurityDir();
  }

  private void validateNodeName() {
    cluster.nodeContexts().forEach(nodeContext -> {
      Node node = nodeContext.getNode();
      if (node.getNodeName() != null && parameterSubstitutor.containsSubstitutionParams(node.getNodeName())) {
        throw new IllegalArgumentException("Node " + nodeContext.getNodeId() + " of stripe " + nodeContext.getStripeId() + " is invalid: <node-name> cannot contain substitution parameters");
      }
    });
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

  private void validateSecurityDir() {
    cluster.nodeContexts().forEach(nodeContext -> {
      Node node = nodeContext.getNode();
      if ("certificate".equals(node.getSecurityAuthc()) && !node.isSecuritySslTls()) {
        throw new IllegalArgumentException("Node " + nodeContext.getNodeId() + " of stripe " + nodeContext.getStripeId() + " is invalid: " +
            SECURITY_SSL_TLS + " is required for " + SECURITY_AUTHC + "=certificate");
      }
      if ((node.getSecurityAuthc() != null && node.getSecurityDir() == null)
          || (node.getSecurityAuditLogDir() != null && node.getSecurityDir() == null)
          || (node.isSecuritySslTls() && node.getSecurityDir() == null)
          || (node.isSecurityWhitelist() && node.getSecurityDir() == null)) {
        throw new IllegalArgumentException("Node " + nodeContext.getNodeId() + " of stripe " + nodeContext.getStripeId() + " is invalid: " +
            SECURITY_DIR + " is mandatory for any of the security configuration");
      }
      if (node.getSecurityDir() != null && !node.isSecuritySslTls() && node.getSecurityAuthc() == null && !node.isSecurityWhitelist()) {
        throw new IllegalArgumentException("Node " + nodeContext.getNodeId() + " of stripe " + nodeContext.getStripeId() + " is invalid: " +
            "One of " + SECURITY_SSL_TLS + ", " + SECURITY_AUTHC + ", or " + SECURITY_WHITELIST + " is required for security configuration");
      }
    });
  }

  private void validate(Function<? super Node, Object> function, String errorMsg) {
    Collection<Object> settings = cluster.getNodes().stream()
        .map(function)
        .collect(Collectors.toSet());
    if (settings.size() != 1) {
      throw new MalformedClusterException(errorMsg + ", but found the following mismatches: " + settings);
    }
  }
}
