/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.system_tests;

import com.terracottatech.dynamic_config.system_tests.util.ConfigRepositoryGenerator;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;

public class OldServerStartupScriptIT extends BaseStartupIT {
  public OldServerStartupScriptIT() {
    super(2, 2);
  }

  @Test
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    String configurationRepo = generateNodeRepositoryDir(1, 2, ConfigRepositoryGenerator::generate1Stripe2Nodes).toString();
    startTcServer("-r", configurationRepo, "-n", "node-2", "--node-name", "node-2");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithMultiStripeRepo() throws Exception {
    String configurationRepo = generateNodeRepositoryDir(2, 1, ConfigRepositoryGenerator::generate2Stripes2Nodes).toString();
    startTcServer("-r", configurationRepo, "-n", "node-1", "--node-name", "node-1");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }
}