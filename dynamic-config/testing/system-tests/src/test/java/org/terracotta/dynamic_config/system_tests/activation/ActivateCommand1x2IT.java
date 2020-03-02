/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.activation;

import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;
import org.terracotta.dynamic_config.system_tests.util.NodeOutputRule;

import java.net.InetSocketAddress;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsLog;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2)
public class ActivateCommand1x2IT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @Test
  public void testSingleNodeActivation() {
    assertThat(activateCluster(),
        allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));
    waitUntil(out.getLog(1, 1), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testMultiNodeSingleStripeActivation() {
    MatcherAssert.assertThat(
        configToolInvocation("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)),
        is(successful()));

    MatcherAssert.assertThat(activateCluster(), allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));
    waitUntil(out.getLog(1, findActive(1).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out.getLog(1, findPassives(1)[0]), containsLog("Moved to State[ PASSIVE-STANDBY ]"));
  }

  @Test
  public void testSingleNodeActivationWithConfigFile() throws Exception {
    assertThat(
        configToolInvocation("activate", "-f", copyConfigProperty("/config-property-files/single-stripe.properties").toString(), "-n", "my-cluster"),
        allOf(
            containsOutput("No license installed"),
            containsOutput("came back up"),
            is(successful())));

    waitUntil(out.getLog(1, 1), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));

    // TDB-4726
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(InetSocketAddress.createUnresolved("localhost", getNodePort()), "diag", ofSeconds(10), ofSeconds(10), null)) {
      NodeContext runtimeNodeContext = diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext();
      assertThat(runtimeNodeContext.getCluster().getName(), is(equalTo("my-cluster")));
    }
  }

  @Test
  public void testMultiNodeSingleStripeActivationWithConfigFile() {
    MatcherAssert.assertThat(
        configToolInvocation(
            "-r", timeout + "s",
            "activate",
            "-f", copyConfigProperty("/config-property-files/single-stripe_multi-node.properties").toString()),
        allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));

    waitUntil(out.getLog(1, findActive(1).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out.getLog(1, findPassives(1)[0]), containsLog("Moved to State[ PASSIVE-STANDBY ]"));
  }
}