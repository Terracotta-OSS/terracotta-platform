/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class Node {
  private String nodeName;
  private String nodeHostname;
  private int nodePort;
  private int nodeGroupPort;
  private String nodeBindAddress;
  private String nodeGroupBindAddress;
  private Path nodeConfigDir;
  private Path nodeMetadataDir;
  private Path nodeLogDir;
  private Path nodeBackupDir;
  private Path securityDir;
  private Path securityAuditLogDir;
  private String securityAuthc;
  private boolean securitySslTls;
  private boolean securityWhitelist;
  private String failoverPriority;
  private Measure<TimeUnit> clientReconnectWindow;
  private Measure<TimeUnit> clientLeaseDuration;
  private Map<String, Measure<MemoryUnit>> offheapResources = new HashMap<>();
  private Map<String, Path> dataDirs = new HashMap<>();
  private String clusterName;

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

  public Path getNodeConfigDir() {
    return nodeConfigDir;
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

  public String getFailoverPriority() {
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

  public String getClusterName() {
    return clusterName;
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

  public Node setNodeConfigDir(Path nodeConfigDir) {
    this.nodeConfigDir = nodeConfigDir;
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

  public Node setFailoverPriority(String failoverPriority) {
    this.failoverPriority = failoverPriority;
    return this;
  }

  public Node setClientReconnectWindow(long clientReconnectWindow, TimeUnit timeUnit) {
    this.clientReconnectWindow = Measure.of(clientReconnectWindow, timeUnit);
    return this;
  }

  public Node setClientReconnectWindow(long clientReconnectWindow, java.util.concurrent.TimeUnit jdkUnit) {
    this.clientReconnectWindow = Measure.of(clientReconnectWindow, TimeUnit.from(jdkUnit).orElseThrow(() -> new IllegalArgumentException(jdkUnit.name())));
    return this;
  }

  public Node setClientLeaseDuration(long clientLeaseDuration, TimeUnit timeUnit) {
    this.clientLeaseDuration = Measure.of(clientLeaseDuration, timeUnit);
    return this;
  }

  public Node setClientLeaseDuration(long clientLeaseDuration, java.util.concurrent.TimeUnit jdkUnit) {
    this.clientLeaseDuration = Measure.of(clientLeaseDuration, TimeUnit.from(jdkUnit).orElseThrow(() -> new IllegalArgumentException(jdkUnit.name())));
    return this;
  }

  public Node setOffheapResource(String name, long quantity, MemoryUnit memoryUnit) {
    this.offheapResources.put(name, Measure.of(quantity, memoryUnit));
    return this;
  }

  public Node setDataDir(String name, Path path) {
    this.dataDirs.put(name, path);
    return this;
  }

  public Node setClusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
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
        Objects.equals(nodeConfigDir, node.nodeConfigDir) &&
        Objects.equals(nodeMetadataDir, node.nodeMetadataDir) &&
        Objects.equals(nodeLogDir, node.nodeLogDir) &&
        Objects.equals(nodeBackupDir, node.nodeBackupDir) &&
        Objects.equals(securityDir, node.securityDir) &&
        Objects.equals(securityAuditLogDir, node.securityAuditLogDir) &&
        Objects.equals(securityAuthc, node.securityAuthc) &&
        Objects.equals(failoverPriority, node.failoverPriority) &&
        Objects.equals(clientReconnectWindow, node.clientReconnectWindow) &&
        Objects.equals(clientLeaseDuration, node.clientLeaseDuration) &&
        Objects.equals(offheapResources, node.offheapResources) &&
        Objects.equals(dataDirs, node.dataDirs) &&
        Objects.equals(clusterName, node.clusterName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeName, nodeHostname, nodePort, nodeGroupPort, nodeBindAddress, nodeGroupBindAddress, nodeConfigDir, nodeMetadataDir, nodeLogDir, nodeBackupDir, securityDir, securityAuditLogDir, securityAuthc, securitySslTls, securityWhitelist, failoverPriority, clientReconnectWindow, clientLeaseDuration, offheapResources, dataDirs, clusterName);
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
        ", nodeConfigDir='" + nodeConfigDir + '\'' +
        ", nodeMetadataDir='" + nodeMetadataDir + '\'' +
        ", nodeLogDir='" + nodeLogDir + '\'' +
        ", nodeBackupDir='" + nodeBackupDir + '\'' +
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
        ", clusterName='" + clusterName + '\'' +
        '}';
  }
}
