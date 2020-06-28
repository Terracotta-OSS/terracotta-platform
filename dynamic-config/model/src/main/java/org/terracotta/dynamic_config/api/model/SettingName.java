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

/**
 * Contains the parameter values of the settings.
 * Theses values are used both in the config file and CLI and are the same in both cases.
 */
public class SettingName {
  public static final String CLUSTER_NAME = "cluster-name";
  public static final String NODE_NAME = "name";
  public static final String NODE_HOSTNAME = "hostname";
  public static final String NODE_PUBLIC_HOSTNAME = "public-hostname";
  public static final String NODE_PORT = "port";
  public static final String NODE_PUBLIC_PORT = "public-port";
  public static final String NODE_GROUP_PORT = "group-port";
  public static final String NODE_BIND_ADDRESS = "bind-address";
  public static final String NODE_GROUP_BIND_ADDRESS = "group-bind-address";
  public static final String NODE_CONFIG_DIR = "config-dir";
  public static final String NODE_METADATA_DIR = "metadata-dir";
  public static final String NODE_LOG_DIR = "log-dir";
  public static final String NODE_BACKUP_DIR = "backup-dir";
  public static final String NODE_LOGGER_OVERRIDES = "logger-overrides";
  public static final String SECURITY_DIR = "security-dir";
  public static final String SECURITY_AUDIT_LOG_DIR = "audit-log-dir";
  public static final String SECURITY_AUTHC = "authc";
  public static final String SECURITY_SSL_TLS = "ssl-tls";
  public static final String SECURITY_PERMIT_LIST = "permit-list";
  public static final String FAILOVER_PRIORITY = "failover-priority";
  public static final String CLIENT_RECONNECT_WINDOW = "client-reconnect-window";
  public static final String CLIENT_LEASE_DURATION = "client-lease-duration";
  public static final String OFFHEAP_RESOURCES = "offheap-resources";
  public static final String TC_PROPERTIES = "tc-properties";
  public static final String DATA_DIRS = "data-dirs";
  public static final String LICENSE_FILE = "license-file";
  public static final String CONFIG_FILE = "config-file";
  public static final String REPAIR_MODE = "repair-mode";
  public static final String AUTO_ACTIVATE = "auto-activate";
}
