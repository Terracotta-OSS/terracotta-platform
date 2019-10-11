/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.cli.ConfigTool;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import static java.io.File.separator;
import static org.hamcrest.Matchers.containsString;

public class SimpleSetCommandIT extends BaseStartupIT {
  @Rule
  public ExpectedSystemExit systemExit = ExpectedSystemExit.none();

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

    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  /*<--Single Node Tests-->*/
  @Test
  public void testNode_set_stripeIdInvalid() {
    systemExit.expectSystemExit();
    systemExit.checkAssertionAfterwards(() -> waitedAssert(out::getLog, containsString("Specified stripe id: 2, but cluster contains: 1 stripe(s) only")));
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "stripe.2.node.1.node-backup-dir=foo");
  }

  @Test
  public void testNode_set_nodeIdInvalid() {
    systemExit.expectSystemExit();
    systemExit.checkAssertionAfterwards(() -> waitedAssert(out::getLog, containsString("Error: Invalid input: 'stripe.1.node.2.node-backup-dir=foo'. Reason: Specified node id: 2, but stripe 1 contains: 1 node(s) only")));
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "stripe.1.node.2.node-backup-dir=foo");
  }

  @Test
  public void testNode_setOffheapResource() {
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "offheap-resources.main=512MB");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "offheap-resources.main");
    waitedAssert(out::getLog, containsString("offheap-resources.main=512MB"));
  }

  @Test
  public void testNode_setTcProperties() {
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "stripe.1.node.1.tc-properties.something=value");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "stripe.1.node.1.tc-properties.something");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.tc-properties.something=value"));
  }

  @Test
  public void testNode_setClientReconnectWindow() {
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "client-reconnect-window=10s");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "client-reconnect-window");
    waitedAssert(out::getLog, containsString("client-reconnect-window=10s"));
  }

  @Test
  public void testNode_setSecurityAuthc() {
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "security-dir=/path/to/security/dir", "-c", "security-authc=file");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "security-authc");
    waitedAssert(out::getLog, containsString("security-authc=file"));
  }

  @Test
  public void testNode_setNodeGroupPort() {
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "stripe.1.node.1.node-group-port=9630");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "stripe.1.node.1.node-group-port");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.node-group-port=9630"));
  }

  @Test
  public void testNode_setSecurityWhitelist() {
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "security-dir=/path/to/security/dir", "-c", "security-whitelist=true");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "security-whitelist");
    waitedAssert(out::getLog, containsString("security-whitelist=true"));
  }

  @Test
  public void testNode_setDataDir() {
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "stripe.1.node.1.data-dirs.main=user-data/main/stripe1-node1-data-dir");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "stripe.1.node.1.data-dirs.main");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs.main=user-data" + separator + "main" + separator + "stripe1-node1-data-dir"));
  }

  @Test
  public void testNode_setNodeBackupDir() {
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "stripe.1.node.1.node-backup-dir=backup/stripe1-node1-backup");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "stripe.1.node.1.node-backup-dir");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.node-backup-dir=backup" + separator + "stripe1-node1-backup"));
  }

  @Test
  public void testNode_setTwoProperties() {
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "offheap-resources.main=1GB", "-c", "stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir");
    waitedAssert(out::getLog, containsString("Command successful"));

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "offheap-resources.main", "-c", "stripe.1.node.1.data-dirs.main");
    waitedAssert(out::getLog, containsString("offheap-resources.main=1GB"));
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir"));
  }
}
