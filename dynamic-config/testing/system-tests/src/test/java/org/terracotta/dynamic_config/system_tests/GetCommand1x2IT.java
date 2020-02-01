/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;
import org.terracotta.dynamic_config.system_tests.util.NodeProcess;

import static java.io.File.separator;
import static org.hamcrest.Matchers.containsString;

@ClusterDefinition(nodesPerStripe = 2)
public class GetCommand1x2IT extends DynamicConfigIT {

  @Override
  protected NodeProcess startNode(int stripeId, int nodeId, int port, int groupPort) {
    return startNode(stripeId, nodeId,
        "--node-name", "node-" + nodeId,
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(port),
        "--node-group-port", String.valueOf(groupPort),
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
        "--node-backup-dir", "backup/stripe" + stripeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + nodeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId,
        "--offheap-resources", "main:512MB,foo:1GB"
    );
  }

  @Before
  @Override
  public void before() throws Exception {
    super.before();
    ConfigTool.main("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2));
    assertCommandSuccessful();
  }

  @Test
  public void testStripe_getOneOffheap() {
    ConfigTool.main("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main");
    waitUntil(out::getLog, containsString("offheap-resources.main=512MB"));
  }

  @Test
  public void testStripe_getTwoOffheaps() {
    ConfigTool.main("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "offheap-resources.foo");
    waitUntil(out::getLog, containsString("offheap-resources.main=512MB"));
    waitUntil(out::getLog, containsString("offheap-resources.foo=1GB"));
  }

  @Test
  public void testStripe_getAllOffheaps() {
    ConfigTool.main("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources");
    waitUntil(out::getLog, containsString("offheap-resources=foo:1GB,main:512MB"));
  }

  @Test
  public void testStripe_getAllDataDirs() {
    ConfigTool.main("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs=main:user-data" + separator + "main" + separator + "stripe1"));
    waitUntil(out::getLog, containsString("stripe.1.node.2.data-dirs=main:user-data" + separator + "main" + separator + "stripe1"));
  }

  @Test
  public void testStripe_getAllNodeHostnames() {
    ConfigTool.main("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node-hostname");
    waitUntil(out::getLog, containsString("stripe.1.node.1.node-hostname=localhost"));
    waitUntil(out::getLog, containsString("stripe.1.node.2.node-hostname=localhost"));
  }
}
