/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.repository;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RepositoryConstantsTest {
  @Test
  public void testConfigNameRegex() {
    assertThat("cluster-config.A.1.xml".matches(RepositoryConstants.CONFIG_REPO_FILENAME_REGEX), is(true));
    assertThat("cluster-config.1.10.xml".matches(RepositoryConstants.CONFIG_REPO_FILENAME_REGEX), is(true));
    assertThat("cluster-config.@.100.xml".matches(RepositoryConstants.CONFIG_REPO_FILENAME_REGEX), is(true));
    assertThat("cluster-config.a.9.xml".matches(RepositoryConstants.CONFIG_REPO_FILENAME_REGEX), is(true));
    assertThat("cluster-config.node-1.19.xml".matches(RepositoryConstants.CONFIG_REPO_FILENAME_REGEX), is(true));
    assertThat("cluster-config.server-1-abc_1234@@#*$.199.xml".matches(RepositoryConstants.CONFIG_REPO_FILENAME_REGEX), is(true));

    assertThat("cluster-config.1.xml".matches(RepositoryConstants.CONFIG_REPO_FILENAME_REGEX), is(false));
    assertThat("cluster-config. .1.xml".matches(RepositoryConstants.CONFIG_REPO_FILENAME_REGEX), is(false));
    assertThat("cluster-config.09.xml".matches(RepositoryConstants.CONFIG_REPO_FILENAME_REGEX), is(false));
    assertThat("cluster-configA1.xml".matches(RepositoryConstants.CONFIG_REPO_FILENAME_REGEX), is(false));
    assertThat("cluster-configA1Bxml".matches(RepositoryConstants.CONFIG_REPO_FILENAME_REGEX), is(false));
  }

  @Test
  public void testNodeNameExtractionFromConfigRepoFile() {
    String nodeName = "cluster-config.node-1.19.xml".replaceAll("^" + RepositoryConstants.REGEX_PREFIX, "").replaceAll(RepositoryConstants.REGEX_SUFFIX + "$", "");
    assertThat(nodeName, CoreMatchers.is("node-1"));
    nodeName = "cluster-config.server-1-abc_1234@@#*$.199.xml".replaceAll("^" + RepositoryConstants.REGEX_PREFIX, "").replaceAll(RepositoryConstants.REGEX_SUFFIX + "$", "");
    assertThat(nodeName, CoreMatchers.is("server-1-abc_1234@@#*$"));

    nodeName = "server-1-abc_1234@@#*$.199.xml.cluster-config.".replaceAll("^" + RepositoryConstants.REGEX_PREFIX, "").replaceAll(RepositoryConstants.REGEX_SUFFIX + "$", "");
    assertThat(nodeName, CoreMatchers.is("server-1-abc_1234@@#*$.199.xml.cluster-config.")); // replaceAll() chain had no effect
  }
}