/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.xml.bind.DatatypeConverter;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static com.terracottatech.dynamic_config.model.Setting.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.model.Setting.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.model.Setting.DATA_DIRS;
import static com.terracottatech.dynamic_config.model.Setting.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.model.Setting.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.Setting.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.model.Setting.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.model.Setting.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.model.Setting.NODE_NAME;
import static com.terracottatech.dynamic_config.model.Setting.NODE_PORT;
import static com.terracottatech.dynamic_config.model.Setting.OFFHEAP_RESOURCES;

public class Node implements Cloneable {
  private String nodeName;
  private String nodeHostname;
  private int nodePort;
  private int nodeGroupPort;
  private String nodeBindAddress;
  private String nodeGroupBindAddress;
  private Path nodeMetadataDir;
  private Path nodeLogDir;
  private Path nodeBackupDir;
  private Path securityDir;
  private Path securityAuditLogDir;
  private String securityAuthc;
  private boolean securitySslTls;
  private boolean securityWhitelist;
  private FailoverPriority failoverPriority;
  private Map<String, String> tcProperties = new ConcurrentHashMap<>();
  private Measure<TimeUnit> clientReconnectWindow;
  private Measure<TimeUnit> clientLeaseDuration;
  private Map<String, Measure<MemoryUnit>> offheapResources = new ConcurrentHashMap<>();
  private Map<String, Path> dataDirs = new ConcurrentHashMap<>();

  public String getNodeName() {
    return nodeName;
  }

  public String getNodeHostname() {
    return nodeHostname;
  }

  public int getNodePort() {
    return nodePort;
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

  public Node setNodePort(int nodePort) {
    this.nodePort = nodePort;
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

  @JsonIgnore
  public InetSocketAddress getNodeAddress() {
    return InetSocketAddress.createUnresolved(getNodeHostname(), getNodePort());
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
        .setNodeLogDir(nodeLogDir)
        .setNodeMetadataDir(nodeMetadataDir)
        .setNodeName(nodeName)
        .setNodePort(nodePort)
        .setTcProperties(tcProperties)
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
        Objects.equals(nodeBindAddress, node.nodeBindAddress) &&
        Objects.equals(nodeGroupBindAddress, node.nodeGroupBindAddress) &&
        Objects.equals(nodeMetadataDir, node.nodeMetadataDir) &&
        Objects.equals(nodeLogDir, node.nodeLogDir) &&
        Objects.equals(nodeBackupDir, node.nodeBackupDir) &&
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
    return Objects.hash(nodeName, nodeHostname, nodePort, nodeGroupPort, nodeBindAddress, nodeGroupBindAddress, tcProperties,
        nodeMetadataDir, nodeLogDir, nodeBackupDir, securityDir, securityAuditLogDir, securityAuthc, securitySslTls,
        securityWhitelist, failoverPriority, clientReconnectWindow, clientLeaseDuration, offheapResources, dataDirs);
  }

  @Override
  public String toString() {
    return "Node{" +
        "nodeName='" + nodeName + '\'' +
        ", nodeHostname='" + nodeHostname + '\'' +
        ", nodePort=" + nodePort +
        ", nodeGroupPort=" + nodeGroupPort +
        ", nodeBindAddress='" + nodeBindAddress + '\'' +
        ", nodeGroupBindAddress='" + nodeGroupBindAddress + '\'' +
        ", nodeMetadataDir='" + nodeMetadataDir + '\'' +
        ", nodeLogDir='" + nodeLogDir + '\'' +
        ", nodeBackupDir='" + nodeBackupDir + '\'' +
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

  public Node fillDefaults(BiConsumer<Setting, String> filledPropertyConsumer) {
    if (getNodeName() == null) {
      String generateNodeName = generateNodeName();
      setNodeName(generateNodeName);
      filledPropertyConsumer.accept(NODE_NAME, generateNodeName);
    }

    if (getNodePort() == 0) {
      NODE_PORT.fillDefault(this);
      filledPropertyConsumer.accept(NODE_PORT, NODE_PORT.getDefaultValue());
    }

    if (getNodeGroupPort() == 0) {
      NODE_GROUP_PORT.fillDefault(this);
      filledPropertyConsumer.accept(NODE_GROUP_PORT, NODE_GROUP_PORT.getDefaultValue());
    }

    if (getOffheapResources().isEmpty()) {
      OFFHEAP_RESOURCES.fillDefault(this);
      filledPropertyConsumer.accept(OFFHEAP_RESOURCES, OFFHEAP_RESOURCES.getDefaultValue());
    }

    if (getDataDirs().isEmpty()) {
      DATA_DIRS.fillDefault(this);
      filledPropertyConsumer.accept(DATA_DIRS, DATA_DIRS.getDefaultValue());
    }

    if (getNodeBindAddress() == null) {
      NODE_BIND_ADDRESS.fillDefault(this);
      filledPropertyConsumer.accept(NODE_BIND_ADDRESS, NODE_BIND_ADDRESS.getDefaultValue());
    }

    if (getNodeGroupBindAddress() == null) {
      NODE_GROUP_BIND_ADDRESS.fillDefault(this);
      filledPropertyConsumer.accept(NODE_GROUP_BIND_ADDRESS, NODE_GROUP_BIND_ADDRESS.getDefaultValue());
    }

    if (getNodeLogDir() == null) {
      NODE_LOG_DIR.fillDefault(this);
      filledPropertyConsumer.accept(NODE_LOG_DIR, NODE_LOG_DIR.getDefaultValue());
    }

    if (getNodeMetadataDir() == null) {
      NODE_METADATA_DIR.fillDefault(this);
      filledPropertyConsumer.accept(NODE_METADATA_DIR, NODE_METADATA_DIR.getDefaultValue());
    }

    if (getFailoverPriority() == null) {
      FAILOVER_PRIORITY.fillDefault(this);
      filledPropertyConsumer.accept(FAILOVER_PRIORITY, FAILOVER_PRIORITY.getDefaultValue());
    }

    if (getClientReconnectWindow() == null) {
      CLIENT_RECONNECT_WINDOW.fillDefault(this);
      filledPropertyConsumer.accept(CLIENT_RECONNECT_WINDOW, CLIENT_RECONNECT_WINDOW.getDefaultValue());
    }

    if (getClientLeaseDuration() == null) {
      CLIENT_LEASE_DURATION.fillDefault(this);
      filledPropertyConsumer.accept(CLIENT_LEASE_DURATION, CLIENT_LEASE_DURATION.getDefaultValue());
    }

    return this;
  }

  public Node fillDefaults() {
    return fillDefaults((s, s2) -> {
    });
  }

  public static Node newDefaultNode() {
    return new Node().fillDefaults();
  }

  public static Node newDefaultNode(String hostname) {
    return newDefaultNode()
        .setNodeHostname(hostname);
  }

  public static Node newDefaultNode(String name, String hostname) {
    return newDefaultNode()
        .setNodeName(name)
        .setNodeHostname(hostname);
  }

  public static Node newDefaultNode(String hostname, int port) {
    return newDefaultNode()
        .setNodePort(port)
        .setNodeHostname(hostname);
  }

  public static Node newDefaultNode(String name, String hostname, int port) {
    return newDefaultNode()
        .setNodeName(name)
        .setNodePort(port)
        .setNodeHostname(hostname);
  }

  private static String generateNodeName() {
    UUID uuid = UUID.randomUUID();
    byte[] data = new byte[16];
    long msb = uuid.getMostSignificantBits();
    long lsb = uuid.getLeastSignificantBits();
    for (int i = 0; i < 8; i++) {
      data[i] = (byte) (msb & 0xff);
      msb >>>= 8;
    }
    for (int i = 8; i < 16; i++) {
      data[i] = (byte) (lsb & 0xff);
      lsb >>>= 8;
    }

    return "node-" + DatatypeConverter.printBase64Binary(data)
        // java-8 and other - compatible B64 url decoder use - and _ instead of + and /
        // padding can be ignored to shorten the UUID
        .replace('+', '-')
        .replace('/', '_')
        .replace("=", "");
  }
}
