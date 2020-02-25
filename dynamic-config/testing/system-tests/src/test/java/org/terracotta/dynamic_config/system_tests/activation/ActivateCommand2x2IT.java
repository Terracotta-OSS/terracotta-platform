/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2)
public class ActivateCommand2x2IT extends DynamicConfigIT {

  @Rule public final SystemOutRule out = new SystemOutRule().enableLog();

  @Test
  public void testMultiNodeSingleStripeActivation() {
    assertThat(
        configToolInvocation("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)),
        is(successful()));

    assertThat(activateCluster(), allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
  }

  @Test
  public void testMultiNodeSingleStripeActivationWithConfigFile() {
    assertThat(
        configToolInvocation(
            "-r", timeout + "s",
            "activate",
            "-f", copyConfigProperty("/config-property-files/single-stripe_multi-node.properties").toString()),
        allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));

    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
  }

  @Test
  public void testMultiStripeActivation() {
    assertThat(
        configToolInvocation("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1)),
        is(successful()));

    assertThat(activateCluster(), allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));
    waitUntil(out::getLog, stringContainsInOrder(asList("Moved to State[ ACTIVE-COORDINATOR ]", "Moved to State[ ACTIVE-COORDINATOR ]")));
  }

  @Test
  public void testMultiStripeActivationWithConfigFile() {
    assertThat(
        configToolInvocation(
            "-r", timeout + "s",
            "activate",
            "-f", copyConfigProperty("/config-property-files/multi-stripe.properties").toString()),
        allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));

    waitUntil(out::getLog, stringContainsInOrder(asList("Moved to State[ ACTIVE-COORDINATOR ]", "Moved to State[ ACTIVE-COORDINATOR ]")));
  }
}
