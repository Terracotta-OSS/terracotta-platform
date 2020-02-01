/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2)
public class ActivateCommandIT extends DynamicConfigIT {

  @Test
  public void testMultiNodeSingleStripeActivation() throws Exception {
    ConfigTool.start("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2));
    assertCommandSuccessful();

    activateCluster(() -> {
      waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
      waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

      waitUntil(out::getLog, containsString("No license installed"));
      waitUntil(out::getLog, containsString("came back up"));
    });
  }

  @Test
  public void testMultiNodeSingleStripeActivationWithConfigFile() throws Exception {
    ConfigTool.start(
        "-r", timeout + "s",
        "activate",
        "-f", copyConfigProperty("/config-property-files/single-stripe_multi-node.properties").toString()
    );
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    waitUntil(out::getLog, containsString("No license installed"));
    waitUntil(out::getLog, containsString("came back up"));
    assertCommandSuccessful();
  }

  @Test
  public void testMultiStripeActivation() throws Exception {
    ConfigTool.start("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1));
    assertCommandSuccessful();

    activateCluster(() -> {
      waitUntil(out::getLog, containsString("No license installed"));
      waitUntil(out::getLog, containsString("came back up"));
      waitUntil(out::getLog, stringContainsInOrder(
          Arrays.asList("Moved to State[ ACTIVE-COORDINATOR ]", "Moved to State[ ACTIVE-COORDINATOR ]")
      ));
    });
  }

  @Test
  public void testMultiStripeActivationWithConfigFile() throws Exception {
    ConfigTool.start(
        "-r", timeout + "s",
        "activate",
        "-f", copyConfigProperty("/config-property-files/multi-stripe.properties").toString()
    );
    waitUntil(out::getLog, stringContainsInOrder(
        Arrays.asList("Moved to State[ ACTIVE-COORDINATOR ]", "Moved to State[ ACTIVE-COORDINATOR ]")
    ));

    waitUntil(out::getLog, containsString("No license installed"));
    waitUntil(out::getLog, containsString("came back up"));
    assertCommandSuccessful();
  }
}
