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
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;

public class Ipv6IT extends BaseStartupIT {
  public Ipv6IT() {
    super(2, 1);
  }

  @Test
  public void testSingleNodeStartupFromCliParamsAndActivateCommand() {
    startSingleNode(1);
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));

    int[] ports = this.ports.getPorts();
    ConfigTool.start("activate", "-s", "[::1]:" + ports[0], "-n", "tc-cluster");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testMultiNodeStartupFromCliParamsAndActivateCommand() {
    startSingleNode(1);
    startSingleNode(2);
    waitedAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));

    int[] ports = this.ports.getPorts();
    ConfigTool.start("attach", "-d", "[::1]:" + ports[0], "-s", "[::1]:" + ports[1]);
    assertCommandSuccessful();

    ConfigTool.start("activate", "-s", "[::1]:" + ports[0], "-n", "tc-cluster");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitedAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
  }

  @Test
  public void testStartupFromConfigFileAndExportCommand() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_multi-node_ipv6.properties");
    NodeProcess nodeProcess = startNode("-f", configurationFile.toString(), "-s", "[::1]", "-p", String.valueOf(ports.getPort()), "--node-repository-dir", "repository/stripe1/node-1");
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));

    ConfigTool.start("export", "-s", "[::1]:" + ports.getPorts()[0], "-f", "build/output.json", "-t", "json");

    nodeProcess.close();
    startNode("-f", configurationFile.toString(), "-s", "::1", "-p", String.valueOf(ports.getPort()), "--node-repository-dir", "repository/stripe1/node-1");
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartupFromMigratedConfigRepoAndGetCommand() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 1, ConfigRepositoryGenerator::generate1Stripe1NodeIpv6);
    startNode("--node-repository-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

    ConfigTool.main("get", "-s", "[::1]:" + ports.getPort(), "-c", "offheap-resources.main");
    waitedAssert(out::getLog, containsString("offheap-resources.main=512MB"));
  }

  private void startSingleNode(int nodeId) {
    // these arguments are required to be added to isolate the node data files into the build/test-data directory to not conflict with other processes
    Collection<String> args = Arrays.asList(
        "--node-name", "node-" + nodeId,
        "--node-hostname", "::1",
        "--node-bind-address", "::",
        "--node-group-bind-address", "::",
        "--node-port", String.valueOf(ports.getPorts()[nodeId - 1]),
        "--node-group-port", String.valueOf(ports.getPorts()[nodeId - 1] + 10),
        "--node-log-dir", "logs/stripe1/node-" + nodeId,
        "--node-backup-dir", "backup/stripe1",
        "--node-metadata-dir", "metadata/stripe1",
        "--node-repository-dir", "repository/stripe1/node-" + nodeId,
        "--data-dirs", "main:user-data/main/stripe1"
    );
    startNode(args.toArray(new String[0]));
  }
}
