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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterValidator.class);
  private final Cluster cluster;

  public ClusterValidator(Cluster cluster) {
    this.cluster = cluster;
  }

  public void validate() throws MalformedClusterException {
    validateNodeNames();
    validateStripeNames();
    validateAddresses();
    validateBackupDirs();
    validateDataDirs();
    validateSecurity();
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
        .filter(node -> !node.getPublicAddress().isPresent())
        .map(Node::getName)
        .collect(toList());
    if (nodesWithNoPublicAddresses.size() != 0 && nodesWithNoPublicAddresses.size() != cluster.getNodeCount()) {
      throw new MalformedClusterException("Nodes with names: " + nodesWithNoPublicAddresses +
          " don't have public addresses " + "defined, but other nodes in the cluster do." +
          " Mutative operations on public addresses must be done simultaneously on every node in the cluster");
    }
  }

  private void checkPublicAddressContent() {
    cluster.getStripes()
        .stream()
        .flatMap(s -> s.getNodes().stream())
        .filter(node -> (node.getPublicHostname().isConfigured() && !node.getPublicPort().isConfigured()) || (!node.getPublicHostname().isConfigured() && node.getPublicPort().isConfigured()))
        .findFirst()
        .ifPresent(node -> {
          throw new MalformedClusterException("Public address: '" + (node.getPublicHostname().orDefault() + ":" + node.getPublicPort().orDefault())
              + "' of node with name: " + node.getName() + " isn't well-formed. Public hostname and port need to be set together");
        });
  }

  private void checkDuplicateInternalAddresses() {
    cluster.getStripes()
        .stream()
        .flatMap(s -> s.getNodes().stream())
        .collect(groupingBy(Node::getInternalAddress, Collectors.toList()))
        .entrySet()
        .stream()
        .filter(e -> e.getValue().size() > 1)
        .findAny()
        .ifPresent(entry -> {
          throw new MalformedClusterException("Nodes with names: " + entry.getValue().stream().map(Node::getName).collect(Collectors.joining(", ")) +
              " have the same address: '" + entry.getKey() + "'");
        });
  }

  private void checkDuplicatePublicAddresses() {
    cluster.getStripes()
        .stream()
        .flatMap(s -> s.getNodes().stream())
        .collect(groupingBy(Node::getPublicAddress, Collectors.toList()))
        .entrySet()
        .stream()
        .filter(e -> e.getKey().isPresent() && e.getValue().size() > 1)
        .findAny()
        .ifPresent(entry -> {
          throw new MalformedClusterException("Nodes with names: " + entry.getValue().stream().map(Node::getName).collect(Collectors.joining(", ")) +
              " have the same public address: '" + entry.getKey().get() + "'");
        });
  }

  private void validateFailoverSetting() {
    FailoverPriority failoverPriority = cluster.getFailoverPriority();
    if (failoverPriority == null) {
      throw new MalformedClusterException(Setting.FAILOVER_PRIORITY + " setting is missing");
    }

    if (failoverPriority.equals(consistency())) {
      int voterCount = failoverPriority.getVoters();
      int nodeCount = cluster.getNodes().size();
      int sum = voterCount + nodeCount;
      if (sum % 2 == 0) {
        LOGGER.warn(lineSeparator() +
            "========================================================================================" + lineSeparator() +
            "The sum (" + sum + ") of voter count (" + voterCount + ") and number of nodes (" + nodeCount + ") " +
            "in this stripe is an even number." + lineSeparator() +
            "An even-numbered configuration is more likely to experience split-brain situations." + lineSeparator() +
            "========================================================================================" + lineSeparator());
      }
    }
  }

  private void validateNodeNames() {
    cluster.getNodes()
        .stream()
        .filter(node -> node.getName() == null)
        .findAny()
        .ifPresent(nodeName -> {
          throw new MalformedClusterException("Found node without name");
        });

    cluster.getNodes()
        .stream()
        .map(Node::getName)
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

  private void validateStripeNames() {
    cluster.getStripes()
        .stream()
        .filter(stripe -> stripe.getName() == null)
        .findAny()
        .ifPresent(stripeName -> {
          throw new MalformedClusterException("Found stripe without name");
        });

    cluster.getStripes()
        .stream()
        .map(Stripe::getName)
        .filter(Objects::nonNull)
        .collect(groupingBy(identity(), counting()))
        .entrySet()
        .stream()
        .filter(e -> e.getValue() > 1)
        .map(Map.Entry::getKey)
        .findAny()
        .ifPresent(stripeName -> {
          throw new MalformedClusterException("Found duplicate stripe name: " + stripeName);
        });
  }

  private void validateDataDirs() {
    Set<Set<String>> uniqueDataDirNames = cluster.getNodes().stream()
        .map(node -> node.getDataDirs().orDefault().keySet())
        .collect(Collectors.toSet());
    if (uniqueDataDirNames.size() > 1) {
      throw new MalformedClusterException("Data directory names need to match across the cluster," +
          " but found the following mismatches: " + uniqueDataDirNames + ". " +
          "Mutative operations on data dirs must be done simultaneously on every node in the cluster");
    }
  }

  private void validateBackupDirs() {
    List<String> nodesWithBackupDirs = cluster.getNodes().stream()
        .filter(node -> node.getBackupDir().isConfigured())
        .map(Node::getName)
        .collect(toList());
    if (nodesWithBackupDirs.size() != 0 && nodesWithBackupDirs.size() != cluster.getNodeCount()) {
      throw new MalformedClusterException("Nodes with names: " + nodesWithBackupDirs +
          " don't have backup directories defined, but other nodes in the cluster do." +
          " Mutative operations on backup dirs must be done simultaneously on every node in the cluster");
    }
  }

  private void validateSecurity() {
    validateAuthc();
    validateSecurityRequirements();
    validateAuditLogDir();
  }

  private void validateAuthc() {
    if (cluster.getSecurityAuthc().is("certificate") && !cluster.getSecuritySslTls().orDefault()) {
      throw new MalformedClusterException(SECURITY_SSL_TLS + " is required for " + SECURITY_AUTHC + "=certificate");
    }
  }

  private void validateSecurityRequirements() {
    for (Node node : cluster.getNodes()) {
      boolean securityDirConfigured = node.getSecurityDir().isConfigured();
      if (!securityDirConfigured) {
        if (cluster.getSecurityAuthc().isConfigured() || node.getSecurityAuditLogDir().isConfigured() ||
            cluster.getSecuritySslTls().orDefault() || cluster.getSecurityWhitelist().orDefault()) {
          throw new MalformedClusterException(SECURITY_DIR + " is mandatory for any of the security configuration, but not found on node with name: " + node.getName());
        }
      }

      if (securityDirConfigured && !cluster.getSecuritySslTls().orDefault() && !cluster.getSecurityAuthc().isConfigured() && !cluster.getSecurityWhitelist().orDefault()) {
        throw new MalformedClusterException("One of " + SECURITY_SSL_TLS + ", " + SECURITY_AUTHC + ", or " + SECURITY_WHITELIST +
            " is required for security configuration, but not found on node with name: " + node.getName());
      }
    }
  }

  private void validateAuditLogDir() {
    List<String> nodesWithNoAuditLogDirs = cluster.getStripes()
        .stream()
        .flatMap(s -> s.getNodes().stream())
        .filter(node -> !node.getSecurityAuditLogDir().isConfigured())
        .map(Node::getName)
        .collect(toList());
    if (nodesWithNoAuditLogDirs.size() != 0 && nodesWithNoAuditLogDirs.size() != cluster.getNodeCount()) {
      throw new MalformedClusterException("Nodes with names: " + nodesWithNoAuditLogDirs +
          " don't have audit log directories defined, but other nodes in the cluster do." +
          " Mutative operations on audit log dirs must be done simultaneously on every node in the cluster");
    }
  }
}
