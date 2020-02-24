/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;

import java.net.InetSocketAddress;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;

@ClusterDefinition
public class ActivateCommand1x1IT extends DynamicConfigIT {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void testSingleNodeActivation() {
    activateCluster(() -> {
      waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

      waitUntil(out::getLog, containsString("No license installed"));
      waitUntil(out::getLog, containsString("came back up"));
      waitUntil(out::getLog, containsString("Command successful"));
    });
  }

  @Test
  public void testSingleNodeActivationWithConfigFile() throws Exception {
    assertThat(
        configToolInvocation("activate", "-f", copyConfigProperty("/config-property-files/single-stripe.properties").toString(), "-n", "my-cluster"),
        allOf(
            containsOutput("No license installed"),
            containsOutput("came back up"),
            containsOutput("Command successful")));

    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

    // TDB-4726
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(InetSocketAddress.createUnresolved("localhost", getNodePort()), "diag", ofSeconds(10), ofSeconds(10), null)) {
      NodeContext runtimeNodeContext = diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext();
      assertThat(runtimeNodeContext.getCluster().getName(), is(equalTo("my-cluster")));
    }
  }
}
