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
import org.terracotta.angela.client.support.junit.NodeOutputRule;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.ConfigurationGenerator;

import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsLog;

@ClusterDefinition(autoStart = false)
public class NodeStartupIT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @Test
  public void testStartingWithNonExistentRepo() {
    startSingleNode("-r", getNodeConfigDir(1, 1).toString());
    waitForDiagnostic(1, 1);
  }

  @Test
  public void testStartingWithSingleNodeConfigFile() {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startNode(1, 1, "--config-file", configurationFile.toString(), "--config-dir", "config/stripe1/node-1");
    waitForDiagnostic(1, 1);
  }

  @Test
  public void testStartingWithSingleNodeConfigFileWithHostPort() {
    String port = String.valueOf(getNodePort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startNode(1, 1, "-f", configurationFile.toString(), "-s", "localhost", "-p", port, "--config-dir", "config/stripe1/node-1");
    waitForDiagnostic(1, 1);
  }

  @Test
  public void testStartingWithSingleNodeConfigFileWithNodeName() {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startNode(1, 1, "-f", configurationFile.toString(), "-n", "node-1-1", "--config-dir", "config/stripe1/node-1");
    waitForDiagnostic(1, 1);
  }

  @Test
  public void testStartingWithConfigFile() throws Exception {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    startNode(1, 1, "--config-file", configurationFile.toString(), "--config-dir", "config/stripe1/node-1");

    waitForDiagnostic(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getNodeHostname(), is(equalTo("localhost")));
  }

  @Test
  public void testFailedStartupConfigFile_nonExistentFile() {
    Path configurationFile = Paths.get(".").resolve("blah.properties");
    try {
      startNode(1, 1, "--config-file", configurationFile.toString(), "--config-dir", "config/stripe1/node-1");
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
      startNode(1, 1, "--config-file", configurationFile.toString(), "--hostname", "localhost", "--port", port, "--config-dir", "config/stripe1/node-1");
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("<port> specified in port=<port> must be an integer between 1 and 65535"));
    }
  }

  @Test
  public void testFailedStartupConfigFile_invalidSecurity() {
    String port = String.valueOf(getNodePort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_invalid2.properties");
    try {
      startNode(1, 1, "--config-file", configurationFile.toString(), "--hostname", "localhost", "--port", port, "--config-dir", "config/stripe1/node-1");
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("security-dir is mandatory for any of the security configuration"));
    }
  }

  @Test
  public void testFailedStartupConfigFile_invalidCliParams() {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    try {
      startSingleNode("--config-file", configurationFile.toString(), "--bind-address", "::1");
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("'--config-file' parameter can only be used with '--repair-mode', '--name', '--hostname', '--port' and '--config-dir' parameters"));
    }
  }

  @Test
  public void testFailedStartupConfigFile_invalidCliParams_2() {
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    try {
      startNode(1, 1, "-f", configurationFile.toString(), "-m", getNodeConfigDir(1, 1).toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("'--config-file' parameter can only be used with '--repair-mode', '--name', '--hostname', '--port' and '--config-dir' parameters"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidAuthc() {
    try {
      startSingleNode("--authc=blah", "-r", getNodeConfigDir(1, 1).toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("authc should be one of: [file, ldap, certificate]"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidHostname() {
    try {
      startNode(1, 1, "--hostname=:::", "-r", getNodeConfigDir(1, 1).toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("<address> specified in hostname=<address> must be a valid hostname or IP address"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidFailoverPriority() {
    try {
      startSingleNode("--failover-priority", "blah", "-r", getNodeConfigDir(1, 1).toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a non-negative integer)"));
    }
  }

  @Test
  public void testFailedStartupCliParams_invalidSecurity() {
    try {
      startSingleNode("--audit-log-dir", "audit-dir", "-r", getNodeConfigDir(1, 1).toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("security-dir is mandatory for any of the security configuration"));
    }
  }

  @Test
  public void testSuccessfulStartupCliParams() {
    startSingleNode("-p", String.valueOf(getNodePort()), "-r", getNodeConfigDir(1, 1).toString());
    waitForDiagnostic(1, 1);
  }

  @Test
  public void testSuccessfulStartupCliParamsContainingSubstitutionParams() throws Exception {
    startSingleNode(
        "--port", String.valueOf(getNodePort()),
        "--group-port", String.valueOf(getNodeGroupPort(1, 1)),
        "--config-dir", getNodeConfigDir(1, 1).toString(),
        "--hostname", "%c"
    );
    waitForDiagnostic(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getNodeHostname(), is(InetAddress.getLocalHost().getCanonicalHostName()));
  }

  @Test
  public void testFailedStartupCliParamsWithConfigFileAndConfigDir() {
    String port = String.valueOf(getNodePort());
    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe.properties");
    try {
      startNode(1, 1,
          "--config-file", configurationFile.toString(),
          "--hostname", "localhost",
          "--port", port,
          "--metadata-dir", "foo"
      );
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("'--config-file' parameter can only be used with '--repair-mode', '--name', '--hostname', '--port' and '--config-dir' parameters"));
    }
  }

  @Test
  public void testStartingNodeWhenMigrationDidNotCommit() throws Exception {
    Path configurationRepo = generateNodeConfigDir(1, 1, ConfigurationGenerator::generate1Stripe1NodeAndSkipCommit);
    try {
      startSingleNode("--config-dir", configurationRepo.toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 1), containsLog("Node has not been activated or migrated properly: unable find the latest committed configuration to use at startup. Please delete the configuration directory and try again."));
      waitUntil(out.getLog(1, 1), not(containsLog("Moved to State[ ACTIVE-COORDINATOR ]")));
    }
  }

  private void startSingleNode(String... args) {
    // these arguments are required to be added to isolate the node data files into the build/test-data directory to not conflict with other processes
    Collection<String> defaultArgs = new ArrayList<>(Arrays.asList(
        "--failover-priority", "availability",
        "--hostname", "localhost",
        "--log-dir", getNodePath(1, 1).resolve("logs").toString(),
        "--backup-dir", getNodePath(1, 1).resolve("backup").toString(),
        "--metadata-dir", getNodePath(1, 1).resolve("metadata").toString(),
        "--data-dirs", "main:" + getNodePath(1, 1).resolve("data-dir").toString()
    ));
    List<String> provided = Arrays.asList(args);
    if (provided.contains("-n")) {
      throw new AssertionError("Do not use -n. use --name instead");
    }
    if (provided.contains("-s") || provided.contains("--hostname")) {
      defaultArgs.remove("--hostname");
      defaultArgs.remove("localhost");
    }
    if (provided.contains("--failover-priority")) {
      defaultArgs.remove("--failover-priority");
      defaultArgs.remove("availability");
    }
    defaultArgs.addAll(provided);
    startNode(1, 1, defaultArgs.toArray(new String[0]));
  }
}