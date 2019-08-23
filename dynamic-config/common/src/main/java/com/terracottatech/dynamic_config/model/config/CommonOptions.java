/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import static java.util.stream.Collectors.toCollection;

/**
 * Contains options common to CLI and config properties file.
 */
public class CommonOptions {
  public static final String NODE_NAME = "node-name";
  public static final String NODE_HOSTNAME = "node-hostname";
  public static final String NODE_PORT = "node-port";
  public static final String NODE_GROUP_PORT = "node-group-port";
  public static final String NODE_BIND_ADDRESS = "node-bind-address";
  public static final String NODE_GROUP_BIND_ADDRESS = "node-group-bind-address";
  public static final String NODE_REPOSITORY_DIR = "node-repository-dir";
  public static final String NODE_METADATA_DIR = "node-metadata-dir";
  public static final String NODE_LOG_DIR = "node-log-dir";
  public static final String NODE_BACKUP_DIR = "node-backup-dir";
  public static final String SECURITY_DIR = "security-dir";
  public static final String SECURITY_AUDIT_LOG_DIR = "security-audit-log-dir";
  public static final String SECURITY_AUTHC = "security-authc";
  public static final String SECURITY_SSL_TLS = "security-ssl-tls";
  public static final String SECURITY_WHITELIST = "security-whitelist";
  public static final String FAILOVER_PRIORITY = "failover-priority";
  public static final String CLIENT_RECONNECT_WINDOW = "client-reconnect-window";
  public static final String CLIENT_LEASE_DURATION = "client-lease-duration";
  public static final String OFFHEAP_RESOURCES = "offheap-resources";
  public static final String DATA_DIRS = "data-dirs";

  public static Collection<String> getAllOptions() {
    return Arrays.stream(CommonOptions.class.getDeclaredFields())
        .filter(CommonOptions::getConstants)
        .map(field -> {
          try {
            return (String) field.get(CommonOptions.class);
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        })
        .collect(toCollection(TreeSet::new));
  }

  private static boolean getConstants(Field field) {
    int modifiers = field.getModifiers();
    return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
  }
}
