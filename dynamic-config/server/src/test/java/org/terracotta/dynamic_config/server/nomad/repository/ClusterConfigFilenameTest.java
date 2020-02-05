/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.repository;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ClusterConfigFilenameTest {
  @Test
  public void testValidFilenames() {
    String[] validFilenames = {
        "A.1.xml",
        "1.10.xml",
        "@.100.xml",
        "a.9.xml",
        "node-1.19.xml",
        "server-1-abc_1234@@#*$.199.xml",
        "A.B.1.xml"
    };

    for (String fileName : validFilenames) {
      assertThat(ClusterConfigFilename.from(fileName).getNodeName(), notNullValue());
    }
  }

  @Test
  public void testInvalidFilenames() {
    String[] invalidFilenames = {
        "1.xml",
        " .1.xml",
        "09.xml",
        "cluster-configA1.xml",
        "cluster-configA1Bxml",
    };

    for (String fileName : invalidFilenames) {
      assertThat(fileName, ClusterConfigFilename.from(fileName).getNodeName(), nullValue());
    }
  }

  @Test
  public void testGetNodeName() {
    assertThat(ClusterConfigFilename.from("node-1.19.xml").getNodeName(), is("node-1"));
    assertThat(ClusterConfigFilename.from("server-1-abc_1234@@#*$.199.xml").getNodeName(), is("server-1-abc_1234@@#*$"));
  }

  @Test
  public void testGetVersion() {
    assertThat(ClusterConfigFilename.from("node-1.19.xml").getVersion(), is(19L));
    assertThat(ClusterConfigFilename.from("server-1-abc_1234@@#*$.199.xml").getVersion(), is(199L));
  }
}