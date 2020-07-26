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

  private String name;
  private String hostname;
  private String publicHostname;
  private Integer port;
  private Integer publicPort;
  private Integer groupPort;
  private String bindAddress;
  private String groupBindAddress;
  private Path metadataDir;
  private Path logDir;
  private Path backupDir;
  private Path securityDir;
  private Path securityAuditLogDir;
  private Map<String, String> tcProperties = new ConcurrentHashMap<>();
  private Map<String, String> loggerOverrides = new ConcurrentHashMap<>();
  private Map<String, Path> dataDirs = new ConcurrentHashMap<>();

  @Override
  public Scope getScope() {
    return NODE;
  }

  public String getName() {
    return name;
  }

  public String getHostname() {
    return hostname;
  }

  public String getPublicHostname() {
    return publicHostname;
  }

  public Integer getPort() {
    return port;
  }

  public Integer getPublicPort() {
    return publicPort;
  }

  public Integer getGroupPort() {
    return groupPort;
  }

  public String getBindAddress() {
    return bindAddress;
  }

  public String getGroupBindAddress() {
    return groupBindAddress;
  }

  public Path getMetadataDir() {
    return metadataDir;
  }

  public Path getLogDir() {
    return logDir;
  }

  public Path getBackupDir() {
    return backupDir;
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

  public Map<String, String> getLoggerOverrides() {
    return Collections.unmodifiableMap(loggerOverrides);
  }

  public Map<String, String> getTcProperties() {
    return tcProperties;
  }

  public Node setName(String name) {
    this.name = name;
    return this;
  }

  public Node setHostname(String hostname) {
    this.hostname = hostname;
    return this;
  }

  public Node setPublicHostname(String publicHostname) {
    this.publicHostname = publicHostname;
    return this;
  }

  public Node setPort(Integer port) {
    this.port = port;
    return this;
  }

  public Node setPublicPort(Integer publicPort) {
    this.publicPort = publicPort;
    return this;
  }

  public Node setGroupPort(Integer groupPort) {
    this.groupPort = groupPort;
    return this;
  }

  public Node setBindAddress(String bindAddress) {
    this.bindAddress = bindAddress;
    return this;
  }

  public Node setGroupBindAddress(String groupBindAddress) {
    this.groupBindAddress = groupBindAddress;
    return this;
  }

  public Node setMetadataDir(Path metadataDir) {
    this.metadataDir = metadataDir;
    return this;
  }

  public Node setLogDir(Path logDir) {
    this.logDir = logDir;
    return this;
  }

  public Node setBackupDir(Path backupDir) {
    this.backupDir = backupDir;
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

  public Node putLoggerOverride(String logger, String level) {
    this.loggerOverrides.put(logger, level);
    return this;
  }

  public Node putLoggerOverrides(Map<String, String> loggerOverrides) {
    this.loggerOverrides.putAll(loggerOverrides);
    return this;
  }

  public Node setLoggerOverrides(Map<String, String> loggerOverrides) {
    this.loggerOverrides = loggerOverrides == null ? null : new ConcurrentHashMap<>(loggerOverrides);
    return this;
  }

  public Node removeLoggerOverride(String logger) {
    this.loggerOverrides.remove(logger);
    return this;
  }

  public Node clearLoggerOverrides() {
    loggerOverrides.clear();
    return this;
  }

  public Node putTcProperty(String key, String value) {
    this.tcProperties.put(key, value);
    return this;
  }

  public Node putTcProperties(Map<String, String> tcProperties) {
    this.tcProperties.putAll(tcProperties);
    return this;
  }

  public Node setTcProperties(Map<String, String> tcProperties) {
    this.tcProperties = tcProperties == null ? null : new ConcurrentHashMap<>(tcProperties);
    return this;
  }

  public Node removeTcProperty(String key) {
    this.tcProperties.remove(key);
    return this;
  }

  public Node clearTcProperties() {
    this.tcProperties.clear();
    return this;
  }

  public Node putDataDir(String name, Path path) {
    this.dataDirs.put(name, path);
    return this;
  }

  public Node putDataDirs(Map<String, Path> dataDirs) {
    this.dataDirs.putAll(dataDirs);
    return this;
  }

  public Node setDataDirs(Map<String, Path> dataDirs) {
    this.dataDirs = dataDirs == null ? null : new ConcurrentHashMap<>(dataDirs);
    return this;
  }

  public Node removeDataDir(String key) {
    dataDirs.remove(key);
    return this;
  }

  public Node clearDataDirs() {
    this.dataDirs.clear();
    return this;
  }

  /**
   * @return true if this node has this public or internal address
   */
  public boolean hasAddress(InetSocketAddress address) {
    return InetSocketAddressUtils.areEqual(address, getInternalAddress()) ||
        getPublicAddress().map(addr -> InetSocketAddressUtils.areEqual(address, addr)).orElse(false);
  }

  public InetSocketAddress getAddress() {
    return getPublicAddress().orElseGet(this::getInternalAddress);
  }

  public InetSocketAddress getInternalAddress() {
    if (hostname == null || Substitutor.containsSubstitutionParams(hostname)) {
      throw new AssertionError("Node " + name + " is not correctly defined with internal address: " + hostname + ":" + port);
    }
    return InetSocketAddress.createUnresolved(hostname, port);
  }

  public Optional<InetSocketAddress> getPublicAddress() {
    if (publicHostname == null || publicPort == null) {
      return Optional.empty();
    }
    if (Substitutor.containsSubstitutionParams(publicHostname)) {
      throw new AssertionError("Node " + name + " is not correctly defined with public address: " + publicHostname + ":" + publicPort);
    }
    return Optional.of(InetSocketAddress.createUnresolved(publicHostname, publicPort));
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Node clone() {
    return new Node()
        .setDataDirs(dataDirs)
        .setBackupDir(backupDir)
        .setBindAddress(bindAddress)
        .setGroupBindAddress(groupBindAddress)
        .setGroupPort(groupPort)
        .setHostname(hostname)
        .setPublicHostname(publicHostname)
        .setLogDir(logDir)
        .setMetadataDir(metadataDir)
        .setName(name)
        .setPort(port)
        .setPublicPort(publicPort)
        .setTcProperties(tcProperties)
        .setLoggerOverrides(loggerOverrides)
        .setSecurityAuditLogDir(securityAuditLogDir)
        .setSecurityDir(securityDir);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Node node = (Node) o;
    return Objects.equals(port, node.port) &&
        Objects.equals(groupPort, node.groupPort) &&
        Objects.equals(name, node.name) &&
        Objects.equals(hostname, node.hostname) &&
        Objects.equals(publicHostname, node.publicHostname) &&
        Objects.equals(publicPort, node.publicPort) &&
        Objects.equals(bindAddress, node.bindAddress) &&
        Objects.equals(groupBindAddress, node.groupBindAddress) &&
        Objects.equals(metadataDir, node.metadataDir) &&
        Objects.equals(logDir, node.logDir) &&
        Objects.equals(backupDir, node.backupDir) &&
        Objects.equals(loggerOverrides, node.loggerOverrides) &&
        Objects.equals(tcProperties, node.tcProperties) &&
        Objects.equals(securityDir, node.securityDir) &&
        Objects.equals(securityAuditLogDir, node.securityAuditLogDir) &&
        Objects.equals(dataDirs, node.dataDirs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, hostname, publicHostname, port, publicPort, groupPort,
        bindAddress, groupBindAddress, tcProperties, loggerOverrides, metadataDir, logDir, backupDir,
        securityDir, securityAuditLogDir, dataDirs);
  }

  @Override
  public String toString() {
    return "Node{" +
        "name='" + name + '\'' +
        ", hostname='" + hostname + '\'' +
        ", port=" + port +
        ", publicHostname='" + publicHostname + '\'' +
        ", publicPort=" + publicPort +
        ", groupPort=" + groupPort +
        ", bindAddress='" + bindAddress + '\'' +
        ", groupBindAddress='" + groupBindAddress + '\'' +
        ", metadataDir='" + metadataDir + '\'' +
        ", logDir='" + logDir + '\'' +
        ", backupDir='" + backupDir + '\'' +
        ", loggers='" + loggerOverrides + '\'' +
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
}
