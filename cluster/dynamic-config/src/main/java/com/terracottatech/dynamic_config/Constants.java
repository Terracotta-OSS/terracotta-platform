/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config;

import static java.io.File.separator;

public class Constants {
  public static final int DEFAULT_PORT = 9410;
  public static final int DEFAULT_GROUP_PORT = 9430;
  public static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";
  public static final String DEFAULT_GROUP_BIND_ADDRESS = "0.0.0.0";
  public static final String DEFAULT_HOSTNAME = "%h";
  public static final String DEFAULT_CONFIG_DIR = "%H" + separator + "terracotta" + separator + "config";
  public static final String DEFAULT_METADATA_DIR = "%H" + separator + "terracotta" + separator + "metadata";
  public static final String DEFAULT_LOG_DIR = "%H" + separator + "terracotta" + separator + "logs";
  public static final String DEFAULT_FAILOVER_PRIORITY = "availability";
  public static final String DEFAULT_CLIENT_RECONNECT_WINDOW = "120s";
  public static final String DEFAULT_CLIENT_LEASE_DURATION = "20s";
  public static final String DEFAULT_OFFHEAP_RESOURCE = "main:512MB";
  public static final String DEFAULT_DATA_DIR = "main:%H" + separator + "terracotta" + separator + "user-data" + separator + "main";

  public static final String REGEX_PREFIX = "cluster-config\\.";
  public static final String REGEX_SUFFIX = "\\.[1-9][0-9]*\\.xml";
  public static final String CONFIG_REPO_FILENAME_REGEX = REGEX_PREFIX + "[\\S]+" + REGEX_SUFFIX;
  public static final String MULTI_VALUE_SEP = ",";
  public static final String PARAM_INTERNAL_SEP = ":";

  public static final String NOMAD_CONFIG_DIR = "config";
  public static final String NOMAD_META_DIR = "sanskrit";
}
