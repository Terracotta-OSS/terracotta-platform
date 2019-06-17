/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.test.util.Kit;
import com.terracottatech.dynamic_config.test.util.NodeProcess;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.containsString;

public class NewServerStartupScriptIT extends BaseStartupIT {
  @Test
  public void testStartingWithSingleStripeSingleNodeRepo() throws Exception {
    String stripeName = "stripe1";
    String nodeName = "testServer1";
    Path configurationRepo = configRepoPath(singleStripeSingleNodeNomadRoot(stripeName, nodeName));
    startServer("--node-config-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    String stripeName = "stripe1";
    String nodeName = "testServer2";
    Path configurationRepo = configRepoPath(singleStripeMultiNodeNomadRoot(stripeName, nodeName));
    startServer("--node-config-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithMultiStripeRepo() throws Exception {
    String stripeName = "stripe2";
    String nodeName = "testServer1";
    Path configurationRepo = configRepoPath(multiStripeNomadRoot(stripeName, nodeName));
    startServer("--node-config-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithNonExistentRepo() throws Exception {
    String configurationRepo = temporaryFolder.newFolder().getAbsolutePath();
    startServer("-c", configurationRepo);
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFile() throws Exception {
    Path configurationFile = configFilePath();
    startServer("--config-file", configurationFile.toString());
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFileWithHostPort() throws Exception {
    String port = String.valueOf(ports.getPort());
    Path configurationFile = configFilePath("", port);
    startServer("-f", configurationFile.toString(), "-s", "localhost", "-p", port);
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testFailedStartupConfigFile_nonExistentFile() {
    Path configurationFile = Paths.get(".").resolve("blah");
    startServer("--config-file", configurationFile.toString());
    waitedAssert(out::getLog, containsString("FileNotFoundException"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidPort() throws Exception {
    String port = String.valueOf(ports.getPort());
    Path configurationFile = configFilePath("_invalid1", port);
    startServer("--config-file", configurationFile.toString(), "--node-hostname", "localhost", "--node-port", port);
    waitedAssert(out::getLog, containsString("<port> specified in node-port=<port> must be an integer between 1 and 65535"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidSecurity() throws Exception {
    String port = String.valueOf(ports.getPort());
    Path configurationFile = configFilePath("_invalid2", port);
    startServer("--config-file", configurationFile.toString(), "--node-hostname", "localhost", "--node-port", port);
    waitedAssert(out::getLog, containsString("security-dir is mandatory for any of the security configuration"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidCliParams() throws Exception {
    Path configurationFile = configFilePath();
    startServer("--config-file", configurationFile.toString(), "--node-bind-address", "::1");
    waitedAssert(out::getLog, containsString("'--config-file' parameter can only be used with '--node-hostname', '--node-port', and '--node-config-dir' parameters"));
  }

  @Test
  public void testFailedStartupCliParams_invalidAuthc() {
    startServer("--security-authc=blah");
    waitedAssert(out::getLog, containsString("security-authc should be one of: [file, ldap, certificate]"));
  }

  @Test
  public void testFailedStartupCliParams_invalidHostname() {
    startServer("--node-hostname=:::");
    waitedAssert(out::getLog, containsString("<address> specified in node-hostname=<address> must be a valid hostname or IP address"));
  }

  @Test
  public void testFailedStartupCliParams_invalidFailoverPriority() {
    startServer("--failover-priority=blah");
    waitedAssert(out::getLog, containsString("failover-priority should be one of: [availability, consistency]"));
  }

  @Test
  public void testFailedStartupCliParams_invalidSecurity() {
    startServer("--security-audit-log-dir", "audit-dir");
    waitedAssert(out::getLog, containsString("security-dir is mandatory for any of the security configuration"));
  }

  @Test
  public void testSuccessfulStartupCliParams() {
    startServer("-p", String.valueOf(ports.getPort()));
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  private void startServer(String... cli) {
    nodeProcess = NodeProcess.startNode(Kit.getOrCreatePath(), cli);
  }
}