/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Before;
import org.junit.Test;

import static java.io.File.separator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;

@ClusterDefinition(nodesPerStripe = 2)
public class GetCommand1x2IT extends DynamicConfigIT {
  @Before
  @Override
  public void before() {
    super.before();
    assertThat(
        configToolInvocation("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)),
        containsOutput("Command successful"));
  }

  @Test
  public void testStripe_getOneOffheap() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=512MB"));
  }

  @Test
  public void testStripe_getTwoOffheaps() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "offheap-resources.foo"),
        allOf(containsOutput("offheap-resources.main=512MB"), containsOutput("offheap-resources.foo=1GB")));
  }

  @Test
  public void testStripe_getAllOffheaps() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        containsOutput("offheap-resources=foo:1GB,main:512MB"));
  }

  @Test
  public void testStripe_getAllDataDirs() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs"),
        allOf(
            containsOutput("stripe.1.node.1.data-dirs=main:terracotta1-1" + separator + "data-dir"),
            containsOutput("stripe.1.node.2.data-dirs=main:terracotta1-2" + separator + "data-dir")));
  }

  @Test
  public void testStripe_getAllNodeHostnames() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node-hostname"),
        allOf(
            containsOutput("stripe.1.node.1.node-hostname=localhost"),
            containsOutput("stripe.1.node.2.node-hostname=localhost")));
  }
}
