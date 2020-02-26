/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;
import org.terracotta.dynamic_config.system_tests.util.NodeOutputRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsLog;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2)
public class ActivateCommand2x2IT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @Test
  public void testMultiNodeSingleStripeActivation() {
    assertThat(
        configToolInvocation("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)),
        is(successful()));

    assertThat(activateCluster(), allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));
    waitUntil(out.getLog(1, findActive(1).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out.getLog(1, findPassives(1)[0]), containsLog("Moved to State[ PASSIVE-STANDBY ]"));
  }

  @Test
  public void testMultiNodeSingleStripeActivationWithConfigFile() {
    assertThat(
        configToolInvocation(
            "-r", timeout + "s",
            "activate",
            "-f", copyConfigProperty("/config-property-files/single-stripe_multi-node.properties").toString()),
        allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));

    waitUntil(out.getLog(1, findActive(1).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out.getLog(1, findPassives(1)[0]), containsLog("Moved to State[ PASSIVE-STANDBY ]"));
  }

  @Test
  public void testMultiStripeActivation() {
    assertThat(
        configToolInvocation("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1)),
        is(successful()));

    assertThat(activateCluster(), allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));
    waitUntil(out.getLog(1, findActive(1).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out.getLog(2, findActive(2).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testMultiStripeActivationWithConfigFile() {
    assertThat(
        configToolInvocation(
            "-r", timeout + "s",
            "activate",
            "-f", copyConfigProperty("/config-property-files/multi-stripe.properties").toString()),
        allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));

    waitUntil(out.getLog(1, findActive(1).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out.getLog(2, findActive(2).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
  }
}
