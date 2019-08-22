/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.containsString;

/**
 * @author Mathieu Carbou
 */
public class ConfigRepositoryIT extends BaseStartupIT {
  @Before
  public void setUp() throws Exception {
    startNode(
        "--node-name", "node-1",
        "--node-hostname", "localhost",
        "--node-port", "" + ports.getPort(),
        "--node-log-dir", "logs/stripe1/node-1",
        "--node-backup-dir", "backup/stripe1",
        "--node-metadata-dir", "metadata/stripe1",
        "--data-dirs", "main:user-data/main/stripe1",
        "--node-repository-dir", "repository/stripe1/node-1",
        "-N", "tc-cluster",
        "--license-file", licensePath().toString()
    );
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void test_prevent_concurrent_use() throws Exception {
    startNode("--node-name", "node-1", "--node-repository-dir", "repository/stripe1/node-1");
    waitedAssert(out::getLog, containsString("Exception initializing Nomad Server: java.io.IOException: File lock already held: " + getBaseDir().resolve(Paths.get("repository", "stripe1", "node-1", "sanskrit"))));
  }
}
