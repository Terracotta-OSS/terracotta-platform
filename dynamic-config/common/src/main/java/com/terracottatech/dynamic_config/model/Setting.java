/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import com.terracottatech.utilities.Tuple2;
import com.terracottatech.utilities.Unit;

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

import static com.terracottatech.dynamic_config.DynamicConfigConstants.MULTI_VALUE_SEP;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.PARAM_INTERNAL_SEP;
import static com.terracottatech.dynamic_config.model.Operation.GET;
import static com.terracottatech.dynamic_config.model.Operation.SET;
import static com.terracottatech.dynamic_config.model.Operation.UNSET;
import static com.terracottatech.dynamic_config.model.Requirement.ACTIVES_ONLINE;
import static com.terracottatech.dynamic_config.model.Requirement.ALL_NODES_ONLINE;
import static com.terracottatech.dynamic_config.model.Requirement.RESTART;
import static com.terracottatech.dynamic_config.model.Scope.CLUSTER;
import static com.terracottatech.dynamic_config.model.Scope.NODE;
import static com.terracottatech.dynamic_config.model.validation.SettingValidator.ADDRESS_VALIDATOR;
import static com.terracottatech.dynamic_config.model.validation.SettingValidator.DATA_DIRS_VALIDATOR;
import static com.terracottatech.dynamic_config.model.validation.SettingValidator.DEFAULT;
import static com.terracottatech.dynamic_config.model.validation.SettingValidator.HOST_VALIDATOR;
import static com.terracottatech.dynamic_config.model.validation.SettingValidator.OFFHEAP_VALIDATOR;
import static com.terracottatech.dynamic_config.model.validation.SettingValidator.PORT_VALIDATOR;
import static com.terracottatech.dynamic_config.model.validation.SettingValidator.TIME_VALIDATOR;
import static com.terracottatech.utilities.Assertion.assertNull;
import static com.terracottatech.utilities.TimeUnit.HOURS;
import static com.terracottatech.utilities.TimeUnit.MILLISECONDS;
import static com.terracottatech.utilities.TimeUnit.MINUTES;
import static com.terracottatech.utilities.TimeUnit.SECONDS;
import static com.terracottatech.utilities.Tuple2.tuple2;
import static java.io.File.separator;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.noneOf;
import static java.util.EnumSet.of;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * See API doc Config-tool.adoc
 *
 * @author Mathieu Carbou
 */
//TODO [DYNAMIC-CONFIG]: TDB-4601: For supported setting, change from ALL_NODES_ONLINE to ACTIVES_ONLINE
public enum Setting {

  // ==== Settings applied to a specific node only

  NODE_NAME(SettingName.NODE_NAME,
      false,
      null,
      extractor(Node::getNodeName),
      setter(SettingName.NODE_NAME, Node::setNodeName),
      unsupported(),
      of(GET)
  ),
  NODE_HOSTNAME(SettingName.NODE_HOSTNAME,
      false,
      "%h",
      extractor(Node::getNodeHostname),
      setter(SettingName.NODE_HOSTNAME, Node::setNodeHostname),
      unsupported(),
      of(GET, SET),
      of(NODE),
      of(ALL_NODES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> HOST_VALIDATOR.accept(SettingName.NODE_HOSTNAME, tuple2(key, value))
  ),
  NODE_PORT(SettingName.NODE_PORT,
      false,
      "9410",
      extractor(Node::getNodePort),
      setter(SettingName.NODE_PORT, (node, value) -> node.setNodePort(Integer.parseInt(value))),
      unsupported(),
      of(GET, SET),
      of(NODE),
      of(ACTIVES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> PORT_VALIDATOR.accept(SettingName.NODE_PORT, tuple2(key, value))
  ),
  NODE_GROUP_PORT(SettingName.NODE_GROUP_PORT,
      false,
      "9430",
      extractor(Node::getNodeGroupPort),
      setter(SettingName.NODE_GROUP_PORT, (node, value) -> node.setNodeGroupPort(Integer.parseInt(value))),
      unsupported(),
      of(GET, SET),
      of(NODE),
      of(ACTIVES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> PORT_VALIDATOR.accept(SettingName.NODE_GROUP_PORT, tuple2(key, value))
  ),
  NODE_BIND_ADDRESS(SettingName.NODE_BIND_ADDRESS,
      false,
      "0.0.0.0",
      extractor(Node::getNodeBindAddress),
      setter(SettingName.NODE_BIND_ADDRESS, Node::setNodeBindAddress),
      unsupported(),
      of(GET, SET),
      of(NODE),
      of(ACTIVES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> ADDRESS_VALIDATOR.accept(SettingName.NODE_BIND_ADDRESS, tuple2(key, value))
  ),
  NODE_GROUP_BIND_ADDRESS(SettingName.NODE_GROUP_BIND_ADDRESS,
      false,
      "0.0.0.0",
      extractor(Node::getNodeGroupBindAddress),
      setter(SettingName.NODE_GROUP_BIND_ADDRESS, Node::setNodeGroupBindAddress),
      unsupported(),
      of(GET, SET),
      of(NODE),
      of(ALL_NODES_ONLINE, RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> ADDRESS_VALIDATOR.accept(SettingName.NODE_GROUP_BIND_ADDRESS, tuple2(key, value))
  ),

  // ==== Node configuration

  CLUSTER_NAME(SettingName.CLUSTER_NAME,
      false,
      null,
      node -> {
        throw new UnsupportedOperationException("Unable to get a cluster name from a node");
      },
      unsupported(),
      unsupported(),
      of(GET, SET),
      of(CLUSTER),
      of(ALL_NODES_ONLINE, RESTART)
  ),
  NODE_REPOSITORY_DIR(SettingName.NODE_REPOSITORY_DIR,
      false,
      "%H" + separator + "terracotta" + separator + "repository",
      node -> {
        throw new UnsupportedOperationException("Unable to get the repository directory from a node");
      },
      unsupported(),
      unsupported(),
      of(GET)
  ),
  NODE_METADATA_DIR(SettingName.NODE_METADATA_DIR,
      false,
      "%H" + separator + "terracotta" + separator + "metadata",
      extractor(Node::getNodeMetadataDir),
      setter(SettingName.NODE_METADATA_DIR, (node, value) -> node.setNodeMetadataDir(Paths.get(value))),
      unsupported(),
      of(GET, SET),
      allOf(Scope.class),
      of(ACTIVES_ONLINE, RESTART)
  ),
  NODE_LOG_DIR(SettingName.NODE_LOG_DIR,
      false,
      "%H" + separator + "terracotta" + separator + "logs",
      extractor(Node::getNodeLogDir),
      setter(SettingName.NODE_LOG_DIR, (node, value) -> node.setNodeLogDir(Paths.get(value))),
      unsupported(),
      of(GET, SET),
      allOf(Scope.class),
      of(ACTIVES_ONLINE, RESTART)
  ),
  NODE_BACKUP_DIR(SettingName.NODE_BACKUP_DIR,
      false,
      null,
      extractor(Node::getNodeBackupDir),
      setter(SettingName.NODE_BACKUP_DIR, (node, value) -> node.setNodeBackupDir(Paths.get(value))),
      (node, key) -> node.setNodeBackupDir(null),
      of(GET, SET, UNSET),
      allOf(Scope.class),
      of(ACTIVES_ONLINE, RESTART)
  ),
  TC_PROPERTIES(SettingName.TC_PROPERTIES,
      true,
      null,
      extractor(Node::getTcProperties),
      mapSetter(SettingName.TC_PROPERTIES, (node, tuple) -> {
        String value = tuple.t2;
        if (tuple.t1 == null) {
          Stream.of(value.split(MULTI_VALUE_SEP)).map(kv -> kv.split(PARAM_INTERNAL_SEP)).forEach(kv -> node.setTcProperty(kv[0], kv[1]));
        } else {
          node.setTcProperty(tuple.t1, value);
        }
      }),
      (node, key) -> {
        if (key == null) {
          node.clearTcProperties();
        } else {
          node.removeTcProperty(key);
        }
      },
      of(GET, SET, UNSET),
      allOf(Scope.class),
      of(ACTIVES_ONLINE, RESTART)
  ),
  CLIENT_RECONNECT_WINDOW(SettingName.CLIENT_RECONNECT_WINDOW,
      false,
      "120s",
      extractor(Node::getClientReconnectWindow),
      setter(SettingName.CLIENT_RECONNECT_WINDOW, (node, value) -> node.setClientReconnectWindow(Measure.parse(value, TimeUnit.class))),
      unsupported(),
      of(GET, SET),
      of(CLUSTER),
      of(ACTIVES_ONLINE, RESTART),
      emptyList(),
      asList(SECONDS, MINUTES, HOURS),
      (key, value) -> TIME_VALIDATOR.accept(SettingName.CLIENT_RECONNECT_WINDOW, tuple2(key, value))
  ),
  FAILOVER_PRIORITY(SettingName.FAILOVER_PRIORITY,
      false,
      "availability",
      extractor(Node::getFailoverPriority),
      setter(SettingName.FAILOVER_PRIORITY, (node, value) -> node.setFailoverPriority(FailoverPriority.valueOf(value))),
      unsupported(),
      of(GET, SET),
      of(CLUSTER),
      of(ACTIVES_ONLINE, RESTART),
      asList("availability", "consistency"),
      emptyList(),
      (key, value) -> FailoverPriority.valueOf(value)
  ),

  // ==== Lease

  CLIENT_LEASE_DURATION(SettingName.CLIENT_LEASE_DURATION,
      false,
      "20s",
      extractor(Node::getClientLeaseDuration),
      setter(SettingName.CLIENT_LEASE_DURATION, (node, value) -> node.setClientLeaseDuration(Measure.parse(value, TimeUnit.class))),
      unsupported(),
      of(GET, SET),
      of(CLUSTER),
      of(ACTIVES_ONLINE),
      emptyList(),
      asList(MILLISECONDS, SECONDS, MINUTES, HOURS),
      (key, value) -> TIME_VALIDATOR.accept(SettingName.CLIENT_LEASE_DURATION, tuple2(key, value))
  ),

  // ==== License update

  LICENSE_FILE(SettingName.LICENSE_FILE,
      false,
      null,
      node -> {
        throw new UnsupportedOperationException("Unable to get a license file from a node");
      },
      unsupported(),
      unsupported(),
      of(SET),
      of(CLUSTER),
      of(ALL_NODES_ONLINE)
  ),

  // ==== Security

  SECURITY_DIR(SettingName.SECURITY_DIR,
      false,
      null,
      extractor(Node::getSecurityDir),
      setter(SettingName.SECURITY_DIR, (node, value) -> node.setSecurityDir(Paths.get(value))),
      (node, key) -> node.setSecurityDir(null),
      of(GET, SET, UNSET),
      allOf(Scope.class),
      of(ALL_NODES_ONLINE, RESTART)
  ),
  SECURITY_AUDIT_LOG_DIR(SettingName.SECURITY_AUDIT_LOG_DIR,
      false,
      null,
      extractor(Node::getSecurityAuditLogDir),
      setter(SettingName.SECURITY_AUDIT_LOG_DIR, (node, value) -> node.setSecurityAuditLogDir(Paths.get(value))),
      (node, key) -> node.setSecurityAuditLogDir(null),
      of(GET, SET, UNSET),
      allOf(Scope.class),
      of(ALL_NODES_ONLINE)
  ),
  SECURITY_AUTHC(SettingName.SECURITY_AUTHC,
      false,
      null,
      extractor(Node::getSecurityAuthc),
      setter(SettingName.SECURITY_AUTHC, Node::setSecurityAuthc),
      (node, key) -> node.setSecurityAuthc(null),
      of(GET, SET, UNSET),
      of(CLUSTER),
      of(ALL_NODES_ONLINE, RESTART),
      asList("file", "ldap", "certificate")
  ),
  SECURITY_SSL_TLS(SettingName.SECURITY_SSL_TLS,
      false,
      "false",
      extractor(Node::isSecuritySslTls),
      setter(SettingName.SECURITY_SSL_TLS, (node, value) -> node.setSecuritySslTls(Boolean.parseBoolean(value))),
      unsupported(),
      of(GET, SET),
      of(CLUSTER),
      of(ALL_NODES_ONLINE, RESTART),
      asList("true", "false")
  ),
  SECURITY_WHITELIST(SettingName.SECURITY_WHITELIST,
      false,
      "false",
      extractor(Node::isSecurityWhitelist),
      setter(SettingName.SECURITY_WHITELIST, (node, value) -> node.setSecurityWhitelist(Boolean.parseBoolean(value))),
      unsupported(),
      of(GET, SET),
      of(CLUSTER),
      of(ALL_NODES_ONLINE),
      asList("true", "false")
  ),

  // ==== Resources configuration

  OFFHEAP_RESOURCES(SettingName.OFFHEAP_RESOURCES,
      true,
      "main:512MB",
      extractor(Node::getOffheapResources),
      mapSetter(SettingName.OFFHEAP_RESOURCES, (node, tuple) -> {
        String value = tuple.t2;
        if (tuple.t1 == null) {
          Stream.of(value.split(MULTI_VALUE_SEP)).map(kv -> kv.split(PARAM_INTERNAL_SEP)).forEach(kv -> node.setOffheapResource(kv[0], Measure.parse(kv[1], MemoryUnit.class)));
        } else {
          node.setOffheapResource(tuple.t1, Measure.parse(tuple.t2, MemoryUnit.class));
        }
      }),
      (node, key) -> {
        if (key == null) {
          node.clearOffheapResources();
        } else {
          node.removeOffheapResource(key);
        }
      },
      of(GET, SET, UNSET),
      of(CLUSTER),
      of(ACTIVES_ONLINE),
      emptyList(),
      asList(MemoryUnit.values()),
      (key, value) -> OFFHEAP_VALIDATOR.accept(SettingName.OFFHEAP_RESOURCES, tuple2(key, value))
  ),
  DATA_DIRS(SettingName.DATA_DIRS,
      true,
      "main:%H" + separator + "terracotta" + separator + "user-data" + separator + "main",
      extractor(Node::getDataDirs),
      mapSetter(SettingName.DATA_DIRS, (node, tuple) -> {
        String value = tuple.t2;
        if (tuple.t1 == null) {
          Stream.of(value.split(MULTI_VALUE_SEP)).forEach(kv -> {
            int firstColon = kv.indexOf(PARAM_INTERNAL_SEP);
            node.setDataDir(kv.substring(0, firstColon), Paths.get(kv.substring(firstColon + 1)));
          });
        } else {
          node.setDataDir(tuple.t1, Paths.get(tuple.t2));
        }
      }),
      (node, key) -> {
        if (key == null) {
          node.clearDataDirs();
        } else {
          node.removeDataDir(key);
        }
      },
      of(GET, SET, UNSET),
      of(NODE, CLUSTER),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> DATA_DIRS_VALIDATOR.accept(SettingName.DATA_DIRS, tuple2(key, value))
  );

  private final String name;
  private final boolean map;
  private final String defaultValue;
  private final Function<Node, Stream<Tuple2<String, String>>> extractor;
  private final Collection<Operation> operations;
  private final Collection<Requirement> requirements;
  private final Collection<Scope> scopes;
  private final Collection<String> allowedValues;
  private final Collection<? extends Enum<?>> allowedUnits;
  private final BiConsumer<String, String> validator;
  private final BiConsumer<Node, Tuple2<String, String>> setter;
  private final BiConsumer<Node, String> unsetter;

  Setting(String name, boolean map, String defaultValue, Function<Node, Stream<Tuple2<String, String>>> extractor, BiConsumer<Node, Tuple2<String, String>> setter, BiConsumer<Node, String> unsetter, EnumSet<Operation> operations) {
    this(name, map, defaultValue, extractor, setter, unsetter, operations, allOf(Scope.class), noneOf(Requirement.class));
  }

  Setting(String name, boolean map, String defaultValue, Function<Node, Stream<Tuple2<String, String>>> extractor, BiConsumer<Node, Tuple2<String, String>> setter, BiConsumer<Node, String> unsetter, EnumSet<Operation> operations, EnumSet<Scope> scopes, EnumSet<Requirement> requirements) {
    this(name, map, defaultValue, extractor, setter, unsetter, operations, scopes, requirements, emptyList(), emptyList());
  }

  Setting(String name, boolean map, String defaultValue, Function<Node, Stream<Tuple2<String, String>>> extractor, BiConsumer<Node, Tuple2<String, String>> setter, BiConsumer<Node, String> unsetter, EnumSet<Operation> operations, EnumSet<Scope> scopes, EnumSet<Requirement> requirements, Collection<String> allowedValues) {
    this(name, map, defaultValue, extractor, setter, unsetter, operations, scopes, requirements, allowedValues, emptyList());
  }

  Setting(String name, boolean map, String defaultValue, Function<Node, Stream<Tuple2<String, String>>> extractor, BiConsumer<Node, Tuple2<String, String>> setter, BiConsumer<Node, String> unsetter, EnumSet<Operation> operations, EnumSet<Scope> scopes, EnumSet<Requirement> requirements, Collection<String> allowedValues, Collection<? extends Enum<?>> allowedUnits) {
    this(name, map, defaultValue, extractor, setter, unsetter, operations, scopes, requirements, allowedValues, allowedUnits, (key, value) -> DEFAULT.accept(name, tuple2(key, value)));
  }

  Setting(String name, boolean map, String defaultValue, Function<Node, Stream<Tuple2<String, String>>> extractor, BiConsumer<Node, Tuple2<String, String>> setter, BiConsumer<Node, String> unsetter, EnumSet<Operation> operations, EnumSet<Scope> scopes, EnumSet<Requirement> requirements, Collection<String> allowedValues, Collection<? extends Enum<?>> allowedUnits, BiConsumer<String, String> validator) {
    this.name = name;
    this.map = map;
    this.defaultValue = defaultValue;
    this.extractor = extractor;
    this.setter = setter;
    this.unsetter = unsetter;
    this.operations = operations;
    this.scopes = scopes;
    this.requirements = requirements;
    this.allowedValues = Collections.unmodifiableSet(new LinkedHashSet<>(allowedValues));
    this.allowedUnits = Collections.unmodifiableSet(new LinkedHashSet<>(allowedUnits));
    this.validator = validator;

    if ((operations.contains(SET) || operations.contains(UNSET)) && !requirements.contains(ACTIVES_ONLINE) && !requirements.contains(ALL_NODES_ONLINE)) {
      throw new AssertionError("Invalid definition of setting " + name + ": settings supporting mutative operations require either " + ACTIVES_ONLINE + " or " + ALL_NODES_ONLINE);
    }
  }

  @Override
  public String toString() {
    return name;
  }

  public boolean isMap() {
    return map;
  }

  public void fillDefault(Node node) {
    String v = getDefaultValue();
    if (v != null) {
      setProperty(node, v);
    }
  }

  public String getDefaultValue() {
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

  public boolean allowsOperation(Operation operation) {
    return this.operations.isEmpty() || this.operations.contains(operation);
  }

  public boolean allowsScope(Scope scope) {
    return this.scopes.isEmpty() || this.scopes.contains(scope);
  }

  public void validate(String key, String value) {
    validator.accept(key, value);
  }

  public void validate(String value) {
    validate(null, value);
  }

  public Optional<String> getPropertyValue(Node node) {
    return extractor.apply(node)
        .map(tuple -> tuple.t1 == null ? tuple.t2 : (tuple.t1 + ":" + tuple.t2))
        .reduce((result, element) -> result + "," + element);
  }

  public Stream<Tuple2<String, String>> getExpandedProperties(Node node) {
    if (!isMap()) {
      throw new UnsupportedOperationException();
    }
    return extractor.apply(node).filter(tuple -> tuple.t1 != null);
  }

  public void setProperty(Node node, String value) {
    setProperty(node, null, value);
  }

  public void setProperty(Node node, String key, String value) {
    this.setter.accept(node, tuple2(key, value));
  }

  public void unsetProperty(Node node, String key) {
    this.unsetter.accept(node, key);
  }

  public boolean allowsValue(String value) {
    return this.allowedValues.isEmpty() || this.allowedValues.contains(value);
  }

  public boolean requiresEagerSubstitution() {
    return this == NODE_HOSTNAME;
  }

  public <U extends Enum<U> & Unit<U>> boolean allowsUnit(U unit) {
    return this.allowedUnits.isEmpty() || this.allowedUnits.contains(unit);
  }

  public static Setting fromName(String name) {
    return Stream.of(values()).filter(setting -> setting.name.equals(name)).findFirst().orElseThrow(() -> new IllegalArgumentException("Illegal setting name: " + name));
  }

  public static Collection<Setting> getAll() {
    return asList(Setting.values());
  }

  public static Collection<String> getAllNames() {
    return Stream.of(Setting.values()).map(Setting::toString).collect(toList());
  }

  private static Function<Node, Stream<Tuple2<String, String>>> extractor(Function<Node, Object> extractor) {
    return node -> {
      Object o = extractor.apply(node);
      if (o == null) {
        return Stream.empty();
      }
      if (o instanceof Map) {
        return ((Map<?, ?>) o).entrySet()
            .stream()
            .filter(e -> e.getValue() != null)
            .map(e -> tuple2(e.getKey().toString(), e.getValue().toString()));
      }
      return Stream.of(tuple2(null, String.valueOf(o)));
    };
  }

  private static BiConsumer<Node, Tuple2<String, String>> setter(String paramName, BiConsumer<Node, String> setter) {
    return (node, tuple) -> {
      assertNull(tuple.t1, "Key must be null: parameter is not a map");
      requireNonNull(tuple.t2, "Null value for node parameter '" + paramName + "'");
      if (tuple.t2.trim().isEmpty()) {
        throw new IllegalArgumentException("Empty value for node parameter '" + paramName + "'");
      }
      setter.accept(node, tuple.t2);
    };
  }

  private static BiConsumer<Node, Tuple2<String, String>> mapSetter(String paramName, BiConsumer<Node, Tuple2<String, String>> setter) {
    return (node, tuple) -> {
      requireNonNull(tuple.t2, "Null value for node parameter '" + paramName + "'");
      if (tuple.t2.trim().isEmpty()) {
        throw new IllegalArgumentException("Empty value for node parameter '" + paramName + "'");
      }
      setter.accept(node, tuple);
    };
  }

  private static <U, V> BiConsumer<U, V> unsupported() {
    return (u, v) -> {
      throw new UnsupportedOperationException();
    };
  }
}
