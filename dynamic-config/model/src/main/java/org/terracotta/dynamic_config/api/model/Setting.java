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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.EnumSet.of;
import static java.util.Objects.requireNonNull;
import static org.terracotta.common.struct.MemoryUnit.MB;
import static org.terracotta.common.struct.TimeUnit.HOURS;
import static org.terracotta.common.struct.TimeUnit.MILLISECONDS;
import static org.terracotta.common.struct.TimeUnit.MINUTES;
import static org.terracotta.common.struct.TimeUnit.SECONDS;
import static org.terracotta.common.struct.Tuple2.tuple2;
import static org.terracotta.dynamic_config.api.model.ClusterState.ACTIVATED;
import static org.terracotta.dynamic_config.api.model.ClusterState.CONFIGURING;
import static org.terracotta.dynamic_config.api.model.Operation.GET;
import static org.terracotta.dynamic_config.api.model.Operation.IMPORT;
import static org.terracotta.dynamic_config.api.model.Operation.SET;
import static org.terracotta.dynamic_config.api.model.Operation.UNSET;
import static org.terracotta.dynamic_config.api.model.Permission.Builder.when;
import static org.terracotta.dynamic_config.api.model.Requirement.ACTIVES_ONLINE;
import static org.terracotta.dynamic_config.api.model.Requirement.ALL_NODES_ONLINE;
import static org.terracotta.dynamic_config.api.model.Requirement.CLUSTER_RESTART;
import static org.terracotta.dynamic_config.api.model.Requirement.CONFIG;
import static org.terracotta.dynamic_config.api.model.Requirement.HIDDEN;
import static org.terracotta.dynamic_config.api.model.Requirement.NODE_RESTART;
import static org.terracotta.dynamic_config.api.model.Requirement.PRESENCE;
import static org.terracotta.dynamic_config.api.model.Requirement.RESOLVE_EAGERLY;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Scope.STRIPE;
import static org.terracotta.dynamic_config.api.model.SettingValidator.ADDRESS_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.DATA_DIRS_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.DEFAULT_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.HOST_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.LOGGER_LEVEL_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.NAME_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.OFFHEAP_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.PATH_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.PORT_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.PROPS_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.SettingValidator.TIME_VALIDATOR;
import static org.terracotta.dynamic_config.api.model.Version.V1;
import static org.terracotta.dynamic_config.api.model.Version.V2;

/**
 * <pre>
 *  Here is below a summary of all the settings and their permissions, which can be generated with the following code:
 *
 *  Stream.of(Setting.values()).collect(Collectors.groupingBy(Setting::getPermissions)).entrySet().forEach(System.out::println);
 *
 *  config-dir
 *  No permission
 *
 *  license-file
 *  Permission: when: [activated, configuring] allow: [set] at levels: [cluster]
 *
 *  metadata-dir
 *    * Permission: when: [configuring] allow: [import] at levels: [node],
 *    * Permission: when: [configuring] allow: [set, get] at levels: [node, stripe, cluster],
 *    * Permission: when: [activated] allow: [get] at levels: [node, stripe, cluster]
 *
 *  hostname, port
 *    * Permission: when: [activated, configuring] allow: [get] at levels: [node, stripe, cluster],
 *    * Permission: when: [configuring] allow: [import] at levels: [node]
 *
 *  group-port, bind-address, group-bind-address
 *    * Permission: when: [activated, configuring] allow: [get] at levels: [node, stripe, cluster],
 *    * Permission: when: [configuring] allow: [set] at levels: [node, stripe, cluster],
 *    * Permission: when: [configuring] allow: [import] at levels: [node]
 *
 *  data-dirs
 *    * Permission: when: [configuring] allow: [import] at levels: [node]
 *    * Permission: when: [configuring] allow: [set, get, unset] at levels: [node, stripe, cluster]
 *    * Permission: when: [activated, configuring] allow: [set, get] at levels: [node, stripe, cluster]
 *
 *  name
 *    * Permission: when: [activated, configuring] allow: [get] at levels: [node, stripe, cluster],
 *    * Permission: when: [configuring] allow: [set, import] at levels: [node]
 *
 *  public-hostname, public-port, backup-dir, tc-properties, logger-overrides, security-dir, audit-log-dir
 *    * Permission: when: [configuring] allow: [import] at levels: [node],
 *    * Permission: when: [activated, configuring] allow: [set, get, unset] at levels: [node, stripe, cluster]
 *
 *  authc
 *    * Permission: when: [configuring] allow: [import] at levels: [cluster],
 *    * Permission: when: [activated, configuring] allow: [set, get, unset] at levels: [cluster]
 *
 *  client-reconnect-window, failover-priority, client-lease-duration, ssl-tls, whitelist
 *    * Permission: when: [configuring] allow: [import] at levels: [cluster],
 *    * Permission: when: [activated, configuring] allow: [set, get] at levels: [cluster]
 *
 *  log-dir
 *    * Permission: when: [configuring] allow: [import] at levels: [node],
 *    * Permission: when: [configuring] allow: [set, get] at levels: [node, stripe, cluster],
 *    * Permission: when: [activated] allow: [set, get] at levels: [node, stripe, cluster]
 *
 *  cluster-name, offheap-resources
 *    * Permission: when: [configuring] allow: [set, get, import, unset] at levels: [cluster],
 *    * Permission: when: [activated] allow: [set, get] at levels: [cluster]
 * </pre>
 *
 * @author Mathieu Carbou
 */
public enum Setting {

  // ==== Settings applied to a specific node only

  NODE_UID(SettingName.NODE_UID,
      of(V2),
      false,
      UID::newUID,
      NODE,
      fromNode(Node::getUID),
      intoNode((o, value) -> o.setUID(UID.valueOf(value))),
      asList(
          when(CONFIGURING, ACTIVATED).allow(GET).atAnyLevels(),
          when(CONFIGURING).allow(IMPORT).atLevel(NODE)
      ),
      of(RESOLVE_EAGERLY, PRESENCE, HIDDEN)
  ),
  STRIPE_UID(SettingName.STRIPE_UID,
      of(V2),
      false,
      UID::newUID,
      STRIPE,
      fromStripe(Stripe::getUID),
      intoStripe((o, value) -> o.setUID(UID.valueOf(value))),
      asList(
          when(CONFIGURING, ACTIVATED).allow(GET).atLevels(CLUSTER, STRIPE),
          when(CONFIGURING).allow(IMPORT).atLevel(STRIPE)
      ),
      of(RESOLVE_EAGERLY, PRESENCE, HIDDEN)
  ),
  CLUSTER_UID(SettingName.CLUSTER_UID,
      of(V2),
      false,
      UID::newUID,
      CLUSTER,
      fromCluster(Cluster::getUID),
      intoCluster((o, value) -> o.setUID(UID.valueOf(value))),
      asList(
          when(CONFIGURING, ACTIVATED).allow(GET).atLevel(CLUSTER),
          when(CONFIGURING).allow(IMPORT).atLevel(CLUSTER)
      ),
      of(RESOLVE_EAGERLY, PRESENCE, HIDDEN)
  ),
  NODE_NAME(SettingName.NODE_NAME,
      of(V1, V2),
      false,
      () -> "node-" + UID.newUID(),
      NODE,
      fromNode(Node::getName),
      intoNode(Node::setName),
      asList(
          when(CONFIGURING, ACTIVATED).allow(GET).atAnyLevels(),
          when(CONFIGURING).allow(SET, IMPORT).atLevel(NODE)
      ),
      of(RESOLVE_EAGERLY, PRESENCE),
      emptyList(),
      emptyList(),
      (key, value) -> NAME_VALIDATOR.accept(SettingName.NODE_NAME, tuple2(key, value))
  ),
  STRIPE_NAME(SettingName.STRIPE_NAME,
      of(V2),
      false,
      () -> "stripe-" + UID.newUID(),
      STRIPE,
      fromStripe(Stripe::getName),
      intoStripe(Stripe::setName),
      asList(
          when(CONFIGURING, ACTIVATED).allow(GET).atLevels(CLUSTER, STRIPE),
          when(CONFIGURING).allow(SET, IMPORT).atLevel(STRIPE)
      ),
      of(RESOLVE_EAGERLY, PRESENCE),
      emptyList(),
      emptyList(),
      (key, value) -> NAME_VALIDATOR.accept(SettingName.STRIPE_NAME, tuple2(key, value))
  ),
  NODE_HOSTNAME(SettingName.NODE_HOSTNAME,
      of(V1, V2),
      false,
      always("%h"),
      NODE,
      fromNode(Node::getHostname),
      intoNode(Node::setHostname),
      asList(
          when(CONFIGURING, ACTIVATED).allow(GET).atAnyLevels(),
          when(CONFIGURING).allow(IMPORT).atLevel(NODE)
      ),
      of(RESOLVE_EAGERLY, PRESENCE),
      emptyList(),
      emptyList(),
      (key, value) -> HOST_VALIDATOR.accept(SettingName.NODE_HOSTNAME, tuple2(key, value))
  ),
  NODE_PORT(SettingName.NODE_PORT,
      of(V1, V2),
      false,
      always(9410),
      NODE,
      fromNode(Node::getPort),
      intoNode((node, value) -> node.setPort(Integer.parseInt(value))),
      asList(
          when(CONFIGURING, ACTIVATED).allow(GET).atAnyLevels(),
          when(CONFIGURING).allow(IMPORT).atLevel(NODE)
      ),
      of(PRESENCE),
      emptyList(),
      emptyList(),
      (key, value) -> PORT_VALIDATOR.accept(SettingName.NODE_PORT, tuple2(key, value))
  ),
  NODE_PUBLIC_HOSTNAME(SettingName.NODE_PUBLIC_HOSTNAME,
      of(V1, V2),
      false,
      always(null),
      NODE,
      fromNode(Node::getPublicHostname),
      intoNode(Node::setPublicHostname),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(NODE),
          when(CONFIGURING, ACTIVATED).allow(GET, SET, UNSET).atAnyLevels()
      ),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> HOST_VALIDATOR.accept(SettingName.NODE_PUBLIC_HOSTNAME, tuple2(key, value))
  ),
  NODE_PUBLIC_PORT(SettingName.NODE_PUBLIC_PORT,
      of(V1, V2),
      false,
      always(null),
      NODE,
      fromNode(Node::getPublicPort),
      intoNode((node, value) -> node.setPublicPort(value == null ? null : Integer.parseInt(value))),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(NODE),
          when(CONFIGURING, ACTIVATED).allow(GET, SET, UNSET).atAnyLevels()
      ),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> PORT_VALIDATOR.accept(SettingName.NODE_PUBLIC_PORT, tuple2(key, value))
  ),
  NODE_GROUP_PORT(SettingName.NODE_GROUP_PORT,
      of(V1, V2),
      false,
      always(9430),
      NODE,
      fromNode(Node::getGroupPort),
      intoNode((node, value) -> node.setGroupPort(Integer.parseInt(value))),
      asList(
          when(CONFIGURING, ACTIVATED).allow(GET).atAnyLevels(),
          when(CONFIGURING).allow(SET).atAnyLevels(),
          when(CONFIGURING).allow(IMPORT).atLevel(NODE)
      ),
      of(PRESENCE),
      emptyList(),
      emptyList(),
      (key, value) -> PORT_VALIDATOR.accept(SettingName.NODE_GROUP_PORT, tuple2(key, value))
  ),
  NODE_BIND_ADDRESS(SettingName.NODE_BIND_ADDRESS,
      of(V1, V2),
      false,
      always("0.0.0.0"),
      NODE,
      fromNode(Node::getBindAddress),
      intoNode(Node::setBindAddress),
      asList(
          when(CONFIGURING, ACTIVATED).allow(GET).atAnyLevels(),
          when(CONFIGURING).allow(SET).atAnyLevels(),
          when(CONFIGURING).allow(IMPORT).atLevel(NODE)
      ),
      of(PRESENCE),
      emptyList(),
      emptyList(),
      (key, value) -> ADDRESS_VALIDATOR.accept(SettingName.NODE_BIND_ADDRESS, tuple2(key, value))
  ),
  NODE_GROUP_BIND_ADDRESS(SettingName.NODE_GROUP_BIND_ADDRESS,
      of(V1, V2),
      false,
      always("0.0.0.0"),
      NODE,
      fromNode(Node::getGroupBindAddress),
      intoNode(Node::setGroupBindAddress),
      asList(
          when(CONFIGURING, ACTIVATED).allow(GET).atAnyLevels(),
          when(CONFIGURING).allow(SET).atAnyLevels(),
          when(CONFIGURING).allow(IMPORT).atLevel(NODE)
      ),
      of(PRESENCE),
      emptyList(),
      emptyList(),
      (key, value) -> ADDRESS_VALIDATOR.accept(SettingName.NODE_GROUP_BIND_ADDRESS, tuple2(key, value))
  ),

  // ==== Node configuration

  CLUSTER_NAME(SettingName.CLUSTER_NAME,
      of(V1, V2),
      false,
      always(null),
      CLUSTER,
      fromCluster(Cluster::getName),
      intoCluster(Cluster::setName),
      asList(
          when(CONFIGURING).allowAnyOperations().atLevel(CLUSTER),
          when(ACTIVATED).allow(GET, SET).atLevel(CLUSTER)
      ),
      of(ALL_NODES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> NAME_VALIDATOR.accept(SettingName.CLUSTER_NAME, tuple2(key, value))
  ),

  LOCK_CONTEXT(SettingName.LOCK_CONTEXT,
      of(V2),
      false,
      always(null),
      CLUSTER,
      fromCluster(Cluster::getConfigurationLockContext),
      intoCluster((cluster, context) -> cluster.setConfigurationLockContext(context != null ? LockContext.from(context) : null)),
      asList(
          // only allow loading and parsing a property file
          // do not allow the use of get/set/unset commands when configuring
          when(CONFIGURING).allow(IMPORT).atLevel(CLUSTER),
          when(ACTIVATED).allow(SET, UNSET).atLevel(CLUSTER)
      ),
      of(ACTIVES_ONLINE, HIDDEN)
  ),

  NODE_CONFIG_DIR(SettingName.NODE_CONFIG_DIR,
      of(V1, V2),
      false,
      always(RawPath.valueOf(Paths.get("%H", "terracotta", "config").toString())),
      NODE,
      o -> Optional.empty(),
      noop(),
      emptyList(),
      of(PRESENCE, CONFIG),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.NODE_CONFIG_DIR, tuple2(key, value))
  ),
  NODE_METADATA_DIR(SettingName.NODE_METADATA_DIR,
      of(V1, V2),
      false,
      always(RawPath.valueOf(Paths.get("%H", "terracotta", "metadata").toString())),
      NODE,
      fromNode(Node::getMetadataDir),
      intoNode((node, value) -> node.setMetadataDir(RawPath.valueOf(value))),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(NODE),
          when(CONFIGURING).allow(GET, SET).atAnyLevels(),
          when(ACTIVATED).allow(GET).atAnyLevels()
      ),
      of(PRESENCE),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.NODE_METADATA_DIR, tuple2(key, value))
  ),
  NODE_LOG_DIR(SettingName.NODE_LOG_DIR,
      of(V1, V2),
      false,
      always(RawPath.valueOf(Paths.get("%H", "terracotta", "logs").toString())),
      NODE,
      fromNode(Node::getLogDir),
      intoNode((node, value) -> node.setLogDir(RawPath.valueOf(value))),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(NODE),
          when(CONFIGURING).allow(GET, SET).atAnyLevels(),
          when(ACTIVATED).allow(GET, SET).atAnyLevels()
      ),
      of(ACTIVES_ONLINE, NODE_RESTART, PRESENCE),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.NODE_LOG_DIR, tuple2(key, value))
  ),
  NODE_BACKUP_DIR(SettingName.NODE_BACKUP_DIR,
      of(V1, V2),
      false,
      always(null),
      NODE,
      fromNode(Node::getBackupDir),
      intoNode((node, value) -> node.setBackupDir(value == null ? null : RawPath.valueOf(value))),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(NODE),
          when(CONFIGURING, ACTIVATED).allow(GET, SET, UNSET).atAnyLevels()
      ),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.NODE_BACKUP_DIR, tuple2(key, value))
  ),
  TC_PROPERTIES(SettingName.TC_PROPERTIES,
      of(V1, V2),
      true,
      always(emptyMap()),
      NODE,
      fromNode(Node::getTcProperties),
      intoNodeMap((node, tuple) -> {
        if (tuple.t1 == null && empty(tuple.t2)) {
          if (tuple.t2 == null) {
            // null assignment will reset the map or do nothing if map si null
            node.unsetTcProperties();
          } else {
            // if user sets "" in a config file, he specifically asks for an empty map to be set
            node.setTcProperties(emptyMap());
          }
        } else if (tuple.t1 != null && empty(tuple.t2)) {
          node.removeTcProperty(tuple.t1);
        } else if (tuple.t1 == null) {
          // tuple.t2 != null
          // complete reset of all entries
          node.setTcProperties(emptyMap());
          Stream.of(tuple.t2.split(",")).map(kv -> kv.split(":")).forEach(kv -> node.putTcProperty(kv[0], kv[1]));
        } else {
          // tuple.t1 != null && tuple.t2 != null
          node.putTcProperty(tuple.t1, tuple.t2);
        }
      }),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(NODE),
          when(CONFIGURING, ACTIVATED).allow(GET, SET, UNSET).atAnyLevels()
      ),
      of(ACTIVES_ONLINE, CLUSTER_RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> PROPS_VALIDATOR.accept(SettingName.TC_PROPERTIES, tuple2(key, value))
  ),
  NODE_LOGGER_OVERRIDES(SettingName.NODE_LOGGER_OVERRIDES,
      of(V1, V2),
      true,
      always(emptyMap()),
      NODE,
      fromNode(Node::getLoggerOverrides),
      intoNodeMap((node, tuple) -> {
        if (tuple.t1 == null && empty(tuple.t2)) {
          if (tuple.t2 == null) {
            // null assignment will reset the map or do nothing if map si null
            node.unsetLoggerOverrides();
          } else {
            // if user sets "" in a config file, he specifically asks for an empty map to be set
            node.setLoggerOverrides(emptyMap());
          }
        } else if (tuple.t1 != null && empty(tuple.t2)) {
          node.removeLoggerOverride(tuple.t1);
        } else if (tuple.t1 == null) {
          // tuple.t2 != null
          // complete reset of all entries
          node.setLoggerOverrides(emptyMap());
          Stream.of(tuple.t2.split(",")).map(kv -> kv.split(":")).forEach(kv -> node.putLoggerOverride(kv[0], kv[1].toUpperCase(Locale.ROOT)));
        } else {
          // tuple.t1 != null && tuple.t2 != null
          node.putLoggerOverride(tuple.t1, tuple.t2.toUpperCase(Locale.ROOT));
        }
      }),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(NODE),
          when(CONFIGURING, ACTIVATED).allow(GET, SET, UNSET).atAnyLevels()
      ),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> LOGGER_LEVEL_VALIDATOR.accept(SettingName.NODE_LOGGER_OVERRIDES, tuple2(key, value))
  ),
  CLIENT_RECONNECT_WINDOW(SettingName.CLIENT_RECONNECT_WINDOW,
      of(V1, V2),
      false,
      always(Measure.of(120, SECONDS)),
      CLUSTER,
      fromCluster(Cluster::getClientReconnectWindow),
      intoCluster((cluster, value) -> cluster.setClientReconnectWindow(Measure.parse(value, TimeUnit.class))),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(CLUSTER),
          when(CONFIGURING, ACTIVATED).allow(GET, SET).atLevel(CLUSTER)
      ),
      of(ACTIVES_ONLINE, PRESENCE),
      emptyList(),
      asList(SECONDS, MINUTES, HOURS),
      (key, value) -> TIME_VALIDATOR.accept(SettingName.CLIENT_RECONNECT_WINDOW, tuple2(key, value))
  ),
  FAILOVER_PRIORITY(SettingName.FAILOVER_PRIORITY,
      of(V1, V2),
      false,
      always(null),
      CLUSTER,
      fromCluster(Cluster::getFailoverPriority),
      intoCluster((cluster, value) -> cluster.setFailoverPriority(FailoverPriority.valueOf(value))),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(CLUSTER),
          when(CONFIGURING, ACTIVATED).allow(GET, SET).atLevel(CLUSTER)
      ),
      of(ALL_NODES_ONLINE, CLUSTER_RESTART, PRESENCE, CONFIG, RESOLVE_EAGERLY),
      emptyList(),
      emptyList(),
      (key, value) -> DEFAULT_VALIDATOR.andThen((k, v) -> FailoverPriority.valueOf(v.t2)).accept(SettingName.FAILOVER_PRIORITY, tuple2(key, value))
  ),

  // ==== Lease

  CLIENT_LEASE_DURATION(SettingName.CLIENT_LEASE_DURATION,
      of(V1, V2),
      false,
      always(Measure.of(150, SECONDS)),
      CLUSTER,
      fromCluster(Cluster::getClientLeaseDuration),
      intoCluster((cluster, value) -> cluster.setClientLeaseDuration(Measure.parse(value, TimeUnit.class))),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(CLUSTER),
          when(CONFIGURING, ACTIVATED).allow(GET, SET).atLevel(CLUSTER)
      ),
      of(ACTIVES_ONLINE, PRESENCE),
      emptyList(),
      asList(MILLISECONDS, SECONDS, MINUTES, HOURS),
      (key, value) -> TIME_VALIDATOR.accept(SettingName.CLIENT_LEASE_DURATION, tuple2(key, value))
  ),

  // ==== License update

  LICENSE_FILE(SettingName.LICENSE_FILE,
      of(V1, V2),
      false,
      always(null),
      CLUSTER,
      o -> Optional.empty(),
      noop(),
      singletonList(
          when(CONFIGURING, ACTIVATED).allow(SET).atLevel(CLUSTER)
      ),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.LICENSE_FILE, tuple2(key, value))
  ),

  // ==== Security

  SECURITY_DIR(SettingName.SECURITY_DIR,
      of(V1, V2),
      false,
      always(null),
      NODE,
      fromNode(Node::getSecurityDir),
      intoNode((node, value) -> node.setSecurityDir(value == null ? null : RawPath.valueOf(value))),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(NODE),
          when(CONFIGURING, ACTIVATED).allow(GET, SET, UNSET).atAnyLevels()
      ),
      of(ALL_NODES_ONLINE, CLUSTER_RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.SECURITY_DIR, tuple2(key, value))
  ),
  SECURITY_AUDIT_LOG_DIR(SettingName.SECURITY_AUDIT_LOG_DIR,
      of(V1, V2),
      false,
      always(null),
      NODE,
      fromNode(Node::getSecurityAuditLogDir),
      intoNode((node, value) -> node.setSecurityAuditLogDir(value == null ? null : RawPath.valueOf(value))),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(NODE),
          when(CONFIGURING, ACTIVATED).allow(GET, SET, UNSET).atAnyLevels()
      ),
      of(ALL_NODES_ONLINE, CLUSTER_RESTART),
      emptyList(),
      emptyList(),
      (key, value) -> PATH_VALIDATOR.accept(SettingName.SECURITY_AUDIT_LOG_DIR, tuple2(key, value))
  ),
  SECURITY_AUTHC(SettingName.SECURITY_AUTHC,
      of(V1, V2),
      false,
      always(null),
      CLUSTER,
      fromCluster(Cluster::getSecurityAuthc),
      intoCluster(Cluster::setSecurityAuthc),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(CLUSTER),
          when(CONFIGURING, ACTIVATED).allow(GET, SET, UNSET).atLevel(CLUSTER)
      ),
      of(ALL_NODES_ONLINE, CLUSTER_RESTART),
      asList("file", "ldap", "certificate")
  ),
  SECURITY_SSL_TLS(SettingName.SECURITY_SSL_TLS,
      of(V1, V2),
      false,
      always(false),
      CLUSTER,
      fromCluster(Cluster::getSecuritySslTls),
      intoCluster((cluster, value) -> cluster.setSecuritySslTls(Boolean.parseBoolean(value))),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(CLUSTER),
          when(CONFIGURING, ACTIVATED).allow(GET, SET).atLevel(CLUSTER)
      ),
      of(ALL_NODES_ONLINE, CLUSTER_RESTART, PRESENCE),
      asList("true", "false")
  ),
  SECURITY_WHITELIST(SettingName.SECURITY_WHITELIST,
      of(V1, V2),
      false,
      always(false),
      CLUSTER,
      fromCluster(Cluster::getSecurityWhitelist),
      intoCluster((cluster, value) -> cluster.setSecurityWhitelist(Boolean.parseBoolean(value))),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(CLUSTER),
          when(CONFIGURING, ACTIVATED).allow(GET, SET).atLevel(CLUSTER)
      ),
      of(ALL_NODES_ONLINE, CLUSTER_RESTART, PRESENCE),
      asList("true", "false")
  ),

  // ==== Resources configuration

  OFFHEAP_RESOURCES(SettingName.OFFHEAP_RESOURCES,
      of(V1, V2),
      true,
      always(singletonMap("main", Measure.of(512, MB))),
      CLUSTER,
      fromCluster(Cluster::getOffheapResources),
      intoClusterMap((cluster, tuple) -> {
        if (tuple.t1 == null && empty(tuple.t2)) {
          if (tuple.t2 == null) {
            // null assignment will reset the map or do nothing if map si null
            cluster.unsetOffheapResources();
          } else {
            // if user sets "" in a config file, he specifically asks for an empty map to be set
            cluster.setOffheapResources(emptyMap());
          }
        } else if (tuple.t1 != null && empty(tuple.t2)) {
          cluster.removeOffheapResource(tuple.t1);
        } else if (tuple.t1 == null) {
          // tuple.t2 != null
          // complete reset of all entries
          cluster.setOffheapResources(emptyMap());
          Stream.of(tuple.t2.split(",")).map(kv -> kv.split(":")).forEach(kv -> cluster.putOffheapResource(kv[0], Measure.parse(kv[1], MemoryUnit.class)));
        } else {
          // tuple.t1 != null && tuple.t2 != null
          cluster.putOffheapResource(tuple.t1, Measure.parse(tuple.t2, MemoryUnit.class));
        }
      }),
      asList(
          when(CONFIGURING).allowAnyOperations().atLevel(CLUSTER),
          when(ACTIVATED).allow(GET, SET).atLevel(CLUSTER)
      ),
      of(ACTIVES_ONLINE),
      emptyList(),
      asList(MemoryUnit.values()),
      (key, value) -> OFFHEAP_VALIDATOR.accept(SettingName.OFFHEAP_RESOURCES, tuple2(key, value))
  ),
  DATA_DIRS(SettingName.DATA_DIRS,
      of(V1, V2),
      true,
      always(singletonMap("main", RawPath.valueOf(Paths.get("%H", "terracotta", "user-data", "main").toString()))),
      NODE,
      fromNode(Node::getDataDirs),
      intoNodeMap((node, tuple) -> {
        if (tuple.t1 == null && empty(tuple.t2)) {
          if (tuple.t2 == null) {
            // null assignment will reset the map or do nothing if map si null
            node.unsetDataDirs();
          } else {
            // if user sets "" in a config file, he specifically asks for an empty map to be set
            node.setDataDirs(emptyMap());
          }
        } else if (tuple.t1 != null && empty(tuple.t2)) {
          node.removeDataDir(tuple.t1);
        } else if (tuple.t1 == null) {
          // tuple.t2 != null
          // complete reset of all entries
          node.setDataDirs(emptyMap());
          Stream.of(tuple.t2.split(",")).forEach(kv -> {
            int firstColon = kv.indexOf(":");
            node.putDataDir(kv.substring(0, firstColon), RawPath.valueOf(kv.substring(firstColon + 1)));
          });
        } else {
          // tuple.t1 != null && tuple.t2 != null
          node.putDataDir(tuple.t1, RawPath.valueOf(tuple.t2));
        }
      }),
      asList(
          when(CONFIGURING).allow(IMPORT).atLevel(NODE),
          when(CONFIGURING).allow(GET, SET, UNSET).atAnyLevels(),
          when(ACTIVATED, CONFIGURING).allow(GET, SET).atAnyLevels()
      ),
      of(ACTIVES_ONLINE),
      emptyList(),
      emptyList(),
      (key, value) -> DATA_DIRS_VALIDATOR.accept(SettingName.DATA_DIRS, tuple2(key, value))
  );

  private final String name;
  private final Collection<Version> versions;
  private final boolean map;
  private final Supplier<Object> defaultValue;
  private final Scope scope;
  private final Function<PropertyHolder, Optional<Stream<Tuple2<String, String>>>> extractor;
  private final Collection<Permission> permissions;
  private final Collection<Requirement> requirements;
  private final Collection<String> allowedValues;
  private final Collection<? extends Enum<?>> allowedUnits;
  private final BiConsumer<String, String> validator;
  private final BiConsumer<PropertyHolder, Tuple2<String, String>> setter;

  Setting(String name,
          Collection<Version> versions,
          boolean map,
          Supplier<Object> defaultValue,
          Scope scope,
          Function<PropertyHolder, Optional<Stream<Tuple2<String, String>>>> extractor,
          BiConsumer<PropertyHolder, Tuple2<String, String>> setter,
          Collection<Permission> permissions,
          EnumSet<Requirement> requirements) {
    this(name, versions, map, defaultValue, scope, extractor, setter, permissions, requirements, emptyList(), emptyList());
  }

  Setting(String name,
          Collection<Version> versions,
          boolean map,
          Supplier<Object> defaultValue,
          Scope scope,
          Function<PropertyHolder, Optional<Stream<Tuple2<String, String>>>> extractor,
          BiConsumer<PropertyHolder, Tuple2<String, String>> setter,
          Collection<Permission> permissions,
          EnumSet<Requirement> requirements,
          Collection<String> allowedValues) {
    this(name, versions, map, defaultValue, scope, extractor, setter, permissions, requirements, allowedValues, emptyList());
  }

  Setting(String name,
          Collection<Version> versions,
          boolean map,
          Supplier<Object> defaultValue,
          Scope scope,
          Function<PropertyHolder, Optional<Stream<Tuple2<String, String>>>> extractor,
          BiConsumer<PropertyHolder, Tuple2<String, String>> setter,
          Collection<Permission> permissions,
          EnumSet<Requirement> requirements,
          Collection<String> allowedValues,
          Collection<? extends Enum<?>> allowedUnits) {
    this(name, versions, map, defaultValue, scope, extractor, setter, permissions, requirements, allowedValues, allowedUnits, (key, value) -> DEFAULT_VALIDATOR.accept(name, tuple2(key, value)));
  }

  Setting(String name,
          Collection<Version> versions,
          boolean map,
          Supplier<Object> defaultValue,
          Scope scope,
          Function<PropertyHolder, Optional<Stream<Tuple2<String, String>>>> extractor,
          BiConsumer<PropertyHolder, Tuple2<String, String>> setter,
          Collection<Permission> permissions,
          EnumSet<Requirement> requirements,
          Collection<String> allowedValues,
          Collection<? extends Enum<?>> allowedUnits,
          BiConsumer<String, String> validator) {
    this.name = requireNonNull(name);
    this.versions = versions;
    this.map = map;
    this.defaultValue = requireNonNull(defaultValue);
    this.scope = requireNonNull(scope);
    this.extractor = requireNonNull(extractor);
    this.setter = requireNonNull(setter);
    this.permissions = new LinkedHashSet<>(requireNonNull(permissions));
    this.requirements = requireNonNull(requirements);
    this.allowedValues = Collections.unmodifiableSet(new LinkedHashSet<>(requireNonNull(allowedValues)));
    this.allowedUnits = Collections.unmodifiableSet(new LinkedHashSet<>(requireNonNull(allowedUnits)));
    this.validator = requireNonNull(validator);

    if (scope == CLUSTER && permissions.stream().anyMatch(permission -> permission.allows(NODE) || permission.allows(STRIPE))) {
      throw new AssertionError("Scope error for setting " + name + ": " + permissions);
    }
    if (scope == STRIPE && permissions.stream().anyMatch(permission -> permission.allows(NODE))) {
      throw new AssertionError("Scope error for setting " + name + ": " + permissions);
    }
  }

  @Override
  public String toString() {
    return name;
  }

  public Collection<Version> getVersions() {
    return versions;
  }

  public Collection<Permission> getPermissions() {
    return permissions;
  }

  public boolean isMap() {
    return map;
  }

  // we expect the caller to know what it is doing...
  @SuppressWarnings("unchecked")
  public <T> T getDefaultValue() {
    return (T) defaultValue.get();
  }

  public Optional<String> getDefaultProperty() {
    return toProperty(defaultValue.get());
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
   * Example: name is defined as scope NODE, but we could execute "get name"
   */
  public boolean allows(Scope scope) {
    return permissions.stream().anyMatch(permission -> permission.allows(scope));
  }

  public boolean allows(Operation operation) {
    return permissions.stream().anyMatch(permission -> permission.allows(operation));
  }

  public boolean allows(ClusterState clusterState) {
    return permissions.stream().anyMatch(permission -> permission.allows(clusterState));
  }

  public boolean allows(Operation operation, Scope scope) {
    return permissions.stream().anyMatch(permissions -> permissions.allows(operation) && permissions.allows(scope));
  }

  public boolean allows(ClusterState clusterState, Operation operation) {
    return permissions.stream().anyMatch(permissions -> permissions.allows(operation) && permissions.allows(clusterState));
  }

  public boolean allows(ClusterState clusterState, Operation operation, Scope scope) {
    return permissions.stream().anyMatch(permissions -> permissions.allows(clusterState) && permissions.allows(operation) && permissions.allows(scope));
  }

  private boolean isUserExportable() {
    return !requires(HIDDEN) && permissions.stream().anyMatch(Permission::isUserExportable);
  }

  public boolean mustBePresent() {
    return requires(PRESENCE);
  }

  public boolean mustBeProvided() {
    return requires(CONFIG);
  }

  public boolean canBeCleared(Scope scope) {
    return allows(ACTIVATED, UNSET, scope) || allows(CONFIGURING, UNSET, scope) || getDefaultValue() != null;
  }

  public boolean isWritable() {
    return isWritableWhen(CONFIGURING) || isWritableWhen(ACTIVATED);
  }

  public boolean isWritableWhen(ClusterState clusterState) {
    return permissions.stream().anyMatch(permission -> permission.isWritableWhen(clusterState));
  }

  public boolean isScope(Scope scope) {
    return this.scope == scope;
  }

  public Scope getScope() {
    return scope;
  }

  public void validate(String key, String value) {
    // do not validate if value is null and setting optional
    if (key == null && value == null && !mustBePresent()) {
      return;
    }
    validator.accept(key, value);
  }

  public void validate(String value) {
    validate(null, value);
  }

  public Optional<String> getProperty(PropertyHolder o) {
    return extractor.apply(o).map(Setting::reduceToPropertyString);
  }

  public void setProperty(PropertyHolder o, String value) {
    setProperty(o, null, value);
  }

  public void setProperty(NodeContext nodeContext, String key, String value) {
    setProperty(scope == CLUSTER ? nodeContext.getCluster() : nodeContext.getNode(), key, value);
  }

  public void setProperty(PropertyHolder o, String key, String value) {
    if (!isWritable()) {
      throw new IllegalArgumentException("Setting: " + this + " is not writable");
    }
    // value can be null or "", depending on which system is setting the property:
    // i.e. configuration parsing will lead to "" which means that the user has specifically decided to "empty" the setting.
    // for most of the settings, "empty" means nullify.
    // but for maps, "empty" means specifically set an empty map and do not use the default values
    if (!isMap() && empty(value)) {
      // if the setting is not a map, threat any "" like null
      value = null;
    }
    validate(key, value);
    this.setter.accept(o, tuple2(key, value));
  }

  public Properties toProperties(PropertyHolder o, boolean expanded, boolean includeDefaultValues) {
    Properties properties = new Properties();
    // get the value set by the user if any as a property-like format
    Optional<String> currentValue = getProperty(o);
    // get the default value if any as a property-like format
    Optional<String> defaultValue = getDefaultProperty();

    Runnable normalWrite = () -> {
      if (currentValue.isPresent()) {
        // write user-input ("" included)
        properties.setProperty(name, currentValue.get());
      } else if (defaultValue.isPresent() && !defaultValue.get().isEmpty()) {
        // only write default values if there is really one (will exclude out the empty maps)
        properties.setProperty(name, defaultValue.get());
      }
    };

    // we expose this property if:
    // 1. the user input is there
    // 2. or, the user input is missing, but we have a default value and we want to include the default values in the export
    if (currentValue.isPresent() || defaultValue.isPresent() && includeDefaultValues) {
      if (expanded && isMap()) {
        // case where we want to expand the properties if this setting is a map.
        // we extract the value set by the user, if it is there.
        // if not, we then have a default and we extract it.
        // "extract" means returning a stream of all key-value pairs for this "map" like setting.
        List<Tuple2<String, String>> pairs = extractor.apply(o).orElseGet(() -> stream(getDefaultValue()).get()).collect(Collectors.toList());
        if (pairs.isEmpty()) {
          // special case where the user has specifically set the setting to an empty map (i.e. no data-dir, no offheap)
          // we need to output the setting has being empty
          normalWrite.run();
        } else {
          // we have a map settings with some things defined
          pairs.forEach(tuple -> properties.setProperty(name + "." + tuple.t1, tuple.t2));
        }
      } else {
        normalWrite.run();
      }
    }
    return properties;
  }

  public boolean allowsValue(String value) {
    return this.allowedValues.isEmpty() || this.allowedValues.contains(value);
  }

  public static Setting fromName(String name) {
    return findSetting(name).orElseThrow(() -> new IllegalArgumentException("Illegal setting name: " + name));
  }

  public static Optional<Setting> findSetting(String name) {
    return Stream.of(values()).filter(setting -> setting.name.equals(name)).findAny();
  }

  public static Properties modelToProperties(PropertyHolder o, boolean expanded, boolean includeDefaultValues, boolean includeHiddenSettings, Version version) {
    Properties properties = new Properties();
    Stream.of(Setting.values())
        .filter(setting -> version.amongst(setting.getVersions()))
        .filter(setting -> setting.isUserExportable() || (includeHiddenSettings && setting.requires(HIDDEN)))
        .filter(setting -> setting.isScope(o.getScope()))
        .forEach(setting -> properties.putAll(setting.toProperties(o, expanded, includeDefaultValues)));
    return properties;
  }

  private static Function<PropertyHolder, Optional<Stream<Tuple2<String, String>>>> fromNode(Function<Node, Object> extractor) {
    return node -> stream(extractor.apply((Node) node));
  }

  private static Function<PropertyHolder, Optional<Stream<Tuple2<String, String>>>> fromStripe(Function<Stripe, Object> extractor) {
    return stripe -> stream(extractor.apply((Stripe) stripe));
  }

  private static Function<PropertyHolder, Optional<Stream<Tuple2<String, String>>>> fromCluster(Function<Cluster, Object> extractor) {
    return cluster -> stream(extractor.apply((Cluster) cluster));
  }

  @SuppressWarnings("unchecked")
  private static Optional<Stream<Tuple2<String, String>>> stream(Object o) {
    if (o instanceof OptionalConfig) {
      o = ((OptionalConfig<Object>) o).orElse(null);
    }
    if (o == null) {
      return Optional.empty();
    }
    if (o instanceof Map) {
      return Optional.of(((Map<String, ?>) o).entrySet()
          .stream()
          .filter(e -> e.getValue() != null)
          .sorted(Map.Entry.comparingByKey())
          .map(e -> tuple2(e.getKey(), e.getValue().toString())));
    }
    return Optional.of(Stream.of(tuple2(null, String.valueOf(o))));
  }

  public static Optional<String> toProperty(Object o) {
    // simulate what we would have in a config file, such as: backup-dir=
    return o == null ? Optional.empty() : stream(o).map(Setting::reduceToPropertyString);
  }

  private static String reduceToPropertyString(Stream<Tuple2<String, String>> s) {
    return s.filter(tuple -> !tuple.allNulls())
        .map(tuple -> tuple.t1 == null ? tuple.t2 : (tuple.t1 + ":" + tuple.t2))
        .reduce((result, element) -> result + "," + element)
        .orElse("");
  }

  private static BiConsumer<PropertyHolder, Tuple2<String, String>> intoNode(BiConsumer<Node, String> setter) {
    return (node, tuple) -> {
      if (tuple.t1 != null) {
        throw new IllegalArgumentException("Key must be null: parameter is not a map");
      }
      setter.accept((Node) node, empty(tuple.t2) ? null : tuple.t2.trim());
    };
  }

  private static BiConsumer<PropertyHolder, Tuple2<String, String>> intoStripe(BiConsumer<Stripe, String> setter) {
    return (stripe, tuple) -> {
      if (tuple.t1 != null) {
        throw new IllegalArgumentException("Key must be null: parameter is not a map");
      }
      setter.accept((Stripe) stripe, empty(tuple.t2) ? null : tuple.t2.trim());
    };
  }

  private static BiConsumer<PropertyHolder, Tuple2<String, String>> intoCluster(BiConsumer<Cluster, String> setter) {
    return (cluster, tuple) -> {
      if (tuple.t1 != null) {
        throw new IllegalArgumentException("Key must be null: parameter is not a map");
      }
      setter.accept((Cluster) cluster, empty(tuple.t2) ? null : tuple.t2.trim());
    };
  }

  private static BiConsumer<PropertyHolder, Tuple2<String, String>> intoNodeMap(BiConsumer<Node, Tuple2<String, String>> setter) {
    return (node, tuple) -> setter.accept((Node) node, tuple2(tuple.t1, tuple.t2));
  }

  private static BiConsumer<PropertyHolder, Tuple2<String, String>> intoClusterMap(BiConsumer<Cluster, Tuple2<String, String>> setter) {
    return (cluster, tuple) -> setter.accept((Cluster) cluster, tuple2(tuple.t1, tuple.t2));
  }

  private static <U, V> BiConsumer<U, V> noop() {
    return (u, v) -> {
    };
  }

  private static <V> Supplier<V> always(V value) {
    return () -> value;
  }

  private static boolean empty(String s) {
    return s == null || s.trim().isEmpty();
  }
}
