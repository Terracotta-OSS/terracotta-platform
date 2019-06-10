/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.repository;

public class RepositoryConstants {
  public static final String FILENAME_PREFIX = "cluster-config";
  public static final String FILENAME_EXT = "xml";
  public static final String REGEX_PREFIX = FILENAME_PREFIX + "\\.";
  public static final String REGEX_SUFFIX = "\\.[1-9][0-9]*\\." + FILENAME_EXT;
  public static final String CONFIG_REPO_FILENAME_REGEX = REGEX_PREFIX + "[\\S]+" + REGEX_SUFFIX;
}
