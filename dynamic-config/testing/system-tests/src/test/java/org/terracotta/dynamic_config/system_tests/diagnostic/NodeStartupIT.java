/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;
import org.terracotta.dynamic_config.system_tests.util.ConfigRepositoryGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@ClusterDefinition(autoStart = false)
public class NodeStartupIT extends DynamicConfigIT {

  @Test
  public void testStartingWithNonExistentRepo() {
    startSingleNode("-r", getNodeRepositoryDir().toString());
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFile() {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startNode(1, 1, "--config-file", configurationFile.toString(), "--node-repository-dir", "repository/stripe1/node-1");
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFileWithHostPort() {
    String port = String.valueOf(getNodePort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startNode(1, 1, "-f", configurationFile.toString(), "-s", "localhost", "-p", port, "--node-repository-dir", "repository/stripe1/node-1");
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithConfigFile() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startNode(1, 1, "--config-file", configurationFile.toString(), "--node-repository-dir", "repository/stripe1/node-1");

    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getNodeHostname(), is(equalTo("localhost")));
  }

  @Test
  public void testFailedStartupConfigFile_nonExistentFile() {
    Path configurationFile = Paths.get(".").resolve("blah");
    try {
      startNode(1, 1, "--config-file", configurationFile.toString(), "--node-repository-dir", "repository/stripe1/node-1");
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("Failed to read config file"));
    }
  }

  @Test
  public void testFailedStartupConfigFile_invalidPort() {
    String port = String.valueOf(getNodePort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_invalid1.properties");
    try {
      startNode(1, 1, "--config-file", configurationFile.toString(), "--node-hostname", "localhost", "--node-port", port, "--node-repository-dir", "repository/stripe1/node-1");
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("<port> specified in node-port=<port> must be an integer between 1 and 65535"));
    }
  }

  @Test
  public void testFailedStartupConfigFile_invalidSecurity() {
    String port = String.valueOf(getNodePort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_invalid2.properties");
    try {
      startNode(1, 1, "--config-file", configurationFile.toString(), "--node-hostname", "localhost", "--node-port", port, "--node-repository-dir", "repository/stripe1/node-1");
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("security-dir is mandatory for any of the security configuration"));
    }
  }

  @Test
  public void testFailedStartupConfigFile_invalidCliParams() {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    try {
      startSingleNode("--config-file", configurationFile.toString(), "--node-bind-address", "::1");
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("'--config-file' parameter can only be used with '--diagnostic-mode', '--license-file', '--cluster-name', '--node-hostname', '--node-port' and '--node-repository-dir' parameters"));
    }
  }

  @Test
  public void testFailedStartupConfigFile_invalidCliParams_2() {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    try {
      startNode(1, 1, "-f", configurationFile.toString(), "-m", getNodeRepositoryDir().toString());
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("'--config-file' parameter can only be used with '--diagnostic-mode', '--license-file', '--cluster-name', '--node-hostname', '--node-port' and '--node-repository-dir' parameters"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidAuthc() {
    try {
      startSingleNode("--security-authc=blah", "-r", getNodeRepositoryDir().toString());
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("security-authc should be one of: [file, ldap, certificate]"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidHostname() {
    try {
      startNode(1, 1, "--node-hostname=:::", "-r", getNodeRepositoryDir().toString());
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("<address> specified in node-hostname=<address> must be a valid hostname or IP address"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidFailoverPriority() {
    try {
      startSingleNode("--failover-priority=blah", "-r", getNodeRepositoryDir().toString());
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidSecurity() {
    try {
      startSingleNode("--security-audit-log-dir", "audit-dir", "-r", getNodeRepositoryDir().toString());
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("security-dir is mandatory for any of the security configuration"));
    }
  }

  @Test
  public void testSuccessfulStartupCliParams() {
    startSingleNode("-p", String.valueOf(getNodePort()), "-r", getNodeRepositoryDir().toString());
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testSuccessfulStartupCliParamsContainingSubstitutionParams() throws Exception {
    startSingleNode(
        "--node-port", String.valueOf(getNodePort()),
        "--node-group-port", String.valueOf(getNodeGroupPort()),
        "--node-repository-dir", getNodeRepositoryDir().toString(),
        "--node-hostname", "%c"
    );
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getNodeHostname(), is(PARAMETER_SUBSTITUTOR.substitute("%c")));
  }

  @Test
  public void testFailedStartupCliParamsWithConfigFileAndRepositoryDir() {
    String port = String.valueOf(getNodePort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    try {
      startNode(1, 1,
          "--config-file", configurationFile.toString(),
          "--node-hostname", "localhost",
          "--node-port", port,
          "--node-metadata-dir", "foo"
      );
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("'--config-file' parameter can only be used with '--diagnostic-mode', '--license-file', '--cluster-name', '--node-hostname', '--node-port' and '--node-repository-dir' parameters"));
    }
  }

  @Test
  public void testStartingNodeWhenMigrationDidNotCommit() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 1, ConfigRepositoryGenerator::generate1Stripe1NodeAndSkipCommit);
    try {
      startSingleNode("--node-repository-dir", configurationRepo.toString());
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("The configuration of this node has not been committed or rolled back. Please run the 'diagnostic' command to diagnose the configuration state."));
      waitUntil(out::getLog, containsString("java.lang.IllegalStateException: Node has not been activated or migrated properly: unable find the latest committed configuration to use at startup. Please delete the repository folder and try again."));
      waitUntil(out::getLog, not(containsString("Moved to State[ ACTIVE-COORDINATOR ]")));
    }
  }

  private void startSingleNode(String... args) {
    // these arguments are required to be added to isolate the node data files into the build/test-data directory to not conflict with other processes
    Collection<String> defaultArgs = new ArrayList<>(Arrays.asList(
        "--node-name", "node1-1",
        "--node-hostname", "localhost",
        "--node-log-dir", "terracotta1-1/logs",
        "--node-backup-dir", "terracotta1-1/backup",
        "--node-metadata-dir", "terracotta1-1/metadata",
        "--data-dirs", "main:terracotta1-1/data-dir"
    ));
    List<String> provided = Arrays.asList(args);
    if (provided.contains("-n") || provided.contains("--node-name")) {
      defaultArgs.remove("--node-name");
      defaultArgs.remove("node1-1");
    }
    if (provided.contains("-s") || provided.contains("--node-hostname")) {
      defaultArgs.remove("--node-hostname");
      defaultArgs.remove("localhost");
    }
    defaultArgs.addAll(provided);
    startNode(1, 1, defaultArgs.toArray(new String[0]));
  }
}