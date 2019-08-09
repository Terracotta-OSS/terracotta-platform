/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.test.util.ConfigRepositoryGenerator;
import com.terracottatech.dynamic_config.test.util.Kit;
import com.terracottatech.dynamic_config.test.util.NodeProcess;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class NewServerStartupScriptIT extends BaseStartupIT {
  @Test
  public void testStartingWithSingleStripeSingleNodeRepo() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 1, ConfigRepositoryGenerator::generate1Stripe1Node);
    startServer("--node-repository-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 2, ConfigRepositoryGenerator::generate1Stripe2Nodes);
    startServer("--node-repository-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithMultiStripeRepo() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(2, 1, ConfigRepositoryGenerator::generate2Stripes2Nodes);
    startServer("--node-repository-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithNonExistentRepo() {
    startServer(1, 1, "-r", getNodeRepositoryDir().toString());
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFile() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("--config-file", configurationFile.toString(), "--node-repository-dir", "repository/stripe1/node-1");
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFileWithHostPort() throws Exception {
    String port = String.valueOf(ports.getPort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("-f", configurationFile.toString(), "-s", "localhost", "-p", port, "--node-repository-dir", "repository/stripe1/node-1");
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFileAndLicense() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("-f", configurationFile.toString(), "-l", licensePath().toString(), "--node-repository-dir", "repository/stripe1/node-1");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFileLicenseAndClusterName() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("-f", configurationFile.toString(), "-l", licensePath().toString(), "-N", "tc-cluster", "--node-repository-dir", "repository/stripe1/node-1");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithConfigFileContainingSubstitutionParams() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_substitution_params.properties");
    startServer("--config-file", configurationFile.toString(), "--node-repository-dir", "repository/stripe1/node-1");

    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
    assertThat(getCluster("localhost", ports.getPort()).getSingleNode().get().getNodeHostname(), is(equalTo("%h")));
  }

  @Test
  public void testStartingWithConfigFileContainingSubstitutionParamsAndLicense() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_substitution_params.properties");
    startServer("--config-file", configurationFile.toString(), "--license-file", licensePath().toString(), "--node-repository-dir", "repository/stripe1/node-1");

    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testFailedStartupWithMultiNodeConfigFileAndLicense() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/multi-stripe.properties");
    startServer("-f", configurationFile.toString(), "-l", licensePath().toString(), "-s", "localhost", "-p", String.valueOf(ports.getPorts()[0]), "--node-repository-dir", "repository/stripe1/node-1");
    waitedAssert(out::getLog, containsString("License file option can be used only with a one-node cluster config file"));
  }

  @Test
  public void testFailedStartupConfigFile_nonExistentFile() {
    Path configurationFile = Paths.get(".").resolve("blah");
    startServer("--config-file", configurationFile.toString(), "--node-repository-dir", "repository/stripe1/node-1");
    waitedAssert(out::getLog, containsString("Failed to read config file"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidPort() throws Exception {
    String port = String.valueOf(ports.getPort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_invalid1.properties");
    startServer("--config-file", configurationFile.toString(), "--node-hostname", "localhost", "--node-port", port, "--node-repository-dir", "repository/stripe1/node-1");
    waitedAssert(out::getLog, containsString("<port> specified in node-port=<port> must be an integer between 1 and 65535"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidSecurity() throws Exception {
    String port = String.valueOf(ports.getPort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_invalid2.properties");
    startServer("--config-file", configurationFile.toString(), "--node-hostname", "localhost", "--node-port", port, "--node-repository-dir", "repository/stripe1/node-1");
    waitedAssert(out::getLog, containsString("security-dir is mandatory for any of the security configuration"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidCliParams() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("--config-file", configurationFile.toString(), "--node-bind-address", "::1");
    waitedAssert(out::getLog, containsString("'--config-file' parameter can only be used with '--license-file', '--cluster-name', '--node-hostname', '--node-port' and '--node-repository-dir' parameters"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidCliParams_2() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer("-f", configurationFile.toString(), "-m", getNodeRepositoryDir().toString());
    waitedAssert(out::getLog, containsString("'--config-file' parameter can only be used with '--license-file', '--cluster-name', '--node-hostname', '--node-port' and '--node-repository-dir' parameters"));
  }

  @Test
  public void testFailedStartupCliParams_invalidAuthc() {
    startServer("--security-authc=blah", "-r", getNodeRepositoryDir().toString());
    waitedAssert(out::getLog, containsString("security-authc should be one of: [file, ldap, certificate]"));
  }

  @Test
  public void testFailedStartupCliParams_invalidHostname() {
    startServer("--node-hostname=:::", "-r", getNodeRepositoryDir().toString());
    waitedAssert(out::getLog, containsString("<address> specified in node-hostname=<address> must be a valid hostname or IP address"));
  }

  @Test
  public void testFailedStartupCliParams_invalidFailoverPriority() {
    startServer("--failover-priority=blah", "-r", getNodeRepositoryDir().toString());
    waitedAssert(out::getLog, containsString("failover-priority should be one of: [availability, consistency]"));
  }

  @Test
  public void testFailedStartupCliParams_invalidSecurity() {
    startServer("--security-audit-log-dir", "audit-dir", "-r", getNodeRepositoryDir().toString());
    waitedAssert(out::getLog, containsString("security-dir is mandatory for any of the security configuration"));
  }

  @Test
  public void testSuccessfulStartupCliParams() {
    startServer(1, 1, "-p", String.valueOf(ports.getPort()), "-r", getNodeRepositoryDir().toString());
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testSuccessfulStartupCliParamsContainingSubstitutionParams() throws Exception {
    startServer(
        1, 1,
        "--node-port", String.valueOf(ports.getPort()),
        "--node-repository-dir", getNodeRepositoryDir().toString(),
        "--node-hostname", "%c"
    );
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
    assertThat(getCluster("localhost", ports.getPort()).getSingleNode().get().getNodeHostname(), is(equalTo("%c")));
  }

  @Test
  //TODO [DYNAMIC-CONFIG]: Un-ignore this test once this bug is fixed in parser
  @Ignore("TCConfigurationParser doesn't substitute tsa-port bind value")
  public void testSuccessfulStartupCliParamsContainingSubstitutionParamsAndLicense() throws Exception {
    startServer(
        1, 1,
        "--node-port", String.valueOf(ports.getPort()),
        "--node-repository-dir", getNodeRepositoryDir().toString(),
        "--node-hostname", "%h",
        "--node-bind-address", "%i",
        "--license-file", licensePath().toString(),
        "--cluster-name", "tc-cluster",
        "--node-repository-dir", "%(user.dir)/repository/stripe1/node-1"
    );
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testSuccessfulStartupCliParamsContainingSubstitutionParamsAndLicense_2() throws Exception {
    startServer(
        1, 1,
        "--node-port", String.valueOf(ports.getPort()),
        "--node-repository-dir", getNodeRepositoryDir().toString(),
        "--node-hostname", "%c",
        "--license-file", licensePath().toString(),
        "--cluster-name", "tc-cluster"
    );
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testSuccessfulStartupCliParamsWithLicense() throws Exception {
    startServer(
        1, 1,
        "--node-port", String.valueOf(ports.getPort()),
        "--license-file", licensePath().toString(),
        "--cluster-name", "tc-cluster",
        "--node-repository-dir", getNodeRepositoryDir().toString()
    );
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testFailedStartupCliParamsWithLicense_noClusterName() throws Exception {
    startServer(
        "--node-port", String.valueOf(ports.getPort()),
        "--license-file", licensePath().toString(),
        "--node-repository-dir", getNodeRepositoryDir().toString()
    );
    waitedAssert(out::getLog, containsString("'--license-file' parameter must be used with 'cluster-name' parameter"));
  }

  @Test
  public void testFailedStartupCliParamsWithConfigFileAndRepositoryDir() throws Exception {
    String port = String.valueOf(ports.getPort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startServer(
        "--config-file", configurationFile.toString(),
        "--node-hostname", "localhost",
        "--node-port", port,
        "--node-metadata-dir", "foo"
    );
    waitedAssert(out::getLog, containsString("'--config-file' parameter can only be used with '--license-file', '--cluster-name', '--node-hostname', '--node-port' and '--node-repository-dir' parameters"));
  }

  private void startServer(String... cli) {
    nodeProcesses.add(NodeProcess.startNode(Kit.getOrCreatePath(), getBaseDir(), cli));
  }

  private void startServer(int stripeId, int nodeId, String... args) {
    // these arguments are required to be added to isolate the node data files into the build/test-data directory to not conflict with other processes
    Collection<String> defaultArgs = new ArrayList<>(Arrays.asList(
        "--node-name", "node-" + nodeId,
        "--node-hostname", "localhost",
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
        "--node-backup-dir", "backup/stripe" + stripeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId
    ));
    List<String> provided = Arrays.asList(args);
    if (provided.contains("-n") || provided.contains("--node-name")) {
      defaultArgs.remove("--node-name");
      defaultArgs.remove("node-" + nodeId);
    }
    if (provided.contains("-s") || provided.contains("--node-hostname")) {
      defaultArgs.remove("--node-hostname");
      defaultArgs.remove("localhost");
    }
    defaultArgs.addAll(provided);
    nodeProcesses.add(NodeProcess.startNode(Kit.getOrCreatePath(), getBaseDir(), defaultArgs.toArray(new String[0])));
  }
}