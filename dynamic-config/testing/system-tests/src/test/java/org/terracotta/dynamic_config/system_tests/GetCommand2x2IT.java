/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;
import org.terracotta.dynamic_config.system_tests.util.NodeProcess;

import static java.io.File.separator;
import static org.hamcrest.Matchers.containsString;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2, autoActivate = true)
public class GetCommand2x2IT extends DynamicConfigIT {

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

  @Test
  public void testCluster_getOneOffheap() throws Exception {
    ConfigTool.main("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main");
    waitUntil(out::getLog, containsString("offheap-resources.main=512MB"));
  }

  @Test
  public void testCluster_getTwoOffheaps() throws Exception {
    ConfigTool.main("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "offheap-resources.foo");
    waitUntil(out::getLog, containsString("offheap-resources.main=512MB"));
    waitUntil(out::getLog, containsString("offheap-resources.foo=1GB"));
  }

  @Test
  public void testCluster_getAllOffheaps() throws Exception {
    ConfigTool.main("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources");
    waitUntil(out::getLog, containsString("offheap-resources=foo:1GB,main:512MB"));
  }

  @Test
  public void testCluster_getAllDataDirs() throws Exception {
    ConfigTool.main("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs=main:user-data" + separator + "main" + separator + "stripe1"));
    waitUntil(out::getLog, containsString("stripe.1.node.2.data-dirs=main:user-data" + separator + "main" + separator + "stripe1"));
    waitUntil(out::getLog, containsString("stripe.2.node.1.data-dirs=main:user-data" + separator + "main" + separator + "stripe2"));
    waitUntil(out::getLog, containsString("stripe.2.node.2.data-dirs=main:user-data" + separator + "main" + separator + "stripe2"));
  }

  @Test
  public void testCluster_getNodeName() throws Exception {
    ConfigTool.main("get", "-s", "localhost:" + getNodePort(), "-c", "node-name");
    waitUntil(out::getLog, containsString("stripe.1.node.1.node-name=node-1"));
    waitUntil(out::getLog, containsString("stripe.1.node.2.node-name=node-2"));
    waitUntil(out::getLog, containsString("stripe.2.node.1.node-name=node-1"));
    waitUntil(out::getLog, containsString("stripe.2.node.2.node-name=node-2"));
  }
}
