/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;

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

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  public void setNodeHostname(String nodeHostname) {
    this.nodeHostname = nodeHostname;
  }

  public void setNodePort(int nodePort) {
    this.nodePort = nodePort;
  }

  public void setNodeGroupPort(int nodeGroupPort) {
    this.nodeGroupPort = nodeGroupPort;
  }

  public void setNodeBindAddress(String nodeBindAddress) {
    this.nodeBindAddress = nodeBindAddress;
  }

  public void setNodeGroupBindAddress(String nodeGroupBindAddress) {
    this.nodeGroupBindAddress = nodeGroupBindAddress;
  }

  public void setNodeConfigDir(Path nodeConfigDir) {
    this.nodeConfigDir = nodeConfigDir;
  }

  public void setNodeMetadataDir(Path nodeMetadataDir) {
    this.nodeMetadataDir = nodeMetadataDir;
  }

  public void setNodeLogDir(Path nodeLogDir) {
    this.nodeLogDir = nodeLogDir;
  }

  public void setNodeBackupDir(Path nodeBackupDir) {
    this.nodeBackupDir = nodeBackupDir;
  }

  public void setSecurityDir(Path securityDir) {
    this.securityDir = securityDir;
  }

  public void setSecurityAuditLogDir(Path securityAuditLogDir) {
    this.securityAuditLogDir = securityAuditLogDir;
  }

  public void setSecurityAuthc(String securityAuthc) {
    this.securityAuthc = securityAuthc;
  }

  public void setSecuritySslTls(boolean securitySslTls) {
    this.securitySslTls = securitySslTls;
  }

  public void setSecurityWhitelist(boolean securityWhitelist) {
    this.securityWhitelist = securityWhitelist;
  }

  public void setFailoverPriority(String failoverPriority) {
    this.failoverPriority = failoverPriority;
  }

  public void setClientReconnectWindow(long clientReconnectWindow, TimeUnit timeUnit) {
    this.clientReconnectWindow = Measure.of(clientReconnectWindow, timeUnit);
  }

  public void setClientLeaseDuration(long clientLeaseDuration, TimeUnit timeUnit) {
    this.clientLeaseDuration = Measure.of(clientLeaseDuration, timeUnit);
  }

  public void setOffheapResource(String name, long quantity, MemoryUnit memoryUnit) {
    this.offheapResources.put(name, Measure.of(quantity, memoryUnit));
  }

  public void setDataDir(String name, Path path) {
    this.dataDirs.put(name, path);
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Node node = (Node) o;
    return nodePort == node.nodePort &&
        nodeGroupPort == node.nodeGroupPort &&
        nodeName.equals(node.nodeName) &&
        nodeHostname.equals(node.nodeHostname) &&
        nodeBindAddress.equals(node.nodeBindAddress) &&
        nodeGroupBindAddress.equals(node.nodeGroupBindAddress) &&
        nodeConfigDir.equals(node.nodeConfigDir) &&
        nodeMetadataDir.equals(node.nodeMetadataDir) &&
        nodeLogDir.equals(node.nodeLogDir) &&
        nodeBackupDir.equals(node.nodeBackupDir) &&
        securitySslTls == node.securitySslTls &&
        securityWhitelist == node.securityWhitelist &&
        securityDir.equals(node.securityDir) &&
        securityAuditLogDir.equals(node.securityAuditLogDir) &&
        securityAuthc.equals(node.securityAuthc) &&
        failoverPriority.equals(node.failoverPriority) &&
        clientReconnectWindow.equals(node.clientReconnectWindow) &&
        clientLeaseDuration.equals(node.clientLeaseDuration) &&
        offheapResources.equals(node.offheapResources) &&
        dataDirs.equals(node.dataDirs) &&
        clusterName.equals(node.clusterName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeName, nodeHostname, nodePort, nodeGroupPort, nodeBindAddress, nodeGroupBindAddress, nodeConfigDir,
        nodeMetadataDir, nodeLogDir, nodeBackupDir, securityDir, securityAuditLogDir, securityAuthc, securitySslTls,
        securityWhitelist, failoverPriority, clientReconnectWindow, clientLeaseDuration, offheapResources, dataDirs, clusterName);
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
