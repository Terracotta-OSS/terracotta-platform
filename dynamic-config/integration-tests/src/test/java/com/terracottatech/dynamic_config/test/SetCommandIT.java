/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.cli.ConfigTool;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;

public class SetCommandIT extends BaseStartupIT {
  public SetCommandIT() {
    super(2, 1);
  }

  @Before
  public void setUp() {
    forEachNode((stripeId, nodeId, port) -> startNode(
        "--node-name", "node-" + nodeId,
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(port),
        "--node-group-port", String.valueOf(port + 10),
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
        "--node-backup-dir", "backup/stripe" + stripeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + nodeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId));

    waitedAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));
  }

  /*<--Stripe-wide Tests-->*/
  @Test
  public void testStripe_level_setDataDirectory() {
    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.data-dirs.main=stripe1-node1-data-dir");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs=main:stripe1-node1-data-dir"));
    waitedAssert(out::getLog, containsString("stripe.1.node.2.data-dirs=main:stripe1-node1-data-dir"));
  }

  @Test
  public void testStripe_level_setBackupDirectory() {
    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node-backup-dir=backup"+File.separator+"stripe-1");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "node-backup-dir");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.node-backup-dir=backup"+File.separator+"stripe-1"));
    waitedAssert(out::getLog, containsString("stripe.1.node.2.node-backup-dir=backup"+File.separator+"stripe-1"));
  }


  /*<--Cluster-wide Tests-->*/
  @Test
  public void testCluster_setOffheap() {
    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main=1GB");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources");
    waitedAssert(out::getLog, containsString("offheap-resources=main:1GB"));
  }

  @Test
  public void testCluster_setBackupDirectory() {
    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "node-backup-dir=backup"+File.separator+"data");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "node-backup-dir");
    waitedAssert(out::getLog, containsString("node-backup-dir=backup" + File.separator + "data"));
  }

  @Test
  public void testCluster_setClientLeaseTime() {
    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "client-lease-duration=10s");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "client-lease-duration");
    waitedAssert(out::getLog, containsString("client-lease-duration=10s"));
  }

  @Test
  public void testCluster_setFailoverPriorityAvailability() {
    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "failover-priority=availability");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "failover-priority");
    waitedAssert(out::getLog, containsString("failover-priority=availability"));
  }

  @Test
  public void testCluster_setFailoverPriorityConsistency() {
    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "failover-priority=consistency:2");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "failover-priority");
    waitedAssert(out::getLog, containsString("failover-priority=consistency:2"));
  }

  @Test
  public void testCluster_setClientReconnectWindow() {
    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "client-reconnect-window=10s");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "client-reconnect-window");
    waitedAssert(out::getLog, containsString("client-reconnect-window=10s"));
  }

  @Test
  public void testCluster_setClientReconnectWindow_postActivation() throws Exception {
    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[1]);
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "client-reconnect-window=10s");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "client-reconnect-window");
    waitedAssert(out::getLog, containsString("client-reconnect-window=10s"));
  }

  private void activateCluster() throws Exception {
    ConfigTool.start("activate", "-s", "localhost:" + ports.getPort(), "-n", "tc-cluster", "-l", licensePath().toString());
    out.clearLog();
  }
}
