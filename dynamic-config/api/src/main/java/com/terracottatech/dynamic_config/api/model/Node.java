/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terracottatech.common.struct.Measure;
import com.terracottatech.common.struct.MemoryUnit;
import com.terracottatech.common.struct.TimeUnit;
import com.terracottatech.dynamic_config.api.service.IParameterSubstitutor;
import com.terracottatech.inet.InetSocketAddressUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.event.Level;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.api.model.Setting.CLUSTER_NAME;
import static com.terracottatech.dynamic_config.api.model.Setting.LICENSE_FILE;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_REPOSITORY_DIR;
import static java.util.function.Predicate.isEqual;

public class Node implements Cloneable {

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
  private String securityAuthc;
  private boolean securitySslTls = Boolean.parseBoolean(Setting.SECURITY_SSL_TLS.getDefaultValue());
  private boolean securityWhitelist = Boolean.parseBoolean(Setting.SECURITY_WHITELIST.getDefaultValue());
  private FailoverPriority failoverPriority;
  private Map<String, String> tcProperties = new ConcurrentHashMap<>();
  private Map<String, Level> nodeLoggerOverrides = new ConcurrentHashMap<>();
  private Measure<TimeUnit> clientReconnectWindow;
  private Measure<TimeUnit> clientLeaseDuration;
  private Map<String, Measure<MemoryUnit>> offheapResources = new ConcurrentHashMap<>();
  private Map<String, Path> dataDirs = new ConcurrentHashMap<>();

  Node() {
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

  public String getSecurityAuthc() {
    return securityAuthc;
  }

  public boolean isSecuritySslTls() {
    return securitySslTls;
  }

  public boolean isSecurityWhitelist() {
    return securityWhitelist;
  }

  public FailoverPriority getFailoverPriority() {
    return failoverPriority;
  }

  public Measure<TimeUnit> getClientReconnectWindow() {
    return clientReconnectWindow;
  }

  public Measure<TimeUnit> getClientLeaseDuration() {
    return clientLeaseDuration;
  }

  public Map<String, Measure<MemoryUnit>> getOffheapResources() {
    return Collections.unmodifiableMap(offheapResources);
  }

  public Map<String, Path> getDataDirs() {
    return Collections.unmodifiableMap(dataDirs);
  }

  public Map<String, Level> getNodeLoggerOverrides() {
    return Collections.unmodifiableMap(nodeLoggerOverrides);
  }

  public Node setNodeLoggerOverrides(Map<String, Level> nodeLoggerOverrides) {
    this.nodeLoggerOverrides.putAll(nodeLoggerOverrides);
    return this;
  }

  public Node setNodeLoggerOverride(String logger, Level level) {
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

  public Node setSecurityAuthc(String securityAuthc) {
    this.securityAuthc = securityAuthc;
    return this;
  }

  public Node setSecuritySslTls(boolean securitySslTls) {
    this.securitySslTls = securitySslTls;
    return this;
  }

  public Node setSecurityWhitelist(boolean securityWhitelist) {
    this.securityWhitelist = securityWhitelist;
    return this;
  }

  public Node setFailoverPriority(FailoverPriority failoverPriority) {
    this.failoverPriority = failoverPriority;
    return this;
  }

  public Node setClientReconnectWindow(long clientReconnectWindow, TimeUnit timeUnit) {
    return setClientReconnectWindow(Measure.of(clientReconnectWindow, timeUnit));
  }

  public Node setClientReconnectWindow(long clientReconnectWindow, java.util.concurrent.TimeUnit jdkUnit) {
    return setClientReconnectWindow(Measure.of(clientReconnectWindow, TimeUnit.from(jdkUnit).orElseThrow(() -> new IllegalArgumentException(jdkUnit.name()))));
  }

  public Node setClientReconnectWindow(Measure<TimeUnit> measure) {
    this.clientReconnectWindow = measure;
    return this;
  }

  public Node setClientLeaseDuration(long clientLeaseDuration, TimeUnit timeUnit) {
    return setClientLeaseDuration(Measure.of(clientLeaseDuration, timeUnit));
  }

  public Node setClientLeaseDuration(long clientLeaseDuration, java.util.concurrent.TimeUnit jdkUnit) {
    return setClientLeaseDuration(Measure.of(clientLeaseDuration, TimeUnit.from(jdkUnit).orElseThrow(() -> new IllegalArgumentException(jdkUnit.name()))));
  }

  public Node setClientLeaseDuration(Measure<TimeUnit> measure) {
    this.clientLeaseDuration = measure;
    return this;
  }

  public Node setOffheapResource(String name, long quantity, MemoryUnit memoryUnit) {
    return setOffheapResource(name, Measure.of(quantity, memoryUnit));
  }

  public Node setOffheapResource(String name, Measure<MemoryUnit> measure) {
    this.offheapResources.put(name, measure);
    return this;
  }

  public Node setOffheapResources(Map<String, Measure<MemoryUnit>> offheapResources) {
    this.offheapResources.putAll(offheapResources);
    return this;
  }

  public Node removeOffheapResource(String key) {
    this.offheapResources.remove(key);
    return this;
  }

  public Node clearOffheapResources() {
    this.offheapResources.clear();
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

  @JsonIgnore
  public InetSocketAddress getNodeAddress() {
    return getNodePublicAddress().orElseGet(this::getNodeInternalAddress);
  }

  @JsonIgnore
  public InetSocketAddress getNodeInternalAddress() {
    if (nodeHostname == null || IParameterSubstitutor.containsSubstitutionParams(nodeHostname)) {
      throw new AssertionError("Node " + nodeName + " is not correctly defined with internal address: " + nodeHostname + ":" + nodePort);
    }
    return InetSocketAddress.createUnresolved(nodeHostname, nodePort);
  }

  @JsonIgnore
  public Optional<InetSocketAddress> getNodePublicAddress() {
    if (nodePublicHostname == null || nodePublicPort == null) {
      return Optional.empty();
    }
    if (IParameterSubstitutor.containsSubstitutionParams(nodePublicHostname)) {
      throw new AssertionError("Node " + nodeName + " is not correctly defined with public address: " + nodePublicHostname + ":" + nodePublicPort);
    }
    return Optional.of(InetSocketAddress.createUnresolved(nodePublicHostname, nodePublicPort));
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Node clone() {
    return new Node()
        .setClientLeaseDuration(clientLeaseDuration)
        .setClientReconnectWindow(clientReconnectWindow)
        .setDataDirs(dataDirs)
        .setFailoverPriority(failoverPriority)
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
        .setOffheapResources(offheapResources)
        .setSecurityAuditLogDir(securityAuditLogDir)
        .setSecurityAuthc(securityAuthc)
        .setSecurityDir(securityDir)
        .setSecuritySslTls(securitySslTls)
        .setSecurityWhitelist(securityWhitelist);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Node node = (Node) o;
    return nodePort == node.nodePort &&
        nodeGroupPort == node.nodeGroupPort &&
        securitySslTls == node.securitySslTls &&
        securityWhitelist == node.securityWhitelist &&
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
        Objects.equals(securityAuthc, node.securityAuthc) &&
        Objects.equals(failoverPriority, node.failoverPriority) &&
        Objects.equals(clientReconnectWindow, node.clientReconnectWindow) &&
        Objects.equals(clientLeaseDuration, node.clientLeaseDuration) &&
        Objects.equals(offheapResources, node.offheapResources) &&
        Objects.equals(dataDirs, node.dataDirs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeName, nodeHostname, nodePublicHostname, nodePort, nodePublicPort, nodeGroupPort,
        nodeBindAddress, nodeGroupBindAddress, tcProperties, nodeLoggerOverrides, nodeMetadataDir, nodeLogDir, nodeBackupDir,
        securityDir, securityAuditLogDir, securityAuthc, securitySslTls, securityWhitelist,
        failoverPriority, clientReconnectWindow, clientLeaseDuration, offheapResources, dataDirs);
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
        ", securityAuthc='" + securityAuthc + '\'' +
        ", securitySslTls=" + securitySslTls +
        ", securityWhitelist=" + securityWhitelist +
        ", failoverPriority='" + failoverPriority + '\'' +
        ", clientReconnectWindow=" + clientReconnectWindow +
        ", clientLeaseDuration=" + clientLeaseDuration +
        ", offheapResources=" + offheapResources +
        ", dataDirs=" + dataDirs +
        '}';
  }

  public Node cloneForAttachment(Node aNodeFromTargetCluster) {
    // validate security folder
    if (aNodeFromTargetCluster.getSecurityDir() != null && securityDir == null) {
      throw new IllegalArgumentException("Node " + getNodeAddress() + " must be started with a security directory.");
    }

    // Validate the user data directories.
    // We validate that the node we want to attach has EXACTLY the same user data directories ID as the destination cluster.
    Set<String> requiredDataDirs = new TreeSet<>(aNodeFromTargetCluster.getDataDirs().keySet());
    Set<String> dataDirs = new TreeSet<>(this.dataDirs.keySet());
    if (!dataDirs.containsAll(requiredDataDirs)) {
      // case where the attached node would not have all the required IDs
      requiredDataDirs.removeAll(dataDirs);
      throw new IllegalArgumentException("Node " + getNodeAddress() + " must declare the following data directories: " + String.join(", ", requiredDataDirs) + ".");
    }
    if (dataDirs.size() > requiredDataDirs.size()) {
      // case where the attached node would have more than the required IDs
      dataDirs.removeAll(requiredDataDirs);
      throw new IllegalArgumentException("Node " + getNodeAddress() + " must not declare the following data directories: " + String.join(", ", dataDirs) + ".");
    }

    // override all the cluster-wide parameters of the node to be attached
    Node thisCopy = clone()
        .setSecurityAuthc(aNodeFromTargetCluster.getSecurityAuthc())
        .setSecuritySslTls(aNodeFromTargetCluster.isSecuritySslTls())
        .setSecurityWhitelist(aNodeFromTargetCluster.isSecurityWhitelist())
        .setFailoverPriority(aNodeFromTargetCluster.getFailoverPriority())
        .setClientReconnectWindow(aNodeFromTargetCluster.getClientReconnectWindow())
        .setClientLeaseDuration(aNodeFromTargetCluster.getClientLeaseDuration())
        .clearOffheapResources()
        .setOffheapResources(aNodeFromTargetCluster.getOffheapResources());

    if (aNodeFromTargetCluster.getSecurityDir() == null && securityDir != null) {
      // node was started with a security directory but destination cluster is not secured so we do not need one
      thisCopy.setSecurityDir(null);
    }

    return thisCopy;
  }

  private Node fillDefaults() {
    Stream.of(Setting.values())
        .filter(isEqual(NODE_HOSTNAME).negate())
        .filter(isEqual(NODE_REPOSITORY_DIR).negate())
        .filter(isEqual(CLUSTER_NAME).negate())
        .filter(isEqual(LICENSE_FILE).negate())
        .forEach(setting -> setting.fillDefault(this));
    return this;
  }

  public static Node newDefaultNode(String hostname) {
    return new Node()
        .fillDefaults()
        .setNodeHostname(hostname);
  }

  public static Node newDefaultNode(String name, String hostname) {
    return new Node()
        .fillDefaults()
        .setNodeName(name)
        .setNodeHostname(hostname);
  }

  public static Node newDefaultNode(String hostname, int port) {
    return new Node()
        .fillDefaults()
        .setNodePort(port)
        .setNodeHostname(hostname);
  }

  public static Node newDefaultNode(String name, String hostname, int port) {
    return new Node()
        .fillDefaults()
        .setNodeName(name)
        .setNodePort(port)
        .setNodeHostname(hostname);
  }

  public static Node empty() {
    return new Node();
  }
}
