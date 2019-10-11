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

import java.util.Arrays;

import static java.io.File.separator;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;

public class GetCommandIT extends BaseStartupIT {
  public GetCommandIT() {
    super(2, 2);
  }

  @Rule
  public ExpectedSystemExit systemExit = ExpectedSystemExit.none();

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

    waitedAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));
  }

  /*<--Stripe-wide Tests-->*/
  @Test
  public void testStripe_getOneOffheap() {
    ConfigTool.main("attach", "-d", "localhost:" + ports.getPorts()[0], "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "offheap-resources.main");
    waitedAssert(out::getLog, containsString("offheap-resources.main=512MB"));
  }

  @Test
  public void testStripe_getTwoOffheaps() {
    ConfigTool.main("attach", "-d", "localhost:" + ports.getPorts()[0], "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "offheap-resources.main", "-c", "offheap-resources.foo");
    waitedAssert(out::getLog, containsString("offheap-resources.main=512MB"));
    waitedAssert(out::getLog, containsString("offheap-resources.foo=1GB"));
  }

  @Test
  public void testStripe_getAllOffheaps() {
    ConfigTool.main("attach", "-d", "localhost:" + ports.getPorts()[0], "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "offheap-resources");
    waitedAssert(out::getLog, containsString("offheap-resources=main:512MB,foo:1GB"));
  }

  @Test
  public void testStripe_getAllDataDirs() {
    ConfigTool.main("attach", "-d", "localhost:" + ports.getPorts()[0], "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "data-dirs");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs=main:user-data" + separator + "main" + separator + "stripe1"));
    waitedAssert(out::getLog, containsString("stripe.1.node.2.data-dirs=main:user-data" + separator + "main" + separator + "stripe1"));
  }

  @Test
  public void testStripe_getAllNodeHostnames() {
    ConfigTool.main("attach", "-d", "localhost:" + ports.getPorts()[0], "-s", "localhost:" + ports.getPorts()[1]);
    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "stripe.1.node-hostname");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.node-hostname=localhost"));
    waitedAssert(out::getLog, containsString("stripe.1.node.2.node-hostname=localhost"));
  }


  /*<--Cluster-wide Tests-->*/
  @Test
  public void testCluster_getOneOffheap() throws Exception {
    activate2x2Cluster();

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "offheap-resources.main");
    waitedAssert(out::getLog, containsString("offheap-resources.main=512MB"));
  }

  @Test
  public void testCluster_getTwoOffheaps() throws Exception {
    activate2x2Cluster();

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "offheap-resources.main", "-c", "offheap-resources.foo");
    waitedAssert(out::getLog, containsString("offheap-resources.main=512MB"));
    waitedAssert(out::getLog, containsString("offheap-resources.foo=1GB"));
  }

  @Test
  public void testCluster_getAllOffheaps() throws Exception {
    activate2x2Cluster();

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "offheap-resources");
    waitedAssert(out::getLog, containsString("offheap-resources=main:512MB,foo:1GB"));
  }

  @Test
  public void testCluster_getAllDataDirs() throws Exception {
    activate2x2Cluster();

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "data-dirs");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs=main:user-data" + separator + "main" + separator + "stripe1"));
    waitedAssert(out::getLog, containsString("stripe.1.node.2.data-dirs=main:user-data" + separator + "main" + separator + "stripe1"));
    waitedAssert(out::getLog, containsString("stripe.2.node.1.data-dirs=main:user-data" + separator + "main" + separator + "stripe2"));
    waitedAssert(out::getLog, containsString("stripe.2.node.2.data-dirs=main:user-data" + separator + "main" + separator + "stripe2"));
  }

  @Test
  public void testCluster_getNodeName() throws Exception {
    activate2x2Cluster();

    ConfigTool.main("get", "-s", "localhost:" + ports.getPorts()[0], "-c", "node-name");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.node-name=node-1"));
    waitedAssert(out::getLog, containsString("stripe.1.node.2.node-name=node-2"));
    waitedAssert(out::getLog, containsString("stripe.2.node.1.node-name=node-1"));
    waitedAssert(out::getLog, containsString("stripe.2.node.2.node-name=node-2"));
  }

  private void activate2x2Cluster() throws Exception {
    int[] ports = this.ports.getPorts();
    ConfigTool.main("attach", "-d", "localhost:" + ports[0], "-s", "localhost:" + ports[1]);
    ConfigTool.main("attach", "-t", "stripe", "-d", "localhost:" + ports[0], "-s", "localhost:" + ports[2], "localhost:" + ports[3]);
    ConfigTool.main("activate", "-s", "localhost:" + ports[0], "-n", "tc-cluster", "-l", licensePath().toString());
  }
}
