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
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.Version;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUDIT_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUTHC;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_SSL_TLS;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_WHITELIST;
import static org.terracotta.dynamic_config.api.model.Version.V2;

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
    validate(Version.CURRENT);
  }

  public void validate(Version version) throws MalformedClusterException {
    validateNodeNames();
    validateAddresses();
    validateBackupDirs();
    validateDataDirs();
    validateSecurity();
    validateFailoverSetting();
    if (version.amongst(EnumSet.of(V2))) {
      validateStripeNames();
      validateUIDs();
    }
  }

  private void validateUIDs() {
    Map<UID, String> discovered = new HashMap<>();
    if (cluster.getUID() == null) {
      throw new MalformedClusterException("Missing UID on cluster");
    }
    discovered.put(cluster.getUID(), "cluster");
    for (Stripe stripe : cluster.getStripes()) {
      String label = "stripe: " + stripe.getName();
      if (stripe.getUID() == null) {
        throw new MalformedClusterException("Missing UID on " + label);
      }
      String prev = discovered.put(stripe.getUID(), label);
      if (prev != null) {
        throw new MalformedClusterException("Duplicate UID for " + label + ". UID: " + stripe.getUID() + " was used on " + prev);
      }
      for (Node node : stripe.getNodes()) {
        label = "node: " + node.getName() + " in stripe: " + stripe.getName();
        if (node.getUID() == null) {
          throw new MalformedClusterException("Missing UID on " + label);
        }
        prev = discovered.put(node.getUID(), label);
        if (prev != null) {
          throw new MalformedClusterException("Duplicate UID for " + label + ". UID: " + node.getUID() + " was used on " + prev);
        }
      }
    }
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
      throw new MalformedClusterException("Nodes: " + nodesWithBackupDirs +
          " currently have (or will have) backup directories defined, while some nodes in the cluster do not (or will not)." +
          " Within a cluster, all nodes must have a backup directory defined or no backup directory defined.");
    }
  }

  private void validateSecurity() {
    boolean securityDirIsConfigured = validateSecurityDirs();
    validateSecurityRequirements(securityDirIsConfigured);
    validateAuditLogDir(securityDirIsConfigured);
  }

  private boolean validateSecurityDirs() {
    // 'security-dir' is an 'all-or-none' node configuration.
    // Check that all nodes have/do not have a security root directory configured
    List<String> nodesWithSecurityRootDirs = cluster.getNodes().stream()
        .filter(node -> node.getSecurityDir().isConfigured())
        .map(Node::getName)
        .collect(toList());
    int count = nodesWithSecurityRootDirs.size();
    if (count > 0 && count != cluster.getNodeCount()) {
      throw new MalformedClusterException("Nodes: " + nodesWithSecurityRootDirs +
          " currently have (or will have) security root directories defined, while some nodes in the cluster do not (or will not)." +
          " Within a cluster, all nodes must have a security root directory defined or no security root directory defined.");
    }
    return count > 0; // security-dir is or is not configured
  }

  private void validateSecurityRequirements(boolean securityDirIsConfigured) {

    boolean minimumRequired = cluster.getSecurityAuthc().isConfigured() ||
            cluster.getSecuritySslTls().orDefault() ||
            cluster.getSecurityWhitelist().orDefault();
    if (securityDirIsConfigured) {
      if (!minimumRequired) {
        throw new MalformedClusterException("When security root directories are configured across the cluster" +
            " at least one of " + SECURITY_AUTHC + ", " + SECURITY_SSL_TLS + " or " + SECURITY_WHITELIST +
            " must also be configured.");
      }
      if (cluster.getSecurityAuthc().is("certificate") && !cluster.getSecuritySslTls().orDefault()) {
        throw new MalformedClusterException("When " + SECURITY_AUTHC + "=certificate " + SECURITY_SSL_TLS + " must be configured.");
      }
    } else if (minimumRequired) {
      throw new MalformedClusterException("There are no (or will be no) security root directories configured across the cluster." +
          " But " + SECURITY_AUTHC + ", " + SECURITY_SSL_TLS + ", and/or " + SECURITY_WHITELIST +
          " is (or will be) configured.  When no security root directories are configured" +
          " all other security settings should also be unconfigured (unset).");
    }
  }

  private void validateAuditLogDir(boolean securityDirIsConfigured) {
    // 'audit-log-dir' is an 'all-or-none' node configuration.
    // Check that all nodes have/do not have an audit log directory configured
    List<String> nodesWithAuditLogDirs = cluster.getNodes().stream()
        .filter(node -> node.getSecurityAuditLogDir().isConfigured())
        .map(Node::getName)
        .collect(toList());
    int count = nodesWithAuditLogDirs.size();
    if (securityDirIsConfigured) {
      if (count > 0 && count != cluster.getNodeCount()) {
        throw new MalformedClusterException("Nodes: " + nodesWithAuditLogDirs +
            " currently have (or will have) audit log directories defined, while some nodes in the cluster do not (or will not)." +
            " Within a cluster, all nodes must have an audit log directory defined or no audit log directory defined.");
      }
    }
    else {
      if (count > 0) {
        throw new MalformedClusterException("There are no (or will be no) security root directories configured across the cluster." +
            " But nodes: " + nodesWithAuditLogDirs +
            " currently have (or will have) audit log directories defined.  When no security root directories are" +
            " configured "  + SECURITY_AUDIT_LOG_DIR + " should also be unconfigured (unset) for all nodes in the cluster.");
      }
    }
  }
}