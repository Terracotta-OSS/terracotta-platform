/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;
import org.terracotta.dynamic_config.system_tests.util.ConfigRepositoryGenerator;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class Ipv6ConfigActivationIT extends DynamicConfigIT {
  @Test
  public void testStartupFromMigratedConfigRepoAndGetCommand() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 1, ConfigRepositoryGenerator::generate1Stripe1NodeIpv6);
    startNode(1, 1, "--node-repository-dir", configurationRepo.toString());
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

    configToolInvocation("get", "-s", "[::1]:" + getNodePort(), "-c", "offheap-resources.main");
    waitUntil(out::getLog, containsString("offheap-resources.main=512MB"));
  }
}
