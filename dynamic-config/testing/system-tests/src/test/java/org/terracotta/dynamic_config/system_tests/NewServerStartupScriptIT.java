/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.util.ConfigRepositoryGenerator;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;

public class NewServerStartupScriptIT extends BaseStartupIT {

  public NewServerStartupScriptIT() {
    super(2, 2);
  }

  @Test
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 2, ConfigRepositoryGenerator::generate1Stripe2Nodes);
    startNode("--node-repository-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithMultiStripeRepo() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(2, 1, ConfigRepositoryGenerator::generate2Stripes2Nodes);
    startNode("--node-repository-dir", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }
}