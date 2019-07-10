/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.cli.ConfigTool;
import com.terracottatech.dynamic_config.test.util.Kit;
import com.terracottatech.dynamic_config.test.util.NodeProcess;
import com.terracottatech.store.manager.DatasetManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;

public class ActivateIT extends BaseStartupIT {
  @Rule
  public ExpectedSystemExit systemExit = ExpectedSystemExit.none();

  @Before
  public void setUp() {
    int[] ports = this.ports.getPorts();
    for (int i = 0; i < NODE_COUNT; i += 2) {
      int port = ports[i];
      int groupPort = ports[i + 1];
      nodeProcesses.add(NodeProcess.startNode(
          Kit.getOrCreatePath(),
          "--node-hostname", "localhost",
          "--node-port", String.valueOf(port),
          "--node-group-port", String.valueOf(groupPort),
          "--node-log-dir", "build/logs-" + port,
          "--node-backup-dir", "build/backup-" + port,
          "--node-metadata-dir", "build/metadata-" + port,
          "--node-config-dir", "build/config-" + port)
      );
    }

    waitedAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));
  }

  @Test
  public void testSingleStripeActivation() throws Exception {
    int[] ports = this.ports.getPorts();
    ConfigTool.main("attach", "-d", "127.0.0.1:" + ports[0], "-s", "127.0.0.1:" + ports[2]);
    waitedAssert(out::getLog, containsString("Command successful"));

    out.clearLog();
    ConfigTool.main("activate", "-s", "127.0.0.1:" + ports[0], "-n", "tc-cluster", "-l", licensePath().toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitedAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
    waitedAssert(out::getLog, containsString("Command successful"));

    Set<InetSocketAddress> node = Collections.singleton(InetSocketAddress.createUnresolved("127.0.0.1", ports[0]));
    try (DatasetManager ignored = DatasetManager.clustered(node).build()) {
      //Connection successful - we're good!
    }
  }

  @Test
  public void testMultiStripeActivation() throws Exception {
    int[] ports = this.ports.getPorts();
    ConfigTool.main("attach", "-t", "stripe", "-d", "127.0.0.1:" + ports[0], "-s", "127.0.0.1:" + ports[2]);
    waitedAssert(out::getLog, containsString("Command successful"));

    out.clearLog();
    ConfigTool.main("activate", "-s", "127.0.0.1:" + ports[0], "-n", "tc-cluster", "-l", licensePath().toString());
    waitedAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Moved to State[ ACTIVE-COORDINATOR ]", "Moved to State[ ACTIVE-COORDINATOR ]")
    ));
    waitedAssert(out::getLog, containsString("Command successful"));

    Set<InetSocketAddress> node = Collections.singleton(InetSocketAddress.createUnresolved("127.0.0.1", ports[0]));
    try (DatasetManager ignored = DatasetManager.clustered(node).build()) {
      //Connection successful - we're good!
    }
  }
}
