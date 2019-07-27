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
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static org.hamcrest.Matchers.containsString;

public class NewServerStartupScriptIT extends BaseStartupIT {
  @Test
  public void testStartingWithSingleStripeSingleNodeRepo() throws Exception {
    Path configurationRepo = copyServerConfigFiles(1, 1, this::singleStripeSingleNode);
    startServer("--node-config-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    Path configurationRepo = copyServerConfigFiles(1, 2, this::singleStripeMultiNode);
    startServer("--node-config-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithMultiStripeRepo() throws Exception {
    Path configurationRepo = copyServerConfigFiles(2, 1, this::multiStripe);
    startServer("--node-config-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithNonExistentRepo() throws Exception {
    startServer(1, 1, "-c", configRepositoryPath().toString());
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFile() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("--config-file", configurationFile.toString());
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFileWithHostPort() throws Exception {
    String port = String.valueOf(ports.getPort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("-f", configurationFile.toString(), "-s", "localhost", "-p", port);
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFileAndLicense() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("-f", configurationFile.toString(), "-l", licensePath().toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFileLicenseAndClusterName() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("-f", configurationFile.toString(), "-l", licensePath().toString(), "-N", "tc-cluster");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testFailedStartupWithMultiNodeConfigFileAndLicense() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/multi-stripe.properties");
    startServer("-f", configurationFile.toString(), "-l", licensePath().toString(), "-s", "localhost", "-p", String.valueOf(ports.getPorts()[0]));
    waitedAssert(out::getLog, containsString("License file option can be used only with a one-node cluster config file"));
  }

  @Test
  public void testFailedStartupConfigFile_nonExistentFile() throws Exception {
    Path configurationFile = Paths.get(".").resolve("blah");
    startServer("--config-file", configurationFile.toString());
    waitedAssert(out::getLog, containsString("FileNotFoundException"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidPort() throws Exception {
    String port = String.valueOf(ports.getPort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_invalid1.properties");
    startServer("--config-file", configurationFile.toString(), "--node-hostname", "localhost", "--node-port", port);
    waitedAssert(out::getLog, containsString("<port> specified in node-port=<port> must be an integer between 1 and 65535"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidSecurity() throws Exception {
    String port = String.valueOf(ports.getPort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_invalid2.properties");
    startServer("--config-file", configurationFile.toString(), "--node-hostname", "localhost", "--node-port", port);
    waitedAssert(out::getLog, containsString("security-dir is mandatory for any of the security configuration"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidCliParams() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("--config-file", configurationFile.toString(), "--node-bind-address", "::1");
    waitedAssert(out::getLog, containsString("'--config-file' parameter can only be used with '--license-file', '--cluster-name', '--node-hostname' and '--node-port' parameters"));
  }

  @Test
  public void testFailedStartupCliParams_invalidAuthc() throws Exception {
    startServer("--security-authc=blah", "-c", configRepositoryPath().toString());
    waitedAssert(out::getLog, containsString("security-authc should be one of: [file, ldap, certificate]"));
  }

  @Test
  public void testFailedStartupCliParams_invalidHostname() throws Exception {
    startServer("--node-hostname=:::", "-c", configRepositoryPath().toString());
    waitedAssert(out::getLog, containsString("<address> specified in node-hostname=<address> must be a valid hostname or IP address"));
  }

  @Test
  public void testFailedStartupCliParams_invalidFailoverPriority() throws Exception {
    startServer("--failover-priority=blah", "-c", configRepositoryPath().toString());
    waitedAssert(out::getLog, containsString("failover-priority should be one of: [availability, consistency]"));
  }

  @Test
  public void testFailedStartupCliParams_invalidSecurity() throws Exception {
    startServer("--security-audit-log-dir", "audit-dir", "-c", configRepositoryPath().toString());
    waitedAssert(out::getLog, containsString("security-dir is mandatory for any of the security configuration"));
  }

  @Test
  public void testSuccessfulStartupCliParams() throws Exception {
    startServer(1, 1, "-p", String.valueOf(ports.getPort()), "-c", configRepositoryPath().toString());
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testSuccessfulStartupCliParamsWithLicense() throws Exception {
    startServer(1, 1,
        "-p", String.valueOf(ports.getPort()),
        "-l", licensePath().toString(),
        "-N", "tc-cluster",
        "-c", configRepositoryPath().toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testFailedStartupCliParamsWithLicense_noClusterName() throws Exception {
    startServer("-p", String.valueOf(ports.getPort()), "-l", licensePath().toString(), "-c", configRepositoryPath().toString());
    waitedAssert(out::getLog, containsString("'--license-file' parameter must be used with 'cluster-name' parameter"));
  }

  @Test
  public void testFailedStartupCliParamsWithConfigAndConfigDir() throws Exception {
    String port = String.valueOf(ports.getPort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("-f", configurationFile.toString(), "-s", "localhost", "-p", port, "-c", "foo");
    waitedAssert(out::getLog, containsString("'--config-file' parameter can only be used with '--license-file', '--cluster-name', '--node-hostname' and '--node-port' parameters"));
  }

  private void startServer(String... cli) {
    nodeProcesses.add(NodeProcess.startNode(Kit.getOrCreatePath(), getBaseDir(), cli));
  }

  private void startServer(int stripeId, int nodeId, String... cli) {
    String[] args = concat(Stream.of(cli), Stream.of(
        "--node-name", "node-" + nodeId,
        "--node-hostname", "localhost",
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
        "--node-backup-dir", "backup/stripe" + stripeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId
    )).toArray(String[]::new);
    nodeProcesses.add(NodeProcess.startNode(Kit.getOrCreatePath(), getBaseDir(), args));
  }
}