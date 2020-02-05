/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;

import static java.io.File.separator;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@ClusterDefinition(autoActivate = true)
public class SimpleSetCommandActivatedIT extends DynamicConfigIT {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void setOffheapResource_postActivation_decreaseSize() throws Exception {
    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("should be larger than the old size"));
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1MB");
  }

  @Test
  public void setOffheapResource_postActivation_increaseSize() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main");
    waitUntil(out::getLog, containsString("offheap-resources.main=1GB"));
  }

  @Test
  public void setOffheapResource_postActivation_addResource() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=second:1GB");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second");
    waitUntil(out::getLog, containsString("offheap-resources.second=1GB"));
  }

  @Test
  public void setOffheapResources_postActivation_addResources() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.third=1GB");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second", "-c", "offheap-resources.third");
    waitUntil(out::getLog, containsString("offheap-resources.second=1GB"));
    waitUntil(out::getLog, containsString("offheap-resources.third=1GB"));
  }

  @Test
  public void setOffheapResources_postActivation_addResource_increaseSize() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=main:1GB,second:1GB");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources");
    waitUntil(out::getLog, containsString("offheap-resources=main:1GB,second:1GB"));
  }

  @Test
  public void setOffheapResources_postActivation_newResource_decreaseSize() throws Exception {
    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("should be larger than the old size"));
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.main=1MB");
  }

  @Test
  public void setDataDir_postActivation_updatePath() throws Exception {
    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("A data directory with name: main already exists"));
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.main=user-data/main/stripe1-node1-data-dir");
  }

  @Test
  public void setDataDir_postActivation_overlappingPaths() throws Exception {
    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("overlaps with the existing data directory"));
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.first=user-data/main/stripe1/node1");
  }

  @Test
  public void setDataDir_postActivation_addMultipleNonExistentDataDirs_overLappingPaths() throws Exception {
    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("overlaps with the existing data directory"));
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-1");
  }

  @Test
  public void setDataDir_postActivation_addMultipleNonExistentDataDirs_overLappingPaths_flavor2() throws Exception {
    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("overlaps with the existing data directory"));
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-1");
  }

  @Test
  public void setDataDir_postActivation_addOneNonExistentDataDir() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));
  }

  @Test
  public void setDataDir_postActivation_addMultipleNonExistentDataDirs() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-2");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));

    out.clearLog();
    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.third");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2"));
  }

  @Test
  public void setDataDir_postActivation_addMultipleNonExistentDataDirs_flavor2() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-2");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));

    out.clearLog();
    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.third");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2"));
  }

  @Test
  public void setFailover_Priority_postActivation_Consistency() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2");
    waitUntil(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority");
    waitUntil(out::getLog, containsString("failover-priority=consistency:2"));
  }

  @Test
  public void setNodeLogDir_postActivation() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "node-log-dir=logs/stripe1");
    waitUntil(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "node-log-dir");
    waitUntil(out::getLog, containsString("stripe.1.node.1.node-log-dir=logs" + separator + "stripe1"));
  }

  @Test
  public void setNodeBindAddress_postActivation() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "node-bind-address=127.0.0.1");
    waitUntil(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "node-bind-address");
    waitUntil(out::getLog, containsString("stripe.1.node.1.node-bind-address=127.0.0.1"));
  }

  @Test
  public void setNodeGroupBindAddress_postActivation() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "node-group-bind-address=127.0.0.1");
    waitUntil(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "node-group-bind-address");
    waitUntil(out::getLog, containsString("stripe.1.node.1.node-group-bind-address=127.0.0.1"));
  }

  @Test
  public void testTcProperty_postActivation() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo=bar");
    waitUntil(out::getLog, containsString("IMPORTANT: A restart of the cluster is required to apply the changes"));
    assertCommandSuccessful();

    ConfigTool.start("get", "-r", "-s", "localhost:" + getNodePort(), "-c", "tc-properties");
    waitUntil(out::getLog, not(containsString("tc-properties=foo:bar")));

    out.clearLog();
    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties");
    waitUntil(out::getLog, containsString("tc-properties=foo:bar"));

    out.clearLog();
    ConfigTool.start("unset", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties");
    waitUntil(out::getLog, not(containsString("tc-properties=foo:bar")));
  }

  @Test
  public void testSetLogger() throws Exception {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-logger-overrides=org.terracotta:TRACE,com.tc:TRACE");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "node-logger-overrides");
    waitUntil(out::getLog, containsString("node-logger-overrides=com.tc:TRACE,org.terracotta:TRACE"));

    ConfigTool.start("unset", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-logger-overrides.com.tc");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + getNodePort(), "-c", "node-logger-overrides");
    waitUntil(out::getLog, containsString("node-logger-overrides=org.terracotta:TRACE"));
  }
}