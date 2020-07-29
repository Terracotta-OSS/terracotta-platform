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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Setting.DATA_DIRS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_GROUP_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOGGER_OVERRIDES;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_METADATA_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PUBLIC_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PUBLIC_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUDIT_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.TC_PROPERTIES;
import static org.terracotta.dynamic_config.api.model.Setting.modelToProperties;

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
  private Map<String, String> tcProperties;
  private Map<String, String> loggerOverrides;
  private Map<String, Path> dataDirs;

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

  public OptionalConfig<String> getPublicHostname() {
    return OptionalConfig.of(NODE_PUBLIC_HOSTNAME, publicHostname);
  }

  public OptionalConfig<Integer> getPort() {
    return OptionalConfig.of(NODE_PORT, port);
  }

  public OptionalConfig<Integer> getPublicPort() {
    return OptionalConfig.of(NODE_PUBLIC_PORT, publicPort);
  }

  public OptionalConfig<Integer> getGroupPort() {
    return OptionalConfig.of(NODE_GROUP_PORT, groupPort);
  }

  public OptionalConfig<String> getBindAddress() {
    return OptionalConfig.of(NODE_BIND_ADDRESS, bindAddress);
  }

  public OptionalConfig<String> getGroupBindAddress() {
    return OptionalConfig.of(NODE_GROUP_BIND_ADDRESS, groupBindAddress);
  }

  public OptionalConfig<Path> getMetadataDir() {
    return OptionalConfig.of(NODE_METADATA_DIR, metadataDir);
  }

  public OptionalConfig<Path> getLogDir() {
    return OptionalConfig.of(NODE_LOG_DIR, logDir);
  }

  public OptionalConfig<Path> getBackupDir() {
    return OptionalConfig.of(NODE_BACKUP_DIR, backupDir);
  }

  public OptionalConfig<Path> getSecurityDir() {
    return OptionalConfig.of(SECURITY_DIR, securityDir);
  }

  public OptionalConfig<Path> getSecurityAuditLogDir() {
    return OptionalConfig.of(SECURITY_AUDIT_LOG_DIR, securityAuditLogDir);
  }

  public OptionalConfig<Map<String, Path>> getDataDirs() {
    return OptionalConfig.of(DATA_DIRS, dataDirs);
  }

  public OptionalConfig<Map<String, String>> getLoggerOverrides() {
    return OptionalConfig.of(NODE_LOGGER_OVERRIDES, loggerOverrides);
  }

  public OptionalConfig<Map<String, String>> getTcProperties() {
    return OptionalConfig.of(TC_PROPERTIES, tcProperties);
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
    return putLoggerOverrides(singletonMap(logger, level));
  }

  public Node putLoggerOverrides(Map<String, String> loggerOverrides) {
    if (this.loggerOverrides == null) {
      setLoggerOverrides(Optional.ofNullable(NODE_LOGGER_OVERRIDES.<Map<String, String>>getDefaultValue()).orElse(emptyMap()));
    }
    this.loggerOverrides.putAll(loggerOverrides);
    return this;
  }

  public Node setLoggerOverrides(Map<String, String> loggerOverrides) {
    this.loggerOverrides = loggerOverrides == null ? null : new ConcurrentHashMap<>(loggerOverrides);
    return this;
  }

  public Node removeLoggerOverride(String logger) {
    if (this.loggerOverrides == null) {
      // this code is handling the removal of any default value set
      Map<String, String> def = NODE_LOGGER_OVERRIDES.getDefaultValue();
      if (def != null && def.containsKey(logger)) {
        setLoggerOverrides(def);
      }
    }
    if (this.loggerOverrides != null) {
      this.loggerOverrides.remove(logger);
    }
    return this;
  }

  public Node unsetLoggerOverrides() {
    if (this.loggerOverrides != null) {
      setLoggerOverrides(emptyMap());
    } else {
      Map<String, String> def = NODE_LOGGER_OVERRIDES.getDefaultValue();
      if (def != null && !def.isEmpty()) {
        setLoggerOverrides(emptyMap());
      }
    }
    return this;
  }

  public Node putTcProperty(String key, String value) {
    return putTcProperties(singletonMap(key, value));
  }

  public Node putTcProperties(Map<String, String> tcProperties) {
    if (this.tcProperties == null) {
      setTcProperties(Optional.ofNullable(TC_PROPERTIES.<Map<String, String>>getDefaultValue()).orElse(emptyMap()));
    }
    this.tcProperties.putAll(tcProperties);
    return this;
  }

  public Node setTcProperties(Map<String, String> tcProperties) {
    this.tcProperties = tcProperties == null ? null : new ConcurrentHashMap<>(tcProperties);
    return this;
  }

  public Node removeTcProperty(String key) {
    if (this.tcProperties == null) {
      // this code is handling the removal of any default value set
      Map<String, String> def = TC_PROPERTIES.getDefaultValue();
      if (def != null && def.containsKey(key)) {
        setTcProperties(def);
      }
    }
    if (this.tcProperties != null) {
      this.tcProperties.remove(key);
    }
    return this;
  }

  public Node unsetTcProperties() {
    if (this.tcProperties != null) {
      setTcProperties(emptyMap());
    } else {
      Map<String, String> def = TC_PROPERTIES.getDefaultValue();
      if (def != null && !def.isEmpty()) {
        setTcProperties(emptyMap());
      }
    }
    return this;
  }

  public Node putDataDir(String name, Path path) {
    return putDataDirs(singletonMap(name, path));
  }

  public Node putDataDirs(Map<String, Path> dataDirs) {
    if (this.dataDirs == null) {
      setDataDirs(Optional.ofNullable(DATA_DIRS.<Map<String, Path>>getDefaultValue()).orElse(emptyMap()));
    }
    this.dataDirs.putAll(dataDirs);
    return this;
  }

  public Node setDataDirs(Map<String, Path> dataDirs) {
    this.dataDirs = dataDirs == null ? null : new ConcurrentHashMap<>(dataDirs);
    return this;
  }

  public Node removeDataDir(String key) {
    if (this.dataDirs == null) {
      // this code is handling the removal of any default value set
      Map<String, Path> def = DATA_DIRS.getDefaultValue();
      if (def != null && def.containsKey(key)) {
        setDataDirs(def);
      }
    }
    if (this.dataDirs != null) {
      this.dataDirs.remove(key);
    }
    return this;
  }

  public Node unsetDataDirs() {
    if (this.dataDirs != null) {
      setDataDirs(emptyMap());
    } else {
      Map<String, Path> def = DATA_DIRS.getDefaultValue();
      if (def != null && !def.isEmpty()) {
        setDataDirs(emptyMap());
      }
    }
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
    final String hostname = getHostname();
    final Integer port = getPort().orDefault();
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
  public Properties toProperties(boolean expanded, boolean includeDefaultValues, boolean includeHiddenSettings) {
    return modelToProperties(this, expanded, includeDefaultValues, includeHiddenSettings);
  }
}
