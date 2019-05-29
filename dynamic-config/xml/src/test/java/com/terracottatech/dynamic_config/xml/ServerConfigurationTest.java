/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.terracotta.config.Config;
import org.terracotta.config.FailoverPriority;
import org.terracotta.config.Servers;
import org.terracotta.config.Service;
import org.terracotta.config.Services;
import org.terracotta.config.TcConfig;

import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

public class ServerConfigurationTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testCreation() throws IOException {
    Node node = new Node();
    node.setOffheapResource("main", 100, MemoryUnit.MB);
    node.setNodeBackupDir(temporaryFolder.newFolder().toPath());
    node.setSecurityDir(temporaryFolder.newFolder().toPath());
    node.setClientLeaseDuration(10, TimeUnit.MINUTES);
    node.setDataDir("root", temporaryFolder.newFolder().toPath());
    node.setNodeMetadataDir(temporaryFolder.newFolder().toPath());
    node.setFailoverPriority("consistency:2");

    Servers servers = new Servers();

    ServerConfiguration serverConfiguration = new ServerConfiguration(node, servers);

    TcConfig tcConfig = serverConfiguration.getTcConfig();

    assertThat(tcConfig, notNullValue());
    assertThat(tcConfig.getServers(), is(servers));

    verifyPlugins(tcConfig);
    verifyFailOverPriority(tcConfig);
  }

  private static void verifyFailOverPriority(TcConfig tcConfig) {
    FailoverPriority failoverPriority = tcConfig.getFailoverPriority();
    assertThat(failoverPriority, notNullValue());
    assertThat(failoverPriority.getConsistency(), notNullValue());
    assertThat(failoverPriority.getConsistency().getVoter(), notNullValue());
    assertThat(failoverPriority.getConsistency().getVoter().getCount(), is(2));
  }

  private static void verifyPlugins(TcConfig tcConfig) {
    Services plugins = tcConfig.getPlugins();

    assertThat(plugins, notNullValue());

    List<Object> configOrService = plugins.getConfigOrService();
    assertThat(configOrService, notNullValue());

    Set<String> actualPlugins = new HashSet<>();

    for (Object obj : configOrService) {
      if (obj instanceof Config) {
        actualPlugins.add(((Config)obj).getConfigContent().getTagName());
      } else {
        actualPlugins.add(((Service)obj).getServiceContent().getTagName());
      }
    }

    Set<String> expectedPlugins =
        new HashSet<>(Arrays.asList("offheap-resources", "connection-leasing", "data-directories", "backup-restore", "security"));

    assertThat(actualPlugins, is(expectedPlugins));
  }
}