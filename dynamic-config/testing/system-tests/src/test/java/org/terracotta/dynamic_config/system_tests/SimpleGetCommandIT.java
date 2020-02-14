/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;

import static java.io.File.separator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.hasExitStatus;

@ClusterDefinition
public class SimpleGetCommandIT extends DynamicConfigIT {
  @Test
  public void testNode_getOneOffheap_unknownOffheap() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.blah"),
        allOf(
            not(hasExitStatus(0)),
            containsOutput("No configuration found for: offheap-resources.blah")));
  }

  @Test
  public void testNode_getOneOffheap() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=512MB"));
  }

  @Test
  public void testNode_getTwoOffheaps() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "offheap-resources.foo"),
        allOf(
            containsOutput("offheap-resources.main=512MB"),
            containsOutput("offheap-resources.foo=1GB")));
  }

  @Test
  public void testNode_getAllOffheaps() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        containsOutput("offheap-resources=foo:1GB,main:512MB"));
  }

  @Test
  public void testNode_getAllDataDirs() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs"),
        containsOutput("stripe.1.node.1.data-dirs=main:terracotta1-1" + separator + "data-dir"));
  }

  @Test
  public void testNode_getClientReconnectWindow() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        containsOutput("client-reconnect-window=120s"));
  }

  @Test
  public void testNode_getSecurityAuthc() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "security-authc"),
        containsOutput("security-authc="));
  }

  @Test
  public void testNode_getNodePort() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-port"),
        containsOutput("stripe.1.node.1.node-port=" + getNodePort()));
  }
}
