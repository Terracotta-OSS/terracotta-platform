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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.hasExitStatus;

@ClusterDefinition(autoActivate = true)
public class SetCommand1x1IT extends DynamicConfigIT {
  @Test
  public void setOffheapResource_decreaseSize() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1MB"),
        allOf(not(hasExitStatus(0)), containsOutput("should be larger than the old size")));
  }

  @Test
  public void setOffheapResource_increaseSize() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main");
    waitUntil(out::getLog, containsString("offheap-resources.main=1GB"));
  }

  @Test
  public void setOffheapResource_addResource() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=second:1GB");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second");
    waitUntil(out::getLog, containsString("offheap-resources.second=1GB"));
  }

  @Test
  public void setOffheapResources_addResources() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.third=1GB");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second", "-c", "offheap-resources.third");
    waitUntil(out::getLog, containsString("offheap-resources.second=1GB"));
    waitUntil(out::getLog, containsString("offheap-resources.third=1GB"));
  }

  @Test
  public void setOffheapResources_addResource_increaseSize() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=main:1GB,second:1GB");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources");
    waitUntil(out::getLog, containsString("offheap-resources=foo:1GB,main:1GB,second:1GB"));
  }

  @Test
  public void setOffheapResources_newResource_decreaseSize() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.main=1MB"),
        allOf(not(hasExitStatus(0)), containsOutput("should be larger than the old size")));
  }

  @Test
  public void setDataDir_updatePath() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.main=user-data/main/stripe1-node1-data-dir"),
        allOf(not(hasExitStatus(0)), containsOutput("A data directory with name: main already exists")));
  }

  @Test
  public void setDataDir_overlappingPaths() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.first=terracotta1-1/data-dir"),
        allOf(not(hasExitStatus(0)), containsOutput("overlaps with the existing data directory")));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_overLappingPaths() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-1"),
        allOf(not(hasExitStatus(0)), containsOutput("overlaps with the existing data directory")));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_overLappingPaths_flavor2() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-1"),
        allOf(not(hasExitStatus(0)), containsOutput("overlaps with the existing data directory")));
  }

  @Test
  public void setDataDir_addOneNonExistentDataDir() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-2");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));

    out.clearLog();
    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.third");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2"));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_flavor2() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-2");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));

    out.clearLog();
    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.third");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2"));
  }

  @Test
  public void setFailover_Priority_Consistency() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2");
    waitUntil(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority");
    waitUntil(out::getLog, containsString("failover-priority=consistency:2"));
  }

  @Test
  public void setNodeLogDir_postActivation() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "node-log-dir=logs/stripe1");
    waitUntil(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-log-dir");
    waitUntil(out::getLog, containsString("stripe.1.node.1.node-log-dir=logs" + separator + "stripe1"));
  }

  @Test
  public void setNodeBindAddress_postActivation() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "node-bind-address=127.0.0.1");
    waitUntil(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-bind-address");
    waitUntil(out::getLog, containsString("stripe.1.node.1.node-bind-address=127.0.0.1"));
  }

  @Test
  public void setNodeGroupBindAddress_postActivation() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "node-group-bind-address=127.0.0.1");
    waitUntil(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-group-bind-address");
    waitUntil(out::getLog, containsString("stripe.1.node.1.node-group-bind-address=127.0.0.1"));
  }

  @Test
  public void testTcProperty_postActivation() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo=bar");
    waitUntil(out::getLog, containsString("IMPORTANT: A restart of the cluster is required to apply the changes"));
    assertCommandSuccessful();

    configToolInvocation("get", "-r", "-s", "localhost:" + getNodePort(), "-c", "tc-properties");
    waitUntil(out::getLog, not(containsString("tc-properties=foo:bar")));

    out.clearLog();
    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties");
    waitUntil(out::getLog, containsString("tc-properties=foo:bar"));

    out.clearLog();
    configToolInvocation("unset", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties");
    waitUntil(out::getLog, not(containsString("tc-properties=foo:bar")));
  }

  @Test
  public void testSetLogger() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-logger-overrides=org.terracotta:TRACE,com.tc:TRACE");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-logger-overrides");
    waitUntil(out::getLog, containsString("node-logger-overrides=com.tc:TRACE,org.terracotta:TRACE"));

    configToolInvocation("unset", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-logger-overrides.com.tc");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-logger-overrides");
    waitUntil(out::getLog, containsString("node-logger-overrides=org.terracotta:TRACE"));
  }
}
