/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.system_tests;

import com.beust.jcommander.ParameterException;
import com.terracottatech.dynamic_config.cli.config_tool.ConfigTool;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.io.File.separator;
import static org.hamcrest.Matchers.containsString;

public class SimpleGetCommandIT extends BaseStartupIT {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    forEachNode((stripeId, nodeId, port) -> startNode(
        "--offheap-resources", "main:512MB,foo:1GB",
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

  @Test
  public void testNode_getOneOffheap_unknownOffheap() {
    exception.expect(ParameterException.class);
    exception.expectMessage(containsString("No configuration found for: offheap-resources.blah"));
    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.blah");
  }

  @Test
  public void testNode_getOneOffheap() {
    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main");
    waitedAssert(out::getLog, containsString("offheap-resources.main=512MB"));
  }

  @Test
  public void testNode_getTwoOffheaps() {
    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main", "-c", "offheap-resources.foo");
    waitedAssert(out::getLog, containsString("offheap-resources.main=512MB"));
    waitedAssert(out::getLog, containsString("offheap-resources.foo=1GB"));
  }

  @Test
  public void testNode_getAllOffheaps() {
    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources");
    waitedAssert(out::getLog, containsString("offheap-resources=main:512MB,foo:1GB"));
  }

  @Test
  public void testNode_getAllDataDirs() {
    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.data-dirs");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs=main:user-data" + separator + "main" + separator + "stripe1"));
  }

  @Test
  public void testNode_getClientReconnectWindow() {
    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "client-reconnect-window");
    waitedAssert(out::getLog, containsString("client-reconnect-window=120s"));
  }

  @Test
  public void testNode_getSecurityAuthc() {
    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "security-authc");
    waitedAssert(out::getLog, containsString("security-authc="));
  }

  @Test
  public void testNode_getNodePort() {
    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.node-port");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.node-port=" + ports.getPort()));
  }
}
