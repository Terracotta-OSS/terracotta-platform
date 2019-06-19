/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.DiagnosticServiceFactory;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.test.util.Kit;
import com.terracottatech.dynamic_config.test.util.NodeProcess;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static com.terracottatech.utilities.MemoryUnit.GB;
import static com.terracottatech.utilities.MemoryUnit.MB;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigServiceIT extends BaseStartupIT {

  @Before
  public void setUp() throws Exception {
    nodeProcesses.add(NodeProcess.startNode(Kit.getOrCreatePath(), "--config-file", configFilePath().toString()));
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

      DynamicConfigService proxy = diagnosticService.getProxy(DynamicConfigService.class);
      Cluster pendingTopology = proxy.getTopology();

      // keep for debug please
      //System.out.println(toPrettyJson(pendingTopology));

      assertThat(pendingTopology, is(equalTo(new Cluster(new Stripe(new Node()
          .setNodeName("node-1")
          .setClusterName("my-cluster")
          .setNodeHostname("localhost")
          .setNodePort(ports.getPort())
          .setNodeGroupPort(9430)
          .setNodeBindAddress("0.0.0.0")
          .setNodeGroupBindAddress("0.0.0.0")
          .setNodeConfigDir(Paths.get("build/config"))
          .setNodeMetadataDir(Paths.get("build/metadata"))
          .setNodeLogDir(Paths.get("build/logs"))
          .setNodeBackupDir(Paths.get("build/backup"))
          .setClientReconnectWindow(100, SECONDS)
          .setClientLeaseDuration(50, SECONDS)
          .setFailoverPriority("consistency:2")
          .setOffheapResource("main", 512, MB)
          .setOffheapResource("second", 1, GB)
          .setDataDir("main", Paths.get("/home/terracotta/user-data/main"))
          .setDataDir("second", Paths.get("/home/terracotta/user-data/second"))
      )))));
    }
  }

}
