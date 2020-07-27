/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.io.File;

import static java.io.File.separator;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.testing.ExceptionMatcher.throwing;

@ClusterDefinition(autoActivate = true)
public class SetCommand1x1IT extends DynamicConfigIT {
  @Test
  public void setOffheapResource_decreaseSize() {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1MB"),
        exceptionMatcher("should be larger than the old size"));
  }

  @Test
  public void setOffheapResource_increaseSize() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=1GB"));
  }

  @Test
  public void setOffheapResource_addResource() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=second:1GB");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second"),
        containsOutput("offheap-resources.second=1GB"));
  }

  @Test
  public void setOffheapResources_addResources() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.third=1GB");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second", "-c", "offheap-resources.third"),
        allOf(containsOutput("offheap-resources.second=1GB"), containsOutput("offheap-resources.third=1GB")));
  }

  @Test
  public void setOffheapResources_addResources_sane_new_keys_and_lower_value() {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.foo=1GB", "-c", "offheap-resources.foo=500MB"),
        exceptionMatcher("Duplicate configurations found: offheap-resources.foo=1GB and offheap-resources.foo=500MB"));
  }

  @Test
  public void setOffheapResources_addResource_increaseSize() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=main:1GB,second:1GB");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        containsOutput("offheap-resources=foo:1GB,main:1GB,second:1GB"));
  }

  @Test
  public void setOffheapResources_newResource_decreaseSize() {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.main=1MB"),
        exceptionMatcher("should be larger than the old size"));
  }

  @Test
  public void setDataDir_updatePath() {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.main=user-data/main/stripe1-node1-data-dir"),
        exceptionMatcher("A data directory with name: main already exists"));
  }

  @Test
  public void setDataDir_overlappingPaths() throws Exception {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.first=node-1-1/data-dir"),
        is(throwing(instanceOf(RuntimeException.class)).andMessage(allOf(
            containsString("Prepare rejected"),
            containsString("Reason: 'set data-dirs.first=node-1-1/data-dir': Data directory: first overlaps with: main")))));

    // prepare change should be rolled back
    withTopologyService(1, 1, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_overLappingPaths() throws Exception {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-1"),
        is(throwing(instanceOf(RuntimeException.class)).andMessage(allOf(
            containsString("Prepare rejected"),
            containsString("Reason: 'set data-dirs.third=user-data/main/stripe1-node1-data-dir-1': Data directory: third overlaps with: second")))));

    // prepare change should be rolled back
    withTopologyService(1, 1, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_same_keys() {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.foo=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.foo=user-data/main/stripe1-node1-data-dir-2"),
        exceptionMatcher("Duplicate configurations found: data-dirs.foo=user-data/main/stripe1-node1-data-dir-1 and data-dirs.foo=user-data/main/stripe1-node1-data-dir-2"));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_overLappingPaths_flavor2() throws Exception {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-1"),
        is(throwing(instanceOf(RuntimeException.class)).andMessage(allOf(
            containsString("Prepare rejected"),
            containsString("Reason: 'set data-dirs.third=user-data/main/stripe1-node1-data-dir-1': Data directory: third overlaps with: second")))));

    // prepare change should be rolled back
    withTopologyService(1, 1, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
  }

  @Test
  public void setDataDir_addOneNonExistentDataDir() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second"),
        containsOutput("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-2");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second"),
        containsOutput("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.third"),
        containsOutput("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2"));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_flavor2() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-2");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second"),
        containsOutput("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.third"),
        containsOutput("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2"));
  }

  @Test
  public void setFailover_Priority_Consistency() {
    assertThat(
        invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2"),
        containsOutput("restart of the cluster is required"));

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority"),
        containsOutput("failover-priority=consistency:2"));
  }

  @Test
  public void setNodeLogDir() {
    assertThat(
        invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "log-dir=logs/stripe1"),
        containsOutput("restart of the cluster is required"));

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "log-dir"),
        containsOutput("stripe.1.node.1.log-dir=logs" + separator + "stripe1"));

    // Restart node and verify that the change has taken effect
    stopNode(1, 1);
    startNode(1, 1);

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-r", "-c", "log-dir"),
        containsOutput("stripe.1.node.1.log-dir=logs" + separator + "stripe1"));
  }

  @Test
  public void setNodeLogDir_pathAtDestinationIsAFile() throws Exception {
    File toUpload = new File(SetCommand1x1IT.class.getResource("/dummy.txt").toURI());
    angela.tsa().browse(getNode(1, 1), ".").upload(toUpload);
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "log-dir=dummy.txt"),
        exceptionMatcher("dummy.txt exists, but is not a directory"));
  }

  @Test
  public void setNodeBindAddress() {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.bind-address=127.0.0.1"),
        exceptionMatcher("Reason: Setting 'bind-address' cannot be set when node is activated"));
  }

  @Test
  public void setNodeName() {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.name=foo"),
        exceptionMatcher("Error: Invalid input: 'stripe.1.node.1.name=foo'. Reason: Setting 'name' cannot be set when node is activated"));
  }

  @Test
  public void setNodeGroupPort() {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.group-port=1024"),
        exceptionMatcher("Reason: Setting 'group-port' cannot be set when node is activated"));
  }

  @Test
  public void setNodeGroupBindAddress() {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.group-bind-address=127.0.0.1"),
        exceptionMatcher("Reason: Setting 'group-bind-address' cannot be set when node is activated"));
  }

  @Test
  public void testTcProperty() {
    assertThat(
        invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo=bar"),
        containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes"));

    assertThat(
        invokeConfigTool("get", "-r", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        not(containsOutput("tc-properties=foo:bar")));

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        containsOutput("tc-properties=foo:bar"));

    invokeConfigTool("unset", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        not(containsOutput("tc-properties=foo:bar")));
  }

  @Test
  public void testSetLogger() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides=org.terracotta:TRACE,com.tc:TRACE");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        containsOutput("logger-overrides=com.tc:TRACE,org.terracotta:TRACE"));

    invokeConfigTool("unset", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides.com.tc");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        containsOutput("logger-overrides=org.terracotta:TRACE"));
  }

  @Test
  public void change_cluster_name_back() throws Exception {
    // TDB-5067
    String clusterName = usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getCluster().getName());
    assertThat(
        invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "cluster-name=new-name"),
        not(containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes")));
    assertThat(invokeConfigTool("diagnostic", "-s", "localhost:" + getNodePort(1, 1)),
        allOf(containsOutput("Node restart required: NO"), containsOutput("Node last configuration change details: set cluster-name=new-name")));

    assertThat(invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "cluster-name=" + clusterName),
        not(containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes")));
    assertThat(invokeConfigTool("diagnostic", "-s", "localhost:" + getNodePort(1, 1)),
        allOf(containsOutput("Node restart required: NO"), containsOutput("Node last configuration change details: set cluster-name=" + clusterName)));
  }

  @Test
  public void setNodeMetadataDir() {
    // TDB-5092
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "metadata-dir=foo"),
        exceptionMatcher("Error: Invalid input: 'metadata-dir=foo'. Reason: Setting 'metadata-dir' cannot be set when node is activated"));
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.metadata-dir=foo"),
        exceptionMatcher("Error: Invalid input: 'stripe.1.metadata-dir=foo'. Reason: Setting 'metadata-dir' cannot be set when node is activated"));
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.metadata-dir=foo"),
        exceptionMatcher("Error: Invalid input: 'stripe.1.node.1.metadata-dir=foo'. Reason: Setting 'metadata-dir' cannot be set when node is activated"));
  }

  @Test
  public void testPublicHostPort() {
    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.public-hostname", "-c", "stripe.1.node.1.public-port"),
        allOf(containsOutput("stripe.1.node.1.public-hostname="), containsOutput("stripe.1.node.1.public-port=")));

    String publicHostname = "127.0.0.1";
    int publicPort = getNodePort();
    invokeConfigTool("set", "-s", "localhost:" + publicPort, "-c", "stripe.1.node.1.public-hostname=" + publicHostname, "-c", "stripe.1.node.1.public-port=" + publicPort);

    assertThat(
        invokeConfigTool("get", "-s", publicHostname + ":" + getNodePort(), "-c", "stripe.1.node.1.public-hostname", "-c", "stripe.1.node.1.public-port"),
        allOf(containsOutput("stripe.1.node.1.public-hostname=" + publicHostname), containsOutput("stripe.1.node.1.public-port=" + publicPort)));
  }
}
