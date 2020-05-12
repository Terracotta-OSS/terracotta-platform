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
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.angela.client.support.junit.NodeOutputRule;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.ConfigurationGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsLog;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class PreActivatedNodeStartup1x2IT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @Test
  public void testStartingWithSingleStripeSingleNodeRepo() throws Exception {
    Path configurationRepo = generateNodeConfigDir(1, 1, ConfigurationGenerator::generate1Stripe1Node);
    startSingleNode("--config-dir", configurationRepo.toString());
    waitForActive(1, 1);
  }

  @Test
  public void testStartupAfterConfigConversionWithMinimalConfig() throws Exception {
    Path configurationRepo = generateNodeConfigDir(1, 1, ConfigurationGenerator::generate1Stripe1NodeMinimal);
    startNode(1, 1, "--config-dir", configurationRepo.toString());
    waitUntil(() -> angela.tsa().getActives().size(), is(1));
  }

  @Test
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    Path configurationRepo = generateNodeConfigDir(1, 2, ConfigurationGenerator::generate1Stripe2Nodes);
    startNode(1, 2, "--auto-activate", "--config-dir", configurationRepo.toString());
    waitForActive(1, 2);
  }

  @Test
  public void testPreventConcurrentUseOfConfigDir() throws Exception {
    // Angela work dirs are different for each server instance. We'd need to create a repo at a common place for this test
    String sharedRepo = Files.createDirectories(getBaseDir()).toAbsolutePath().toString();
    startNode(1, 1, "--auto-activate", "--failover-priority", "availability", "--name", "node-1-1", "-r", sharedRepo, "-p", String.valueOf(getNodePort()), "-g", String.valueOf(getNodeGroupPort(1, 1)), "-N", "tc-cluster");
    waitForActive(1, 1);

    try {
      out.clearLog(1, 2);
      startNode(1, 2,
          "--auto-activate",
          "--failover-priority", "availability",
          "--name", getNodeName(1, 2),
          "--config-dir", sharedRepo,
          "-p", String.valueOf(getNodePort(1, 2)),
          "-g", String.valueOf(getNodeGroupPort(1, 2)),
          "--hostname", "localhost",
          "--log-dir", getNodePath(1, 2).resolve("logs").toString(),
          "--backup-dir", getNodePath(1, 2).resolve("backup").toString(),
          "--metadata-dir", getNodePath(1, 2).resolve("metadata").toString(),
          "--data-dirs", "main:" + getNodePath(1, 2).resolve("data-dir").toString());
      fail();
    } catch (Exception e) {
      waitUntil(out.getLog(1, 2), containsLog("Exception initializing Nomad Server: java.io.IOException: File lock already held: " + Paths.get(sharedRepo, "changes")));
    }
  }

  private void startSingleNode(String... args) {
    // these arguments are required to be added to isolate the node data files into the build/test-data directory to not conflict with other processes
    Collection<String> defaultArgs = new ArrayList<>(Arrays.asList(
        "--auto-activate",
        "--failover-priority", "availability",
        "--name", getNodeName(1, 1),
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
    if (provided.contains("--name")) {
      defaultArgs.remove("--name");
      defaultArgs.remove(getNodeName(1, 1));
    }
    if (provided.contains("-s") || provided.contains("--hostname")) {
      defaultArgs.remove("--hostname");
      defaultArgs.remove("localhost");
    }
    defaultArgs.addAll(provided);
    startNode(1, 1, defaultArgs.toArray(new String[0]));
  }
}