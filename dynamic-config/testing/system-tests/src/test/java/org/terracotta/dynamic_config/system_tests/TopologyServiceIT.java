/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.TopologyService;

import java.nio.file.Paths;
import java.time.Duration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.common.struct.MemoryUnit.MB;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;

/**
 * @author Mathieu Carbou
 */
public class TopologyServiceIT extends BaseStartupIT {
  @Before
  public void setUp() throws Exception {
    startNode(
        "--node-repository-dir", "repository/stripe1/node-1",
        "-N", "tc-cluster",
        "-f", copyConfigProperty("/config-property-files/single-stripe.properties").toString()
    );
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void test_getPendingTopology() throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        getServerAddress(),
        getClass().getSimpleName(),
        Duration.ofSeconds(5),
        Duration.ofSeconds(5),
        null)
    ) {

      TopologyService proxy = diagnosticService.getProxy(TopologyService.class);
      Cluster pendingCluster = proxy.getUpcomingNodeContext().getCluster();

      // keep for debug please
      //System.out.println(toPrettyJson(pendingTopology));

      assertThat(pendingCluster, is(equalTo(new Cluster("tc-cluster", new Stripe(Node.newDefaultNode("node-1", "localhost", ports.getPort())
          .setNodeGroupPort(ports.getPort() + 10)
          .setNodeBindAddress("0.0.0.0")
          .setNodeGroupBindAddress("0.0.0.0")
          .setNodeMetadataDir(Paths.get("metadata", "stripe1"))
          .setNodeLogDir(Paths.get("logs", "stripe1", "node-1"))
          .setNodeBackupDir(Paths.get("backup", "stripe1"))
          .setClientReconnectWindow(120, SECONDS)
          .setClientLeaseDuration(20, SECONDS)
          .setFailoverPriority(availability())
          .setOffheapResource("main", 512, MB)
          .setDataDir("main", Paths.get("user-data", "main", "stripe1"))
      )))));
    }
  }

}
