/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConstantsTest {
  @Test
  public void testConfigNameRegex() {
    assertThat("cluster-config.A.1.xml".matches(Constants.CONFIG_REPO_FILENAME_REGEX)).isTrue();
    assertThat("cluster-config.1.10.xml".matches(Constants.CONFIG_REPO_FILENAME_REGEX)).isTrue();
    assertThat("cluster-config.@.100.xml".matches(Constants.CONFIG_REPO_FILENAME_REGEX)).isTrue();
    assertThat("cluster-config.a.9.xml".matches(Constants.CONFIG_REPO_FILENAME_REGEX)).isTrue();
    assertThat("cluster-config.node-1.19.xml".matches(Constants.CONFIG_REPO_FILENAME_REGEX)).isTrue();
    assertThat("cluster-config.server-1-abc_1234@@#*$.199.xml".matches(Constants.CONFIG_REPO_FILENAME_REGEX)).isTrue();

    assertThat("cluster-config.1.xml".matches(Constants.CONFIG_REPO_FILENAME_REGEX)).isFalse();
    assertThat("cluster-config. .1.xml".matches(Constants.CONFIG_REPO_FILENAME_REGEX)).isFalse();
    assertThat("cluster-config.09.xml".matches(Constants.CONFIG_REPO_FILENAME_REGEX)).isFalse();
    assertThat("cluster-configA1.xml".matches(Constants.CONFIG_REPO_FILENAME_REGEX)).isFalse();
    assertThat("cluster-configA1Bxml".matches(Constants.CONFIG_REPO_FILENAME_REGEX)).isFalse();
  }

  @Test
  public void testNodeNameExtractionFromConfigRepoFile() {
    String nodeName = "cluster-config.node-1.19.xml".replaceAll("^" + Constants.REGEX_PREFIX, "").replaceAll(Constants.REGEX_SUFFIX + "$", "");
    assertThat(nodeName).isEqualTo("node-1");
    nodeName = "cluster-config.server-1-abc_1234@@#*$.199.xml".replaceAll("^" + Constants.REGEX_PREFIX, "").replaceAll(Constants.REGEX_SUFFIX + "$", "");
    assertThat(nodeName).isEqualTo("server-1-abc_1234@@#*$");

    nodeName = "server-1-abc_1234@@#*$.199.xml.cluster-config.".replaceAll("^" + Constants.REGEX_PREFIX, "").replaceAll(Constants.REGEX_SUFFIX + "$", "");
    assertThat(nodeName).isEqualTo("server-1-abc_1234@@#*$.199.xml.cluster-config."); // replaceAll() chain had no effect
  }
}
