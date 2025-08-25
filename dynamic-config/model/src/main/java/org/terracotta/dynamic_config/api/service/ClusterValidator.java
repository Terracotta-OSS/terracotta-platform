/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.Version;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.binarySearch;
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
 * This class expects all the fields to be first validated by {@link Setting#validate(String, String, Scope)}.
 * <p>
 * This class will validate the complete cluster object (inter-field checks and dependency checks).
 * <p>
 * It is meant to be used before an activation process (or when nodes are activated).
 */
public class ClusterValidator {

  // For names, we need to support characters used in domain names (usually used by users)
  // and we can also support some other characters that are not invalid for Unix/Win/mac paths
  // Refs:
  // - https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
  // - https://stackoverflow.com/questions/1976007/what-characters-are-forbidden-in-windows-and-linux-directory-names/31976060#31976060
  private static final char[] FORBIDDEN_CTRL_CHARS = new char[]{'\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007', '\u0008', '\u0009', '\n', '\u000B', '\u000C', '\r', '\u000E', '\u000F', '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017', '\u0018', '\u0019', '\u001A', '\u001B', '\u001C', '\u001D', '\u001E', '\u001F'};
  private static final char[] FORBIDDEN_FILE_CHARS = new char[]{':', '/', '\\', '<', '>', '"', '|', '*', '?'};
  private static final char[] FORBIDDEN_ENDING_CHARS = new char[]{' ', '.'};
  private static final String[] FORBIDDEN_NAMES_NO_EXT = new String[]{"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};
  // special chars in DC
  private static final char[] FORBIDDEN_DC_CHARS = new char[]{' ', ',', ':', '=', '%', '{', '}'};

  static {
    // sorting because using binary search after
    Arrays.sort(FORBIDDEN_CTRL_CHARS);
    Arrays.sort(FORBIDDEN_FILE_CHARS);
    Arrays.sort(FORBIDDEN_DC_CHARS);
    Arrays.sort(FORBIDDEN_ENDING_CHARS);
    Arrays.sort(FORBIDDEN_NAMES_NO_EXT);
  }

  private final Cluster cluster;

  public ClusterValidator(Cluster cluster) {
    this.cluster = cluster;
  }

  public void validate(ClusterState clusterState) throws MalformedClusterException {
    validate(clusterState, Version.CURRENT);
  }

  public void validate(ClusterState clusterState, Version version) throws MalformedClusterException {
    validateNodeNames();
    validateNames(clusterState);
    validateAddresses();
    validateBackupDirs();
    validateDataDirs();
    validateSecurity();
    validateFailoverSetting(clusterState);
    if (version.amongst(EnumSet.of(V2))) {
      validateStripeNames();
      validateUIDs();
    }
  }

  private void validateNames(ClusterState clusterState) {
    Stream.concat(Stream.of(cluster), cluster.descendants())
        .peek(o -> {
          if (clusterState == ClusterState.ACTIVATED && o.getName() == null) {
            throw new MalformedClusterException("Missing " + o.getScope().toString().toLowerCase() + " name");
          }
        })
        .filter(o -> o.getName() != null) // empty names will be validated elsewhere
        .forEach(o -> validateName(o.getName(), o.getScope().toString().toLowerCase()));
  }

  public static void validateName(String name, String scope) {
    if (name.isEmpty()) {
      throw new MalformedClusterException("Empty " + scope.toLowerCase() + " name");
    }
    // invalid chars
    {
      Character invalid = IntStream.range(0, name.length())
          .mapToObj(name::charAt)
          .filter(c -> binarySearch(FORBIDDEN_CTRL_CHARS, c) >= 0 || binarySearch(FORBIDDEN_FILE_CHARS, c) >= 0 || binarySearch(FORBIDDEN_DC_CHARS, c) >= 0)
          .findFirst()
          .orElse(null);
      if (invalid != null) {
        throw new MalformedClusterException("Invalid character in " + scope + " name: '" + invalid + "'");
      }
    }
    // invalid ending characters
    {
      char last = name.charAt(name.length() - 1);
      if (binarySearch(FORBIDDEN_ENDING_CHARS, last) >= 0) {
        throw new MalformedClusterException("Invalid ending character in " + scope + " name: '" + last + "'");
      }
    }
    // invalid filenames
    {
      String noExt = name.lastIndexOf(".") == -1 ? name : name.substring(0, name.lastIndexOf("."));
      if (binarySearch(FORBIDDEN_NAMES_NO_EXT, noExt) >= 0) {
        throw new MalformedClusterException("Invalid name for " + scope + ": '" + noExt + "' is a reserved word");
      }
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
        .filter(node -> !node.getPublicHostPort().isPresent())
        .map(Node::getName)
        .collect(toList());
    if (!nodesWithNoPublicAddresses.isEmpty() && nodesWithNoPublicAddresses.size() != cluster.getNodeCount()) {
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
              + "' of node with name: " + node.getName() + " isn't well-formed. Public hostname and port need to be set (or unset) together");
        });
  }

  private void checkDuplicateInternalAddresses() {
    cluster.getStripes()
        .stream()
        .flatMap(s -> s.getNodes().stream())
        .collect(groupingBy(Node::getInternalHostPort, Collectors.toList()))
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
        .collect(groupingBy(Node::getPublicHostPort, Collectors.toList()))
        .entrySet()
        .stream()
        .filter(e -> e.getKey().isPresent() && e.getValue().size() > 1)
        .findAny()
        .ifPresent(entry -> {
          throw new MalformedClusterException("Nodes with names: " + entry.getValue().stream().map(Node::getName).collect(Collectors.joining(", ")) +
              " have the same public address: '" + entry.getKey().get() + "'");
        });
  }

  private void validateFailoverSetting(ClusterState clusterState) {
    if (clusterState == ClusterState.ACTIVATED && !cluster.getFailoverPriority().isConfigured() && cluster.getNodeCount() > 1) {
      throw new MalformedClusterException(Setting.FAILOVER_PRIORITY + " setting is not configured");
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
    if (!nodesWithBackupDirs.isEmpty() && nodesWithBackupDirs.size() != cluster.getNodeCount()) {
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
    } else {
      if (count > 0) {
        throw new MalformedClusterException("There are no (or will be no) security root directories configured across the cluster." +
            " But nodes: " + nodesWithAuditLogDirs +
            " currently have (or will have) audit log directories defined.  When no security root directories are" +
            " configured " + SECURITY_AUDIT_LOG_DIR + " should also be unconfigured (unset) for all nodes in the cluster.");
      }
    }
  }
}