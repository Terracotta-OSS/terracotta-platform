/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.repository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public interface ClusterConfigFilename {

  String getNodeName();

  long getVersion();

  static ClusterConfigFilename with(String nodeName, long version) {
      return new ValidClusterConfigFilename(nodeName, version);
  }

  static ClusterConfigFilename from(String fileName) {
    try {
      return new ValidClusterConfigFilename(fileName);
    } catch (IllegalArgumentException e) {
      return new InvalidClusterConfigFilename();
    }
  }

  class ValidClusterConfigFilename implements ClusterConfigFilename {
    private static final String CLUSTER_CONFIG = "cluster-config";
    private static final String EXTENSION = "xml";

    private static final String CLUSTER_CONFIG_FILENAME_FORMAT   = construct(".%s.%d.");
    private static final Pattern CLUSTER_CONFIG_FILENAME_PATTERN = compile(construct("\\.(\\S+)\\.([1-9]\\d*)\\."));

    private final String nodeName;
    private final long version;

    private ValidClusterConfigFilename(String nodeName, long version) {
      this.nodeName = nodeName;
      this.version = version;
    }

    private ValidClusterConfigFilename(String filename) {
      Matcher matcher = CLUSTER_CONFIG_FILENAME_PATTERN.matcher(filename);

      if (!matcher.matches()) {
        throw new IllegalArgumentException("Invalid filename: " + filename);
      }

      this.nodeName = matcher.group(1);
      this.version = Long.parseLong(matcher.group(2));
    }

    public String getNodeName() {
      return nodeName;
    }

    public long getVersion() {
      return version;
    }

    public String toString() {
      return String.format(CLUSTER_CONFIG_FILENAME_FORMAT, nodeName, version);
    }

    private static String construct(String pattern) {
      return CLUSTER_CONFIG + pattern + EXTENSION;
    }
  }

  class InvalidClusterConfigFilename implements ClusterConfigFilename {
    @Override
    public String getNodeName() {
      return null;
    }

    @Override
    public long getVersion() {
      return 0L;
    }
  }
}
