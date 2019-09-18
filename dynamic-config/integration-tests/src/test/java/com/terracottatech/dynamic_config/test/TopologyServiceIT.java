/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.DiagnosticServiceFactory;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static com.terracottatech.dynamic_config.model.FailoverPriority.availability;
import static com.terracottatech.utilities.MemoryUnit.MB;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
        5, SECONDS,
        5, SECONDS,
        null)) {

      TopologyService proxy = diagnosticService.getProxy(TopologyService.class);
      Cluster pendingCluster = proxy.getCluster();

      // keep for debug please
      //System.out.println(toPrettyJson(pendingTopology));

      assertThat(pendingCluster, is(equalTo(new Cluster("tc-cluster", new Stripe(new Node()
          .setNodeName("node-1")
          .setNodeHostname("localhost")
          .setNodePort(ports.getPorts()[0])
          .setNodeGroupPort(ports.getPorts()[0] + 10)
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
