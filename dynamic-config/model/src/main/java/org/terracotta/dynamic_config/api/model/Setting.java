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

import org.slf4j.event.Level;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.common.struct.Unit;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.io.File.separator;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.EnumSet.noneOf;
import static java.util.EnumSet.of;
import static org.terracotta.common.struct.TimeUnit.HOURS;
import static org.terracotta.common.struct.TimeUnit.MILLISECONDS;
import static org.terracotta.common.struct.TimeUnit.MINUTES;
import static org.terracotta.common.struct.TimeUnit.SECONDS;
import static org.terracotta.common.struct.Tuple2.tuple2;
import static org.terracotta.dynamic_config.api.model.Operation.CONFIG;
import static org.terracotta.dynamic_config.api.model.Operation.GET;
import static org.terracotta.dynamic_config.api.model.Operation.SET;
import static org.terracotta.dynamic_config.api.model.Operation.UNSET;
import static org.terracotta.dynamic_config.api.model.Requirement.ACTIVES_ONLINE;
import static org.terracotta.dynamic_config.api.model.Requirement.ALL_NODES_ONLINE;
import static org.terracotta.dynamic_config.api.model.Requirement.RESTART;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Scope.STRIPE;
import static org.terracotta.dynamic_config.api.model.SettingValidator.ADDRESS_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.DATA_DIRS_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.DEFAULT_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.HOST_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.LOGGER_LEVEL_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.NODE_NAME_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.OFFHEAP_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.PATH_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.PORT_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.PROPS_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.TIME_VALIDATOR;

/**
 * See API doc Config-tool.adoc
 *
 * @author Mathieu Carbou
 */
public enum Setting {

  // ==== Settings applied to a specific node only

  NODE_NAME(SettingName.NODE_NAME,
      false,
      null,
      NODE,
      extractor(Node::getNodeName),
      setter(Node::setNodeName),
      of(GET, CONFIG),
      noneOf(Requirement.class),
      emptyList(),
      emptyList(),
      (key, value) -> NODE_NAME_VALIDATOR.accept(SettingName.NODE_NAME, tuple2(key, value))
  ),
  NODE_HOSTNAME(SettingName.NODE_HOSTNAME,
      false,
      "%h",
      NODE,
      extractor(Node::getNodeHostname),
      setter(Node::setNodeHostname),
      of(GET, CONFIG),
      noneOf(Requirement.class),
      emptyList(),
      emptyList(),
      (key, value) -> HOST_VALIDATOR.accept(SettingName.NODE_HOSTNAME, tuple2(key, value))
  ),
  NODE_PUBLIC_HOSTNAME(SettingName.NODE_PUBLIC_HOSTNAME,
      false,
      null,
      NODE,
      extractor(Node::getNodePublicHostname),
      setter(Node::setNodePublicHostname),
      of(GET, SET, UNSET, CONFIG),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> HOST_VALIDATOR.accept(SettingName.NODE_PUBLIC_HOSTNAME, tuple2(key, value))
  ),
  NODE_PORT(SettingName.NODE_PORT,
      false,
      "9410",
      NODE,
      extractor(Node::getNodePort),
      setter((node, value) -> node.setNodePort(Integer.parseInt(value))),
      of(GET, CONFIG),
      noneOf(Requirement.class),
      emptyList(),
      emptyList(),
      (key, value) -> PORT_VALIDATOR.accept(SettingName.NODE_PORT, tuple2(key, value))
  ),
  NODE_PUBLIC_PORT(SettingName.NODE_PUBLIC_PORT,
      false,
      null,
      NODE,
      extractor(Node::getNodePublicPort),
      setter((node, value) -> node.setNodePublicPort(value == null ? null : Integer.parseInt(value))),
      of(GET, SET, UNSET, CONFIG),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> PORT_VALIDATOR.accept(SettingName.NODE_PUBLIC_PORT, tuple2(key, value))
  ),
  NODE_GROUP_PORT(SettingName.NODE_GROUP_PORT,
      false,
      "9430",
      NODE,
      extractor(Node::getNodeGroupPort),
      setter((node, value) -> node.setNodeGroupPort(Integer.parseInt(value))),
      of(GET, SET, CONFIG),
      of(ALL_NODES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> PORT_VALIDATOR.accept(SettingName.NODE_GROUP_PORT, tuple2(key, value))
  ),
  NODE_BIND_ADDRESS(SettingName.NODE_BIND_ADDRESS,
      false,
      "0.0.0.0",
      NODE,
      extractor(Node::getNodeBindAddress),
      setter(Node::setNodeBindAddress),
      of(GET, SET, CONFIG),
      of(ACTIVES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> ADDRESS_VALIDATOR.accept(SettingName.NODE_BIND_ADDRESS, tuple2(key, value))
  ),
  NODE_GROUP_BIND_ADDRESS(SettingName.NODE_GROUP_BIND_ADDRESS,
      false,
      "0.0.0.0",
      NODE,
      extractor(Node::getNodeGroupBindAddress),
      setter(Node::setNodeGroupBindAddress),
      of(GET, SET, CONFIG),
      of(ALL_NODES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> ADDRESS_VALIDATOR.accept(SettingName.NODE_GROUP_BIND_ADDRESS, tuple2(key, value))
  ),

  // ==== Node configuration

  CLUSTER_NAME(SettingName.CLUSTER_NAME,
      false,
      null,
      CLUSTER,
      node -> {
        throw new UnsupportedOperationException("Unable to get the cluster name from a node");
      },
      unsupported(),
      of(GET, SET, CONFIG),
      of(ALL_NODES_ONLINE, RESTART)
  ),
  NODE_REPOSITORY_DIR(SettingName.NODE_REPOSITORY_DIR,
      false,
      "%H" + separator + "terracotta" + separator + "repository",
      NODE,
      node -> {
        throw new UnsupportedOperationException("Unable to get the repository directory of a node");
      },
      unsupported(),
      noneOf(Operation.class),
      noneOf(Requirement.class),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.NODE_REPOSITORY_DIR, tuple2(key, value))
  ),
  NODE_METADATA_DIR(SettingName.NODE_METADATA_DIR,
      false,
      "%H" + separator + "terracotta" + separator + "metadata",
      NODE,
      extractor(Node::getNodeMetadataDir),
      setter((node, value) -> node.setNodeMetadataDir(Paths.get(value))),
      of(GET, SET, CONFIG),
      of(ACTIVES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.NODE_METADATA_DIR, tuple2(key, value))
  ),
  NODE_LOG_DIR(SettingName.NODE_LOG_DIR,
      false,
      "%H" + separator + "terracotta" + separator + "logs",
      NODE,
      extractor(Node::getNodeLogDir),
      setter((node, value) -> node.setNodeLogDir(Paths.get(value))),
      of(GET, SET, CONFIG),
      of(ACTIVES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.NODE_LOG_DIR, tuple2(key, value))
  ),
  NODE_BACKUP_DIR(SettingName.NODE_BACKUP_DIR,
      false,
      null,
      NODE,
      extractor(Node::getNodeBackupDir),
      setter((node, value) -> node.setNodeBackupDir(value == null ? null : Paths.get(value))),
      of(GET, SET, UNSET, CONFIG),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.NODE_BACKUP_DIR, tuple2(key, value))
  ),
  TC_PROPERTIES(SettingName.TC_PROPERTIES,
      true,
      null,
      NODE,
      extractor(Node::getTcProperties),
      mapSetter((node, tuple) -> {
        if (tuple.allNulls()) {
          node.clearTcProperties();
        } else if (tuple.t1 != null && tuple.t2 == null) {
          node.removeTcProperty(tuple.t1);
        } else if (tuple.t1 == null) {
          // tuple.t2 != null
          Stream.of(tuple.t2.split(",")).map(kv -> kv.split(":")).forEach(kv -> node.setTcProperty(kv[0], kv[1]));
        } else {
          // tuple.t1 != null && tuple.t2 != null
          node.setTcProperty(tuple.t1, tuple.t2);
        }
      }),
      of(GET, SET, UNSET, CONFIG),
      of(ACTIVES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> PROPS_VALIDATOR.accept(SettingName.TC_PROPERTIES, tuple2(key, value))
  ),
  NODE_LOGGER_OVERRIDES(SettingName.NODE_LOGGER_OVERRIDES,
      true,
      null,
      NODE,
      extractor(Node::getNodeLoggerOverrides),
      mapSetter((node, tuple) -> {
        if (tuple.allNulls()) {
          node.clearNodeLoggerOverrides();
        } else if (tuple.t1 != null && tuple.t2 == null) {
          node.removeNodeLoggerOverride(tuple.t1);
        } else if (tuple.t1 == null) {
          // tuple.t2 != null
          Stream.of(tuple.t2.split(",")).map(kv -> kv.split(":")).forEach(kv -> node.setNodeLoggerOverride(kv[0], Level.valueOf(kv[1].toUpperCase())));
        } else {
          // tuple.t1 != null && tuple.t2 != null
          node.setNodeLoggerOverride(tuple.t1, Level.valueOf(tuple.t2.toUpperCase()));
        }
      }),
      of(GET, SET, UNSET, CONFIG),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> LOGGER_LEVEL_VALIDATOR.accept(SettingName.NODE_LOGGER_OVERRIDES, tuple2(key, value))
  ),
  CLIENT_RECONNECT_WINDOW(SettingName.CLIENT_RECONNECT_WINDOW,
      false,
      "120s",
      CLUSTER,
      extractor(Node::getClientReconnectWindow),
      setter((node, value) -> node.setClientReconnectWindow(Measure.parse(value, TimeUnit.class))),
      of(GET, SET, CONFIG),
      of(ACTIVES_ONLINE),
      emptyList(),
      asList(SECONDS, MINUTES, HOURS),
      (key, value) -> TIME_VALIDATOR.accept(SettingName.CLIENT_RECONNECT_WINDOW, tuple2(key, value))
  ),
  FAILOVER_PRIORITY(SettingName.FAILOVER_PRIORITY,
      false,
      "availability",
      CLUSTER,
      extractor(Node::getFailoverPriority),
      setter((node, value) -> node.setFailoverPriority(FailoverPriority.valueOf(value))),
      of(GET, SET, CONFIG),
      of(ALL_NODES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> DEFAULT_VALIDATOR.andThen((k, v) -> FailoverPriority.valueOf(v.t2)).accept(SettingName.FAILOVER_PRIORITY, tuple2(key, value))
  ),

  // ==== Lease

  CLIENT_LEASE_DURATION(SettingName.CLIENT_LEASE_DURATION,
      false,
      "150s",
      CLUSTER,
      extractor(Node::getClientLeaseDuration),
      setter((node, value) -> node.setClientLeaseDuration(Measure.parse(value, TimeUnit.class))),
      of(GET, SET, CONFIG),
      of(ACTIVES_ONLINE),
      emptyList(),
      asList(MILLISECONDS, SECONDS, MINUTES, HOURS),
      (key, value) -> TIME_VALIDATOR.accept(SettingName.CLIENT_LEASE_DURATION, tuple2(key, value))
  ),

  // ==== License update

  LICENSE_FILE(SettingName.LICENSE_FILE,
      false,
      null,
      CLUSTER,
      node -> {
        throw new UnsupportedOperationException("Unable to get a license file from a node");
      },
      unsupported(),
      of(SET),
      of(ALL_NODES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.LICENSE_FILE, tuple2(key, value))
  ),

  // ==== Security

  SECURITY_DIR(SettingName.SECURITY_DIR,
      false,
      null,
      NODE,
      extractor(Node::getSecurityDir),
      setter((node, value) -> node.setSecurityDir(value == null ? null : Paths.get(value))),
      of(GET, SET, UNSET, CONFIG),
      of(ALL_NODES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.SECURITY_DIR, tuple2(key, value))
  ),
  SECURITY_AUDIT_LOG_DIR(SettingName.SECURITY_AUDIT_LOG_DIR,
      false,
      null,
      NODE,
      extractor(Node::getSecurityAuditLogDir),
      setter((node, value) -> node.setSecurityAuditLogDir(value == null ? null : Paths.get(value))),
      of(GET, SET, UNSET, CONFIG),
      of(ALL_NODES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.SECURITY_AUDIT_LOG_DIR, tuple2(key, value))
  ),
  SECURITY_AUTHC(SettingName.SECURITY_AUTHC,
      false,
      null,
      CLUSTER,
      extractor(Node::getSecurityAuthc),
      setter(Node::setSecurityAuthc),
      of(GET, SET, UNSET, CONFIG),
      of(ALL_NODES_ONLINE, RESTART),
      asList("file", "ldap", "certificate")
  ),
  SECURITY_SSL_TLS(SettingName.SECURITY_SSL_TLS,
      false,
      "false",
      CLUSTER,
      extractor(Node::isSecuritySslTls),
      setter((node, value) -> node.setSecuritySslTls(Boolean.parseBoolean(value))),
      of(GET, SET, CONFIG),
      of(ALL_NODES_ONLINE, RESTART),
      asList("true", "false")
  ),
  SECURITY_WHITELIST(SettingName.SECURITY_WHITELIST,
      false,
      "false",
      CLUSTER,
      extractor(Node::isSecurityWhitelist),
      setter((node, value) -> node.setSecurityWhitelist(Boolean.parseBoolean(value))),
      of(GET, SET, CONFIG),
      of(ALL_NODES_ONLINE, RESTART),
      asList("true", "false")
  ),

  // ==== Resources configuration

  OFFHEAP_RESOURCES(SettingName.OFFHEAP_RESOURCES,
      true,
      "main:512MB",
      CLUSTER,
      extractor(Node::getOffheapResources),
      mapSetter((node, tuple) -> {
        if (tuple.allNulls()) {
          node.clearOffheapResources();
        } else if (tuple.t1 != null && tuple.t2 == null) {
          node.removeOffheapResource(tuple.t1);
        } else if (tuple.t1 == null) {
          // tuple.t2 != null
          Stream.of(tuple.t2.split(",")).map(kv -> kv.split(":")).forEach(kv -> node.setOffheapResource(kv[0], Measure.parse(kv[1], MemoryUnit.class)));
        } else {
          // tuple.t1 != null && tuple.t2 != null
          node.setOffheapResource(tuple.t1, Measure.parse(tuple.t2, MemoryUnit.class));
        }
      }),
      of(GET, SET, UNSET, CONFIG),
      of(ACTIVES_ONLINE),
      emptyList(),
      asList(MemoryUnit.values()),
      (key, value) -> OFFHEAP_VALIDATOR.accept(SettingName.OFFHEAP_RESOURCES, tuple2(key, value))
  ),
  DATA_DIRS(SettingName.DATA_DIRS,
      true,
      "main:%H" + separator + "terracotta" + separator + "user-data" + separator + "main",
      NODE,
      extractor(Node::getDataDirs),
      mapSetter((node, tuple) -> {
        if (tuple.allNulls()) {
          node.clearDataDirs();
        } else if (tuple.t1 != null && tuple.t2 == null) {
          node.removeDataDir(tuple.t1);
        } else if (tuple.t1 == null) {
          // tuple.t2 != null
          Stream.of(tuple.t2.split(",")).forEach(kv -> {
            int firstColon = kv.indexOf(":");
            node.setDataDir(kv.substring(0, firstColon), Paths.get(kv.substring(firstColon + 1)));
          });
        } else {
          // tuple.t1 != null && tuple.t2 != null
          node.setDataDir(tuple.t1, Paths.get(tuple.t2));
        }
      }),
      of(GET, SET, UNSET, CONFIG),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> DATA_DIRS_VALIDATOR.accept(SettingName.DATA_DIRS, tuple2(key, value))
  );

  private final String name;
  private final boolean map;
  private final String defaultValue;
  private final Scope scope;
  private final Function<Node, Stream<Tuple2<String, String>>> extractor;
  private final Collection<Operation> operations;
  private final Collection<Requirement> requirements;
  private final Collection<String> allowedValues;
  private final Collection<? extends Enum<?>> allowedUnits;
  private final BiConsumer<String, String> validator;
  private final BiConsumer<Node, Tuple2<String, String>> setter;

  Setting(String name,
          boolean map,
          String defaultValue,
          Scope scope,
          Function<Node, Stream<Tuple2<String, String>>> extractor,
          BiConsumer<Node, Tuple2<String, String>> setter,
          EnumSet<Operation> operations) {
    this(name, map, defaultValue, scope, extractor, setter, operations, noneOf(Requirement.class));
  }

  Setting(String name,
          boolean map,
          String defaultValue,
          Scope scope,
          Function<Node, Stream<Tuple2<String, String>>> extractor,
          BiConsumer<Node, Tuple2<String, String>> setter,
          EnumSet<Operation> operations,
          EnumSet<Requirement> requirements) {
    this(name, map, defaultValue, scope, extractor, setter, operations, requirements, emptyList(), emptyList());
  }

  Setting(String name,
          boolean map,
          String defaultValue,
          Scope scope,
          Function<Node, Stream<Tuple2<String, String>>> extractor,
          BiConsumer<Node, Tuple2<String, String>> setter,
          EnumSet<Operation> operations,
          EnumSet<Requirement> requirements,
          Collection<String> allowedValues) {
    this(name, map, defaultValue, scope, extractor, setter, operations, requirements, allowedValues, emptyList());
  }

  Setting(String name,
          boolean map,
          String defaultValue,
          Scope scope,
          Function<Node, Stream<Tuple2<String, String>>> extractor,
          BiConsumer<Node, Tuple2<String, String>> setter,
          EnumSet<Operation> operations,
          EnumSet<Requirement> requirements,
          Collection<String> allowedValues,
          Collection<? extends Enum<?>> allowedUnits) {
    this(name, map, defaultValue, scope, extractor, setter, operations, requirements, allowedValues, allowedUnits, (key, value) -> DEFAULT_VALIDATOR.accept(name, tuple2(key, value)));
  }

  Setting(String name,
          boolean map,
          String defaultValue,
          Scope scope,
          Function<Node, Stream<Tuple2<String, String>>> extractor,
          BiConsumer<Node, Tuple2<String, String>> setter,
          EnumSet<Operation> operations,
          EnumSet<Requirement> requirements,
          Collection<String> allowedValues,
          Collection<? extends Enum<?>> allowedUnits,
          BiConsumer<String, String> validator) {
    this.name = name;
    this.map = map;
    this.defaultValue = defaultValue;
    this.extractor = extractor;
    this.setter = setter;
    this.operations = operations;
    this.scope = scope;
    this.requirements = requirements;
    this.allowedValues = Collections.unmodifiableSet(new LinkedHashSet<>(allowedValues));
    this.allowedUnits = Collections.unmodifiableSet(new LinkedHashSet<>(allowedUnits));
    this.validator = validator;

    if ((operations.contains(SET) || operations.contains(UNSET)) && !requirements.contains(ACTIVES_ONLINE) && !requirements.contains(ALL_NODES_ONLINE)) {
      throw new AssertionError("Invalid definition of setting " + name + ": settings supporting mutative operations require either " + ACTIVES_ONLINE + " or " + ALL_NODES_ONLINE);
    }

    if (scope == STRIPE) {
      throw new AssertionError("Invalid scope for setting definition: " + name + ". Must be " + NODE + " or " + CLUSTER);
    }
  }

  @Override
  public String toString() {
    return name;
  }

  public boolean isMap() {
    return map;
  }

  public Scope getScope() {
    return scope;
  }

  public void fillDefault(Node node) {
    String v = getDefaultValue();
    if (v != null && !getProperty(node).isPresent()) {
      setProperty(node, v);
    }
  }

  public String getDefaultValue() {
    if (this == NODE_NAME) {
      // special case for node name where the default value always changes
      return "node-" + Uuid.generateShortUuid();
    }
    return defaultValue;
  }

  public Collection<String> getAllowedValues() {
    return allowedValues;
  }

  @SuppressWarnings("unchecked")
  public <U extends Enum<U> & Unit<U>> Collection<U> getAllowedUnits() {
    return (Collection<U>) allowedUnits;
  }

  public boolean requires(Requirement requirement) {
    return requirements.contains(requirement);
  }

  /**
   * @return true if this setting supports some operations (get, set, unset, config) to be called with a scope passed as parameter.
   * Example: node-name is defined as scope NODE, but we could execute "get node-name"
   */
  public boolean allowsAnyOperationInScope(Scope scope) {
    if (operations.isEmpty()) {
      return false;
    }
    if (allowsOperation(SET) || allowsOperation(UNSET) || allowsOperation(GET)) {
      if (this.scope == CLUSTER) {
        // if the setting scope is CLUSTER, we only allow commands to work at the same level
        return scope == CLUSTER;
      }
      if (this.scope == NODE) {
        // if a setting is at node scope, we allow "batch" get or set of settings over all the cluster or stripe
        return true;
      }
    } else if (allowsOperation(CONFIG)) {
      return scope == this.scope;
    }
    // this.scope cannot be STRIPE (validation in constructor)
    throw new AssertionError("Invalid scope for setting definition: " + this + ". Must be " + NODE + " or " + CLUSTER);
  }

  public boolean allowsOperation(Operation operation) {
    return this.operations.contains(operation);
  }

  public boolean allowsOperationInScope(Operation operation, Scope scope) {
    if (!allowsOperation(operation) || !allowsAnyOperationInScope(scope)) {
      return false;
    }
    switch (operation) {
      case GET:
        return true; // allow any scope for get
      case CONFIG:
        return scope == this.scope;
      case SET:
      case UNSET:
        return this.scope == CLUSTER ? scope == CLUSTER : this.scope == NODE; // note: this.scope cannot be STRIPE
      default:
        throw new AssertionError(operation);
    }
  }

  public boolean isRequired() {
    return !allowsOperation(UNSET) && (!allowsOperation(CONFIG) || getDefaultValue() != null);
  }

  public boolean isReadOnly() {
    return !allowsOperation(SET) && !allowsOperation(CONFIG) && !allowsOperation(UNSET);
  }

  public boolean isScope(Scope scope) {
    return this.scope == scope;
  }

  public String getConfigPrefix(int stripeId, int nodeId) {
    return (isScope(NODE) ? "stripe." + stripeId + ".node." + nodeId + "." : "") + this;
  }

  public void validate(String key, String value) {
    // do not validate if value is null and setting optional
    if (key == null && value == null && !isRequired()) {
      return;
    }
    validator.accept(key, value);
  }

  public void validate(String value) {
    validate(null, value);
  }

  public Optional<String> getProperty(Node node) {
    return extractor.apply(node)
        .filter(tuple -> !tuple.allNulls())
        .map(tuple -> tuple.t1 == null ? tuple.t2 : (tuple.t1 + ":" + tuple.t2))
        .reduce((result, element) -> result + "," + element);
  }

  public Optional<String> getProperty(NodeContext nodeContext) {
    if (this == CLUSTER_NAME) {
      return Optional.ofNullable(nodeContext.getCluster().getName());
    }
    return getProperty(nodeContext.getNode());
  }

  public Stream<Tuple2<String, String>> getExpandedProperties(NodeContext nodeContext) {
    if (!isMap()) {
      throw new UnsupportedOperationException();
    }
    return extractor.apply(nodeContext.getNode()).filter(tuple -> tuple.t1 != null);
  }

  public void setProperty(Node node, String value) {
    setProperty(node, null, value);
  }

  public void setProperty(Node node, String key, String value) {
    if (isReadOnly()) {
      throw new IllegalArgumentException("Setting: " + this + " is read-only");
    }
    validate(key, value);
    this.setter.accept(node, tuple2(key, value));
  }

  public boolean allowsValue(String value) {
    return this.allowedValues.isEmpty() || this.allowedValues.contains(value);
  }

  public boolean requiresEagerSubstitution() {
    return this == NODE_HOSTNAME;
  }

  public static Setting fromName(String name) {
    return findSetting(name).orElseThrow(() -> new IllegalArgumentException("Illegal setting name: " + name));
  }

  public static Optional<Setting> findSetting(String name) {
    return Stream.of(values()).filter(setting -> setting.name.equals(name)).findFirst();
  }

  @SuppressWarnings("unchecked")
  private static Function<Node, Stream<Tuple2<String, String>>> extractor(Function<Node, Object> extractor) {
    return node -> {
      Object o = extractor.apply(node);
      if (o == null) {
        return Stream.empty();
      }
      if (o instanceof Map) {
        return ((Map<String, ?>) o).entrySet()
            .stream()
            .filter(e -> e.getValue() != null)
            .sorted(Map.Entry.comparingByKey())
            .map(e -> tuple2(e.getKey(), e.getValue().toString()));
      }
      return Stream.of(tuple2(null, String.valueOf(o)));
    };
  }

  private static BiConsumer<Node, Tuple2<String, String>> setter(BiConsumer<Node, String> setter) {
    return (node, tuple) -> {
      if (tuple.t1 != null) {
        throw new IllegalArgumentException("Key must be null: parameter is not a map");
      }
      setter.accept(node, tuple.t2 == null || tuple.t2.trim().isEmpty() ? null : tuple.t2.trim());
    };
  }

  private static BiConsumer<Node, Tuple2<String, String>> mapSetter(BiConsumer<Node, Tuple2<String, String>> setter) {
    return (node, tuple) -> setter.accept(node, tuple2(tuple.t1, tuple.t2 == null || tuple.t2.trim().isEmpty() ? null : tuple.t2.trim()));
  }

  private static <U, V> BiConsumer<U, V> unsupported() {
    return (u, v) -> {
      throw new UnsupportedOperationException();
    };
  }
}
