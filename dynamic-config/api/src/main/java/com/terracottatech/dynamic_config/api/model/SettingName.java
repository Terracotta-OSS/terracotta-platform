/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.model;

/**
 * Contains the parameter values of the settings.
 * Theses values are used both in the config file and CLI and are the same in both cases.
 */
public class SettingName {
  public static final String CLUSTER_NAME = "cluster-name";
  public static final String NODE_NAME = "node-name";
  public static final String NODE_HOSTNAME = "node-hostname";
  public static final String NODE_PUBLIC_HOSTNAME = "node-public-hostname";
  public static final String NODE_PORT = "node-port";
  public static final String NODE_PUBLIC_PORT = "node-public-port";
  public static final String NODE_GROUP_PORT = "node-group-port";
  public static final String NODE_BIND_ADDRESS = "node-bind-address";
  public static final String NODE_GROUP_BIND_ADDRESS = "node-group-bind-address";
  public static final String NODE_REPOSITORY_DIR = "node-repository-dir";
  public static final String NODE_METADATA_DIR = "node-metadata-dir";
  public static final String NODE_LOG_DIR = "node-log-dir";
  public static final String NODE_BACKUP_DIR = "node-backup-dir";
  public static final String NODE_LOGGER_OVERRIDES = "node-logger-overrides";
  public static final String SECURITY_DIR = "security-dir";
  public static final String SECURITY_AUDIT_LOG_DIR = "security-audit-log-dir";
  public static final String SECURITY_AUTHC = "security-authc";
  public static final String SECURITY_SSL_TLS = "security-ssl-tls";
  public static final String SECURITY_WHITELIST = "security-whitelist";
  public static final String FAILOVER_PRIORITY = "failover-priority";
  public static final String CLIENT_RECONNECT_WINDOW = "client-reconnect-window";
  public static final String CLIENT_LEASE_DURATION = "client-lease-duration";
  public static final String OFFHEAP_RESOURCES = "offheap-resources";
  public static final String TC_PROPERTIES = "tc-properties";
  public static final String DATA_DIRS = "data-dirs";
  public static final String LICENSE_FILE = "license-file";
}
