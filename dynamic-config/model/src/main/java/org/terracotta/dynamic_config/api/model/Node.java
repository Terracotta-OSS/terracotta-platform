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
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.inet.InetSocketAddressUtils;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
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

  private static final String ADDR_GROUP_PUBLIC = "public";
  private static final String ADDR_GROUP_INTERNAL = "internal";

  private UID uid;
  private String name;
  private String hostname;
  private String publicHostname;
  private Integer port;
  private Integer publicPort;
  private Integer groupPort;
  private String bindAddress;
  private String groupBindAddress;
  private RawPath metadataDir;
  private RawPath logDir;
  private RawPath backupDir;
  private RawPath securityDir;
  private RawPath securityAuditLogDir;
  private Map<String, String> tcProperties;
  private Map<String, String> loggerOverrides;
  private Map<String, RawPath> dataDirs;

  @Override
  public Scope getScope() {
    return NODE;
  }

  @Override
  public UID getUID() {
    return uid;
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

  public OptionalConfig<RawPath> getMetadataDir() {
    return OptionalConfig.of(NODE_METADATA_DIR, metadataDir);
  }

  public OptionalConfig<RawPath> getLogDir() {
    return OptionalConfig.of(NODE_LOG_DIR, logDir);
  }

  public OptionalConfig<RawPath> getBackupDir() {
    return OptionalConfig.of(NODE_BACKUP_DIR, backupDir);
  }

  public OptionalConfig<RawPath> getSecurityDir() {
    return OptionalConfig.of(SECURITY_DIR, securityDir);
  }

  public OptionalConfig<RawPath> getSecurityAuditLogDir() {
    return OptionalConfig.of(SECURITY_AUDIT_LOG_DIR, securityAuditLogDir);
  }

  public OptionalConfig<Map<String, RawPath>> getDataDirs() {
    return OptionalConfig.of(DATA_DIRS, dataDirs);
  }

  public OptionalConfig<Map<String, String>> getLoggerOverrides() {
    return OptionalConfig.of(NODE_LOGGER_OVERRIDES, loggerOverrides);
  }

  public OptionalConfig<Map<String, String>> getTcProperties() {
    return OptionalConfig.of(TC_PROPERTIES, tcProperties);
  }

  public Node setUID(UID uid) {
    this.uid = requireNonNull(uid);
    return this;
  }

  public Node setName(String name) {
    this.name = requireNonNull(name);
    return this;
  }

  public Node setHostname(String hostname) {
    this.hostname = requireNonNull(hostname);
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

  public Node setMetadataDir(RawPath metadataDir) {
    this.metadataDir = metadataDir;
    return this;
  }

  public Node setLogDir(RawPath logDir) {
    this.logDir = logDir;
    return this;
  }

  public Node setBackupDir(RawPath backupDir) {
    this.backupDir = backupDir;
    return this;
  }

  public Node setSecurityDir(RawPath securityDir) {
    this.securityDir = securityDir;
    return this;
  }

  public Node setSecurityAuditLogDir(RawPath securityAuditLogDir) {
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
    Map<String, String> def = NODE_LOGGER_OVERRIDES.getDefaultValue();
    setLoggerOverrides(def == null || def.isEmpty() ? null : emptyMap());
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
    Map<String, String> def = TC_PROPERTIES.getDefaultValue();
    setTcProperties(def == null || def.isEmpty() ? null : emptyMap());
    return this;
  }

  public Node putDataDir(String name, RawPath path) {
    return putDataDirs(singletonMap(name, path));
  }

  public Node putDataDirs(Map<String, RawPath> dataDirs) {
    if (this.dataDirs == null) {
      setDataDirs(Optional.ofNullable(DATA_DIRS.<Map<String, RawPath>>getDefaultValue()).orElse(emptyMap()));
    }
    this.dataDirs.putAll(dataDirs);
    return this;
  }

  public Node setDataDirs(Map<String, RawPath> dataDirs) {
    this.dataDirs = dataDirs == null ? null : new ConcurrentHashMap<>(dataDirs);
    return this;
  }

  public Node removeDataDir(String key) {
    if (this.dataDirs == null) {
      // this code is handling the removal of any default value set
      Map<String, RawPath> def = DATA_DIRS.getDefaultValue();
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
    Map<String, String> def = DATA_DIRS.getDefaultValue();
    setDataDirs(def == null || def.isEmpty() ? null : emptyMap());
    return this;
  }

  /**
   * @return true if this node has this public or internal address
   */
  public boolean hasAddress(InetSocketAddress address) {
    return InetSocketAddressUtils.areEqual(address, getInternalAddress()) ||
        getPublicAddress().map(addr -> InetSocketAddressUtils.areEqual(address, addr)).orElse(false);
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

  /**
   * Get an endpoint to connect to this node based on the address used to initiate the connection.
   * <p>
   * If the address used to initiate the connection is the public address, then use it.
   * <p>
   * If the address used to initiate the connection is the internal address, then use it.
   * <p>
   * Otherwise, use the public address and if not set, the internal one
   */
  public Endpoint getEndpoint(InetSocketAddress initiator) {
    Optional<InetSocketAddress> publicAddress = getPublicAddress();
    InetSocketAddress internalAddress = getInternalAddress();
    if (publicAddress.isPresent() && publicAddress.get().equals(initiator)) {
      return getPublicEndpoint().get();
    }
    if (internalAddress.equals(initiator)) {
      return getInternalEndpoint();
    }
    // fallback: public first, otherwise internal
    return getPublicEndpoint().orElseGet(this::getInternalEndpoint);
  }

  public Endpoint getSimilarEndpoint(Endpoint initiator) {
    return ADDR_GROUP_INTERNAL.equals(initiator.getGroup()) ?
        getInternalEndpoint() :
        getPublicEndpoint().orElseGet(this::getInternalEndpoint);
  }

  public Endpoint getInternalEndpoint() {
    return new Endpoint(this, ADDR_GROUP_INTERNAL, getInternalAddress());
  }

  public Optional<Endpoint> getPublicEndpoint() {
    return getPublicAddress().map(addr -> new Endpoint(this, ADDR_GROUP_PUBLIC, addr));
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public Node clone() {
    Node clone = new Node();
    clone.dataDirs = this.dataDirs == null ? null : new ConcurrentHashMap<>(this.dataDirs);
    clone.backupDir = this.backupDir;
    clone.bindAddress = this.bindAddress;
    clone.groupBindAddress = this.groupBindAddress;
    clone.groupPort = this.groupPort;
    clone.hostname = this.hostname;
    clone.logDir = this.logDir;
    clone.loggerOverrides = this.loggerOverrides == null ? null : new ConcurrentHashMap<>(this.loggerOverrides);
    clone.metadataDir = this.metadataDir;
    clone.name = this.name;
    clone.port = this.port;
    clone.publicHostname = this.publicHostname;
    clone.publicPort = this.publicPort;
    clone.securityAuditLogDir = this.securityAuditLogDir;
    clone.securityDir = this.securityDir;
    clone.tcProperties = this.tcProperties == null ? null : new ConcurrentHashMap<>(this.tcProperties);
    clone.uid = this.uid;
    return clone;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Node node = (Node) o;
    return Objects.equals(port, node.port) &&
        Objects.equals(groupPort, node.groupPort) &&
        Objects.equals(name, node.name) &&
        Objects.equals(uid, node.uid) &&
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
        securityDir, securityAuditLogDir, dataDirs, uid);
  }

  @Override
  public String toString() {
    return Props.toString(toProperties(false, false, true));
  }

  /**
   * Transform this model into a config file where all the "map" like settings can be expanded (one item per line)
   */
  @Override
  public Properties toProperties(boolean expanded, boolean includeDefaultValues, boolean includeHiddenSettings, Version version) {
    return modelToProperties(this, expanded, includeDefaultValues, includeHiddenSettings, version);
  }

  public String toShapeString() {
    return getInternalEndpoint().toString();
  }

  /**
   * This class represents an endpoint to use when connecting to a node.
   * <p>
   * The address will be either the internal address or the public address.
   * <p>
   * IMPORTANT FOR USAGE: the equals/hashcode of an endpoint is based on the UID.
   * Other properties like address, name, group are characteristics.
   *
   * @author Mathieu Carbou
   */
  public static class Endpoint {

    private final Node node;
    private final String group;
    private final InetSocketAddress address;

    private Endpoint(Node node, String group, InetSocketAddress address) {
      this.node = requireNonNull(node);
      this.group = requireNonNull(group);
      this.address = requireNonNull(address);
    }

    public String getGroup() {
      return group;
    }

    public String getNodeName() {
      return node.getName();
    }

    public UID getNodeUID() {
      return node.getUID();
    }

    public InetSocketAddress getAddress() {
      return address;
    }

    @Override
    public String toString() {
      return getNodeName() + "@" + getAddress();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Endpoint)) return false;
      Endpoint endpoint = (Endpoint) o;
      return getNodeUID().equals(endpoint.getNodeUID());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getNodeUID());
    }
  }
}
