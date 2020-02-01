/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;
import org.terracotta.dynamic_config.system_tests.util.ConfigRepositoryGenerator;
import org.terracotta.dynamic_config.system_tests.util.NodeProcess;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class Ipv6ConfigIT extends DynamicConfigIT {

  @Test
  public void testStartupFromConfigFileAndExportCommand() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_multi-node_ipv6.properties");
    NodeProcess nodeProcess = startNode(1, 1, "-f", configurationFile.toString(), "-s", "[::1]", "-p", String.valueOf(getNodePort()), "--node-repository-dir", "repository/stripe1/node-1");
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));

    ConfigTool.start("export", "-s", "[::1]:" + getNodePort(), "-f", "build/output.json", "-t", "json");

    nodeProcess.close();
    startNode(1, 1, "-f", configurationFile.toString(), "-s", "::1", "-p", String.valueOf(getNodePort()), "--node-repository-dir", "repository/stripe1/node-1");
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartupFromMigratedConfigRepoAndGetCommand() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 1, ConfigRepositoryGenerator::generate1Stripe1NodeIpv6);
    startNode(1, 1, "--node-repository-dir", configurationRepo.toString());
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

    ConfigTool.main("get", "-s", "[::1]:" + getNodePort(), "-c", "offheap-resources.main");
    waitUntil(out::getLog, containsString("offheap-resources.main=512MB"));
  }
}
