/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.ConfigRepositoryGenerator;
import org.terracotta.dynamic_config.test_support.util.NodeOutputRule;

import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsLog;

@ClusterDefinition(autoStart = false)
public class NodeStartupIT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @Test
  public void testStartingWithNonExistentRepo() {
    startSingleNode("-r", getNodeRepositoryDir().toString());
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFile() {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startNode(1, 1, "--config-file", configurationFile.toString(), "--node-repository-dir", "repository/stripe1/node-1");
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFileWithHostPort() {
    String port = String.valueOf(getNodePort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startNode(1, 1, "-f", configurationFile.toString(), "-s", "localhost", "-p", port, "--node-repository-dir", "repository/stripe1/node-1");
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithConfigFile() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startNode(1, 1, "--config-file", configurationFile.toString(), "--node-repository-dir", "repository/stripe1/node-1");

    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getNodeHostname(), is(equalTo("localhost")));
  }

  @Test
  public void testFailedStartupConfigFile_nonExistentFile() {
    Path configurationFile = Paths.get(".").resolve("blah");
    try {
      startNode(1, 1, "--config-file", configurationFile.toString(), "--node-repository-dir", "repository/stripe1/node-1");
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("Failed to read config file"));
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
      waitUntil(out.getLog(1, 1), containsLog("<port> specified in node-port=<port> must be an integer between 1 and 65535"));
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
      waitUntil(out.getLog(1, 1), containsLog("security-dir is mandatory for any of the security configuration"));
    }
  }

  @Test
  public void testFailedStartupConfigFile_invalidCliParams() {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    try {
      startSingleNode("--config-file", configurationFile.toString(), "--node-bind-address", "::1");
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("'--config-file' parameter can only be used with '--diagnostic-mode', '--license-file', '--cluster-name', '--node-hostname', '--node-port' and '--node-repository-dir' parameters"));
    }
  }

  @Test
  public void testFailedStartupConfigFile_invalidCliParams_2() {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    try {
      startNode(1, 1, "-f", configurationFile.toString(), "-m", getNodeRepositoryDir().toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("'--config-file' parameter can only be used with '--diagnostic-mode', '--license-file', '--cluster-name', '--node-hostname', '--node-port' and '--node-repository-dir' parameters"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidAuthc() {
    try {
      startSingleNode("--security-authc=blah", "-r", getNodeRepositoryDir().toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("security-authc should be one of: [file, ldap, certificate]"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidHostname() {
    try {
      startNode(1, 1, "--node-hostname=:::", "-r", getNodeRepositoryDir().toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("<address> specified in node-hostname=<address> must be a valid hostname or IP address"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidFailoverPriority() {
    try {
      startSingleNode("--failover-priority=blah", "-r", getNodeRepositoryDir().toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidSecurity() {
    try {
      startSingleNode("--security-audit-log-dir", "audit-dir", "-r", getNodeRepositoryDir().toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("security-dir is mandatory for any of the security configuration"));
    }
  }

  @Test
  public void testSuccessfulStartupCliParams() {
    startSingleNode("-p", String.valueOf(getNodePort()), "-r", getNodeRepositoryDir().toString());
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));
  }

  @Test
  public void testSuccessfulStartupCliParamsContainingSubstitutionParams() throws Exception {
    startSingleNode(
        "--node-port", String.valueOf(getNodePort()),
        "--node-group-port", String.valueOf(getNodeGroupPort()),
        "--node-repository-dir", getNodeRepositoryDir().toString(),
        "--node-hostname", "%c"
    );
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getNodeHostname(), is(InetAddress.getLocalHost().getCanonicalHostName()));
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
      waitUntil(out.getLog(1, 1), containsLog("'--config-file' parameter can only be used with '--diagnostic-mode', '--license-file', '--cluster-name', '--node-hostname', '--node-port' and '--node-repository-dir' parameters"));
    }
  }

  @Test
  public void testStartingNodeWhenMigrationDidNotCommit() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 1, ConfigRepositoryGenerator::generate1Stripe1NodeAndSkipCommit);
    try {
      startSingleNode("--node-repository-dir", configurationRepo.toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("The configuration of this node has not been committed or rolled back. Please run the 'diagnostic' command to diagnose the configuration state."));
      waitUntil(out.getLog(1, 1), containsLog("java.lang.IllegalStateException: Node has not been activated or migrated properly: unable find the latest committed configuration to use at startup. Please delete the repository folder and try again."));
      waitUntil(out.getLog(1, 1), not(containsLog("Moved to State[ ACTIVE-COORDINATOR ]")));
    }
  }

  private void startSingleNode(String... args) {
    // these arguments are required to be added to isolate the node data files into the build/test-data directory to not conflict with other processes
    Collection<String> defaultArgs = new ArrayList<>(Arrays.asList(
        "--node-name", "node-1-1",
        "--node-hostname", "localhost",
        "--node-log-dir", "terracotta1-1/logs",
        "--node-backup-dir", "terracotta1-1/backup",
        "--node-metadata-dir", "terracotta1-1/metadata",
        "--data-dirs", "main:terracotta1-1/data-dir"
    ));
    List<String> provided = Arrays.asList(args);
    if(provided.contains("-n")) {
      throw new AssertionError("Do not use -n. use --node-name instead");
    }
    if (provided.contains("--node-name")) {
      defaultArgs.remove("--node-name");
      defaultArgs.remove("node-1-1");
    }
    if (provided.contains("-s") || provided.contains("--node-hostname")) {
      defaultArgs.remove("--node-hostname");
      defaultArgs.remove("localhost");
    }
    defaultArgs.addAll(provided);
    startNode(1, 1, defaultArgs.toArray(new String[0]));
  }
}