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
package org.terracotta.dynamic_config.api.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.inet.InetSocketAddressUtils;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static org.terracotta.dynamic_config.api.model.Scope.NODE;

public class Node implements Cloneable, PropertyHolder {

  // Note: primitive fields need to be initialized with their default value,
  // otherwise we will wrongly detect that they have been initialized (Setting.getPropertyValue will return 0 for a port for example)

  private String nodeName;
  private String nodeHostname;
  private String nodePublicHostname;
  private int nodePort = Integer.parseInt(Setting.NODE_PORT.getDefaultValue());
  private Integer nodePublicPort;
  private int nodeGroupPort = Integer.parseInt(Setting.NODE_GROUP_PORT.getDefaultValue());
  private String nodeBindAddress;
  private String nodeGroupBindAddress;
  private Path nodeMetadataDir;
  private Path nodeLogDir;
  private Path nodeBackupDir;
  private Path securityDir;
  private Path securityAuditLogDir;
  private final Map<String, String> tcProperties = new ConcurrentHashMap<>();
  private final Map<String, String> nodeLoggerOverrides = new ConcurrentHashMap<>();
  private final Map<String, Path> dataDirs = new ConcurrentHashMap<>();

  protected Node() {
  }

  @Override
  public Scope getScope() {
    return NODE;
  }

  public String getNodeName() {
    return nodeName;
  }

  public String getNodeHostname() {
    return nodeHostname;
  }

  public String getNodePublicHostname() {
    return nodePublicHostname;
  }

  public int getNodePort() {
    return nodePort;
  }

  public Integer getNodePublicPort() {
    return nodePublicPort;
  }

  public int getNodeGroupPort() {
    return nodeGroupPort;
  }

  public String getNodeBindAddress() {
    return nodeBindAddress;
  }

  public String getNodeGroupBindAddress() {
    return nodeGroupBindAddress;
  }

  public Path getNodeMetadataDir() {
    return nodeMetadataDir;
  }

  public Path getNodeLogDir() {
    return nodeLogDir;
  }

  public Path getNodeBackupDir() {
    return nodeBackupDir;
  }

  public Path getSecurityDir() {
    return securityDir;
  }

  public Path getSecurityAuditLogDir() {
    return securityAuditLogDir;
  }

  public Map<String, Path> getDataDirs() {
    return Collections.unmodifiableMap(dataDirs);
  }

  public Map<String, String> getNodeLoggerOverrides() {
    return Collections.unmodifiableMap(nodeLoggerOverrides);
  }

  public Node setNodeLoggerOverrides(Map<String, String> nodeLoggerOverrides) {
    this.nodeLoggerOverrides.putAll(nodeLoggerOverrides);
    return this;
  }

  public Node setNodeLoggerOverride(String logger, String level) {
    this.nodeLoggerOverrides.put(logger, level);
    return this;
  }

  public Node removeNodeLoggerOverride(String logger) {
    this.nodeLoggerOverrides.remove(logger);
    return this;
  }

  public Node clearNodeLoggerOverrides() {
    nodeLoggerOverrides.clear();
    return this;
  }

  public Map<String, String> getTcProperties() {
    return tcProperties;
  }

  public Node setTcProperties(Map<String, String> tcProperties) {
    this.tcProperties.putAll(tcProperties);
    return this;
  }

  public Node setTcProperty(String key, String value) {
    this.tcProperties.put(key, value);
    return this;
  }

  public Node removeTcProperty(String key) {
    this.tcProperties.remove(key);
    return this;
  }

  public Node setNodeName(String nodeName) {
    this.nodeName = nodeName;
    return this;
  }

  public Node setNodeHostname(String nodeHostname) {
    this.nodeHostname = nodeHostname;
    return this;
  }

  public Node setNodePublicHostname(String nodePublicHostname) {
    this.nodePublicHostname = nodePublicHostname;
    return this;
  }

  public Node setNodePort(int nodePort) {
    this.nodePort = nodePort;
    return this;
  }

  public Node setNodePublicPort(Integer nodePublicPort) {
    this.nodePublicPort = nodePublicPort;
    return this;
  }

  public Node setNodeGroupPort(int nodeGroupPort) {
    this.nodeGroupPort = nodeGroupPort;
    return this;
  }

  public Node setNodeBindAddress(String nodeBindAddress) {
    this.nodeBindAddress = nodeBindAddress;
    return this;
  }

  public Node setNodeGroupBindAddress(String nodeGroupBindAddress) {
    this.nodeGroupBindAddress = nodeGroupBindAddress;
    return this;
  }

  public Node setNodeMetadataDir(Path nodeMetadataDir) {
    this.nodeMetadataDir = nodeMetadataDir;
    return this;
  }

  public Node setNodeLogDir(Path nodeLogDir) {
    this.nodeLogDir = nodeLogDir;
    return this;
  }

  public Node setNodeBackupDir(Path nodeBackupDir) {
    this.nodeBackupDir = nodeBackupDir;
    return this;
  }

  public Node setSecurityDir(Path securityDir) {
    this.securityDir = securityDir;
    return this;
  }

  public Node setSecurityAuditLogDir(Path securityAuditLogDir) {
    this.securityAuditLogDir = securityAuditLogDir;
    return this;
  }

  public Node clearTcProperties() {
    this.tcProperties.clear();
    return this;
  }

  public Node clearDataDirs() {
    this.dataDirs.clear();
    return this;
  }

  public Node setDataDir(String name, Path path) {
    this.dataDirs.put(name, path);
    return this;
  }

  public Node setDataDirs(Map<String, Path> dataDirs) {
    this.dataDirs.putAll(dataDirs);
    return this;
  }

  public Node removeDataDir(String key) {
    dataDirs.remove(key);
    return this;
  }

  /**
   * @return true if this node has this public or internal address
   */
  public boolean hasAddress(InetSocketAddress address) {
    return InetSocketAddressUtils.areEqual(address, getNodeInternalAddress()) ||
        getNodePublicAddress().map(addr -> InetSocketAddressUtils.areEqual(address, addr)).orElse(false);
  }

  public InetSocketAddress getNodeAddress() {
    return getNodePublicAddress().orElseGet(this::getNodeInternalAddress);
  }

  public InetSocketAddress getNodeInternalAddress() {
    if (nodeHostname == null || Substitutor.containsSubstitutionParams(nodeHostname)) {
      throw new AssertionError("Node " + nodeName + " is not correctly defined with internal address: " + nodeHostname + ":" + nodePort);
    }
    return InetSocketAddress.createUnresolved(nodeHostname, nodePort);
  }

  public Optional<InetSocketAddress> getNodePublicAddress() {
    if (nodePublicHostname == null || nodePublicPort == null) {
      return Optional.empty();
    }
    if (Substitutor.containsSubstitutionParams(nodePublicHostname)) {
      throw new AssertionError("Node " + nodeName + " is not correctly defined with public address: " + nodePublicHostname + ":" + nodePublicPort);
    }
    return Optional.of(InetSocketAddress.createUnresolved(nodePublicHostname, nodePublicPort));
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Node clone() {
    return new Node()
        .setDataDirs(dataDirs)
        .setNodeBackupDir(nodeBackupDir)
        .setNodeBindAddress(nodeBindAddress)
        .setNodeGroupBindAddress(nodeGroupBindAddress)
        .setNodeGroupPort(nodeGroupPort)
        .setNodeHostname(nodeHostname)
        .setNodePublicHostname(nodePublicHostname)
        .setNodeLogDir(nodeLogDir)
        .setNodeMetadataDir(nodeMetadataDir)
        .setNodeName(nodeName)
        .setNodePort(nodePort)
        .setNodePublicPort(nodePublicPort)
        .setTcProperties(tcProperties)
        .setNodeLoggerOverrides(nodeLoggerOverrides)
        .setSecurityAuditLogDir(securityAuditLogDir)
        .setSecurityDir(securityDir);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Node node = (Node) o;
    return nodePort == node.nodePort &&
        nodeGroupPort == node.nodeGroupPort &&
        Objects.equals(nodeName, node.nodeName) &&
        Objects.equals(nodeHostname, node.nodeHostname) &&
        Objects.equals(nodePublicHostname, node.nodePublicHostname) &&
        Objects.equals(nodePublicPort, node.nodePublicPort) &&
        Objects.equals(nodeBindAddress, node.nodeBindAddress) &&
        Objects.equals(nodeGroupBindAddress, node.nodeGroupBindAddress) &&
        Objects.equals(nodeMetadataDir, node.nodeMetadataDir) &&
        Objects.equals(nodeLogDir, node.nodeLogDir) &&
        Objects.equals(nodeBackupDir, node.nodeBackupDir) &&
        Objects.equals(nodeLoggerOverrides, node.nodeLoggerOverrides) &&
        Objects.equals(tcProperties, node.tcProperties) &&
        Objects.equals(securityDir, node.securityDir) &&
        Objects.equals(securityAuditLogDir, node.securityAuditLogDir) &&
        Objects.equals(dataDirs, node.dataDirs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeName, nodeHostname, nodePublicHostname, nodePort, nodePublicPort, nodeGroupPort,
        nodeBindAddress, nodeGroupBindAddress, tcProperties, nodeLoggerOverrides, nodeMetadataDir, nodeLogDir, nodeBackupDir,
        securityDir, securityAuditLogDir, dataDirs);
  }

  @Override
  public String toString() {
    return "Node{" +
        "nodeName='" + nodeName + '\'' +
        ", nodeHostname='" + nodeHostname + '\'' +
        ", nodePort=" + nodePort +
        ", nodePublicHostname='" + nodePublicHostname + '\'' +
        ", nodePublicPort=" + nodePublicPort +
        ", nodeGroupPort=" + nodeGroupPort +
        ", nodeBindAddress='" + nodeBindAddress + '\'' +
        ", nodeGroupBindAddress='" + nodeGroupBindAddress + '\'' +
        ", nodeMetadataDir='" + nodeMetadataDir + '\'' +
        ", nodeLogDir='" + nodeLogDir + '\'' +
        ", nodeBackupDir='" + nodeBackupDir + '\'' +
        ", nodeLoggers='" + nodeLoggerOverrides + '\'' +
        ", tcProperties='" + tcProperties + '\'' +
        ", securityDir='" + securityDir + '\'' +
        ", securityAuditLogDir='" + securityAuditLogDir + '\'' +
        ", dataDirs=" + dataDirs +
        '}';
  }

  /**
   * Transform this model into a config file where all the "map" like settings can be expanded (one item per line)
   */
  @Override
  public Properties toProperties(boolean expanded, boolean includeDefaultValues) {
    return Setting.modelToProperties(this, expanded, includeDefaultValues);
  }

  public Node fillRequiredSettings() {
    return Setting.fillRequiredSettings(this);
  }

  private Node fillSettings() {
    return Setting.fillSettings(this);
  }

  public static Node newDefaultNode(String hostname) {
    return new Node()
        .fillSettings()
        .setNodeHostname(hostname);
  }

  public static Node newDefaultNode(String name, String hostname) {
    return new Node()
        .fillSettings()
        .setNodeName(name)
        .setNodeHostname(hostname);
  }

  public static Node newDefaultNode(String hostname, int port) {
    return new Node()
        .fillSettings()
        .setNodePort(port)
        .setNodeHostname(hostname);
  }

  public static Node newDefaultNode(String name, String hostname, int port) {
    return new Node()
        .fillSettings()
        .setNodeName(name)
        .setNodePort(port)
        .setNodeHostname(hostname);
  }

  public static Node empty() {
    return new Node();
  }
}
