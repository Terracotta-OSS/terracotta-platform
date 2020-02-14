/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.util.ConfigRepositoryGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.fail;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2, autoStart = false)
public class NodeStartupIT extends DynamicConfigIT {

  @Test
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 2, ConfigRepositoryGenerator::generate1Stripe2Nodes);
    startNode(1, 2, "--node-repository-dir", configurationRepo.toString());
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithMultiStripeRepo() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(2, 1, ConfigRepositoryGenerator::generate2Stripes2Nodes);
    startNode(2, 1, "--node-repository-dir", configurationRepo.toString());
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testPreventConcurrentUseOfRepository() throws Exception {
    // Angela work dirs are different for each server instance. We'd need to create a repo at a common place for this test
    String sharedRepo = Files.createDirectories(getBaseDir()).toAbsolutePath().toString();
    startNode(1, 1, "-n", "node1-1", "-r", sharedRepo, "-p", String.valueOf(getNodePort()), "-g", String.valueOf(getNodeGroupPort()), "-N", "tc-cluster");
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

    try {
      startNode(1, 2,
          "--node-name", "node1-2",
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
      waitUntil(out::getLog, containsString("Exception initializing Nomad Server: java.io.IOException: File lock already held: " + Paths.get(sharedRepo, "sanskrit")));
    }
  }
}