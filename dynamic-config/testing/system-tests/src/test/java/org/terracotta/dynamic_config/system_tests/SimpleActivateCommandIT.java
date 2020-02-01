/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;

import java.net.InetSocketAddress;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@ClusterDefinition
public class SimpleActivateCommandIT extends DynamicConfigIT {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void testWrongParams_1() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("Cluster name should be provided when node is specified"));
    ConfigTool.start("activate", "-s", "localhost:" + getNodePort());
  }

  @Test
  public void testWrongParams_2() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("Either node or config properties file should be specified, not both"));
    ConfigTool.start("activate", "-s", "localhost:" + getNodePort(), "-f", "dummy.properties");
  }

  @Test
  public void testWrongParams_4() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("One of node or config properties file must be specified"));
    ConfigTool.start("activate");
  }

  @Test
  public void testSingleNodeActivation() throws Exception {
    activateCluster(() -> {
      waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

      waitUntil(out::getLog, containsString("No license installed"));
      waitUntil(out::getLog, containsString("came back up"));
      waitUntil(out::getLog, containsString("Command successful"));
    });
  }

  @Test
  public void testSingleNodeActivationWithConfigFile() throws Exception {
    ConfigTool.start("activate", "-f", copyConfigProperty("/config-property-files/single-stripe.properties").toString(), "-n", "my-cluster");
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

    waitUntil(out::getLog, containsString("No license installed"));
    waitUntil(out::getLog, containsString("came back up"));
    waitUntil(out::getLog, containsString("Command successful"));

    // TDB-4726
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(InetSocketAddress.createUnresolved("localhost", getNodePort()), "diag", ofSeconds(10), ofSeconds(10), null)) {
      NodeContext runtimeNodeContext = diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext();
      assertThat(runtimeNodeContext.getCluster().getName(), is(equalTo("my-cluster")));
    }
  }
}
