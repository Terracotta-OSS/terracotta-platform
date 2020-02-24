/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;

import static java.io.File.separator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2, autoActivate = true)
public class GetCommand2x2IT extends DynamicConfigIT {
  @Test
  public void testCluster_getOneOffheap() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=512MB"));
  }

  @Test
  public void testCluster_getTwoOffheaps() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "offheap-resources.foo"),
        allOf(
            containsOutput("offheap-resources.main=512MB"),
            containsOutput("offheap-resources.foo=1GB")));
  }

  @Test
  public void testCluster_getAllOffheaps() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        containsOutput("offheap-resources=foo:1GB,main:512MB"));
  }

  @Test
  public void testCluster_getAllDataDirs() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs"),
        allOf(
            containsOutput("stripe.1.node.1.data-dirs=main:terracotta1-1" + separator + "data-dir"),
            containsOutput("stripe.1.node.2.data-dirs=main:terracotta1-2" + separator + "data-dir"),
            containsOutput("stripe.2.node.1.data-dirs=main:terracotta2-1" + separator + "data-dir"),
            containsOutput("stripe.2.node.2.data-dirs=main:terracotta2-2" + separator + "data-dir")));
  }

  @Test
  public void testCluster_getNodeName() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-name"),
        allOf(
            containsOutput("stripe.1.node.1.node-name=node1-1"),
            containsOutput("stripe.1.node.2.node-name=node1-2"),
            containsOutput("stripe.2.node.1.node-name=node2-1"),
            containsOutput("stripe.2.node.2.node-name=node2-2")));
  }
}
