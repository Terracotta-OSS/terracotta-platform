/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUTHC;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_SSL_TLS;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_WHITELIST;

/**
 * This class expects all the fields to be first validated by {@link Setting#validate(String)}.
 * <p>
 * This class will validate the complete cluster object (inter-field checks and dependency checks).
 */
public class ClusterValidator {
  private final Cluster cluster;

  public ClusterValidator(Cluster cluster) {
    this.cluster = cluster;
  }

  public void validate() throws MalformedClusterException {
    validateNodeName();
    validateAddresses();
    validateDataDirs();
    validateSecurityDir();
    validateFailoverSetting();
  }

  private void validateAddresses() {
    checkDuplicateInternalAddresses();
    checkPublicAddressContent();
    checkDuplicatePublicAddresses();
    checkAllOrNoPublicAddresses();
  }

  private void checkAllOrNoPublicAddresses() {
    List<String> nodesWithNoPublicAddresses = cluster.getStripes()
        .stream()
        .flatMap(s -> s.getNodes().stream())
        .filter(node -> !node.getNodePublicAddress().isPresent())
        .map(Node::getNodeName)
        .collect(toList());
    if (nodesWithNoPublicAddresses.size() != 0 && nodesWithNoPublicAddresses.size() != cluster.getNodeCount()) {
      throw new MalformedClusterException("Nodes with names: " + nodesWithNoPublicAddresses + " don't have public addresses " +
          "defined, but other nodes in the cluster do. Public addresses, if configured, need to be defined on all nodes in the cluster");
    }
  }

  private void checkPublicAddressContent() {
    cluster.getStripes()
        .stream()
        .flatMap(s -> s.getNodes().stream())
        .filter(node -> (node.getNodePublicHostname() != null && node.getNodePublicPort() == null) || (node.getNodePublicHostname() == null && node.getNodePublicPort() != null))
        .findFirst()
        .ifPresent(node -> {
          throw new MalformedClusterException("Public address: '" + (node.getNodePublicHostname() + ":" + node.getNodePublicPort())
              + "' of node with name: " + node.getNodeName() + " isn't well-formed. Public hostname and port need to be set together");
        });
  }

  private void checkDuplicateInternalAddresses() {
    cluster.getStripes()
        .stream()
        .flatMap(s -> s.getNodes().stream())
        .collect(groupingBy(Node::getNodeInternalAddress, Collectors.toList()))
        .entrySet()
        .stream()
        .filter(e -> e.getValue().size() > 1)
        .findAny()
        .ifPresent(entry -> {
          throw new MalformedClusterException("Nodes with names: " + entry.getValue().stream().map(Node::getNodeName).collect(Collectors.joining(", ")) +
              " have the same address: '" + entry.getKey() + "'");
        });
  }

  private void checkDuplicatePublicAddresses() {
    cluster.getStripes()
        .stream()
        .flatMap(s -> s.getNodes().stream())
        .collect(groupingBy(Node::getNodePublicAddress, Collectors.toList()))
        .entrySet()
        .stream()
        .filter(e -> e.getKey().isPresent() && e.getValue().size() > 1)
        .findAny()
        .ifPresent(entry -> {
          throw new MalformedClusterException("Nodes with names: " + entry.getValue().stream().map(Node::getNodeName).collect(Collectors.joining(", ")) +
              " have the same public address: '" + entry.getKey().get() + "'");
        });
  }

  private void validateFailoverSetting() {
    if (cluster.getFailoverPriority() == null) {
      throw new MalformedClusterException(Setting.FAILOVER_PRIORITY + " setting is missing");
    }
  }

  private void validateNodeName() {
    cluster.getNodes()
        .stream()
        .map(Node::getNodeName)
        .filter(Objects::nonNull)
        .collect(groupingBy(identity(), counting()))
        .entrySet()
        .stream()
        .filter(e -> e.getValue() > 1)
        .map(Map.Entry::getKey)
        .findAny()
        .ifPresent(nodeName -> {
          throw new MalformedClusterException("Found duplicate node name: " + nodeName);
        });
  }

  private void validateDataDirs() {
    Set<Set<String>> uniqueDataDirNames = cluster.getNodes().stream()
        .map(node -> node.getDataDirs().keySet())
        .collect(Collectors.toSet());
    if (uniqueDataDirNames.size() > 1) {
      throw new MalformedClusterException("Data directory names need to match across the cluster," +
          " but found the following mismatches: " + uniqueDataDirNames + ". " +
          "Mutative operations on data dirs must be done simultaneously on every node in cluster");
    }
  }

  private void validateSecurityDir() {
    if ("certificate".equals(cluster.getSecurityAuthc()) && !cluster.isSecuritySslTls()) {
      throw new MalformedClusterException(SECURITY_SSL_TLS + " is required for " + SECURITY_AUTHC + "=certificate");
    }
    cluster.nodeContexts().forEach(nodeContext -> {
      Node node = nodeContext.getNode();
      if ((cluster.getSecurityAuthc() != null && node.getSecurityDir() == null)
          || (node.getSecurityAuditLogDir() != null && node.getSecurityDir() == null)
          || (cluster.isSecuritySslTls() && node.getSecurityDir() == null)
          || (cluster.isSecurityWhitelist() && node.getSecurityDir() == null)) {
        throw new MalformedClusterException(SECURITY_DIR + " is mandatory for any of the security configuration");
      }
      if (node.getSecurityDir() != null && !cluster.isSecuritySslTls() && cluster.getSecurityAuthc() == null && !cluster.isSecurityWhitelist()) {
        throw new MalformedClusterException("One of " + SECURITY_SSL_TLS + ", " + SECURITY_AUTHC + ", or " + SECURITY_WHITELIST + " is required for security configuration");
      }
    });
  }
}
