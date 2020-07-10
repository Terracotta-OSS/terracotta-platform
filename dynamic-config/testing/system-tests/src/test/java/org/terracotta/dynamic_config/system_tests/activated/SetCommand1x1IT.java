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

import static java.io.File.separator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.hasExitStatus;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

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
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources.main=1GB")));
  }

  @Test
  public void setOffheapResource_addResource() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=second:1GB"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources.second=1GB")));
  }

  @Test
  public void setOffheapResources_addResources() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.third=1GB"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second", "-c", "offheap-resources.third"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources.second=1GB"), containsOutput("offheap-resources.third=1GB")));
  }

  @Test
  public void setOffheapResources_addResources_sane_new_keys_and_lower_value() throws Exception {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(),
            "-c", "offheap-resources.foo=1GB",
            "-c", "offheap-resources.foo=500MB"),
        allOf(
            not(hasExitStatus(0)),
            containsOutput("Duplicate configurations found: offheap-resources.foo=1GB and offheap-resources.foo=500MB")));
  }

  @Test
  public void setOffheapResources_addResource_increaseSize() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=main:1GB,second:1GB"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources=foo:1GB,main:1GB,second:1GB")));
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
  public void setDataDir_overlappingPaths() throws Exception {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.first=node-1-1/data-dir"),
        allOf(
            not(hasExitStatus(0)),
            containsOutput("Prepare rejected"),
            containsOutput("Reason: 'set data-dirs.first=node-1-1/data-dir': Data directory: first overlaps with: main")));

    // prepare change should be rolled back
    withTopologyService(1, 1, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_overLappingPaths() throws Exception {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(),
            "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1",
            "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-1"),
        allOf(
            not(hasExitStatus(0)),
            containsOutput("Prepare rejected"),
            containsOutput("Reason: 'set data-dirs.third=user-data/main/stripe1-node1-data-dir-1': Data directory: third overlaps with: second")));

    // prepare change should be rolled back
    withTopologyService(1, 1, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_same_keys() throws Exception {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(),
            "-c", "data-dirs.foo=user-data/main/stripe1-node1-data-dir-1",
            "-c", "data-dirs.foo=user-data/main/stripe1-node1-data-dir-2"),
        allOf(
            not(hasExitStatus(0)),
            containsOutput("Duplicate configurations found: data-dirs.foo=user-data/main/stripe1-node1-data-dir-1 and data-dirs.foo=user-data/main/stripe1-node1-data-dir-2")));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_overLappingPaths_flavor2() throws Exception {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-1"),
        allOf(
            not(hasExitStatus(0)),
            containsOutput("Prepare rejected"),
            containsOutput("Reason: 'set data-dirs.third=user-data/main/stripe1-node1-data-dir-1': Data directory: third overlaps with: second")));

    // prepare change should be rolled back
    withTopologyService(1, 1, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
  }

  @Test
  public void setDataDir_addOneNonExistentDataDir() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second"),
        containsOutput("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-2"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1")));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.third"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2")));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_flavor2() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-2"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1")));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.third"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2")));
  }

  @Test
  public void setFailover_Priority_Consistency() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2"),
        allOf(hasExitStatus(0), containsOutput("restart of the cluster is required")));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority"),
        allOf(hasExitStatus(0), containsOutput("failover-priority=consistency:2")));
  }

  @Test
  public void setNodeLogDir_postActivation() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "log-dir=logs/stripe1"),
        containsOutput("restart of the cluster is required"));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "log-dir"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.log-dir=logs" + separator + "stripe1")));
  }

  @Test
  public void setNodeBindAddress_postActivation() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "bind-address=127.0.0.1"),
        containsOutput("restart of the cluster is required"));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "bind-address"),
        containsOutput("stripe.1.node.1.bind-address=127.0.0.1"));
  }

  @Test
  public void setNodeGroupBindAddress_postActivation() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "group-bind-address=127.0.0.1"),
        allOf(hasExitStatus(0), containsOutput("restart of the cluster is required")));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "group-bind-address"),
        containsOutput("stripe.1.node.1.group-bind-address=127.0.0.1"));
  }

  @Test
  public void testTcProperty_postActivation() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo=bar"),
        allOf(hasExitStatus(0), containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes")));

    assertThat(configToolInvocation("get", "-r", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(hasExitStatus(0), not(containsOutput("tc-properties=foo:bar"))));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(hasExitStatus(0), containsOutput("tc-properties=foo:bar")));

    assertThat(configToolInvocation("unset", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(hasExitStatus(0), not(containsOutput("tc-properties=foo:bar"))));
  }

  @Test
  public void testSetLogger() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides=org.terracotta:TRACE,com.tc:TRACE"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        allOf(hasExitStatus(0), containsOutput("logger-overrides=com.tc:TRACE,org.terracotta:TRACE")));

    assertThat(configToolInvocation("unset", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides.com.tc"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        allOf(hasExitStatus(0), containsOutput("logger-overrides=org.terracotta:TRACE")));
  }

  @Test
  public void change_cluster_name_back() throws Exception {
    // TDB-5067
    String clusterName = usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getCluster().getName());
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "cluster-name=new-name"),
        allOf(is(successful()), containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes")));
    assertThat(configToolInvocation("diagnostic", "-s", "localhost:" + getNodePort(1, 1)),
        allOf(containsOutput("Node restart required: YES"), containsOutput("Node last configuration change details: set cluster-name=new-name")));
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "cluster-name=" + clusterName),
        allOf(is(successful()), not(containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes"))));
    assertThat(configToolInvocation("diagnostic", "-s", "localhost:" + getNodePort(1, 1)),
        allOf(containsOutput("Node restart required: NO"), containsOutput("Node last configuration change details: set cluster-name=" + clusterName)));
  }

  @Test
  public void metadata_dir_cannot_be_changed() throws Exception {
    // TDB-5092
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "metadata-dir=foo"),
        containsOutput("Reason: 'set metadata-dir=foo': Setting 'metadata-dir' cannot be changed once a node is activated"));
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.metadata-dir=foo"),
        containsOutput("Reason: 'set metadata-dir=foo (stripe ID: 1)': Setting 'metadata-dir' cannot be changed once a node is activated"));
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.metadata-dir=foo"),
        containsOutput("Reason: 'set metadata-dir=foo (stripe ID: 1, node: node-1-1)': Setting 'metadata-dir' cannot be changed once a node is activated"));
  }
}
