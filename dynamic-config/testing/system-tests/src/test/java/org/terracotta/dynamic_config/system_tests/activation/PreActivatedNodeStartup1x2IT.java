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
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.ConfigRepositoryGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.fail;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class PreActivatedNodeStartup1x2IT extends DynamicConfigIT {

  @Rule public final SystemErrRule err = new SystemErrRule().enableLog();

  @Test
  public void testStartingWithSingleStripeSingleNodeRepo() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 1, ConfigRepositoryGenerator::generate1Stripe1Node);
    startSingleNode("--node-repository-dir", configurationRepo.toString());
    waitForActive(1, 1);
  }

  @Test
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 2, ConfigRepositoryGenerator::generate1Stripe2Nodes);
    startNode(1, 2, "--node-repository-dir", configurationRepo.toString());
    waitForActive(1, 2);
  }

  @Test
  public void testPreventConcurrentUseOfRepository() throws Exception {
    // Angela work dirs are different for each server instance. We'd need to create a repo at a common place for this test
    String sharedRepo = Files.createDirectories(getBaseDir()).toAbsolutePath().toString();
    startNode(1, 1, "--node-name", "node-1-1", "-r", sharedRepo, "-p", String.valueOf(getNodePort()), "-g", String.valueOf(getNodeGroupPort()), "-N", "tc-cluster");
    waitForActive(1, 1);

    try {
      startNode(1, 2,
          "--node-name", "node-1-2",
          "--node-repository-dir", sharedRepo,
          "-p", String.valueOf(getNodePort(1, 2)),
          "-g", String.valueOf(getNodeGroupPort(1, 2)),
          "--node-hostname", "localhost",
          "--node-log-dir", "terracotta1-2/logs",
          "--node-backup-dir", "terracotta1-2/backup",
          "--node-metadata-dir", "terracotta1-2/metadata",
          "--data-dirs", "main:terracotta1-2/data-dir");
      fail();
    } catch (Exception e) {
      waitUntil(err::getLog, containsString("Exception initializing Nomad Server: java.io.IOException: File lock already held: " + Paths.get(sharedRepo, "sanskrit")));
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
    if (provided.contains("-n")) {
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