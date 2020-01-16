/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.utilities.PathResolver;
import com.terracottatech.utilities.junit.TmpDir;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.config.FailoverPriority;
import org.terracotta.config.Servers;
import org.terracotta.config.TcConfig;

import static com.terracottatech.dynamic_config.model.FailoverPriority.consistency;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConsistencyModeTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir();

  PathResolver pathResolver;

  @Before
  public void setUp() {
    pathResolver = new PathResolver(temporaryFolder.getRoot());
  }

  @Test
  public void testCreation() {
    Node node = Node.newDefaultNode("localhost");
    node.setFailoverPriority(consistency());

    Servers servers = new Servers();

    ServerConfiguration serverConfiguration = new ServerConfiguration(node, servers, pathResolver);

    TcConfig tcConfig = serverConfiguration.getTcConfig();

    assertThat(tcConfig, notNullValue());
    assertThat(tcConfig.getServers(), is(servers));

    verifyFailOverPriority(tcConfig);
  }

  private static void verifyFailOverPriority(TcConfig tcConfig) {
    FailoverPriority failoverPriority = tcConfig.getFailoverPriority();
    assertThat(failoverPriority, notNullValue());
    assertThat(failoverPriority.getConsistency(), notNullValue());
    assertThat(failoverPriority.getConsistency().getVoter(), nullValue());
  }
}